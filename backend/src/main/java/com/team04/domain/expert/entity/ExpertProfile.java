package com.team04.domain.expert.entity;

import com.team04.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "expert_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification_type", nullable = false)
    private QualificationType qualificationType;

    @Column(name = "qualification_number", nullable = false)
    private String qualificationNumber;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpertStatus status = ExpertStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "tech_stack")
    private TechStack techStack;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(columnDefinition = "TEXT")
    private String career;

    @Column(name = "file_url")
    private String fileUrl;     // 국가자격증 증명 파일

    @Column(name = "start_date")
    private String startDate;           // 개업일자 (BUSINESS_REGISTRATION 재검증용)

    @Column(name = "representative_name")
    private String representativeName;  // 대표자명 (BUSINESS_REGISTRATION 재검증용)


    // /experts/verify 성공 시 생성 (verified=true)
    @Builder
    public ExpertProfile(User user, QualificationType qualificationType, String qualificationNumber) {
        this.user = user;
        this.qualificationType = qualificationType;
        this.qualificationNumber = qualificationNumber;
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
        this.status = ExpertStatus.ACTIVE;
    }

    // API 장애 시 보류 상태로 생성
    public static ExpertProfile ofPending(
            User user,
            QualificationType qualificationType,
            String qualificationNumber,
            String fileUrl,
            String startDate,
            String representativeName
    ) {
        ExpertProfile profile = new ExpertProfile();
        profile.user = user;
        profile.qualificationType = qualificationType;
        profile.qualificationNumber = qualificationNumber;
        profile.fileUrl = fileUrl;
        profile.startDate = startDate;
        profile.representativeName = representativeName;
        profile.verified = false;
        profile.status = ExpertStatus.PENDING_VERIFICATION;
        return profile;
    }

    // 검증 완료 처리 (배치에서 보류 → 완료 전환 시 사용)
    public void verify() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
        this.status = ExpertStatus.ACTIVE;
    }

    // /experts/profile 등록 시 추가 정보 업데이트
    public void updateProfile(TechStack techStack, String portfolioUrl, String career) {
        this.techStack = techStack;
        this.portfolioUrl = portfolioUrl;
        this.career = career;
    }
}
