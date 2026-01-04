package com.tallybot.backend.tallybot_back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransferDto {
    private Long payerId;
    private Long payeeId;
    private int amount;
}
