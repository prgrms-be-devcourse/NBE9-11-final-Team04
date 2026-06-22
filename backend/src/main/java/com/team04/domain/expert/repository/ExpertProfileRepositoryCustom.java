package com.team04.domain.expert.repository;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.TechStack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpertProfileRepositoryCustom {

    // 전문가 전체 목록 조회 (ACTIVE, 페이징, techStack 필터)
    Page<ExpertProfile> findActiveProfiles(TechStack techStack, Pageable pageable);

    // 스케줄러용
    // offset 방식 (처리 후 status 변경되므로 안전)
    List<ExpertProfile> findActiveBusinessRegistrationProfiles(int offset, int limit);
    List<ExpertProfile> findActiveNationalQualificationProfiles(int offset, int limit);

    // 첫 페이지 반복 방식 (처리 후 조건에서 제외되므로)
    List<ExpertProfile> findExpiredSuspendedProfiles(LocalDateTime deadline, int limit);
}