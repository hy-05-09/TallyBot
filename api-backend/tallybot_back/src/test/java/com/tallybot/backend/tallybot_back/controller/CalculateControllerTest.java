package com.tallybot.backend.tallybot_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.dto.*;
import com.tallybot.backend.tallybot_back.service.*;
import com.tallybot.backend.tallybot_back.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CalculateController.class)
@ActiveProfiles("mock-data")  // 이 프로파일 조합으로 별도 컨텍스트 생성
class CalculateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalculateService calculateService;

    @MockitoBean
    private CalculateRepository calculateRepository;

    @MockitoBean
    private CalculateDetailRepository calculateDetailRepository;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private SettlementRepository settlementRepository;



    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("정산 요청 성공")
    void startCalculate_success() throws Exception {
        CalculateRequestDto request = new CalculateRequestDto(
                1L,
                LocalDateTime.of(2024, 5, 1, 14, 0),
                LocalDateTime.of(2024, 5, 1, 15, 0)
        );

        given(calculateService.groupExists(1L)).willReturn(true);
        given(calculateService.startCalculate(any())).willReturn(42L);

        mockMvc.perform(post("/api/calculate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculateId").value(42));
    }


    @Test
    @DisplayName("404 Not Found : 그룹 없음")
    void startCalculate_groupNotFound() throws Exception {
        CalculateRequestDto request = new CalculateRequestDto(
                999L,
                LocalDateTime.of(2024, 5, 1, 14, 0),
                LocalDateTime.of(2024, 5, 1, 15, 0)
        );

        given(calculateService.groupExists(999L)).willReturn(false);

        mockMvc.perform(post("/api/calculate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Group not found."));
    }

    @Test
    @DisplayName("400 Bad Request : endTime이 startTime보다 이를 때")
    void startCalculate_invalidTimeRange() throws Exception {
        CalculateRequestDto request = new CalculateRequestDto(
                1L,
                LocalDateTime.of(2024, 5, 1, 15, 0),
                LocalDateTime.of(2024, 5, 1, 14, 0)
        );

        given(calculateService.groupExists(1L)).willReturn(true);

        mockMvc.perform(post("/api/calculate/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Start time must be before end time."));
    }

    @Test
    @DisplayName("200 ok : 정산 결과 정상 반환")
    void getBotResult_success() throws Exception {
        // given
        UserGroup group = UserGroup.create(42L, "정산방");

        Calculate calculate = Calculate.builder()
                .status(CalculateStatus.COMPLETED)
                .userGroup(group)
                .build();

        Long id = 42L;
        ReflectionTestUtils.setField(calculate, "calculateId", id); 

        List<TransferDto> transfers = List.of(
                new TransferDto(1001L, 1002L, 12000),
                new TransferDto(1004L, 1002L, 8000),
                new TransferDto(1003L, 1004L, 5000)
        );

        BotResponseDto responseDto = new BotResponseDto(
                "https://tallybot.me/42",
                "https://tallybot.me/42/" + id,
                transfers
        );

        // when
        Mockito.when(calculateRepository.findById(id)).thenReturn(Optional.of(calculate));
        Mockito.when(calculateService.botResultReturn(calculate)).thenReturn(responseDto);

        // then
        mockMvc.perform(get("/api/calculate/"+id+"/brief-result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupUrl").value("https://tallybot.me/42"))
                .andExpect(jsonPath("$.calculateUrl").value("https://tallybot.me/42/"+id))
                .andExpect(jsonPath("$.transfers[0].payerId").value(1001));
    }

    @Test
    @DisplayName("202 Accepted : 정산 처리 중")
    void getBotResult_calculating() throws Exception {
        // given
        Calculate calculate = new Calculate();
        Long id = 42L;
        ReflectionTestUtils.setField(calculate, "calculateId", id); 
        calculate.changeStatus(CalculateStatus.CALCULATING);

        // when
        Mockito.when(calculateRepository.findById(id)).thenReturn(Optional.of(calculate));

        // then
        mockMvc.perform(get("/api/calculate/"+id+"/brief-result"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Calculate result is still being processed. Please try again later."));
    }

    @Test
    @DisplayName("400 Bad Request : 음수 또는 0 ID")
    void getBotResult_invalidId() throws Exception {
        mockMvc.perform(get("/api/calculate/0/brief-result"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Calculate ID must be a positive number."));
    }

    @Test
    void testGetBotResult_NotFound() throws Exception {
        Long calculateId = 999L;

        given(calculateRepository.findById(calculateId))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/calculate/{calculateId}/brief-result", calculateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("정산할 대화 내용이 없거나, 정산 결과가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("200 OK : 정산 내역 리스트 정상 반환")
    void getSettlementList_success() throws Exception {
        // given
        UserGroup group = UserGroup.create(100L, "치킨모임");

        Calculate calculate = new Calculate();
        Long id = 42L;
        ReflectionTestUtils.setField(calculate, "calculateId", id); 


        Member payer = Member.builder()
            .build();

        Member participant1 = Member.builder()
            .build();


        Member participant2 = Member.builder()
            .build();

        Settlement settlement = Settlement.create(
                group,
                payer,
                calculate,
                "카페",
                "커피",
                10000
        );
        ReflectionTestUtils.setField(settlement, "settlementId", 10L);


        Participant.ParticipantKey pk1 = new Participant.ParticipantKey();
        pk1.setSettlement(settlement);
        pk1.setMember(participant1);

        Participant p1 = new Participant();
        p1.setParticipantKey(pk1);
        p1.setConstant(0);
        p1.setRatio(new Ratio(1));
        ReflectionTestUtils.setField(participant1, "memberId", 1001L);


        Participant.ParticipantKey pk2 = new Participant.ParticipantKey();
        pk2.setSettlement(settlement);
        pk2.setMember(participant2);

        Participant p2 = new Participant();
        p2.setParticipantKey(pk2);
        p2.setConstant(0);
        p2.setRatio(new Ratio(2));
        ReflectionTestUtils.setField(participant2, "memberId", 1002L);

        settlement.addParticipant(p1);
        settlement.addParticipant(p2);

        when(calculateRepository.findById(id)).thenReturn(Optional.of(calculate));
        when(settlementRepository.findByCalculate(any(Calculate.class)))
                .thenReturn(List.of(settlement));
        when(settlementRepository.findByCalculateWithParticipants(any(Calculate.class)))
                .thenReturn(List.of(settlement));
        when(settlementRepository.findWithParticipantsByCalculateId(eq(id)))
                .thenReturn(List.of(settlement));


        // when & then
        mockMvc.perform(get("/api/calculate/"+id+"/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementCount").value(1))
                .andExpect(jsonPath("$.settlements[0].settlementId").value(10L))
                .andExpect(jsonPath("$.settlements[0].place").value("카페"))
                .andExpect(jsonPath("$.settlements[0].ratios['1001']").value(1))
                .andExpect(jsonPath("$.settlements[0].ratios['1002']").value(2))
                .andExpect(jsonPath("$.settlements[0].ratioSum").value(3));
    }

    @Test
    @DisplayName("400 Bad Request : calculateId가 0 이하")
    void getSettlementList_invalidId() throws Exception {
        mockMvc.perform(get("/api/calculate/0/settlements"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Calculate ID must be positive."));
    }

    @Test
    @DisplayName("404 Not Found : 존재하지 않는 calculateId")
    void getSettlementList_notFound() throws Exception {
        when(calculateRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/calculate/999/settlements"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Calculate not found."));
    }






    @Test
    @DisplayName("200 ok : 송금 관계 조회 성공")
    void getTransferList_success() throws Exception {
        // given
        Calculate calculate = new Calculate();
        Long id = 42L;
        ReflectionTestUtils.setField(calculate, "calculateId", id); 
        Member payer1 = Member.builder().build();
        ReflectionTestUtils.setField(payer1, "memberId", 1001L);

        Member payee1 = Member.builder().build();
        ReflectionTestUtils.setField(payee1, "memberId", 1002L);

        Member payer2 = Member.builder().build();
        ReflectionTestUtils.setField(payer2, "memberId", 1004L);

        Member payee2 = Member.builder().build();
        ReflectionTestUtils.setField(payee2, "memberId", 1002L);

        Member payer3 = Member.builder().build();
        ReflectionTestUtils.setField(payer3, "memberId", 1003L);

        Member payee3 = Member.builder().build();
        ReflectionTestUtils.setField(payee3, "memberId", 1004L);


        CalculateDetail d1 = CalculateDetail.builder()
            .payer(payer1)
            .payee(payee1)
            .amount(12000)
            .build();
        
        CalculateDetail d2 = CalculateDetail.builder()
            .payer(payer2)
            .payee(payee2)
            .amount(8000)
            .build();

        CalculateDetail d3 = CalculateDetail.builder()
            .payer(payer3)
            .payee(payee3)
            .amount(5000)
            .build();

        // when
        Mockito.when(calculateRepository.findById(id)).thenReturn(Optional.of(calculate));
        Mockito.when(calculateDetailRepository.findAllByCalculate(calculate))
                .thenReturn(List.of(d1, d2, d3));

        // then
        mockMvc.perform(get("/api/calculate/"+id+"/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferCount").value(3))
                .andExpect(jsonPath("$.transfers[0].payerId").value(1001))
                .andExpect(jsonPath("$.transfers[1].amount").value(8000))
                .andExpect(jsonPath("$.transfers[2].payeeId").value(1004));
    }

    @Test
    @DisplayName("400 Bad Request : 잘못된 Calculate ID")
    void getTransferList_invalidId() throws Exception {
        mockMvc.perform(get("/api/calculate/0/transfers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Calculate ID must be positive."));
    }

    @Test
    @DisplayName("404 : Not Found : 없는 Calculate ID")
    void getTransferList_notFound() throws Exception {
        Mockito.when(calculateRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/calculate/999/transfers"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Calculate not found."));
    }

    @Test
    @DisplayName("200 ok : 정산 완료 처리 성공")
    void completeCalculate_success() throws Exception {
        Calculate calculate = new Calculate();
        Long id = 42L;
        ReflectionTestUtils.setField(calculate, "calculateId", id); 

        when(calculateRepository.findById(id)).thenReturn(Optional.of(calculate));
        when(calculateRepository.save(any(Calculate.class))).thenReturn(calculate);

        mockMvc.perform(post("/api/calculate/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("calculateId", id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Calculation marked as completed."));
    }

    @Test
    @DisplayName("400 Bad Request : calculateId 형식 오류")
    void completeCalculate_missingId() throws Exception {
        mockMvc.perform(post("/api/calculate/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Calculate ID must not be null."));
    }

    @Test
    @DisplayName("404 Not Found : 존재하지 않는 calculateId")
    void completeCalculate_notFound() throws Exception {
        when(calculateRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/calculate/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("calculateId", 999))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Calculate entity not found."));
    }

    @Test
    @DisplayName("200 ok : 재정산 성공")
    void recalculate_success() throws Exception {
        // Given: 가짜 Calculate 객체와 상태 설정
        Calculate calculate = new Calculate();
        Long id = 42L;
        ReflectionTestUtils.setField(calculate, "calculateId", id); 
        calculate.changeStatus(CalculateStatus.PENDING);

        when(calculateRepository.findById(id)).thenReturn(Optional.of(calculate));
        when(calculateRepository.save(any(Calculate.class))).thenReturn(calculate);
        doNothing().when(calculateService).recalculate(id);

        mockMvc.perform(post("/api/calculate/recalculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("calculateId", id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recalculation completed successfully."));
    }


    @Test
    @DisplayName("400 Bad Request : 재정산 실패 - calculateId 누락")
    void recalculate_fail_missingId() throws Exception {
        mockMvc.perform(post("/api/calculate/recalculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Calculate ID must not be null."));
    }


}
