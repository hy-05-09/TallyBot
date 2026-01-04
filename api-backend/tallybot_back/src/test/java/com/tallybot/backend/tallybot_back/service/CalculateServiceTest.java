package com.tallybot.backend.tallybot_back.service;

import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.dto.*;
import com.tallybot.backend.tallybot_back.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tallybot.backend.tallybot_back.exception.NoSettlementResultException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("mock-data") // 이 프로파일 조합으로 별도 컨텍스트 생성
class CalculateServiceTest {


    @InjectMocks
    private CalculateService calculateService;

    @Mock private GroupRepository groupRepository;
    @Mock private CalculateRepository calculateRepository;
    @Mock private ChatRepository chatRepository;
    @Mock private GPTService gptService;
    @Mock private CalculateDetailRepository calculateDetailRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private SettlementService settlementService;
    @Mock private ParticipantRepository participantRepository;


    @Test
    void groupExists_true() {
        when(groupRepository.existsById(1L)).thenReturn(true);
        boolean result = calculateService.groupExists(1L);
        assertThat(result).isTrue();
    }

    @Test
    void groupExists_false() {
        when(groupRepository.existsById(999L)).thenReturn(false);
        boolean result = calculateService.groupExists(999L);
        assertThat(result).isFalse();
    }


    @Test
    void startCalculate_shouldProcessSuccessfully_whenGptReturnsResults() {
        // given
        Long groupId = 1L;
        CalculateRequestDto request = new CalculateRequestDto();
        request.setGroupId(groupId);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now());

        UserGroup mockGroup = UserGroup.create(1L, "치킨모임");
        Calculate savedCalculate = new Calculate();
        Calculate saved = calculateRepository.save(savedCalculate);
        Long fakeCalculateId = saved.getCalculateId();


        List<Chat> chats = List.of(createChat("A", "샘플 대화"));

        List<ChatForGptDto> chatDtos = chats.stream()
                .map(chat -> new ChatForGptDto(
                        chat.getChatId(),
                        chat.getMember().getMemberId(),
                        chat.getMember().getNickname(),
                        chat.getMessage(),
                        chat.getTimestamp()
                ))
                .toList();  
         
        
            
        SettlementDto dummyDto = new SettlementDto(); // 내용은 필요시 설정
        List<SettlementDto> gptResults = List.of(dummyDto);

        Participant dummyParticipant = new Participant();
        Participant.ParticipantKey participantKey = new Participant.ParticipantKey();
        dummyParticipant.setParticipantKey(participantKey);

        Settlement dummySettlement = new Settlement();
        dummySettlement.addParticipant(dummyParticipant);

        List<Settlement> settlements = List.of(dummySettlement);

        // mocking
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
        when(calculateRepository.save(any(Calculate.class))).thenReturn(savedCalculate);
        when(chatRepository.findByUserGroupAndTimestampBetween(any(), any(), any())).thenReturn(chats);
        when(gptService.returnResults(groupId, chatDtos)).thenReturn(gptResults);
        when(settlementService.toSettlements(gptResults, fakeCalculateId)).thenReturn(settlements);
        when(settlementRepository.save(any(Settlement.class))).thenReturn(dummySettlement);

        // when
        Long returnedId = calculateService.startCalculate(request);

