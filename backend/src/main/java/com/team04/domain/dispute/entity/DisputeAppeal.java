package com.team04.domain.dispute.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "dispute_appeals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DisputeAppeal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false, unique = true)
    private Dispute dispute;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column
    private String fileUrl;

    @Column(nullable = false)
    private int appealCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppealStatus status = AppealStatus.SUBMITTED;

    private static final int MAX_APPEAL_COUNT = 3;

    public DisputeAppeal(Dispute dispute, String content, String fileUrl) {
        this.dispute = dispute;
        this.content = content;
        this.fileUrl = fileUrl;
    }

    public void update(String content, String fileUrl) {
        if (appealCount >= MAX_APPEAL_COUNT) {
            throw new CustomException(ErrorCode.DISPUTE_APPEAL_LIMIT_EXCEEDED);
        }
        this.content = content;
        this.fileUrl = fileUrl;
        this.status = AppealStatus.SUBMITTED;
        this.appealCount++;
    }

    public void approve() {
        this.status = AppealStatus.APPROVED;
    }

    public void reject() {
        this.status = AppealStatus.REJECTED;
    }
}
