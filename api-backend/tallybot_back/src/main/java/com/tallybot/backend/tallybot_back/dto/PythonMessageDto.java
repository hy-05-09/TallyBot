package com.tallybot.backend.tallybot_back.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PythonMessageDto {
    private String uniqueChatId;
    @JsonProperty("speaker")
    private String speakerId;  // Member ID as String
    private String messageContent;
    private String timestamp;
}
