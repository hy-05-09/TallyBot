package com.tallybot.backend.tallybot_back.service;

import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.dto.SettlementDto;
import com.tallybot.backend.tallybot_back.dto.SettlementUpdateRequest;
import com.tallybot.backend.tallybot_back.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("mock-data")
class SettlementServiceTest {

    @Mock private SettlementRepository settlementRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CalculateRepository calculateRepository;
    @Mock private OptimizationService optimizationService;
    @Mock private CalculateDetailRepository calculateDetailRepository;

    @InjectMocks
    private SettlementService settlementService;

    // =========================
    // add - participants 명시
    // =========================
    @Test
    @DisplayName("applySettlementUpdate(add): participants 포함 시 정상 저장")
    void addSettlement_success_withParticipants() {
        // given
        Long calculateId = 1L;
        Long payerId = 1001L;
        Long p2Id = 1002L;
        Long p3Id = 1003L;

        UserGroup userGroup = UserGroup.create(1L, "치킨모임");

        Member payer = Member.builder().userGroup(userGroup).build();
        setId(payer, "memberId", payerId);

        Member m2 = Member.builder().userGroup(userGroup).build();
        setId(m2, "memberId", p2Id);

        Member m3 = Member.builder().userGroup(userGroup).build();
        setId(m3, "memberId", p3Id);

        Calculate calculate = Calculate.builder().userGroup(userGroup).build();
        setId(calculate, "calculateId", calculateId);

        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setCalculateId(calculateId);
        request.setField("add");

        Map<String, Object> newValue = new HashMap<>();
        newValue.put("place", "가츠벤또");
        newValue.put("item", "점심");
        newValue.put("amount", 30000);
        newValue.put("payer", payerId);
        newValue.put("participants", List.of(p2Id, p3Id));
        request.setNewValue(newValue);

        request.setConstants(Map.of(
                String.valueOf(payerId), 0,
                String.valueOf(p2Id), 0,
                String.valueOf(p3Id), 0
        ));
        request.setRatios(Map.of(
                String.valueOf(payerId), 0,
                String.valueOf(p2Id), 1,
                String.valueOf(p3Id), 1
        ));
        request.setSum(2);

        when(memberRepository.findById(payerId)).thenReturn(Optional.of(payer));
        when(memberRepository.findAllById(List.of(p2Id, p3Id))).thenReturn(List.of(m2, m3));
        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));

        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> {
            Settlement s = inv.getArgument(0);
            setId(s, "settlementId", 123L);
            return s;
        });

        // when
        Long result = settlementService.applySettlementUpdate(request);

        // then
        assertThat(result).isEqualTo(123L);
        verify(settlementRepository).save(any(Settlement.class));
    }

    // =========================
    // add - participants 누락(기본 참여자)
    // =========================
    @Test
    @DisplayName("applySettlementUpdate(add): participants 누락 + constants/ratios도 null이면 전체 멤버 기본 참여")
    void addSettlement_success_withoutParticipants_defaults() {
        // given
        Long calculateId = 42L;
        Long payerId = 1001L;

        UserGroup userGroup = UserGroup.create(1L, "치킨모임");

        Member payer = Member.builder().userGroup(userGroup).nickname("지우").build();
        setId(payer, "memberId", payerId);

        Member m1 = Member.builder().userGroup(userGroup).build();
        setId(m1, "memberId", 1002L);

        Member m2 = Member.builder().userGroup(userGroup).build();
        setId(m2, "memberId", 1003L);

        Calculate calculate = Calculate.builder().userGroup(userGroup).build();
        setId(calculate, "calculateId", calculateId);

        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setCalculateId(calculateId);
        request.setField("add");
        request.setNewValue(new HashMap<>(Map.of(
                "amount", 20000,
                "payer", payerId
        )));
        request.setConstants(null);
        request.setRatios(null);
        request.setSum(null);

        when(memberRepository.findById(payerId)).thenReturn(Optional.of(payer));
        when(memberRepository.findByUserGroup(userGroup)).thenReturn(List.of(m1, m2));
        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> {
            Settlement s = inv.getArgument(0);
            setId(s, "settlementId", 456L);
            return s;
        });

        // when
        Long result = settlementService.applySettlementUpdate(request);

        // then
        assertThat(result).isEqualTo(456L);
        verify(memberRepository).findByUserGroup(userGroup);
        verify(settlementRepository).save(any(Settlement.class));
    }

    // =========================
    // add - participants null인데 constants/ratios 존재 → 예외
    // =========================
    @Test
    @DisplayName("applySettlementUpdate(add): participants null인데 constants/ratios 존재하면 예외")
    void addSettlement_fail_participantsNull_butConstantsExist() {
        // given
        Long calculateId = 42L;
        Long payerId = 1001L;

        UserGroup userGroup = UserGroup.create(1L, "치킨모임");

        Member payer = Member.builder().userGroup(userGroup).build();
        setId(payer, "memberId", payerId);

        Calculate calculate = Calculate.builder().userGroup(userGroup).build();
        setId(calculate, "calculateId", calculateId);

        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setCalculateId(calculateId);
        request.setField("add");
        request.setNewValue(Map.of("amount", 10000, "payer", payerId));
        request.setConstants(Map.of(String.valueOf(payerId), 5000)); // participants 없는데 constants 존재

        when(memberRepository.findById(payerId)).thenReturn(Optional.of(payer));
        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));

        // then
        assertThatThrownBy(() -> settlementService.applySettlementUpdate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("participants가 null일 경우 constants/ratios도 null이어야 합니다.");
    }

    // =========================
    // add - participants 있는데 constants/ratios null이면 기본값 적용
    // =========================
    @Test
    @DisplayName("applySettlementUpdate(add): participants는 있는데 constants/ratios/sum 누락 시 기본값으로 저장")
    void addSettlement_success_participantRatioDefaults() {
        // given
        Long calculateId = 42L;
        Long payerId = 1001L;
        Long participantId = 1002L;

        UserGroup userGroup = UserGroup.create(1L, "치킨모임");

        Member payer = Member.builder().userGroup(userGroup).build();
        setId(payer, "memberId", payerId);

        Member participant = Member.builder().userGroup(userGroup).build();
        setId(participant, "memberId", participantId);

        Calculate calculate = Calculate.builder().userGroup(userGroup).build();
        setId(calculate, "calculateId", calculateId);

        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setCalculateId(calculateId);
        request.setField("add");
        request.setNewValue(Map.of(
                "amount", 10000,
                "payer", payerId,
                "participants", List.of(participantId)
        ));
        request.setConstants(null);
        request.setRatios(null);
        request.setSum(null);

        when(memberRepository.findById(payerId)).thenReturn(Optional.of(payer));
        when(memberRepository.findAllById(List.of(participantId))).thenReturn(List.of(participant));
        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> {
            Settlement s = inv.getArgument(0);
            setId(s, "settlementId", 777L);
            return s;
        });

        // when
        Long result = settlementService.applySettlementUpdate(request);

        // then
        assertThat(result).isEqualTo(777L);
        verify(settlementRepository).save(any(Settlement.class));
    }

    // =========================
    // delete
    // =========================
    @Test
    void deleteSettlement_success() {
        Long calculateId = 1L;

        Settlement settlement = new Settlement();

        when(settlementRepository.findById(settlement.getSettlementId())).thenReturn(Optional.of(settlement));

        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setField("delete");
        request.setSettlementId(settlement.getSettlementId());
        request.setCalculateId(calculateId);

        Long result = settlementService.applySettlementUpdate(request);
        assertEquals(settlement.getSettlementId(), result);
        verify(settlementRepository).delete(settlement);
    }

    // =========================
    // update - place/amount
    // =========================
    @Test
    @DisplayName("applySettlementUpdate(update): place/amount 수정 성공")
    void updateSettlement_success_modifyPlaceAndAmount() {
        // given
        Long calculateId = 2L;
        Long settlementId = 99L;

        UserGroup userGroup = UserGroup.create(1L, "치킨모임");
        Member payer = Member.builder().userGroup(userGroup).build();
        setId(payer, "memberId", 1001L);

        Calculate calculate = Calculate.builder().userGroup(userGroup).build();
        setId(calculate, "calculateId", calculateId);

        Settlement settlement = Settlement.create(userGroup, payer, calculate, "OldPlace", "", 10000);
        setId(settlement, "settlementId", settlementId);

        when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));

        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setField("update");
        request.setSettlementId(settlementId);
        request.setCalculateId(calculateId);
        request.setNewValue(Map.of(
                "place", "NewPlace",
                "amount", 20000
        ));

        // when
        Long result = settlementService.applySettlementUpdate(request);

        // then
        assertThat(result).isEqualTo(settlementId);
        assertThat(settlement.getPlace()).isEqualTo("NewPlace");
        assertThat(settlement.getAmount()).isEqualTo(20000);
        verify(settlementRepository).save(settlement);
    }

    // =========================
    // toSettlement
    // =========================
    @Test
    @DisplayName("toSettlement(): SettlementDto -> Settlement 변환 성공")
    void toSettlement_success() {
        // given
        Long calculateId = 500L;
        UserGroup userGroup = UserGroup.create(10L, "치킨모임");

        Calculate calculate = Calculate.builder().userGroup(userGroup).build();
        setId(calculate, "calculateId", calculateId);

        Long payerId = 1001L;
        Long p1Id = 1002L;
        Long p2Id = 1003L;

        Member payer = Member.builder().userGroup(userGroup).nickname("준호").build();
        setId(payer, "memberId", payerId);

        Member participant1 = Member.builder().userGroup(userGroup).nickname("소연").build();
        setId(participant1, "memberId", p1Id);

        Member participant2 = Member.builder().userGroup(userGroup).nickname("민우").build();
        setId(participant2, "memberId", p2Id);

        SettlementDto dto = new SettlementDto();
        dto.setPlace("호텔");
        dto.setItem("숙박");
        dto.setAmount(90000);
        dto.setPayer(String.valueOf(payerId));
        dto.setParticipants(List.of(String.valueOf(p1Id), String.valueOf(p2Id)));
        dto.setConstants(Map.of(String.valueOf(p1Id), 0, String.valueOf(p2Id), 0));
        dto.setRatios(Map.of(String.valueOf(p1Id), 1, String.valueOf(p2Id), 2));

        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));
        when(memberRepository.findByMemberIdAndUserGroup(payerId, userGroup)).thenReturn(Optional.of(payer));
        when(memberRepository.findByMemberIdAndUserGroup(p1Id, userGroup)).thenReturn(Optional.of(participant1));
        when(memberRepository.findByMemberIdAndUserGroup(p2Id, userGroup)).thenReturn(Optional.of(participant2));

        // when
        Settlement result = settlementService.toSettlement(dto, calculateId);

        // then
        assertThat(result.getPlace()).isEqualTo("호텔");
        assertThat(result.getItem()).isEqualTo("숙박");
        assertThat(result.getAmount()).isEqualTo(90000);
        assertThat(result.getPayer().getMemberId()).isEqualTo(payerId);
        assertThat(result.getParticipants()).hasSize(2);
        assertThat(result.getCalculate().getCalculateId()).isEqualTo(calculateId);
    }

    // =========================
    // toSettlements
    // =========================
    @Test
    @DisplayName("toSettlements(): SettlementDto 리스트 -> Settlement 리스트 변환 성공")
    void toSettlements_success() {
        // given
        Long calculateId = 600L;
        UserGroup userGroup = UserGroup.create(1L, "치킨모임");

        Calculate calculate = Calculate.builder().userGroup(userGroup).build();
        setId(calculate, "calculateId", calculateId);

        Long payerId = 1001L;
        Long participantId = 1002L;

        Member payer = Member.builder().userGroup(userGroup).build();
        setId(payer, "memberId", payerId);

        Member participant = Member.builder().userGroup(userGroup).build();
        setId(participant, "memberId", participantId);

        SettlementDto dto1 = new SettlementDto();
        dto1.setPlace("식당");
        dto1.setItem("점심");
        dto1.setAmount(30000);
        dto1.setPayer(String.valueOf(payerId));
        dto1.setParticipants(List.of(String.valueOf(payerId), String.valueOf(participantId)));
        dto1.setConstants(Map.of(String.valueOf(payerId), 0, String.valueOf(participantId), 0));
        dto1.setRatios(Map.of(String.valueOf(payerId), 1, String.valueOf(participantId), 1));

        when(calculateRepository.findById(calculateId)).thenReturn(Optional.of(calculate));
        when(memberRepository.findByMemberIdAndUserGroup(payerId, userGroup)).thenReturn(Optional.of(payer));
        when(memberRepository.findByMemberIdAndUserGroup(participantId, userGroup)).thenReturn(Optional.of(participant));

        // when
        List<Settlement> result = settlementService.toSettlements(List.of(dto1), calculateId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParticipants()).hasSize(2);
    }

    // =========================
    // helper
    // =========================
    private static void setId(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }
}
