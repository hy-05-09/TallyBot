package com.tallybot.backend.tallybot_back.service;

//import com.tallybot.backend.tallybot_back.debtopt.Graph;
// import com.tallybot.backend.tallybot_back.debtopt.Graph;
import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.dto.*;
import com.tallybot.backend.tallybot_back.exception.NoSettlementResultException;
import com.tallybot.backend.tallybot_back.repository.*;
// import com.tallybot.backend.tallybot_back.util.DateUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
// import org.h2.value.Transfer;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
import java.util.List;

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
//     private final MemberRepository memberRepository;

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

        logger.info("🍀정산 시작 정상 동작 확인 로그입니다.");
//        try {
//
//            // LocalDateTime -> String 변환 후 처리
//            String startTimeStr = request.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
//            String endTimeStr = request.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
//
//            // DateUtil.parseDate() 사용 (String -> LocalDateTime 변환)
//            LocalDateTime startDate = DateUtil.parseDate(startTimeStr);
//            LocalDateTime endDate = DateUtil.parseDate(endTimeStr);
//
//
//            // 처리된 날짜로 계산 시작 로직 수행
//            logger.info("🍀 정산 시작 요청: groupId={}, startTime={}, endTime={}",
//                    request.getGroupId(), startDate, endDate);
//
//        } catch (Exception e) {
//            logger.error("❌ 잘못된 입력: {}", e.getMessage());
//        }
        logger.info("🍀 정산 시작 요청 데이터: groupId={}, startTime={}, endTime={}",
                request.getGroupId(), request.getStartTime(), request.getEndTime());

        UserGroup userGroup = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        Calculate calculate = new Calculate();
        calculate.setUserGroup(userGroup);
        calculate.setStartTime(request.getStartTime());
        calculate.setEndTime(request.getEndTime());
        calculate.setStatus(CalculateStatus.CALCULATING);
        calculate = calculateRepository.save(calculate);
        Long calculateId = calculate.getCalculateId();

//        List<Chat> chats = chatRepository.findByUserGroupAndTimestampBetween(
//                request.getGroupId(),
//                request.getStartTime(),
//                request.getEndTime()
//        );
//        List<Chat> chats = chatRepository.findByGroupIdAndTimestampBetween(
//                request.getGroupId(),  // `groupId` 사용
//                request.getStartTime(),
//                request.getEndTime()
//        );

//        List<Chat> chats = chatRepository.findByUserGroup_GroupIdAndTimestampBetween(
//                request.getGroupId(),  // groupId 대신 userGroup.id 사용
//                request.getStartTime(),
//                request.getEndTime()
//        );

        //그냥 아이디로 조회 -> 성공 확인함
//        List<Chat> chats = chatRepository.findByUserGroup_GroupId(
//                request.getGroupId()  // groupId만 사용하여 조회
//        );

        // mock 데이터로 startTime과 endTime 설정 -> 성공
//        LocalDateTime startTime = LocalDateTime.of(2025, 6, 3, 0, 0, 0, 0);  // 2025년 6월 3일 00:00
//        LocalDateTime endTime = LocalDateTime.of(2025, 6, 4, 23, 59, 59, 999999999);  // 2025년 6월 4일 24:00
//
//        List<Chat> chats = chatRepository.findByUserGroup_GroupIdAndTimestampBetween(
//                request.getGroupId(),
//                startTime,  // mock startTime
//                endTime     // mock endTime
//        );

        // request에서 받은 startTime과 endTime을 LocalDateTime으로 할당 -> 실패
//        LocalDateTime startTime = request.getStartTime(); // request.getStartTime()은 이미 LocalDateTime일 가능성 있음
//        LocalDateTime endTime = request.getEndTime();     // request.getEndTime()도 동일
//
//        // 이제 startTime과 endTime을 쿼리에서 사용
//        List<Chat> chats = chatRepository.findByUserGroup_GroupIdAndTimestampBetween(
//                request.getGroupId(),
//                startTime,   // startTime 사용
//                endTime      // endTime 사용
//        );

        //최종 버전!
        // int year = request.getStartTime().getYear();
        int month = request.getStartTime().getMonthValue();
        int day = request.getStartTime().getDayOfMonth();

        // endTime에서 연, 월, 일 추출
        // int endYear = request.getEndTime().getYear();
        int endMonth = request.getEndTime().getMonthValue();
        int endDay = request.getEndTime().getDayOfMonth();

        // startTime을 00:00:00으로 설정
//        LocalDateTime startTime = LocalDateTime.of(year, month, day, 0, 0, 0, 0);  // 2025년 6월 3일 00:00

        // endTime을 23:59:59.999999999으로 설정
//        LocalDateTime endTime = LocalDateTime.of(endYear, endMonth, endDay, 23, 59, 59, 999999999);

        LocalDateTime startTime = LocalDateTime.of(2025, month, day, 0, 0, 0, 0);  // 2025년 6월 3일 00:00
        LocalDateTime endTime = LocalDateTime.of(2025, endMonth, endDay, 23, 59, 59, 999999999);
        List<Chat> chats = chatRepository.findByUserGroup_GroupIdAndTimestampBetween(
                request.getGroupId(),
                startTime,   // startTime 사용
                endTime      // endTime 사용
        );

