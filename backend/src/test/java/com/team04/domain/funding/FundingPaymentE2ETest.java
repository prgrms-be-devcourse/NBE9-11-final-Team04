package com.team04.domain.funding;

import com.team04.domain.funding.dto.request.SponsorRequest;
import com.team04.domain.funding.dto.response.CreateFundingResponse;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaBadge;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.payment.dto.request.ConfirmPaymentRequest;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.service.PaymentService;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FundingPaymentE2ETest {

    @Autowired
    private FundingService fundingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FundingRepository fundingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IdeaRepository ideaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long sponsorId;
    private Long ideaId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User proposer = userRepository.save(User.create(
                "proposer-" + suffix + "@test.com",
                passwordEncoder.encode("password1!"),
                "창작자",
                "창작자닉",
                30,
                Role.USER
        ));

        User sponsor = userRepository.save(User.create(
                "sponsor-" + suffix + "@test.com",
                passwordEncoder.encode("password1!"),
                "후원자",
                "후원자닉",
                28,
                Role.USER
        ));
        sponsorId = sponsor.getId();

        Idea idea = new Idea(
                proposer.getId(),
                "테스트 아이디어",
                IdeaCategory.TECH,
                "한 줄 소개",
                "문제 정의",
                "해결책",
                "목표",
                "타겟 고객",
                "경쟁사",
                "팀 소개",
                1_000_000L,
                0L,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                RewardType.REWARD_POINT,
                null,
                null
        );
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        idea.changeStatus(IdeaStatus.OPEN);
        idea.changeBadge(IdeaBadge.NO_HISTORY);
        ideaId = ideaRepository.save(idea).getId();
    }

    @Test
    @DisplayName("카드: 후원 생성 -> confirm -> Funding PAID")
    void cardFundingE2E() {
        CreateFundingResponse created = fundingService.applySponsorship(
                ideaId,
                sponsorId,
                new SponsorRequest(10_000L, PaymentMethod.CARD)
        );

        assertThat(created.fundingStatus()).isEqualTo(FundingStatus.PENDING_PAYMENT.name());
        assertThat(created.payment().status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(created.payment().clientKey()).isNotBlank();

        var confirmed = paymentService.confirmPayment(
                created.payment().paymentId(),
                new ConfirmPaymentRequest("mock-key-success-" + UUID.randomUUID(), 10_000L),
                sponsorId
        );

        assertThat(confirmed.status()).isEqualTo(PaymentStatus.SUCCESS);

        var funding = fundingRepository.findById(created.fundingId()).orElseThrow();
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.PAID);
        assertThat(funding.getRewardType()).isEqualTo(RewardType.REWARD_POINT);

        var idea = ideaRepository.findById(ideaId).orElseThrow();
        assertThat(idea.getCurrentAmount()).isEqualTo(10_000L);
        assertThat(idea.getSponsorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("가상계좌: 후원 생성 -> 웹훅 -> Funding PAID")
    void virtualAccountFundingE2E() {
        CreateFundingResponse created = fundingService.applySponsorship(
                ideaId,
                sponsorId,
                new SponsorRequest(50_000L, PaymentMethod.VIRTUAL_ACCOUNT)
        );

        assertThat(created.payment().method()).isEqualTo(PaymentMethod.VIRTUAL_ACCOUNT);
        assertThat(created.payment().vbank()).isNotNull();
        assertThat(created.payment().vbank().accountNumber()).isNotBlank();

        paymentService.processDepositWebhook(
                created.payment().orderId(),
                50_000L,
                "dev-webhook-secret",
                "webhook-event-" + UUID.randomUUID()
        );

        var funding = fundingRepository.findById(created.fundingId()).orElseThrow();
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.PAID);

        var idea = ideaRepository.findById(ideaId).orElseThrow();
        assertThat(idea.getCurrentAmount()).isEqualTo(50_000L);
        assertThat(idea.getSponsorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("카드: 후원 -> confirm -> 환불 완료")
    void cardFundingRefundE2E() {
        CreateFundingResponse created = fundingService.applySponsorship(
                ideaId,
                sponsorId,
                new SponsorRequest(10_000L, PaymentMethod.CARD)
        );

        paymentService.confirmPayment(
                created.payment().paymentId(),
                new ConfirmPaymentRequest("mock-key-success-" + UUID.randomUUID(), 10_000L),
                sponsorId
        );

        paymentService.refundPayment(created.payment().paymentId(), sponsorId);

        var payment = paymentService.getPayment(created.payment().paymentId(), sponsorId, Role.USER);
        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);

        var funding = fundingRepository.findById(created.fundingId()).orElseThrow();
        assertThat(funding.getStatus()).isEqualTo(FundingStatus.REFUNDED);

        var idea = ideaRepository.findById(ideaId).orElseThrow();
        assertThat(idea.getCurrentAmount()).isZero();
    }
}
