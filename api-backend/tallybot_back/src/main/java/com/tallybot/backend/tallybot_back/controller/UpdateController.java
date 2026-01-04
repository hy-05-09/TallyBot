package com.tallybot.backend.tallybot_back.controller;

import com.tallybot.backend.tallybot_back.dto.*;
import com.tallybot.backend.tallybot_back.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/update")
@RequiredArgsConstructor
public class UpdateController {

    private final SettlementService settlementService;

    @PostMapping("/settlement")
    public ResponseEntity<?> updateSettlement (@Valid @RequestBody SettlementUpdateRequest request) {
        String field = request.getField();
        if (!"add".equals(field) && !"update".equals(field) && !"delete".equals(field)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid field type. Must be one of: add, update, delete."));
        }

        if (!settlementService.fieldExists(field, request)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Missing required fields in one or more."));
        }
            
        try {
            settlementService.validateCalculateExists(request.getCalculateId());
            Long settlementId = settlementService.applySettlementUpdate(request);
            return ResponseEntity.ok(new SettlementUpdateResponse(settlementId));
        } catch (NoSuchElementException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Calculate entity not found."));
        }
        
    }
}
