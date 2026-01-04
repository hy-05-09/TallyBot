package com.tallybot.backend.tallybot_back.service;

import com.tallybot.backend.tallybot_back.domain.Chat;
import com.tallybot.backend.tallybot_back.domain.Member;
import com.tallybot.backend.tallybot_back.repository.*;
import com.tallybot.backend.tallybot_back.dto.ChatForGptDto;
import com.tallybot.backend.tallybot_back.dto.SettlementDto;
import com.tallybot.backend.tallybot_back.exception.NoSettlementResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
// import java.util.Collections;
import java.util.List;
import java.util.Map;

// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ActiveProfiles("mock-data")  // 이 프로파일 조합으로 별도 컨텍스트 생성
public class GPTServiceTest {
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
    void returnResults_success() {
        // given
        Member m1 = Member.builder()
                .nickname("지훈")
                .build();


        Chat chat1 = Chat.builder()
                .member(m1)
                .timestamp(LocalDateTime.of(2025, 5, 1, 12, 0))
                .message("정산하자")
                .build();


        List<Chat> chats = List.of(chat1);
        List<ChatForGptDto> chatDtos = chats.stream()
                .map(chat -> new ChatForGptDto(
                        chat.getChatId(),
                        chat.getMember().getMemberId(),
                        chat.getMember().getNickname(),
                        chat.getMessage(),
                        chat.getTimestamp()
                ))
                .toList();


        SettlementDto dto = new SettlementDto();
        dto.setPlace("장소");
        dto.setPayer("지훈");
        dto.setItem("삼겹살");
        dto.setAmount(30000);
        dto.setConstants(Map.of("지훈", 0));
        dto.setRatios(Map.of("지훈", 100));

        SettlementDto[] mockResponse = new SettlementDto[]{ dto };



        when(restTemplate.postForEntity(anyString(), any(), eq(SettlementDto[].class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // when
        List<SettlementDto> results = gptService.returnResults(3L, chatDtos);

        // then
        assertEquals(1, results.size());
        assertEquals("삼겹살", results.get(0).getItem());

        // request body 확인용 캡처
        ArgumentCaptor<Object> requestCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate).postForEntity(anyString(), requestCaptor.capture(), eq(SettlementDto[].class));

        Object requestBody = requestCaptor.getValue();
        assertNotNull(requestBody);
        // 요청 포맷이 PythonRequestDto로 생성되었는지 간단 검증 가능 (자세한 필드 확인은 통합 테스트로)
    }

    @Test
    void returnResults_shouldThrowException_whenResponseBodyIsNull() {
        when(restTemplate.postForEntity(anyString(), any(), eq(SettlementDto[].class)))
                .thenReturn(new ResponseEntity<>(new SettlementDto[0], HttpStatus.OK));

        assertThrows(NoSettlementResultException.class, () ->
                gptService.returnResults(3L, List.of()));
    }

    @Test
    void returnResults_server_error() {
        when(restTemplate.postForEntity(anyString(), any(), eq(SettlementDto[].class)))
                .thenThrow(new RuntimeException("연결 실패"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                gptService.returnResults(3L, List.of()));

        assertTrue(ex.getMessage().contains("GPT 서버 응답 처리 중 오류"));
    }

//     private List<Chat> mockChatList() {
//         Chat chat = new Chat();
//         Member member = new Member();
//         member.setNickname("Alice");
//         chat.setMember(member);
//         chat.setMessage("밥 먹자~");
//         return Collections.singletonList(chat);
//     }
}
