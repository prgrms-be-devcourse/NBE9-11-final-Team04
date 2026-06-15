package com.team04.domain.idea.event;

import com.team04.domain.idea.dto.request.CreateMilestoneRequest;

import java.util.List;

/** 아이디어 등록 완료 시 마일스톤 생성을 요청하는 이벤트입니다. */
public record IdeaCreatedEvent(
        Long ideaId,
        List<CreateMilestoneRequest> milestones
) {
}
