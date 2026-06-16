package com.team04.domain.businessregistration.service;

import com.team04.domain.businessregistration.dto.request.BusinessRegistrationRequest;
import com.team04.domain.businessregistration.dto.response.BusinessRegistrationResponse;
import com.team04.domain.businessregistration.entity.BusinessRegistration;
import com.team04.domain.businessregistration.repository.BusinessRegistrationRepository;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.infra.external.government.BusinessVerificationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessRegistrationService {

    private final BusinessRegistrationRepository businessRegistrationRepository;
    private final BusinessVerificationClient businessVerificationClient;
    private final UserRepository userRepository;

    @Transactional
    public BusinessRegistrationResponse register(Long userId, BusinessRegistrationRequest request){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(user.getStatus()== UserStatus.SUSPENDED){
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if(user.getStatus()== UserStatus.WITHDRAWN){
            throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
        }

        if (businessRegistrationRepository.existsByBusinessNumber(request.businessNumber())) {
            throw new CustomException(ErrorCode.BUSINESS_ALREADY_REGISTERED);
        }

        BusinessRegistration registration = BusinessRegistration.create(user, request.businessNumber());

        boolean verified = businessVerificationClient.verify(
                request.businessNumber(),
                request.representativeName(),
                request.openDate()
        );

        if (!verified) {
            throw new CustomException(ErrorCode.BUSINESS_VERIFICATION_FAILED);
        }

        registration.verify();
        businessRegistrationRepository.save(registration);

        return new BusinessRegistrationResponse(
                registration.getBusinessNumber(),
                registration.isVerified(),
                registration.getVerifiedAt()
        );
    }
}
