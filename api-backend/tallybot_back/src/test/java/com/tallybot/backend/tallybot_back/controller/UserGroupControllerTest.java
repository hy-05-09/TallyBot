package com.tallybot.backend.tallybot_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallybot.backend.tallybot_back.domain.Calculate;
import com.tallybot.backend.tallybot_back.domain.CalculateStatus;
import com.tallybot.backend.tallybot_back.domain.UserGroup;
import com.tallybot.backend.tallybot_back.domain.Member;
import com.tallybot.backend.tallybot_back.dto.FrontGroupDto;
import com.tallybot.backend.tallybot_back.dto.FrontMemberDto;
import com.tallybot.backend.tallybot_back.dto.GroupCreateRequest;
import com.tallybot.backend.tallybot_back.dto.GroupCreateResponse;
import com.tallybot.backend.tallybot_back.repository.CalculateRepository;
import com.tallybot.backend.tallybot_back.repository.GroupRepository;
import com.tallybot.backend.tallybot_back.repository.MemberRepository;
import com.tallybot.backend.tallybot_back.service.GroupService;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
@ActiveProfiles("mock-data")  // 이 프로파일 조합으로 별도 컨텍스트 생성
class UserGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private GroupRepository groupRepository;

    @MockitoBean
    private CalculateRepository calculateRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("200 OK : 그룹 생성 성공")
    void createGroupSuccess() throws Exception {
        GroupCreateRequest request = new GroupCreateRequest(
                2347917394801L,
                "정산방",
                "철수"
        );

        GroupCreateResponse response = new GroupCreateResponse(
                2347917394801L,
                List.of(
                        new GroupCreateResponse.MemberInfo("철수", 1L),
                        new GroupCreateResponse.MemberInfo("영희", 2L)
                )
        );

        given(groupService.createGroupWithMember(any())).willReturn(response);

        mockMvc.perform(post("/api/group/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) //200 ok인지 검증
                .andExpect(jsonPath("$.groupId").value(2347917394801L))
                .andExpect(jsonPath("$.members[0].nickname").value("철수"))
                .andExpect(jsonPath("$.members[1].memberId").value(2));
    }

    @Test
    @DisplayName("400 Bad Request : 그룹 이름 누락")
    void createGroupWithoutName() throws Exception {
        GroupCreateRequest request = new GroupCreateRequest(
                2347917394801L,
                "", // 누락
                "철수"
        );

        mockMvc.perform(post("/api/group/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Group name must not be empty."));
    }

    @Test
    @DisplayName("400 Bad Request : 멤버 이름 누락")
    void createGroupWithoutMembers() throws Exception {
        GroupCreateRequest request = new GroupCreateRequest(
                2347917394801L,
                "정산방",
                "" // 빈 멤버
        );

        mockMvc.perform(post("/api/group/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Member nickname must not be empty."));
    }


    @Test
    @DisplayName("200 ok : 그룹 정보 조회 성공")
    void getGroupInfo_success() throws Exception {
        // given

        FrontGroupDto dto = new FrontGroupDto(42L, "정산방", 3, 2); // 생성자/빌더는 너 DTO에 맞게
        
        when(groupService.getGroupInfo(42L)).thenReturn(dto);

        // when & then
        mockMvc.perform(get("/api/group/42"))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groupId").value(42))
            .andExpect(jsonPath("$.groupName").value("정산방"))
            .andExpect(jsonPath("$.memberCount").value(3))
            .andExpect(jsonPath("$.calculateCount").value(2));
        }



    @Test
    @DisplayName("400 Bad Request : 잘못된 groupId (0 이하)")
    void getGroupInfo_invalidId() throws Exception {
        mockMvc.perform(get("/api/group/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Group ID must be positive."));
    }

    @Test
    @DisplayName("404 Not Found : 존재하지 않는 그룹")
    void getGroupInfo_notFound() throws Exception {
        Mockito.when(groupService.getGroupInfo(999L))
                .thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/api/group/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Group not found."));
    }



    @Test
    @DisplayName("200 ok : 그룹 멤버 조회 성공")
    void getGroupMembers_success() throws Exception {
        /// given
        FrontMemberDto d1 = new FrontMemberDto(1L, "철수");
        FrontMemberDto d2 = new FrontMemberDto(2L, "영희");

        when(groupService.getGroupMembers(42L)).thenReturn(List.of(d1, d2));

        // when & then
        mockMvc.perform(get("/api/group/42/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(1))
                .andExpect(jsonPath("$[0].nickname").value("철수"))
                .andExpect(jsonPath("$[1].memberId").value(2))
                .andExpect(jsonPath("$[1].nickname").value("영희"));
    }

    @Test
    @DisplayName("400 Bad Request : 잘못된 groupId (0 이하)")
    void getGroupMembers_invalidId() throws Exception {
        mockMvc.perform(get("/api/group/0/members"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Group ID must be positive."));
    }

    @Test
    @DisplayName("404 Not Found : 존재하지 않는 그룹")
    void getGroupMembers_notFound() throws Exception {
        when(groupService.getGroupMembers(999L))
            .thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/api/group/999/members"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Group not found."));
    }

    @Disabled
    @Test
    @DisplayName("200 OK : 정산 목록 조회 성공")
    void getGroupCalculates_success() throws Exception {
        // given
        UserGroup userGroup = UserGroup.create(42L, "정산방");

        Calculate c1 = Calculate.builder()
                .startTime(LocalDateTime.of(2025, 5, 8, 11, 0))
                .endTime(LocalDateTime.of(2025, 5, 8, 13, 0))
                .status(CalculateStatus.PENDING)
                .userGroup(userGroup)
                .build();

        Calculate c2 = Calculate.builder()
                .startTime(LocalDateTime.of(2025, 5, 9, 10, 0))
                .endTime(LocalDateTime.of(2025, 5, 9, 12, 0))
                .status(CalculateStatus.COMPLETED)
                .userGroup(userGroup)
                .build();

        when(groupRepository.findById(42L)).thenReturn(Optional.of(userGroup));
        when(calculateRepository.findByUserGroup(userGroup)).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/group/42/calculates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].calculateId").value(101))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].calculateId").value(102))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("400 Bad Request : 잘못된 groupId")
    void getGroupCalculates_invalidId() throws Exception {
        mockMvc.perform(get("/api/group/0/calculates"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Group ID must be positive."));
    }

    @Test
    @DisplayName("404 Not Found : 그룹 없음")
    void getGroupCalculates_notFound() throws Exception {
        when(groupService.getGroupCalculates(999L))
            .thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/api/group/999/calculates"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Group not found."));
    }
}
