package com.team04.domain.idea.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.QIdea;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** QueryDSL로 프로젝트 목록 조회와 키워드 검색 쿼리를 구현하는 레포지토리입니다. */
@RequiredArgsConstructor
public class IdeaRepositoryImpl implements IdeaRepositoryCustom {

    private static final String SORT_DEADLINE = "deadline";
    private static final int TOP_IDEA_LIMIT = 5;
    private static final int MIN_TRUST_SCORE = 80;


    private final JPAQueryFactory queryFactory;

    /** 조건에 맞는 공개 또는 진행 중 프로젝트를 Page로 조회하고 전체 건수를 계산합니다. */
    @Override
    public Page<Idea> searchProjects(
            IdeaCategory category,
            Boolean closingSoonOnly,
            String keyword,
            String sort,
            Pageable pageable
    ) {
        QIdea idea = QIdea.idea;
        List<Idea> ideas = queryFactory
                .selectFrom(idea)
                .where(
                        idea.deletedAt.isNull(),
                        idea.trustScore.goe(MIN_TRUST_SCORE),
                        idea.status.in(IdeaStatus.OPEN, IdeaStatus.IN_PROGRESS),
                        categoryEq(idea, category),
                        closingSoon(idea, closingSoonOnly),
                        titleContains(idea, keyword)
                )
                .orderBy(orderBy(idea, sort), idea.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(idea.count())
                .from(idea)
                .where(
                        idea.deletedAt.isNull(),
                        idea.trustScore.goe(MIN_TRUST_SCORE),
                        idea.status.in(IdeaStatus.OPEN, IdeaStatus.IN_PROGRESS),
                        categoryEq(idea, category),
                        closingSoon(idea, closingSoonOnly),
                        titleContains(idea, keyword)
                )
                .fetchOne();

        return new PageImpl<>(ideas, pageable, total == null ? 0L : total);
    }

    /** 신뢰도 80 이상 프로젝트를 가중치 합산 점수 내림차순으로 최대 5개 조회합니다. */
    @Override
    public List<Idea> findTop5PopularIdeas() {
        QIdea idea = QIdea.idea;
        QIdea subIdea = new QIdea("subIdea");
        NumberExpression<Double> achievementScore = idea.currentAmount
                .doubleValue()
                .divide(idea.goalAmount.doubleValue())
                .multiply(0.4);
        NumberTemplate<Double> sponsorScore = Expressions.numberTemplate(
                Double.class,
                "coalesce(({0} / nullif({1}, 0)) * 0.3, 0.0)",
                idea.sponsorCount.doubleValue(),
                JPAExpressions
                        .select(subIdea.sponsorCount.max())
                        .from(subIdea)
                        .where(subIdea.deletedAt.isNull())
        );
        NumberExpression<Double> trustScore = idea.trustScore
                .doubleValue()
                .divide(100.0)
                .multiply(0.3);

        return queryFactory
                .selectFrom(idea)
                .where(
                        idea.deletedAt.isNull(),
                        idea.trustScore.goe(MIN_TRUST_SCORE),
                        idea.status.in(IdeaStatus.OPEN, IdeaStatus.IN_PROGRESS)
                )
                .orderBy(achievementScore.add(sponsorScore).add(trustScore).desc(), idea.id.desc())
                .limit(TOP_IDEA_LIMIT)
                .fetch();
    }

    /** 카테고리 값이 있는 경우에만 카테고리 조건을 추가합니다. */
    private BooleanExpression categoryEq(QIdea idea, IdeaCategory category) {
        return category == null ? null : idea.category.eq(category);
    }

    /** 마감임박 필터가 켜진 경우 펀딩 마감 7일 이내 프로젝트 조건을 추가합니다. */
    private BooleanExpression closingSoon(QIdea idea, Boolean closingSoonOnly) {
        if (!Boolean.TRUE.equals(closingSoonOnly)) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return idea.fundingEndAt.between(now, now.plusDays(7));
    }

    /** 검색어가 있는 경우 프로젝트명 부분 일치 조건을 추가합니다. */
    private BooleanExpression titleContains(QIdea idea, String keyword) {
        return StringUtils.hasText(keyword) ? idea.title.containsIgnoreCase(keyword) : null;
    }

    /** 마감임박 정렬이면 펀딩 종료일 오름차순, 기본값은 최신순으로 정렬합니다. */
    private OrderSpecifier<?> orderBy(QIdea idea, String sort) {
        if (SORT_DEADLINE.equalsIgnoreCase(sort)) {
            return idea.fundingEndAt.asc();
        }
        return idea.createdAt.desc();
    }
    /** 펀딩 종료일이 지났고 현재 모금액이 목표액에 미달한 아이디어 ID 목록을 반환합니다. */
    @Override
    public List<Long> findFailedFundingIdeaIds(LocalDateTime now) {
        QIdea idea = QIdea.idea;
        return queryFactory
                .select(idea.id)
                .from(idea)
                .where(
                        idea.deletedAt.isNull(),
                        idea.status.eq(IdeaStatus.IN_PROGRESS),
                        idea.fundingEndAt.lt(now),
                        idea.currentAmount.lt(idea.goalAmount)
                )
                .fetch();
    }

    /** 모든 IdeaStatus 값에 대해 아이디어 건수를 0 포함 Map으로 반환합니다. */
    @Override
    public Map<IdeaStatus, Long> countByStatus() {
        QIdea idea = QIdea.idea;
        Map<IdeaStatus, Long> counts = queryFactory
                .select(idea.status, idea.count())
                .from(idea)
                .where(idea.deletedAt.isNull())
                .groupBy(idea.status)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(idea.status),
                        tuple -> tuple.get(idea.count()) == null ? 0L : tuple.get(idea.count())
                ));

        return Arrays.stream(IdeaStatus.values())
                .collect(Collectors.toMap(status -> status, status -> counts.getOrDefault(status, 0L)));
    }
}
