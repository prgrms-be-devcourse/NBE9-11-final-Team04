package com.team04.domain.expert.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "expert_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
    @ManyToOne(fetch = FetchType.LAZY)  // 아이디어 재검토 시나리오에 의해 반려 후 다시 등록할 경우 여러 개의 리뷰가 달릴 수 있음
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea ideaId;    // TODO : Idea 도메인 merge 되면 정상 동작 확인 필요
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private ExpertProfile expertProfile;

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
}