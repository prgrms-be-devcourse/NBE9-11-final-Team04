package com.team04.domain.dispute.entity;

import com.team04.domain.user.entity.User;
import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "disputes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dispute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id", nullable = false)
    private User reported;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    @OneToOne(mappedBy = "dispute", fetch = FetchType.LAZY)
    private DisputeAppeal appeal;

    public Dispute(User reporter, User reported, TargetType targetType, Long targetId,
                   DisputeCategory category, String title, String reason, String evidenceUrl) {
        this.reporter = reporter;
        this.reported = reported;
        this.targetType = targetType;
        this.targetId = targetId;
        this.category = category;
        this.title = title;
        this.reason = reason;
        this.evidenceUrl = evidenceUrl;
        this.status = DisputeStatus.RECEIVED;
    }

    public void updateStatus(DisputeStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new CustomException(ErrorCode.DISPUTE_INVALID_STATUS_TRANSITION);
        }
        this.status = newStatus;
    }

    private static final int APPEAL_DEADLINE_DAYS = 7;

    public boolean isAppealable() {
        return this.status == DisputeStatus.RECEIVED
                && getCreatedAt() != null
                && getCreatedAt().plusDays(APPEAL_DEADLINE_DAYS).isAfter(LocalDateTime.now());
    }

}
