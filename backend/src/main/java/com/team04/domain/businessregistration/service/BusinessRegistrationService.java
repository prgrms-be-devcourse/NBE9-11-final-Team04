package com.team04.domain.businessregistration.service;

import com.team04.domain.businessregistration.client.BusinessVerifyClient;
import com.team04.domain.businessregistration.dto.request.BusinessRegistrationRequest;
import com.team04.domain.businessregistration.dto.response.BusinessRegistrationResponse;
import com.team04.domain.businessregistration.entity.BusinessRegistration;
import com.team04.domain.businessregistration.repository.BusinessRegistrationRepository;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessRegistrationService {

    private final BusinessRegistrationRepository businessRegistrationRepository;
    private final BusinessVerifyClient businessVerifyClient;
    private final UserRepository userRepository;
    private final BusinessRegistrationWriter businessRegistrationWriter;

    @Transactional(readOnly = true)
    public BusinessRegistrationResponse getMyBusiness(Long userId) {
        BusinessRegistration registration = businessRegistrationRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUSINESS_REGISTRATION_NOT_FOUND));
        return new BusinessRegistrationResponse(
                registration.getBusinessNumber(),
                registration.isVerified(),
                registration.getVerifiedAt()
        );
    }

    @Transactional
    public void deleteMyBusiness(Long userId) {
        BusinessRegistration registration = businessRegistrationRepository
                .findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.BUSINESS_REGISTRATION_NOT_FOUND));
        businessRegistrationRepository.delete(registration);
    }

    public BusinessRegistrationResponse register(Long userId, BusinessRegistrationRequest request) {
        // 1. 사전 검증 (각 레포 메서드가 자체 트랜잭션)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
        }
        if (businessRegistrationRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.BUSINESS_ALREADY_REGISTERED);
        }
        if (businessRegistrationRepository.existsByBusinessNumber(request.businessNumber())) {
            throw new CustomException(ErrorCode.BUSINESS_ALREADY_REGISTERED);
        }

        // 2. 외부 API 호출 (트랜잭션 없음 — DB 커넥션 점유 방지)
        boolean verified = businessVerifyClient.verify(
                request.businessNumber(),
                request.representativeName(),
                request.openDate()
        );
        if (!verified) {
            throw new CustomException(ErrorCode.BUSINESS_VERIFICATION_FAILED);
        }

        // 3. 검증 성공 후 저장 (별도 트랜잭션)
        return businessRegistrationWriter.save(userId, request.businessNumber());
    }
}
