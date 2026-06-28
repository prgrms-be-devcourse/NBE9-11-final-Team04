package com.team04.domain.idea.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 아이디어 정식 등록 전 사용자가 작성 중인 임시저장 내용을 보관하는 엔티티입니다. */
@Entity
@Table(name = "idea_draft")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdeaDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private IdeaCategory category;

    @Column(length = 200)
    private String oneLineIntro;

    @Column(columnDefinition = "TEXT")
    private String problemDefinition;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Column(columnDefinition = "TEXT")
    private String targetCustomer;

    @Column(columnDefinition = "TEXT")
    private String competitor;

    @Column(columnDefinition = "TEXT")
    private String teamIntro;

    private Long goalAmount;

    private Long depositAmount;

    private LocalDateTime fundingStartAt;

    private LocalDateTime fundingEndAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RewardType rewardType;

    @Column(length = 2048)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String imageUrls;

    @Column(columnDefinition = "TEXT")
    private String milestones;

    /** 신규 임시저장을 작성자 식별자와 요청 내용으로 생성합니다. */
    public IdeaDraft(Long userId) {
        this.userId = userId;
    }

    /** 요청한 사용자가 임시저장 작성자인지 검증합니다. */
    public void validateOwner(Long requestUserId) {
        if (!this.userId.equals(requestUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    /** 임시저장 내용을 최신 입력값으로 교체합니다. */
    public void update(
            String title,
            IdeaCategory category,
            String oneLineIntro,
            String problemDefinition,
            String solution,
            String goal,
            String targetCustomer,
            String competitor,
            String teamIntro,
            Long goalAmount,
            Long depositAmount,
            LocalDateTime fundingStartAt,
            LocalDateTime fundingEndAt,
            RewardType rewardType,
            String imageUrl
    ) {
        this.title = title;
        this.category = category;
        this.oneLineIntro = oneLineIntro;
        this.problemDefinition = problemDefinition;
        this.solution = solution;
        this.goal = goal;
        this.targetCustomer = targetCustomer;
        this.competitor = competitor;
        this.teamIntro = teamIntro;
        this.goalAmount = goalAmount;
        this.depositAmount = depositAmount;
        this.fundingStartAt = fundingStartAt;
        this.fundingEndAt = fundingEndAt;
        this.rewardType = rewardType;
        this.imageUrl = imageUrl;
    }

    /** 임시저장 본문 이미지 URL 목록 문자열을 변경합니다. */
    public void updateImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    /** 임시저장 마일스톤 목록 JSON 문자열을 변경합니다. */
    public void updateMilestones(String milestones) {
        this.milestones = milestones;
    }
}
