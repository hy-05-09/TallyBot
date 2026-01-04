package com.tallybot.backend.tallybot_back.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id", nullable = false)
    private Long settlementId; // 결제내역 식별 ID

    @Column(name = "place", nullable = false, length = 63)
    private String place;     // 결제 장소

    @Column(name = "item", nullable = false, length = 63)
    private String item;      // 결제 항목

    @Column(name = "amount", nullable = false)
    private int amount;       // 결제 총액

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup userGroup; // 소속 채팅방

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private Member payer; // 결제자

    // Participant 관계는 필요시에만 조회하도록 수정
    @OneToMany(mappedBy = "participantKey.settlement",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private Set<Participant> participants = new HashSet<>(); // 정산 대상자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculate_id", nullable = false)
    private Calculate calculate;

     public static Settlement create(UserGroup userGroup, Member payer, Calculate calculate,
                                    String place, String item, int amount) {
        Settlement s = new Settlement();
        s.userGroup = userGroup;
        s.payer = payer;
        s.calculate = calculate;
        s.place = place;
        s.item = item;
        s.amount = amount;
        return s;
    }

    public void correctPayer(Member correctedPayer) {
        this.payer = correctedPayer;
    }


    public void changePlace(String place) {
        this.place = place;
    }

    public void changeItem(String item) {
        this.item = item;
    }

    public void changeAmount(int amount) {
        this.amount = amount;
    }

    public void addParticipant(Participant participant) {
        this.participants.add(participant);
        participant.attachTo(this); // Participant 쪽에 메서드 추가 권장(아래 참고)
    }

    public void removeParticipant(Participant participant) {
        this.participants.remove(participant);
        participant.detach(); // orphanRemoval 고려
    }
}