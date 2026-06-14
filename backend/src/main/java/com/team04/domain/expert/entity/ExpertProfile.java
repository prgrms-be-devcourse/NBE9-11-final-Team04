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
    private ExpertStatus status = ExpertStatus.ACTIVE;

    @Builder
    public ExpertProfile(User user, QualificationType qualificationType, String qualificationNumber) {
        this.user = user;
        this.qualificationType = qualificationType;
        this.qualificationNumber = qualificationNumber;
    }
}
