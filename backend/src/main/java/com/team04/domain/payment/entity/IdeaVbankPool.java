package com.team04.domain.payment.entity;

import com.team04.global.entity.BaseEntity;
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

/** 아이디어(캠페인)당 1개의 가상계좌 풀 — Mock PG에서 후원자들이 동일 계좌로 입금 */
@Entity
@Table(name = "idea_vbank_pool")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdeaVbankPool extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long ideaId;

    /** PG 조회·웹훅용 풀 대표 orderId */
    @Column(nullable = false, unique = true)
    private String poolOrderId;

    @Column(nullable = false)
    private Long virtualAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdeaVbankPoolStatus status;

    public static IdeaVbankPool createActive(Long ideaId, String poolOrderId, Long virtualAccountId) {
        IdeaVbankPool pool = new IdeaVbankPool();
        pool.ideaId = ideaId;
        pool.poolOrderId = poolOrderId;
        pool.virtualAccountId = virtualAccountId;
        pool.status = IdeaVbankPoolStatus.ACTIVE;
        return pool;
    }

    public void close() {
        this.status = IdeaVbankPoolStatus.CLOSED;
    }
}
