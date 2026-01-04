package com.tallybot.backend.tallybot_back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChatForGptDto {
    private Long chatId;
    private Long memberId;
    private String nickname;
    private String message;
    private LocalDateTime timestamp;

}
