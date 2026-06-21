package com.team04.domain.expert.repository;

import com.team04.domain.expert.entity.ExpertProfile;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpertProfileRepositoryCustom {
    List<ExpertProfile> findActiveBusinessRegistrationProfiles();
    List<ExpertProfile> findActiveNationalQualificationProfiles();
    List<ExpertProfile> findExpiredSuspendedProfiles(LocalDateTime deadline);
}