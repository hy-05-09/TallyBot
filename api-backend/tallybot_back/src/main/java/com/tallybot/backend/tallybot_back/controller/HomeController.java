package com.tallybot.backend.tallybot_back.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HomeController {

    @GetMapping
    public String checkHealth() {
        return "TallyBot backend is running";
    }
}
