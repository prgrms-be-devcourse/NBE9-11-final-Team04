package com.team04.domain.expert.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team04.domain.expert.entity.AppealStatus;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;
import lombok.RequiredArgsConstructor;
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
    public List<ExpertProfile> findActiveBusinessRegistrationProfiles() {
        return queryFactory
                .selectFrom(expertProfile)
                .join(expertProfile.user).fetchJoin()
                .where(
                        expertProfile.status.eq(ExpertStatus.ACTIVE),
                        expertProfile.qualificationType.eq(QualificationType.BUSINESS_REGISTRATION)
                )
                .fetch();
    }

    @Override
    public List<ExpertProfile> findActiveNationalQualificationProfiles() {
        return queryFactory
                .selectFrom(expertProfile)
                .join(expertProfile.user).fetchJoin()
                .where(
                        expertProfile.status.eq(ExpertStatus.ACTIVE),
                        expertProfile.qualificationType.eq(QualificationType.NATIONAL_QUALIFICATION)
                )
                .fetch();
    }


    // appealCount = 0 → 소명 자료 미제출
    // appealCount > 0 이지만 관리자 미검토 → 보호 필요
    // 따라서 appeal이 SUBMITTED 상태로 존재하면 제외
    @Override
    public List<ExpertProfile> findExpiredSuspendedProfiles(LocalDateTime deadline) {
        return queryFactory
                .selectFrom(expertProfile)
                .join(expertProfile.user).fetchJoin()
                .where(
                        expertProfile.status.eq(ExpertStatus.SUSPENDED),
                        expertProfile.suspendedAt.before(deadline),
                        // 소명 자료가 SUBMITTED 상태로 존재하는 경우 제외
                        queryFactory
                                .selectOne()
                                .from(expertAppeal)
                                .where(
                                        expertAppeal.expertProfile.eq(expertProfile),
                                        expertAppeal.status.eq(AppealStatus.SUBMITTED)
                                )
                                .notExists()
                )
                .fetch();
    }
}