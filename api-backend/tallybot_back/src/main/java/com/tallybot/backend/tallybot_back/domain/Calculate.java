package com.tallybot.backend.tallybot_back.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "calculate")
public class Calculate{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calculate_id", nullable = false)
    private Long calculateId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)  // ORDINAL보다 STRING 권장
    private CalculateStatus status; // PENDING, COMPLETED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup userGroup;

    @OneToMany(mappedBy = "calculate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Settlement> settlements = new HashSet<>();

    @Builder
    private Calculate(LocalDateTime startTime,
                      LocalDateTime endTime,
                      CalculateStatus status,
                      UserGroup userGroup) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = (status != null) ? status : CalculateStatus.PENDING;
        this.userGroup = userGroup;
    }

    public void changeStatus(CalculateStatus status){
        this.status = status;
    }
}



