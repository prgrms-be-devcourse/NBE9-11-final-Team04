package com.team04.infra.batch;

import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** 마일스톤 기한 초과 자동 처리 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilestoneScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int OVERDUE_GRACE_DAYS = 7;

    private final MilestoneRepository milestoneRepository;
    private final MilestoneService milestoneService;

    /**
     * 매일 자정 실행 (KST 기준)
     *
     * [1단계] 기한 초과 마일스톤 감지 → overdueAt 기록 (즉시 cancel 하지 않음)
     * 소명 보고서 제출 시 overdueAt이 초기화되므로 3일 카운트가 리셋됩니다.
     *
     * [2단계] overdueAt 기준 3일 지난 마일스톤 감지 → 보증금 몰수 처리
     * 3일 안에 소명 보고서를 제출하지 않으면 먹튀/잠수로 판단합니다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void processOverdueMilestones() {
        log.info("마일스톤 기한 초과 배치 시작");

        LocalDateTime now = LocalDateTime.now(KST);

        // 1단계: 기한 초과 감지 → overdueAt 기록
        List<Milestone> newlyOverdue = milestoneRepository
                .findByStatusAndExpectedDateBeforeAndOverdueAtIsNull(MilestoneStatus.IN_PROGRESS, LocalDate.now(KST));

        for (Milestone milestone : newlyOverdue) {
            try {
                milestone.markOverdue(now);
                milestoneRepository.save(milestone);
                log.info("기한 초과 감지 - ideaId: {}, milestoneId: {}", milestone.getIdeaId(), milestone.getId());
            } catch (Exception e) {
                log.error("기한 초과 기록 실패 - ideaId: {}, milestoneId: {}, error: {}",
                        milestone.getIdeaId(), milestone.getId(), e.getMessage());
            }
        }

        // 2단계: 3일 유예기간 경과 → 보증금 몰수 처리
        LocalDateTime forfeitThreshold = now.minusDays(OVERDUE_GRACE_DAYS);
        List<Milestone> forfeitTargets = milestoneRepository
                .findByStatusAndOverdueAtBeforeAndOverdueAtIsNotNull(MilestoneStatus.IN_PROGRESS, forfeitThreshold);

        for (Milestone milestone : forfeitTargets) {
            try {
                milestoneService.forfeitMilestone(milestone.getIdeaId());
                log.info("보증금 몰수 처리 완료 - ideaId: {}, milestoneId: {}", milestone.getIdeaId(), milestone.getId());
            } catch (Exception e) {
                log.error("보증금 몰수 처리 실패 - ideaId: {}, milestoneId: {}, error: {}",
                        milestone.getIdeaId(), milestone.getId(), e.getMessage());
            }
        }

        log.info("마일스톤 기한 초과 배치 종료");
    }
}