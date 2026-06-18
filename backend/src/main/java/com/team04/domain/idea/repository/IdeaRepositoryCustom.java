package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import java.time.LocalDateTime;
import java.util.List;

/** QueryDSL 기반 프로젝트 목록과 검색 조회 기능을 정의하는 레포지토리 인터페이스입니다. */
public interface IdeaRepositoryCustom {

    /** 카테고리, 마감임박 여부, 검색어, 정렬 조건으로 프로젝트 목록을 Slice로 조회합니다. */
    Slice<Idea> searchProjects(
            IdeaCategory category,
            Boolean closingSoonOnly,
            String keyword,
            String sort,
            Pageable pageable
    );

    /** 펀딩 마감됐고 목표 금액 미달성인 아이디어 ID 목록을 조회합니다. */
    List<Long> findFailedFundingIdeaIds(LocalDateTime now);
}
