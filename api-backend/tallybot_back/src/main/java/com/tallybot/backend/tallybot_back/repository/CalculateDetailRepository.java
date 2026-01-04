package com.tallybot.backend.tallybot_back.repository;

import com.tallybot.backend.tallybot_back.domain.Calculate;
import com.tallybot.backend.tallybot_back.domain.CalculateDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalculateDetailRepository extends JpaRepository<CalculateDetail, Long> {

    List<CalculateDetail> findAllByCalculate(Calculate calculate);
    List<CalculateDetail> findAllByCalculate_CalculateId(Long calculateId);

    void deleteByCalculate(Calculate calculate);
    void deleteByCalculate_CalculateId(Long calculateId);


}
