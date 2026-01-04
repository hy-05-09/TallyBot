package com.tallybot.backend.tallybot_back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateResponse {

    private Long groupId;
    private List<MemberInfo> members;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private String nickname;
        private Long memberId;
    }
}
