package com.team04.domain.businessregistration.dto.response;

import java.time.LocalDateTime;

public record BusinessRegistrationResponse(
        String businessNumber,
        boolean verified,
        LocalDateTime verifiedAt
) {}

