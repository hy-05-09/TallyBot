package com.tallybot.backend.tallybot_back.service;

import com.tallybot.backend.tallybot_back.domain.UserGroup;
import com.tallybot.backend.tallybot_back.dto.ChatForGptDto;
import com.tallybot.backend.tallybot_back.dto.SettlementDto;
import com.tallybot.backend.tallybot_back.dto.SettlementResponseWrapper;
import com.tallybot.backend.tallybot_back.exception.NoSettlementResultException;
import com.tallybot.backend.tallybot_back.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GPTServiceTest {

    private RestTemplate restTemplate;
    private GroupRepository groupRepository;
    private GPTService gptService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        groupRepository = mock(GroupRepository.class);
        gptService = new GPTService(restTemplate, groupRepository);
    }

    @Test
    @DisplayName("returnResults(): 정상 응답 시 finalResult 반환")
    void returnResults_success() {
        // given
        Long groupId = 3L;
        when(groupRepository.findById(groupId))
                .thenReturn(Optional.of(UserGroup.create(groupId, "정산방")));

        List<ChatForGptDto> chatDtos = List.of(
                new ChatForGptDto(
                        1L,
                        10L,
                        "지훈",
                        "정산하자",
                        LocalDateTime.of(2025, 5, 1, 12, 0)
                )
        );

        SettlementDto dto = new SettlementDto();
        dto.setItem("삼겹살");
        dto.setAmount(30000);

        SettlementResponseWrapper wrapper = new SettlementResponseWrapper();
        wrapper.setFinalResult(List.of(dto));

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(SettlementResponseWrapper.class)
        )).thenReturn(new ResponseEntity<>(wrapper, HttpStatus.OK));

        // when
        List<SettlementDto> results = gptService.returnResults(groupId, chatDtos);

        // then
        assertEquals(1, results.size());
        assertEquals("삼겹살", results.get(0).getItem());

        // 요청 바디가 requestDto로 넘어갔는지 캡처 (구체 필드 검증은 필요 시 추가)
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForEntity(anyString(), bodyCaptor.capture(), eq(SettlementResponseWrapper.class));
        assertNotNull(bodyCaptor.getValue());
    }

    @Test
    @DisplayName("returnResults(): wrapper(body)가 null이면 NoSettlementResultException")
    void returnResults_shouldThrow_whenWrapperIsNull() {
        // given
        Long groupId = 3L;
        when(groupRepository.findById(groupId))
                .thenReturn(Optional.of(UserGroup.create(groupId, "정산방")));

        when(restTemplate.postForEntity(anyString(), any(), eq(SettlementResponseWrapper.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // when & then
        assertThrows(NoSettlementResultException.class, () ->
                gptService.returnResults(groupId, sampleChatDtos()));
    }

    @Test
    @DisplayName("returnResults(): finalResult가 null이면 NoSettlementResultException")
    void returnResults_shouldThrow_whenFinalResultIsNull() {
        // given
        Long groupId = 3L;
        when(groupRepository.findById(groupId))
                .thenReturn(Optional.of(UserGroup.create(groupId, "정산방")));

        SettlementResponseWrapper wrapper = new SettlementResponseWrapper();
        wrapper.setFinalResult(null);

        when(restTemplate.postForEntity(anyString(), any(), eq(SettlementResponseWrapper.class)))
                .thenReturn(new ResponseEntity<>(wrapper, HttpStatus.OK));

        // when & then
        assertThrows(NoSettlementResultException.class, () ->
                gptService.returnResults(groupId, sampleChatDtos()));
    }

    @Test
    @DisplayName("returnResults(): finalResult가 empty면 NoSettlementResultException")
    void returnResults_shouldThrow_whenFinalResultIsEmpty() {
        // given
        Long groupId = 3L;
        when(groupRepository.findById(groupId))
                .thenReturn(Optional.of(UserGroup.create(groupId, "정산방")));

        SettlementResponseWrapper wrapper = new SettlementResponseWrapper();
        wrapper.setFinalResult(List.of()); // empty

        when(restTemplate.postForEntity(anyString(), any(), eq(SettlementResponseWrapper.class)))
                .thenReturn(new ResponseEntity<>(wrapper, HttpStatus.OK));

        // when & then
        assertThrows(NoSettlementResultException.class, () ->
                gptService.returnResults(groupId, sampleChatDtos()));
    }

    @Test
    @DisplayName("returnResults(): RestTemplate 호출 중 예외 발생 시 RuntimeException 래핑")
    void returnResults_server_error() {
        // given
        Long groupId = 3L;
        when(groupRepository.findById(groupId))
                .thenReturn(Optional.of(UserGroup.create(groupId, "정산방")));

        when(restTemplate.postForEntity(anyString(), any(), eq(SettlementResponseWrapper.class)))
                .thenThrow(new RuntimeException("연결 실패"));

        // when
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                gptService.returnResults(groupId, sampleChatDtos()));

        // then
        assertTrue(ex.getMessage().contains("GPT 서버 응답 처리 중 오류가 발생했습니다."));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("연결 실패"));
    }

    private List<ChatForGptDto> sampleChatDtos() {
        return List.of(
                new ChatForGptDto(
                        1L,
                        10L,
                        "지훈",
                        "정산하자",
                        LocalDateTime.of(2025, 5, 1, 12, 0)
                )
        );
    }
}
