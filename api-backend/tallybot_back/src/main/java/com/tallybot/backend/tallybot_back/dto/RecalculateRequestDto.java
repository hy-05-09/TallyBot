package com.tallybot.backend.tallybot_back.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecalculateRequestDto {
    @NotNull(message="Calculate ID must not be null.")
    private Long calculateId;

}
