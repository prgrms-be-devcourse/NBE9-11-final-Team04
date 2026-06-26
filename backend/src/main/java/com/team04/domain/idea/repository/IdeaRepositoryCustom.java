package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** QueryDSL 기반 프로젝트 목록과 검색 조회 기능을 정의하는 레포지토리 인터페이스입니다. */
public interface IdeaRepositoryCustom {

    /** 카테고리, 마감임박 여부, 검색어, 정렬 조건으로 프로젝트 목록을 Page로 조회합니다. */
    Page<Idea> searchProjects(
            IdeaCategory category,
            Boolean closingSoonOnly,
            String keyword,
            String sort,
            Pageable pageable
    );

    /** 인기 프로젝트 점수 기준 상위 5개 아이디어를 조회합니다. */
    List<Idea> findTop5PopularIdeas();

    /** 펀딩 마감됐고 목표 금액 미달성인 아이디어 ID 목록을 조회합니다. */
    List<Long> findFailedFundingIdeaIds(LocalDateTime now);

    /** 전체 아이디어 상태별 건수를 조회합니다. */
    Map<IdeaStatus, Long> countByStatus();
}
