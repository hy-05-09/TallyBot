package com.tallybot.backend.tallybot_back.service;

import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.dto.FrontCalculateDto;
import com.tallybot.backend.tallybot_back.dto.FrontGroupDto;
import com.tallybot.backend.tallybot_back.dto.FrontMemberDto;
import com.tallybot.backend.tallybot_back.dto.GroupCreateRequest;
import com.tallybot.backend.tallybot_back.dto.GroupCreateResponse;
import com.tallybot.backend.tallybot_back.repository.*;
// import com.tallybot.backend.tallybot_back.service.*;
import lombok.RequiredArgsConstructor;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final CalculateRepository calculateRepository;
    // private static final Logger logger = LoggerFactory.getLogger(GroupService.class);


    public GroupCreateResponse createGroupWithMember(GroupCreateRequest request) {
        UserGroup userGroup = groupRepository.findById(request.getGroupId())
                .orElseGet(() -> groupRepository.save(new UserGroup(request.getGroupId(), request.getGroupName())));

        // 중복 멤버 확인
        boolean exists = memberRepository.existsByUserGroupAndNickname(userGroup, request.getMember());
        if (!exists) {
            Member member = new Member();
            member.setUserGroup(userGroup);
            member.setNickname(request.getMember());
            memberRepository.save(member);
        }

        // 모든 멤버 조회 후 응답 반환
        List<Member> members = memberRepository.findByUserGroup(userGroup);
        List<GroupCreateResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> new GroupCreateResponse.MemberInfo(m.getNickname(), m.getMemberId()))
                .toList();

        return new GroupCreateResponse(userGroup.getGroupId(), memberInfos);
    }

    public FrontGroupDto getGroupInfo(Long groupId) {
        UserGroup group = groupRepository.findById(groupId)
            .orElseThrow(() -> new NoSuchElementException("Group not found."));
            
            int memberCount = memberRepository.countByUserGroup(group);
            int calculateCount =  calculateRepository.countByUserGroup(group);

            return new FrontGroupDto(group.getGroupId(), group.getGroupName(), memberCount, calculateCount);
    }

    public List<FrontMemberDto> getGroupMembers(Long groupId){
        UserGroup group = groupRepository.findById(groupId)
            .orElseThrow(() -> new NoSuchElementException("Group not found."));

            return memberRepository.findByUserGroup(group).stream()
                .map(m -> new FrontMemberDto(m.getMemberId(), m.getNickname()))
                .toList();
    }

    public List<FrontCalculateDto> getGroupCalculates(Long groupId){
        UserGroup group = groupRepository.findById(groupId)
            .orElseThrow(()-> new NoSuchElementException("Group not found."));

        return calculateRepository.findByUserGroup(group).stream()
            .map(c -> new FrontCalculateDto(c.getCalculateId(), c.getStartTime(),c.getEndTime(), c.getStatus()))
            .toList();
    }

}
