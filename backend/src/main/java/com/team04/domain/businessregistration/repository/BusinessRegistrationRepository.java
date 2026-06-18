package com.team04.domain.businessregistration.repository;

import com.team04.domain.businessregistration.entity.BusinessRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessRegistrationRepository extends JpaRepository<BusinessRegistration, Long> {
    boolean existsByUserId(Long userId);

    boolean existsByBusinessNumber(String businessNumber);

    Optional<BusinessRegistration> findByUserId(Long userId);
}