//         Chat 객체 생성 시 필요한 UserGroup 및 Member 객체를 생성하여 전달해야 합니다.
//        UserGroup userGroup1 = groupRepository.findById(request.getGroupId())
//                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
//
//        Member member9 = memberRepository.findById(9L).orElseThrow(() -> new IllegalArgumentException("Member not found"));
//        Member member10 = memberRepository.findById(10L).orElseThrow(() -> new IllegalArgumentException("Member not found"));
//        Member member11 = memberRepository.findById(11L).orElseThrow(() -> new IllegalArgumentException("Member not found"));
//        Member member12 = memberRepository.findById(12L).orElseThrow(() -> new IllegalArgumentException("Member not found"));
//
//        List<Chat> chats = new ArrayList<>();
//
//        // 예시로 setter를 사용하여 설정
//
//        chats.add(new Chat(1L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 0, 0), member9, "오늘 재밌었다!!!"));
//        chats.add(new Chat(2L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 1, 0), member9, "삼겹살 진짜 맛있었당"));
//        chats.add(new Chat(3L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 2, 0), member10, "그니까 조심히 들어가~~"));
//        chats.add(new Chat(4L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 3, 0), member11, "카페도 커피랑 케잌 다 맛있더라"));
//        chats.add(new Chat(5L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 4, 0), member9, "그니까! 삼겹살 내가 이따 정산할게"));
//        chats.add(new Chat(6L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 5, 0), member12, "카페는 정산 잠시만..."));
//        chats.add(new Chat(7L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 6, 0), member12, "카페 2만 3천원"));
//        chats.add(new Chat(8L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 7, 0), member11, "어 생각보다 별로 안 나왔네"));
//        chats.add(new Chat(9L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 8, 0), member12, "어어 이다빈 빼고 1/3씩 보내줘"));
//        chats.add(new Chat(10L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 9, 0), member9, "삼겹살 총 8만 천원!"));
//        chats.add(new Chat(11L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 10, 0), member9, "내가 2만 천원 낸 걸로 하고 2만원씩 보내줘!"));
//        chats.add(new Chat(12L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 11, 0), member11, "멘토님 선물은 2만 2천인데"));
//        chats.add(new Chat(13L, userGroup, LocalDateTime.of(2025, 6, 3, 18, 12, 0), member11, "내가 만원 상품권으로 냈고 나머지는 n분의 1 하자!"));

        logger.info("🍀 calculate에서 조회된 채팅 수: {}", chats.size());


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

        logger.info("🍀 calculate 채팅 수: {}", chatDtos.size());

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
                System.err.println("정산 결과 없음 - calculate 삭제됨: " + ex.getMessage());
                return;
            } catch (Exception ex) {
                calculateRepository.deleteById(finalCalculateId);
                System.err.println("GPT 처리 중 오류 발생 - calculate 삭제됨: " + ex.getMessage());
//                ex.printStackTrace();
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
//        List<Settlement> settlementList = settlementRepository.findByCalculate(calculate);
        List<Settlement> settlementList = settlementRepository.findByCalculateWithParticipants(calculate);


        // 3. Participants의 ratio & constant 정보를 바탕으로 계산 수행
        calculateAndOptimize(settlementList);  // 내부적으로 그래프 재생성 포함

        // 4. 상태 초기화
        calculate.setStatus(CalculateStatus.PENDING);
        calculateRepository.save(calculate);
    }






    /*
     * 정산 계산 완료를 표시하여 저장한다.
     */
    public void pendingCalculate(Long calculateId) {
        Calculate calculate = calculateRepository.findById(calculateId)
                .orElseThrow(() -> new IllegalArgumentException("정산 없음"));

        calculate.setStatus(CalculateStatus.PENDING);
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
        //     final int finalAmount = amount;
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

    /*
     * 그래프 간소화를 이용하여 산정된 정산 몫을 최적화한다.
     */
//    public List<CalculateDetail> optimize(List<CalculateDetail> lcd) {
//        // 그래프 형태로 만든다.
//        Set<Member> members = new HashSet<>();
//        Calculate calculate = lcd.get(0).getCalculate();
//        for(CalculateDetail cd: lcd) {
//            members.add(cd.getPayer());
//            members.add(cd.getPayee());
//        }
//
//        // index compression
//        List<Member> memberList = members.stream().toList();
//        Graph graph = new Graph(memberList.size());
//        for(CalculateDetail cd: lcd) {
//            int payerNum = memberList.indexOf(cd.getPayer());
//            int payeeNum = memberList.indexOf(cd.getPayee());
//            graph.addEdge(payerNum, payeeNum, cd.getAmount());
//        }
//
//        // 그래프 간소화를 실현하되, 그것이 오히려 간선을 늘리는 경우 원복한다.
//        Graph graph2 = Graph.summarize(graph);
//        if(graph2.getEdgeCount() < graph.getEdgeCount()) {
//            graph = graph2;
//        }
//
//        // 그래프 형태를 되돌린다.
//        List<CalculateDetail> lcd2 = new ArrayList<>();
//        for(int i = 0; i < graph.getVertexCount(); i++) {
//            for(Integer j: graph.getAdjacencyList().get(i).keySet()) {
//                if(graph.getAdjacencyList().get(i).get(j) < 0) continue;
//                lcd2.add(new CalculateDetail(null, calculate, memberList.get(i), memberList.get(j),
//                        graph.getAdjacencyList().get(i).get(j)));
//            }
//        }
//
//        return lcd2;
//    }


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

    /*
     * Calculate ID에 따라 정산 계산된 payer-payee 간
     * 금전 거래 관계를 CalculateDetail울 Response 형태로 반환한다.
     */
//    public ResponseDetailDto resultReturn(Long calculateId) {
//        Calculate calculate = calculateRepository.findById(calculateId)
//                .orElseThrow(() -> new IllegalArgumentException("정산 없음"));
//
//        List<CalculateDetail> details = calculateDetailRepository.findByCalculate(calculate);
//
//        List<ResponseDetail> detailResponses = details.stream().map(detail ->
//                new ResponseDetail(
//                        detail.getPayer().getNickname(),
//                        detail.getPayee().getNickname(),
//                        detail.getAmount()
//                )
//        ).toList();
//
//        String url = "https://tallybot.me/calculate/" + calculateId;
//
//        return new ResponseDetailDto(url, detailResponses);
//    }
//
//    /*
//     * Calculate ID에 따라 정산 계산된 각 item을 Response 형태로 반환한다.
//     */
//    public ResponseBriefSettlementDto resultBriefSettlementReturn(Long calculateId) {
//        Calculate calculate = calculateRepository.findById(calculateId)
//                .orElseThrow(() -> new IllegalArgumentException("정산 없음"));
//
//        List<Settlement> settlements = settlementRepository.findByCalculate(calculate);
//
//        List<ResponseBriefSettlement> settlementResponses = settlements.stream().map(settlement -> {
//            List<String> nicks = settlementService.nicknamesInCalculate(settlements);
//            return new ResponseBriefSettlement(
//                    settlement.getSettlementId(),
//                    settlement.getPlace(),
//                    settlement.getPayer().getNickname(),
//                    nicks.size(),
//                    settlement.getAmount(),
//                    nicks
//            );
//        }).toList();
//
//        String url = "https://tallybot.me/settlement/brief/" + calculateId;
//
//        return new ResponseBriefSettlementDto(url, settlementResponses);
//    }
//
//    /*
//     * Calculate ID에 따라 각자의 정산 비율 등을 포함한 결제의 자세한 정보를 Response 형태로 반환한다.
//     */
//    public ResponseSettlementDto resultSettlementReturn(Long calculateId) {
//        Calculate calculate = calculateRepository.findById(calculateId)
//                .orElseThrow(() -> new IllegalArgumentException("정산 없음"));
//
//        List<Settlement> settlements = settlementRepository.findByCalculate(calculate);
//
//        List<SettlementDto> settlementDtoResponses = settlements.stream().map(settlement -> {
//            int denom = settlement.getParticipants().stream().map(Participant::getRatio).map(Ratio::getDenominator).reduce(1,
//                    (a, b) -> a * b / Ratio.gcd(a, b));
//            return new SettlementDto(
//                    settlement.getPlace(),
//                    settlement.getPayer().getNickname(),
//                    settlement.getItem(),
//                    settlement.getAmount(),
//                    settlement.getParticipants().stream().map(participant -> {
//                        return participant.getParticipantKey().member.getNickname();
//                    }).toList(),
//                    settlement.getParticipants().stream().collect(Collectors.toMap(
//                            participant -> participant.getParticipantKey().member.getNickname(),
//                            Participant::getConstant)),
//                    settlement.getParticipants().stream().collect(Collectors.toMap(
//                            participant -> ((Participant) participant).getParticipantKey().member.getNickname(),
//                            participant -> Ratio.mul(((Participant) participant).getRatio(), new Ratio(denom, 1)).toInt()))
//            );
//        }).toList();
//
//        String url = "https://tallybot.me/settlement/detail" + calculateId;
//
//        return new ResponseSettlementDto(url, settlementDtoResponses);
//    }
//
//    @Transactional
//    public void recalculate(Long calculateId) {
//        Calculate calculate = calculateRepository.findById(calculateId)
//                .orElseThrow(() -> new IllegalArgumentException("정산 없음"));
//
//        // 기존 결과 삭제
//        calculateDetailRepository.deleteByCalculate(calculate);
//
//        // 재분석 없이 프론트에서 전달된 수정 사항만 settlement에 업데이트 (예: 정산 대상자, 금액 등)
//
//        // 웹에서 수정 시 item: value, amount: value 형식으로 들어오니 GPT 추가로 돌리지 않고
//        // 내부 함수만 돌리는 코드 작성 필요
//
//        // 상태는 그대로 유지 (PENDING or COMPLETED)
//    }


}