        // then: 비동기 작업이 모두 끝날 때까지 기다림
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(settlementRepository, times(1)).save(any(Settlement.class));
            verify(participantRepository, times(1)).save(any(Participant.class));
        });

        assertEquals(fakeCalculateId, returnedId);
    }


    @Test
    void startCalculate_shouldDeleteCalculate_whenNoSettlementResult() {
        // given
        Long groupId = 1L;
        CalculateRequestDto request = new CalculateRequestDto();
        request.setGroupId(groupId);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now());

        UserGroup mockGroup = UserGroup.create(1L, "치킨모임");
        Calculate savedCalculate = new Calculate();
        Calculate saved = calculateRepository.save(savedCalculate);
        Long fakeCalculateId = saved.getCalculateId();

        List<Chat> chats = List.of(createChat("A", "샘플 대화"));

        List<ChatForGptDto> chatDtos = chats.stream()
                .map(chat -> new ChatForGptDto(
                        chat.getChatId(),
                        chat.getMember().getMemberId(),
                        chat.getMember().getNickname(),
                        chat.getMessage(),
                        chat.getTimestamp()
                ))
                .toList();

        // mocking
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));
        when(calculateRepository.save(any(Calculate.class))).thenReturn(savedCalculate);
        when(chatRepository.findByUserGroupAndTimestampBetween(any(), any(), any())).thenReturn(chats);
        when(gptService.returnResults(groupId, chatDtos)).thenThrow(new NoSettlementResultException("정산 결과 없음"));

        // when
        Long returnedId = calculateService.startCalculate(request);

        // then: 비동기 작업이 deleteById 호출할 때까지 기다림
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(calculateRepository, times(1)).deleteById(fakeCalculateId);
        });

        assertEquals(fakeCalculateId, returnedId);
    }

    private Chat createChat(String nickname, String message) {
        Member member = Member.builder()
                .nickname(nickname)
                .build();

        Chat chat = Chat.builder()
                .member(member)
                .message(message)
                .build();
        return chat;
    }


    @Test
    @DisplayName("recalculate(): 기존 Settlement로 재정산 처리")
    void recalculate_success() {
        // given
        Calculate calculate = new Calculate();
        Calculate saved = calculateRepository.save(calculate);
        Long calculateId = saved.getCalculateId();

        Member payer = Member.builder()
                .build();
        Member payee = Member.builder()
                .build();

        Settlement settlement = Settlement.create(
                UserGroup.create(1L, "치킨모임");,
                payer,
                calculate,
                "",
                "",
                10000
        );

        Participant.ParticipantKey pk = new Participant.ParticipantKey(settlement, payee);
        Participant participant = new Participant(pk, 0, new Ratio(1, 1));
        settlement.addParticipant(participant);

        // 👉 calculateRepository는 2번 호출되므로 둘 다 처리
        when(calculateRepository.findByCalculateId(calculateId)).thenReturn(Optional.of(calculate));
        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));
        when(settlementRepository.findByCalculate(calculate)).thenReturn(List.of(settlement));

        // when
        calculateService.recalculate(calculateId);

        // then
        verify(calculateDetailRepository).saveAll(any());
    }


    @Test
    @DisplayName("calculateAndOptimize(): 정산 → 최적화 → 저장까지 정상 수행")
    void calculateAndOptimize_success() {
        // given
        UserGroup userGroup = new UserGroup();
        Member m1 = Member.builder()
                .userGroup(userGroup)
                .build();
        Member m2 = Member.builder()
                .userGroup(userGroup)
                .build();

        Calculate calculate = Calculate.builder()
            .userGroup(userGroup)
            .build();

        // Calculate saved = calculateRepository.save(calculate);
        // Long calculateId = saved.getCalculateId();

        // 정산 1건
        Settlement s = Settlement.create(
                userGroup,
                m1,
                calculate,
                "",
                "",
                10000
        );

        Participant.ParticipantKey pk = new Participant.ParticipantKey(s, m2);
        Participant participant = new Participant(pk, 0, new Ratio(1, 1));
        s.addParticipant(participant);

        // when
        calculateService.calculateAndOptimize(List.of(s));

        // then
        verify(calculateDetailRepository).saveAll(any());
    }



    @Test
    @DisplayName("botResultReturn(): 정산 결과 DTO 정상 반환")
    void botResultReturn_success() {
        // given
        UserGroup userGroup = new UserGroup();
        userGroup.setGroupId(42L);

        Calculate calculate = Calculate.builder()
            .userGroup(userGroup)
            .build();
        Calculate saved = calculateRepository.save(calculate);
        Long calculateId = saved.getCalculateId();

        Member payer1 = Member.builder()
                .build();
        Member payee1 = Member.builder()
                .build();

        Member payer2 = Member.builder()
                .build();
        Member payee2 = Member.builder()
                .build();

        CalculateDetail detail1 = CalculateDetail.builder()
            .calculate(calculate)
            .payer(payer1)
            .payee(payee1)
            .amount(12000)
            .build();

        CalculateDetail detail2 = CalculateDetail.builder()
            .calculate(calculate)
            .payer(payer2)
            .payee(payee2)
            .amount(8000)
            .build();

        when(calculateDetailRepository.findAllByCalculate(calculate))
                .thenReturn(List.of(detail1, detail2));

        // when
        BotResponseDto result = calculateService.botResultReturn(calculate);

        // then
        assertThat(result.getGroupUrl()).isEqualTo("https://tallybot.me/42");
        assertThat(result.getCalculateUrl()).isEqualTo("https://tallybot.me/42/"+calculateId);

        List<TransferDto> transfers = result.getTransfers();
        assertThat(transfers).hasSize(2);
        assertThat(transfers.get(0).getPayerId()).isEqualTo(payer1.getMemberId());
        assertThat(transfers.get(0).getPayeeId()).isEqualTo(payee1.getMemberId());
        assertThat(transfers.get(0).getAmount()).isEqualTo(12000);
    }

}
