package com.team04.domain.businessregistration.service;

import com.team04.domain.businessregistration.dto.response.BusinessRegistrationResponse;
import com.team04.domain.businessregistration.entity.BusinessRegistration;
import com.team04.domain.businessregistration.repository.BusinessRegistrationRepository;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessRegistrationWriter {

    private final BusinessRegistrationRepository businessRegistrationRepository;
    private final UserRepository userRepository;

    @Transactional
    public BusinessRegistrationResponse save(User user, String businessNumber) {
        BusinessRegistration registration = BusinessRegistration.create(user, businessNumber);
        registration.verify();
        try {
            businessRegistrationRepository.save(registration);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.BUSINESS_ALREADY_REGISTERED);
        }
        return new BusinessRegistrationResponse(
                registration.getBusinessNumber(),
                registration.isVerified(),
                registration.getVerifiedAt()
        );
    }
}
