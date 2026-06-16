package com.team04.domain.businessregistration.repository;

import com.team04.domain.businessregistration.entity.BusinessRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRegistrationRepository extends JpaRepository<BusinessRegistration, Long> {
    boolean existsByUserId(Long userId);

    boolean existsByBusinessNumber(String businessNumber);
}
