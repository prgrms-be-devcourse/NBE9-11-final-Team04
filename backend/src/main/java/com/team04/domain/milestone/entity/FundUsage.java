package com.team04.domain.milestone.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 아이디어 프로젝트의 자금 사용 내역을 저장하는 엔티티입니다. (Append Only) */
@Entity
@Table(name = "fund_usages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private LocalDate usedAt;

    @Builder
    private FundUsage(Long ideaId, String itemName, Long amount, LocalDate usedAt) {
        this.ideaId = ideaId;
        this.itemName = itemName;
        this.amount = amount;
        this.usedAt = usedAt;
    }
}