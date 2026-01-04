package com.tallybot.backend.tallybot_back.controller;

import com.tallybot.backend.tallybot_back.dto.*;
import com.tallybot.backend.tallybot_back.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping("/create")
    public ResponseEntity<GroupCreateResponse> createGroup(@Valid @RequestBody GroupCreateRequest request) {
        GroupCreateResponse response = groupService.createGroupWithMember(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupInfo(@PathVariable Long groupId) {
        if (groupId <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Group ID must be positive."));
        }

        try {
            FrontGroupDto response = groupService.getGroupInfo(groupId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Group not found."));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long groupId) {
        if (groupId <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Group ID must be positive."));
        }

        try{
            List<FrontMemberDto> result = groupService.getGroupMembers(groupId);
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Group not found."));
        }
    }


    @GetMapping("/{groupId}/calculates")
    public ResponseEntity<?> getGroupCalculates(@PathVariable Long groupId) {
        if (groupId <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Group ID must be positive."));
        }

        try {
            List<FrontCalculateDto> result = groupService.getGroupCalculates(groupId);
            return ResponseEntity.ok(result);
        } catch(NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Group not found."));
        }
    }
}
