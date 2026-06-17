package com.team04.domain.match.entity;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expert_match")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idea_id", nullable = false)
    private Long ideaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private ExpertProfile expertProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    // 제안자 도메인에서 매칭 요청 생성 시 사용
    public static ExpertMatch create(Long ideaId, ExpertProfile expertProfile) {
        ExpertMatch match = new ExpertMatch();
        match.ideaId = ideaId;
        match.expertProfile = expertProfile;
        return match;
    }

    // 수락
    public void accept() {
        validatePending();
        this.status = MatchStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    // 거절
    public void reject(String rejectReason) {
        validatePending();
        this.status = MatchStatus.REJECTED;
        this.rejectReason = rejectReason;
        this.respondedAt = LocalDateTime.now();
    }

    private void validatePending() {
        if (this.status != MatchStatus.PENDING) {
            throw new CustomException(ErrorCode.MATCH_ALREADY_RESPONDED);
        }
    }
}