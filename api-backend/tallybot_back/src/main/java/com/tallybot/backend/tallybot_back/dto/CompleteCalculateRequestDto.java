package com.tallybot.backend.tallybot_back.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteCalculateRequestDto {

    @NotNull(message = "Calculate ID must not be null.")
    private Long calculateId;
}
