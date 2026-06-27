package com.team04.domain.verification.service;

import com.team04.domain.businessregistration.repository.BusinessRegistrationRepository;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.repository.DisputeRepository;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProposerHistoryScoreCalculatorTest {

    @Mock private IdeaRepository ideaRepository;
    @Mock private DisputeRepository disputeRepository;
    @Mock private BusinessRegistrationRepository businessRegistrationRepository;

    @InjectMocks
    private ProposerHistoryScoreCalculator calculator;

    private void givenHistory(boolean hasBusinessReg, long successCount, long disputeCount) {
        given(businessRegistrationRepository.existsByUserId(1L)).willReturn(hasBusinessReg);
        given(ideaRepository.countByUserIdAndStatusAndDeletedAtIsNull(1L, IdeaStatus.COMPLETED))
                .willReturn(successCount);
        given(disputeRepository.countByReportedIdAndStatus(1L, DisputeStatus.RESOLVED))
                .willReturn(disputeCount);
    }

    // ─────────────────────────────────────────────
    // 기본 점수
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("아무 이력 없는 신규 제안자는 기본 10점")
    void calculate_신규제안자_기본10점() {
        givenHistory(false, 0, 0);

        assertThat(calculator.calculate(1L)).isEqualTo(10);
    }

    // ─────────────────────────────────────────────
    // 사업자 등록
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("사업자 등록 인증 시 +5점")
    void calculate_사업자등록_15점() {
        givenHistory(true, 0, 0);

        assertThat(calculator.calculate(1L)).isEqualTo(15);
    }

    // ─────────────────────────────────────────────
    // 프로젝트 성공 이력
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("성공 프로젝트 1건 → 15점")
    void calculate_성공1건_15점() {
        givenHistory(false, 1, 0);

        assertThat(calculator.calculate(1L)).isEqualTo(15);
    }

    @Test
    @DisplayName("성공 프로젝트 2건 → 20점")
    void calculate_성공2건_20점() {
        givenHistory(false, 2, 0);

        assertThat(calculator.calculate(1L)).isEqualTo(20);
    }

    @Test
    @DisplayName("성공 프로젝트 3건 이상 → 20점 상한 적용")
    void calculate_성공3건_상한20점() {
        givenHistory(false, 3, 0);

        assertThat(calculator.calculate(1L)).isEqualTo(20);
    }

    // ─────────────────────────────────────────────
    // 분쟁 패소 이력
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("RESOLVED 분쟁 1건 → 5점")
    void calculate_분쟁1건_5점() {
        givenHistory(false, 0, 1);

        assertThat(calculator.calculate(1L)).isEqualTo(5);
    }

    @Test
    @DisplayName("RESOLVED 분쟁 2건 → 0점")
    void calculate_분쟁2건_0점() {
        givenHistory(false, 0, 2);

        assertThat(calculator.calculate(1L)).isEqualTo(0);
    }

    @Test
    @DisplayName("RESOLVED 분쟁 3건 이상 → 0점 하한 적용")
    void calculate_분쟁3건_하한0점() {
        givenHistory(false, 0, 3);

        assertThat(calculator.calculate(1L)).isEqualTo(0);
    }

    // ─────────────────────────────────────────────
    // 복합 시나리오
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("사업자 등록 + 성공 1건 → 20점")
    void calculate_사업자_성공1건_20점() {
        givenHistory(true, 1, 0);

        assertThat(calculator.calculate(1L)).isEqualTo(20);
    }

    @Test
    @DisplayName("성공 1건 + 분쟁 1건 → 10점")
    void calculate_성공1건_분쟁1건_10점() {
        givenHistory(false, 1, 1);

        assertThat(calculator.calculate(1L)).isEqualTo(10);
    }

    @Test
    @DisplayName("사업자 등록 + 성공 1건 + 분쟁 1건 → 15점")
    void calculate_사업자_성공1건_분쟁1건_15점() {
        givenHistory(true, 1, 1);

        assertThat(calculator.calculate(1L)).isEqualTo(15);
    }
}
