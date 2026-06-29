package com.team04.domain.expert.entity;

import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ExpertProfileTest {

    private User user() {
        return User.create("expert@test.com", "password", "김전문", "expert_kim", 40, Role.EXPERT);
    }

    private ExpertProfile activeProfile() {
        return ExpertProfile.builder()
                .user(user())
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build();
    }

    @Test
    @DisplayName("빌더로 생성된 프로필은 verified=true, status=ACTIVE 상태이다")
    void builder_생성시_verified_true_ACTIVE() {
        ExpertProfile profile = activeProfile();

        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.ACTIVE);
        assertThat(profile.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("ofPending으로 생성된 프로필은 verified=false, status=PENDING_VERIFICATION 상태이다")
    void ofPending_생성시_verified_false_PENDING_VERIFICATION() {
        ExpertProfile profile = ExpertProfile.ofPending(
                user(),
                QualificationType.NATIONAL_QUALIFICATION,
                "Q123456789",
                "https://file.url",
                null,
                null
        );

        assertThat(profile.isVerified()).isFalse();
        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("suspend 호출 시 status=SUSPENDED, suspendedAt이 설정된다")
    void suspend_호출시_SUSPENDED_상태() {
        ExpertProfile profile = activeProfile();

        profile.suspend();

        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.SUSPENDED);
        assertThat(profile.getSuspendedAt()).isNotNull();
    }

    @Test
    @DisplayName("restore 호출 시 status=ACTIVE, suspendedAt=null, appealCount=0으로 초기화된다")
    void restore_호출시_ACTIVE_상태_초기화() {
        ExpertProfile profile = activeProfile();
        profile.suspend();
        profile.increaseAppealCount();

        profile.restore();

        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.ACTIVE);
        assertThat(profile.getSuspendedAt()).isNull();
        assertThat(profile.getAppealCount()).isZero();
    }

    @Test
    @DisplayName("demote 호출 시 status=DEMOTED 상태이다")
    void demote_호출시_DEMOTED_상태() {
        ExpertProfile profile = activeProfile();

        profile.demote();

        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.DEMOTED);
    }

    @Test
    @DisplayName("verify 호출 시 verified=true, status=ACTIVE 상태로 전환된다")
    void verify_호출시_verified_true_ACTIVE() {
        ExpertProfile profile = ExpertProfile.ofPending(
                user(),
                QualificationType.NATIONAL_QUALIFICATION,
                "Q123456789",
                "https://file.url",
                null,
                null
        );

        profile.verify();

        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.ACTIVE);
        assertThat(profile.getVerifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("increaseAppealCount 호출 시 appealCount가 1 증가한다")
    void increaseAppealCount_호출시_1증가() {
        ExpertProfile profile = activeProfile();

        profile.increaseAppealCount();
        profile.increaseAppealCount();

        assertThat(profile.getAppealCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateProfile 호출 시 techStack, portfolioUrl, career가 업데이트된다")
    void updateProfile_호출시_정보업데이트() {
        ExpertProfile profile = activeProfile();

        profile.updateProfile(TechStack.TECH, "https://portfolio.url", "10년 경력");

        assertThat(profile.getTechStack()).isEqualTo(TechStack.TECH);
        assertThat(profile.getPortfolioUrl()).isEqualTo("https://portfolio.url");
        assertThat(profile.getCareer()).isEqualTo("10년 경력");
    }
}