package com.team04.domain.idea.dto.response;

import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.verification.dto.response.VerificationResponse;

import java.util.List;

public record AdminIdeaReviewSummary(
        IdeaResponse idea,
        VerificationResponse verification,
        List<ExpertReviewResponse> expertReviews
) {}