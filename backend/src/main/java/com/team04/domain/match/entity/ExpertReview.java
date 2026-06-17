package com.team04.domain.match.entity;

import com.team04.domain.expert.entity.ExpertProfile;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idea_id", nullable = false)
    private Long ideaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private ExpertProfile expertProfile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private ExpertMatch expertMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Feasibility feasibility;

    @Column(name = "expected_period")
    private String expectedPeriod;

    @Column(name = "tech_stack", columnDefinition = "TEXT")
    private String techStack;

    @Column(name = "risk_factor", columnDefinition = "TEXT")
    private String riskFactor;

    @Column(columnDefinition = "TEXT")
    private String opinion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static ExpertReview create(
            ExpertProfile expertProfile,
            ExpertMatch expertMatch,
            Feasibility feasibility,
            String expectedPeriod,
            String techStack,
            String riskFactor,
            String opinion
    ) {
        ExpertReview review = new ExpertReview();
        review.ideaId = expertMatch.getIdeaId();    // 매칭에서 ideaId 추출
        review.expertProfile = expertProfile;
        review.expertMatch = expertMatch;
        review.feasibility = feasibility;
        review.expectedPeriod = expectedPeriod;
        review.techStack = techStack;
        review.riskFactor = riskFactor;
        review.opinion = opinion;
        return review;
    }
}