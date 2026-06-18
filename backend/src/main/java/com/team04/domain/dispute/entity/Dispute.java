package com.team04.domain.dispute.entity;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.user.entity.User;
import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "disputes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dispute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    public Dispute(Idea idea, User reporter, User proposer, String reason, String evidenceUrl) {
        this.idea = idea;
        this.reporter = reporter;
        this.proposer = proposer;
        this.reason = reason;
        this.evidenceUrl = evidenceUrl;
        this.status = DisputeStatus.RECEIVED;
    }

    public void update(String reason, String evidenceUrl) {
        this.reason = reason;
        this.evidenceUrl = evidenceUrl;
    }

    public void updateStatus(DisputeStatus newStatus) {
        this.status = newStatus;
    }
}
