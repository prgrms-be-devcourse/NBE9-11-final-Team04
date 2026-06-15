package com.team04.domain.milestone.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "milestones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Milestone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Integer step;

    @Column(nullable = false)
    private String goal;

    @Column(columnDefinition = "TEXT")
    private String expectedResult;

    @Column
    private LocalDate expectedDate;

    @Column(nullable = false)
    private Long lockedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status;

    @Builder
    private Milestone(Long ideaId, Integer step, String goal,
                      String expectedResult, LocalDate expectedDate,
                      Long lockedAmount) {
        if (step < 1 || step > 3) {
            throw new IllegalArgumentException("마일스톤 단계는 1~3 사이여야 합니다");
        }
        this.ideaId = ideaId;
        this.step = step;
        this.goal = goal;
        this.expectedResult = expectedResult;
        this.expectedDate = expectedDate;
        this.lockedAmount = lockedAmount;
        this.status = MilestoneStatus.PENDING;
    }
}