// package com.tallybot.backend.tallybot_back.repository;

// import com.tallybot.backend.tallybot_back.domain.*;
// import config.DatabaseTestBase;
// import org.junit.jupiter.api.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.annotation.DirtiesContext;
// import org.springframework.test.annotation.Rollback;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.transaction.annotation.Transactional;

// // Spring Boot 3.x에서는 jakarta.persistence 사용
// import jakarta.persistence.EntityManager;
// import static org.assertj.core.api.Assertions.*;

// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;

// // 실제 테스트
// @SpringBootTest
// @ActiveProfiles("repository-test")  // 다른 프로파일 조합으로 별도 컨텍스트 생성
// @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// @Transactional
// @Rollback
// @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
// class CalculateDetailRepositoryTest extends DatabaseTestBase {

//     @Autowired
//     private CalculateDetailRepository calculateDetailRepository;

//     @Autowired
//     private CalculateRepository calculateRepository;

//     @Autowired
//     private MemberRepository memberRepository;

//     @Autowired
//     private GroupRepository groupRepository;

//     @Autowired
//     private EntityManager entityManager;

//     @BeforeEach
//     void setUp() {
//         // 외래키 제약조건 비활성화
//         entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

//         // 모든 테이블 데이터 삭제 (자식→부모 순서)
//         entityManager.createNativeQuery("DELETE FROM calculate_detail").executeUpdate();
//         entityManager.createNativeQuery("DELETE FROM participant").executeUpdate();
//         entityManager.createNativeQuery("DELETE FROM settlement").executeUpdate();
//         entityManager.createNativeQuery("DELETE FROM calculate").executeUpdate();
//         entityManager.createNativeQuery("DELETE FROM chat").executeUpdate();
//         entityManager.createNativeQuery("DELETE FROM member").executeUpdate();
//         entityManager.createNativeQuery("DELETE FROM user_group").executeUpdate();

//         // 외래키 제약조건 재활성화
//         entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

//         entityManager.flush();
//         entityManager.clear();
//     }

//     @Test
//     @DisplayName("calculateDetail 조회")
//     void saveAndCaculateDetailQuery() {
//         // given
//         List<UserGroup> userGroups = new ArrayList<>();
//         List<List<Member>> userGroupMembers = new ArrayList<>();
//         for(int i = 0; i < 2; i++) {
//             userGroups.add(null);
//             userGroupMembers.add(new ArrayList<>());
//         }

//         UserGroup group = UserGroup.create(8810L, "새로운 톡방");

//         userGroups.set(0, groupRepository.save(group));

//         group = UserGroup.create(4042L, "2019 17기 Withme");

//         userGroups.set(1, groupRepository.save(group));

//         Member member = Member.builder()
//                 .userGroup(userGroups.get(0))
//                 .nickname("철수")
//                 .build();

//         userGroupMembers.get(0).add(memberRepository.save(member));

//         member = Member.builder()
//                 .userGroup(userGroups.get(0))
//                 .nickname("영희")
//                 .build();

//         userGroupMembers.get(0).add(memberRepository.save(member));

//         member = Member.builder()
//                 .userGroup(userGroups.get(1))
//                 .nickname("철수")
//                 .build();

//         userGroupMembers.get(1).add(memberRepository.save(member));

//         member = Member.builder()
//                 .userGroup(userGroups.get(1))
//                 .nickname("민경")
//                 .build();

//         userGroupMembers.get(1).add(memberRepository.save(member));

//         member = Member.builder()
//                 .userGroup(userGroups.get(1))
//                 .nickname("정현")
//                 .build();

//         userGroupMembers.get(1).add(memberRepository.save(member));

//         LocalDateTime startTime = LocalDateTime.of(2024, 11, 8, 13, 0);
//         LocalDateTime endTime = LocalDateTime.of(2024, 11, 9, 13, 0);

//         Calculate cal0 = Calculate.builder()
//                 .startTime(startTime)
//                 .endTime(endTime)
//                 .status(CalculateStatus.CALCULATING)
//                 .userGroup(userGroups.get(0))
//                 .build();
//         calculateRepository.save(cal0);

//         Calculate cal1 = Calculate.builder()
//                 .startTime(startTime)
//                 .endTime(endTime)
//                 .status(CalculateStatus.PENDING)
//                 .userGroup(userGroups.get(1))
//                 .build();
//         calculateRepository.save(cal1);

//         CalculateDetail cd0 = CalculateDetail.builder()
//                 .amount(64050)
//                 .calculate(cal0)
//                 .payer(userGroupMembers.get(0).get(0))
//                 .payee(userGroupMembers.get(0).get(1))
//                 .build();

//         calculateDetailRepository.save(cd0);

//         CalculateDetail cd1 = CalculateDetail.builder()
//                 .amount(34050)
//                 .calculate(cal1)
//                 .payer(userGroupMembers.get(1).get(1))
//                 .payee(userGroupMembers.get(1).get(0))
//                 .build();

//         CalculateDetail cd2 = CalculateDetail.builder()
//                 .amount(4940)
//                 .calculate(cal1)
//                 .payer(userGroupMembers.get(1).get(1))
//                 .payee(userGroupMembers.get(1).get(2))
//                 .build();
//         calculateDetailRepository.saveAll(List.of(cd1, cd2));

