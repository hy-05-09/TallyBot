package com.tallybot.backend.tallybot_back.config;

import com.tallybot.backend.tallybot_back.domain.*;
import com.tallybot.backend.tallybot_back.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Profile("mock-data")
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    // private final CalculateRepository calculateRepository;
    private final SettlementRepository settlementRepository;
    private final ParticipantRepository participantRepository;
    private final CalculateDetailRepository calculateDetailRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (groupRepository.count() > 0) return;

        // 1. 테스트 시나리오를 위한 id 설정
        UserGroup group = UserGroup.create(1L, "치킨모임");
        groupRepository.save(group);

        // 2. 멤버 생성
        Member m1 = Member.builder()
            .nickname("지우")
            .userGroup(group)
            .build();

        Member m2 = Member.builder()
            .nickname("현우")
            .userGroup(group)
            .build();

        Member m3 = Member.builder()
            .nickname("은비")
            .userGroup(group)
            .build();    
        memberRepository.saveAll(Set.of(m1, m2, m3));

        List<Member> members = List.of(m1, m2, m3);

        // 3개의 Calculate 생성
        createCalculateWithSettlements(group, members, CalculateStatus.COMPLETED, "COMPLETED");
        createCalculateWithSettlements(group, members, CalculateStatus.PENDING, "PENDING");
        createCalculateWithSettlements(group, members, CalculateStatus.CALCULATING, "CALCULATING");

        log.info("✅ Mock 데이터가 성공적으로 생성되었습니다.");
    }

    private void createCalculateWithSettlements(UserGroup group, List<Member> members, CalculateStatus status, String label) {
        // Calculate 생성
        Calculate calculate = Calculate.builder()
            .startTime(LocalDateTime.now().minusHours(2))
            .endTime(LocalDateTime.now())
            .status(status)
            .userGroup(group)
            .build();

        // Settlement 3개 생성
        for (int i = 0; i < 3; i++) {

            Settlement settlement = Settlement.create(
                group,
                members.get(i % members.size()),
                calculate,
                label + "_Place" + (i + 1),
                label + "_Item" + (i + 1),
                30000 + i * 5000
            );

            settlementRepository.save(settlement);

            // Participant 3명 등록
            for (Member member : members) {
                Participant participant = new Participant(); 
                Participant.ParticipantKey key = new Participant.ParticipantKey(settlement, member); 
                participant.setParticipantKey(key); 
                participant.setConstant(10000); 
                participant.setRatio(new Ratio(1));
                participantRepository.save(participant);
            }
        }

        // CalculateDetail 생성
        CalculateDetail detail = CalculateDetail.builder()
            .calculate(calculate)
            .payer(members.get(1))   // 현우
            .payee(members.get(0))   // 지우
            .amount(10000)
            .build();

        calculateDetailRepository.save(detail);
    }
}
