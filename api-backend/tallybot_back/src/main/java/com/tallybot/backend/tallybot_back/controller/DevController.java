package com.tallybot.backend.tallybot_back.controller;

import com.tallybot.backend.tallybot_back.domain.Calculate;
import com.tallybot.backend.tallybot_back.repository.CalculateRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevController {

    private final CalculateRepository calculateRepository;

    //개발 환경에서 정산 데이터 확인을 위한 디버깅용 API
    @GetMapping("/calculates")
    public List<Calculate> getAllCalculates() {
        return calculateRepository.findAll();
    }
}
