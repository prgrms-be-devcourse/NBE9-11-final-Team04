package com.team04.domain.expert.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team04.domain.expert.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.team04.domain.expert.entity.QExpertAppeal.expertAppeal;
import static com.team04.domain.expert.entity.QExpertProfile.expertProfile;

@Repository
@RequiredArgsConstructor
public class ExpertProfileRepositoryImpl implements ExpertProfileRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ExpertProfile> findActiveBusinessRegistrationProfiles(int offset, int limit) {
        return queryFactory
                .selectFrom(expertProfile)
                .join(expertProfile.user).fetchJoin()
                .where(
                        expertProfile.status.eq(ExpertStatus.ACTIVE),
                        expertProfile.qualificationType.eq(QualificationType.BUSINESS_REGISTRATION)
                )
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public List<ExpertProfile> findActiveNationalQualificationProfiles(int offset, int limit) {
        return queryFactory
                .selectFrom(expertProfile)
                .join(expertProfile.user).fetchJoin()
                .where(
                        expertProfile.status.eq(ExpertStatus.ACTIVE),
                        expertProfile.qualificationType.eq(QualificationType.NATIONAL_QUALIFICATION)
                )
                .offset(offset)
                .limit(limit)
                .fetch();
    }


    // appealCount = 0 → 소명 자료 미제출
    // appealCount > 0 이지만 관리자 미검토 → 보호 필요
    // 따라서 appeal이 SUBMITTED 상태로 존재하면 제외
    @Override
    public List<ExpertProfile> findExpiredSuspendedProfiles(LocalDateTime deadline, int limit) {
        return queryFactory
                .selectFrom(expertProfile)
                .join(expertProfile.user).fetchJoin()
                .where(
                        expertProfile.status.eq(ExpertStatus.SUSPENDED),
                        expertProfile.suspendedAt.before(deadline),
                        queryFactory
                                .selectOne()
                                .from(expertAppeal)
                                .where(
                                        expertAppeal.expertProfile.eq(expertProfile),
                                        expertAppeal.status.eq(AppealStatus.SUBMITTED)
                                )
                                .notExists()
                )
                .limit(limit)
                .fetch();
    }

    @Override
    public Page<ExpertProfile> findActiveProfiles(TechStack techStack, Pageable pageable) {

        BooleanBuilder where = new BooleanBuilder();
        where.and(expertProfile.status.eq(ExpertStatus.ACTIVE));

        // techStack 필터 (null이면 전체 조회)
        if (techStack != null) {
            where.and(expertProfile.techStack.eq(techStack));
        }

        List<ExpertProfile> content = queryFactory
                .selectFrom(expertProfile)
                .join(expertProfile.user).fetchJoin()
                .where(where)
                .orderBy(expertProfile.id.desc()) // 최신 등록순
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(expertProfile.count())
                .from(expertProfile)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}