package com.tallybot.backend.tallybot_back.service;

import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.dto.*;
import com.tallybot.backend.tallybot_back.exception.NoSettlementResultException;
import com.tallybot.backend.tallybot_back.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("mock-data")
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
    @Mock private OptimizationService optimizationService; 


    @Test
    @DisplayName("startCalculate(): GPT 정상 결과 → settlement 저장/participant 저장/최적화/pending")
    void startCalculate_shouldProcessSuccessfully_whenGptReturnsResults() {
        // given
        Long groupId = 1L;
        CalculateRequestDto request = new CalculateRequestDto();
        request.setGroupId(groupId);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now());

        UserGroup mockGroup = UserGroup.create(groupId, "치킨모임");

        // calculateRepository.save() 호출 시 calculateId를 부여해서 반환
        when(calculateRepository.save(any(Calculate.class))).thenAnswer(inv -> {
            Calculate c = inv.getArgument(0);
            if (getFieldValue(c, "calculateId") == null) {
                setFieldValue(c, "calculateId", 100L);
            }
            return c;
        });

        // pendingCalculate()에서 findById 필요
        when(calculateRepository.findById(100L)).thenAnswer(inv -> {
            Calculate c = new Calculate();
            setFieldValue(c, "calculateId", 100L);
            return Optional.of(c);
        });

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));

        List<Chat> chats = List.of(createChat("A", "샘플 대화"));
        when(chatRepository.findByUserGroupAndTimestampBetween(any(), any(), any())).thenReturn(chats);

        List<SettlementDto> gptResults = List.of(new SettlementDto());
        when(gptService.returnResults(eq(groupId), anyList())).thenReturn(gptResults);

        // settlement & participant 준비
        Member payee = chats.get(0).getMember();
        Settlement dummySettlement = mock(Settlement.class);

        Participant.ParticipantKey pk = new Participant.ParticipantKey(null, payee);
        Participant dummyParticipant = new Participant(pk, 0, new Ratio(1, 1));

        // getParticipants()는 Set일 가능성이 높아서 Set으로 반환
        when(dummySettlement.getParticipants()).thenReturn(Set.of(dummyParticipant));

        when(settlementService.toSettlements(eq(gptResults), eq(100L))).thenReturn(List.of(dummySettlement));

        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Long returnedId = calculateService.startCalculate(request);

        // then (비동기 완료 대기)
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(settlementRepository, times(1)).save(any(Settlement.class));
            verify(participantRepository, times(1)).save(any(Participant.class));
            verify(optimizationService, times(1)).calculateAndOptimize(anyList());
            verify(calculateRepository, atLeastOnce()).save(any(Calculate.class)); // pending에서 save
        });

        assertEquals(100L, returnedId);
    }

    @Test
    @DisplayName("startCalculate(): 정산 결과 없음 → calculate 삭제")
    void startCalculate_shouldDeleteCalculate_whenNoSettlementResult() {
        // given
        Long groupId = 1L;
        CalculateRequestDto request = new CalculateRequestDto();
        request.setGroupId(groupId);
        request.setStartTime(LocalDateTime.now().minusDays(1));
        request.setEndTime(LocalDateTime.now());

        UserGroup mockGroup = UserGroup.create(groupId, "치킨모임");

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(mockGroup));

        when(calculateRepository.save(any(Calculate.class))).thenAnswer(inv -> {
            Calculate c = inv.getArgument(0);
            setFieldValue(c, "calculateId", 200L);
            return c;
        });

        List<Chat> chats = List.of(createChat("A", "샘플 대화"));
        when(chatRepository.findByUserGroupAndTimestampBetween(any(), any(), any())).thenReturn(chats);

        when(gptService.returnResults(eq(groupId), anyList()))
                .thenThrow(new NoSettlementResultException("정산 결과 없음"));

        // when
        Long returnedId = calculateService.startCalculate(request);

        // then
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(calculateRepository, times(1)).deleteById(200L);
            verify(settlementRepository, never()).save(any());
            verify(participantRepository, never()).save(any());
            verify(optimizationService, never()).calculateAndOptimize(anyList());
        });

        assertEquals(200L, returnedId);
    }

    private Chat createChat(String nickname, String message) {
        Member member = Member.builder()
                .nickname(nickname)
                .build();

        Chat chat = Chat.builder()
                .member(member)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return chat;
    }

    @Test
    @DisplayName("recalculate(): 기존 Settlement 기반 재정산 처리")
    void recalculate_success() {
        // given
        Long calculateId = 300L;

        Calculate calculate = new Calculate();
        setFieldValue(calculate, "calculateId", calculateId);

        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));

        Settlement settlement = mock(Settlement.class);

        when(settlementRepository.findByCalculateWithParticipants(calculate)).thenReturn(List.of(settlement));

        // when
        calculateService.recalculate(calculateId);

        // then
        verify(calculateDetailRepository, times(1)).deleteByCalculate(calculate);
        verify(settlementRepository, times(1)).findByCalculateWithParticipants(calculate);
        verify(optimizationService, times(1)).calculateAndOptimize(anyList());
        verify(calculateRepository, times(1)).save(calculate);
    }

    @Test
    @DisplayName("calculateAndOptimize(): OptimizationService 위임")
    void calculateAndOptimize_success() {
        Settlement s = mock(Settlement.class);

        calculateService.calculateAndOptimize(List.of(s));

        verify(optimizationService, times(1)).calculateAndOptimize(anyList());
        verifyNoInteractions(calculateDetailRepository); // 서비스는 saveAll 안함
    }

    @Test
    @DisplayName("botResultReturn(): 정산 결과 DTO 정상 반환")
    void botResultReturn_success() {
        // given
        UserGroup userGroup = UserGroup.create(42L, "치킨모임");

        Calculate calculate = Calculate.builder()
                .userGroup(userGroup)
                .build();
        setFieldValue(calculate, "calculateId", 999L);

        Member payer1 = Member.builder().build();
        setFieldValue(payer1, "memberId", 1L);
        Member payee1 = Member.builder().build();
        setFieldValue(payee1, "memberId", 2L);

        CalculateDetail detail1 = CalculateDetail.builder()
                .calculate(calculate)
                .payer(payer1)
                .payee(payee1)
                .amount(12000)
                .build();

        when(calculateDetailRepository.findAllByCalculate(calculate))
                .thenReturn(List.of(detail1));

        // when
        BotResponseDto result = calculateService.botResultReturn(calculate);

        //then
        assertThat(result.getGroupUrl()).isEqualTo("https://tallybot.vercel.app/42");
        assertThat(result.getCalculateUrl()).isEqualTo("https://tallybot.vercel.app/42/settlements/999");

        List<TransferDto> transfers = result.getTransfers();
        assertThat(transfers).hasSize(1);
        assertThat(transfers.get(0).getPayerId()).isEqualTo(1L);
        assertThat(transfers.get(0).getPayeeId()).isEqualTo(2L);
        assertThat(transfers.get(0).getAmount()).isEqualTo(12000);
    }

    private static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName + " on " + target.getClass(), e);
        }
    }

    private static Object getFieldValue(Object target, String fieldName) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> cur = type;
        while (cur != null) {
            try {
                return cur.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
