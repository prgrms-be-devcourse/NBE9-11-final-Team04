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
import java.time.ZoneId;
import java.util.List;

/** 마일스톤 기한 초과 자동 처리 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilestoneScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MilestoneRepository milestoneRepository;
    private final MilestoneService milestoneService;

    /**
     * 매일 자정 실행 (KST 기준)
     * expectedDate가 오늘 이전이고 IN_PROGRESS 상태인 마일스톤을 감지해
     * cancelMilestone()을 호출하여 이행 중단 처리합니다.
     * cancelMilestone() 내부에서 환불 장부 + 후원자별 환불 레코드 생성까지 처리합니다.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void processOverdueMilestones() {
        log.info("마일스톤 기한 초과 배치 시작");

        List<Milestone> overdueMilestones = milestoneRepository
                .findByStatusAndExpectedDateBefore(MilestoneStatus.IN_PROGRESS, LocalDate.now(KST));

        for (Milestone milestone : overdueMilestones) {
            try {
                milestoneService.cancelMilestone(milestone.getIdeaId());
                log.info("마일스톤 기한 초과 처리 완료 - ideaId: {}, milestoneId: {}",
                        milestone.getIdeaId(), milestone.getId());
            } catch (Exception e) {
                log.error("마일스톤 기한 초과 처리 실패 - ideaId: {}, milestoneId: {}, error: {}",
                        milestone.getIdeaId(), milestone.getId(), e.getMessage());
            }
        }

        log.info("마일스톤 기한 초과 배치 종료");
    }
}