//         // when
//         List<CalculateDetail> cds00 = calculateDetailRepository.findAllByCalculate(cal0);
//         List<CalculateDetail> cds01 = calculateDetailRepository.findAllByCalculate(cal1);
//         List<CalculateDetail> cds02 = calculateDetailRepository.findAllByCalculate_CalculateId(cal0.getCalculateId() + 8081);

//         calculateDetailRepository.deleteByCalculate(cal1);
//         calculateDetailRepository.deleteByCalculate_CalculateId(cal0.getCalculateId() + 8081);

//         List<CalculateDetail> cds10 = calculateDetailRepository.findAllByCalculate(cal0);
//         List<CalculateDetail> cds11 = calculateDetailRepository.findAllByCalculate(cal1);
//         List<CalculateDetail> cds12 = calculateDetailRepository.findAllByCalculate_CalculateId(cal0.getCalculateId() + 8081);

//         // then
//         assertThat(cds00).contains(cd0).hasSize(1);
//         assertThat(cds01).contains(cd1).contains(cd2).hasSize(2);
//         assertThat(cds02).isEmpty();

//         assertThat(cds10).contains(cd0).hasSize(1);
//         assertThat(cds11).isEmpty();
//         assertThat(cds12).isEmpty();
//     }
// }

package com.tallybot.backend.tallybot_back.repository;

import com.tallybot.backend.tallybot_back.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;



@DataJpaTest
@ActiveProfiles("repository-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback
@Tag("slow")
class CalculateDetailRepositoryTest {

    @Autowired private CalculateDetailRepository calculateDetailRepository;
    @Autowired private CalculateRepository calculateRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private EntityManager em;

    @BeforeEach
    void clean() {
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        em.createNativeQuery("DELETE FROM calculate_detail").executeUpdate();
        em.createNativeQuery("DELETE FROM participant").executeUpdate();
        em.createNativeQuery("DELETE FROM settlement").executeUpdate();
        em.createNativeQuery("DELETE FROM calculate").executeUpdate();
        em.createNativeQuery("DELETE FROM chat").executeUpdate();
        em.createNativeQuery("DELETE FROM member").executeUpdate();
        em.createNativeQuery("DELETE FROM user_group").executeUpdate();
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("CalculateDetail 조회/삭제 쿼리 메서드 검증")
    void calculateDetail_queries() {
        UserGroup g0 = groupRepository.save(UserGroup.create(8810L, "새로운 톡방"));
        UserGroup g1 = groupRepository.save(UserGroup.create(4042L, "2019 17기 Withme"));

        Member g0_m0 = memberRepository.save(Member.builder().userGroup(g0).nickname("철수").build());
        Member g0_m1 = memberRepository.save(Member.builder().userGroup(g0).nickname("영희").build());

        Member g1_m0 = memberRepository.save(Member.builder().userGroup(g1).nickname("철수").build());
        Member g1_m1 = memberRepository.save(Member.builder().userGroup(g1).nickname("민경").build());
        Member g1_m2 = memberRepository.save(Member.builder().userGroup(g1).nickname("정현").build());

        LocalDateTime startTime = LocalDateTime.of(2024, 11, 8, 13, 0);
        LocalDateTime endTime   = LocalDateTime.of(2024, 11, 9, 13, 0);

        Calculate cal0 = calculateRepository.save(Calculate.builder()
                .startTime(startTime).endTime(endTime)
                .status(CalculateStatus.CALCULATING)
                .userGroup(g0)
                .build());

        Calculate cal1 = calculateRepository.save(Calculate.builder()
                .startTime(startTime).endTime(endTime)
                .status(CalculateStatus.PENDING)
                .userGroup(g1)
                .build());

        calculateDetailRepository.save(CalculateDetail.builder()
                .amount(64050).calculate(cal0).payer(g0_m0).payee(g0_m1).build());

        calculateDetailRepository.saveAll(List.of(
                CalculateDetail.builder().amount(34050).calculate(cal1).payer(g1_m1).payee(g1_m0).build(),
                CalculateDetail.builder().amount(4940).calculate(cal1).payer(g1_m1).payee(g1_m2).build()
        ));

        em.flush();
        em.clear();

        Long notExistId = cal0.getCalculateId() + 8081;

        // 조회 검증
        assertThat(calculateDetailRepository.findAllByCalculate(cal0))
                .hasSize(1)
                .extracting(CalculateDetail::getAmount)
                .containsExactly(64050);

        assertThat(calculateDetailRepository.findAllByCalculate(cal1))
                .hasSize(2)
                .extracting(CalculateDetail::getAmount)
                .containsExactlyInAnyOrder(34050, 4940);

        assertThat(calculateDetailRepository.findAllByCalculate_CalculateId(notExistId)).isEmpty();

        // 삭제 검증
        calculateDetailRepository.deleteByCalculate(cal1);
        em.flush();
        em.clear();

        assertThat(calculateDetailRepository.findAllByCalculate(cal1)).isEmpty();
        assertThat(calculateDetailRepository.findAllByCalculate(cal0)).hasSize(1);

        calculateDetailRepository.deleteByCalculate_CalculateId(cal0.getCalculateId());
        em.flush();
        em.clear();

        assertThat(calculateDetailRepository.findAllByCalculate(cal0)).isEmpty();
    }
}
