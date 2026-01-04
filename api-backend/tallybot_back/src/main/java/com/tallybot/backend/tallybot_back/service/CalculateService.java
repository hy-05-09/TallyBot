package com.tallybot.backend.tallybot_back.service;

import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.dto.*;
import com.tallybot.backend.tallybot_back.exception.NoSettlementResultException;
import com.tallybot.backend.tallybot_back.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class CalculateService {

    private final GroupRepository groupRepository;
    private final CalculateRepository calculateRepository;
    private final ChatRepository chatRepository;
    private final GPTService gptService;
    private final CalculateDetailRepository calculateDetailRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementService settlementService;
    private final ParticipantRepository participantRepository;
    private final OptimizationService optimizationService;

    private static final Logger logger = LoggerFactory.getLogger(CalculateService.class);

    public boolean groupExists(Long groupId) {
        return groupRepository.existsById(groupId);
    }

    /*
     * group ID와 시작 시간, 종료 시간을 담은 query를 받아
     * 해당 시간대 Chat을 받아와 Settlement를 생성한다.
     * 그 후, 각 Settlement에서 정산해야 할 금액을 산정하여,
     * 최적화 후 Calculate ID를 반환한다.
     * 이 때, 대화 분석이 GPTService를 통해 이루어지며,
     * 각자의 정산 몫 분배와 최적화는 백에서 이루어진다.
     */
    @Transactional
    public Long startCalculate(CalculateRequestDto request) {

        logger.info("정산 시작 정상 동작 확인 로그입니다.");
        logger.info("정산 시작 요청 데이터: groupId={}, startTime={}, endTime={}",
                request.getGroupId(), request.getStartTime(), request.getEndTime());

        UserGroup userGroup = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        Calculate calculate = Calculate.builder()
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .status(CalculateStatus.CALCULATING)
            .userGroup(userGroup)
            .build();

        calculate = calculateRepository.save(calculate);
        Long calculateId = calculate.getCalculateId();

        int year = request.getStartTime().getYear();
        int month = request.getStartTime().getMonthValue();
        int day = request.getStartTime().getDayOfMonth();

        // endTime에서 연, 월, 일 추출
        int endYear = request.getEndTime().getYear();
        int endMonth = request.getEndTime().getMonthValue();
        int endDay = request.getEndTime().getDayOfMonth();

        LocalDateTime startTime = LocalDateTime.of(year, month, day, 0, 0, 0, 0);  // 2025년 6월 3일 00:00
        LocalDateTime endTime = LocalDateTime.of(endYear, endMonth, endDay, 23, 59, 59, 999999999);
        List<Chat> chats = chatRepository.findByUserGroupAndTimestampBetween(
                userGroup,
                startTime,   // startTime 사용
                endTime      // endTime 사용
        );

        logger.info("calculate에서 조회된 채팅 수: {}", chats.size());


        // 나머지 GPT 처리 로직은 비동기로 실행
        Long finalCalculateId = calculateId; // 비동기에서 접근 가능하도록 final 변수로 복사



        List<ChatForGptDto> chatDtos = chats.stream()
                .map(chat -> new ChatForGptDto(
                        chat.getChatId(),
                        chat.getMember().getMemberId(),
                        chat.getMember().getNickname(),
                        chat.getMessage(),
                        chat.getTimestamp()
                ))
                .toList();

        logger.info("calculate 채팅 수: {}", chatDtos.size());

        CompletableFuture.runAsync(() -> {
            try {
                List<SettlementDto> results = gptService.returnResults(request.getGroupId(), chatDtos);

                List<Settlement> settlements = settlementService.toSettlements(results, finalCalculateId);


                for (Settlement settlement : settlements) {
                    Settlement savedSettlement = settlementRepository.save(settlement);
                    for (Participant participant : settlement.getParticipants()) {
                        participant.getParticipantKey().setSettlement(savedSettlement);
                        participantRepository.save(participant);
                    }
                }

                calculateAndOptimize(settlements);
                pendingCalculate(calculateId);

            } catch (NoSettlementResultException ex) {
                calculateRepository.deleteById(finalCalculateId);
                logger.error("정산 결과 없음 - calculateId={}", finalCalculateId, ex);
                return;
            } catch (Exception ex) {
                calculateRepository.deleteById(finalCalculateId);
                logger.error("GPT 처리 중 오류 발생 - calculate 삭제됨: ", finalCalculateId, ex);
                return;
            }
        });


        return calculateId;
    }




    public void calculateAndOptimize(List<Settlement> settlementList) {
        optimizationService.calculateAndOptimize(settlementList);
    }


    @Transactional
    public void recalculate(Long calculateId) {
        Calculate calculate = calculateRepository.findById(calculateId)
                .orElseThrow(() -> new IllegalArgumentException("계산 ID 존재하지 않음"));

        // 1. 기존 CalculateDetail 삭제
        calculateDetailRepository.deleteByCalculate(calculate);

        // 2. Settlement는 유지하고, 내부 계산만 다시 진행
        List<Settlement> settlementList = settlementRepository.findByCalculateWithParticipants(calculate);


        // 3. Participants의 ratio & constant 정보를 바탕으로 계산 수행
        calculateAndOptimize(settlementList);  // 내부적으로 그래프 재생성 포함

        // 4. 상태 초기화
        calculate.changeStatus(CalculateStatus.PENDING);
        calculateRepository.save(calculate);
    }






    /*
     * 정산 계산 완료를 표시하여 저장한다.
     */
    public void pendingCalculate(Long calculateId) {
        Calculate calculate = calculateRepository.findById(calculateId)
                .orElseThrow(() -> new IllegalArgumentException("정산 없음"));

        calculate.changeStatus(CalculateStatus.PENDING);
        calculateRepository.save(calculate);
    }

    /*
     * 각자의 정산 몫을 산정한다.
     */
    public List<CalculateDetail> calculateShare(List<Settlement> sm) {
        Map<Pair<Member, Member>, Integer> m = new HashMap<>();


        for (Settlement s : sm) {
            // 미리 고정 금액으로 정산하는 금액을 뺀다.
            int amount = s.getAmount();
            for(Participant pc: s.getParticipants()) {
                amount -= pc.getConstant();
            }

            // 남은 금액을 각 비율로 나눈다.
            for (Participant pc : s.getParticipants()) {
                Pair<Member, Member> p = Pair.of(s.getPayer(), pc.getParticipantKey().getMember());

                int shareAmount = pc.getConstant()
                        + (int) Math.round((double) amount * pc.getRatio().getNumerator() / pc.getRatio().getDenominator());

                m.put(p, m.getOrDefault(p, 0) + shareAmount);
            }

        }

        Calculate calculate = sm.get(0).getCalculate();

        List<CalculateDetail> lcd = new ArrayList<>();
        for(Pair<Member, Member> mem2Mem: m.keySet()) {
            lcd.add(new CalculateDetail(null, calculate, mem2Mem.getFirst(), mem2Mem.getSecond(), m.get(mem2Mem)));
        }

        return lcd;
    }


    public BotResponseDto botResultReturn(Calculate calculate) {
        Long groupId = calculate.getUserGroup().getGroupId();
        Long calculateId = calculate.getCalculateId();

        String groupUrl = "https://tallybot.vercel.app/" + groupId;
        String calculateUrl = groupUrl + "/settlements/" + calculateId;

        // 실제 정산 결과 리스트 생성
        List<TransferDto> transfers = calculateDetailRepository.findAllByCalculate(calculate)
                .stream()
                .map(detail -> new TransferDto(
                        detail.getPayer().getMemberId(),
                        detail.getPayee().getMemberId(),
                        detail.getAmount()
                ))
                .collect(Collectors.toList());

        return new BotResponseDto(groupUrl, calculateUrl, transfers);
    }


}





