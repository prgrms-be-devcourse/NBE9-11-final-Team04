package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

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
}
