package com.tallybot.backend.tallybot_back.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup userGroup;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "message", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String message;

    @Builder
    private Chat(UserGroup userGroup,
                 LocalDateTime timestamp,
                 Member member,
                 String message) {
        this.userGroup = userGroup;
        this.timestamp = timestamp;
        this.member = member;
        this.message = message;
    }
}
