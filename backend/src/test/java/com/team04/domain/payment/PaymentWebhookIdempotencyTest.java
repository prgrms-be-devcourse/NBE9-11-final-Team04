package com.team04.domain.payment;

import com.team04.domain.funding.dto.request.SponsorRequest;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaBadge;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.entity.PaymentWebhookLog;
import com.team04.domain.payment.repository.PaymentWebhookLogRepository;
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
class PaymentWebhookIdempotencyTest {

    @Autowired private FundingService fundingService;
    @Autowired private PaymentService paymentService;
    @Autowired private FundingRepository fundingRepository;
    @Autowired private PaymentWebhookLogRepository paymentWebhookLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private IdeaRepository ideaRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long sponsorId;
    private Long ideaId;
    private String orderId;
    private String eventId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User proposer = userRepository.save(User.create(
                "p-" + suffix + "@test.com", passwordEncoder.encode("pw1!"),
                "창작자", "창작자", 30, Role.USER));
        User sponsor = userRepository.save(User.create(
                "s-" + suffix + "@test.com", passwordEncoder.encode("pw1!"),
                "후원자", "후원자", 28, Role.USER));
        sponsorId = sponsor.getId();

        Idea idea = new Idea(
                proposer.getId(), "테스트", IdeaCategory.TECH, "한줄", "문제", "해결", "목표",
                "고객", "경쟁", "팀", 1_000_000L, 300_000L,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30),
                RewardType.REWARD_POINT, null, null);
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        idea.changeStatus(IdeaStatus.OPEN);
        idea.changeBadge(IdeaBadge.NO_HISTORY);
        ideaId = ideaRepository.save(idea).getId();
    }

    @Test
    @DisplayName("동일 eventId 웹훅 2회 — 1회만 PAID, 로그 1건")
    void duplicateWebhookIsIdempotent() {
        var created = fundingService.applySponsorship(
                ideaId, sponsorId, new SponsorRequest(20_000L, PaymentMethod.VIRTUAL_ACCOUNT));
        orderId = created.payment().orderId();
        eventId = "evt-" + UUID.randomUUID();

        paymentService.processDepositWebhook(orderId, 20_000L, "dev-webhook-secret", eventId);
        paymentService.processDepositWebhook(orderId, 20_000L, "dev-webhook-secret", eventId);

        assertThat(fundingRepository.findById(created.fundingId()).orElseThrow().getStatus())
                .isEqualTo(FundingStatus.PAID);

        PaymentWebhookLog log = paymentWebhookLogRepository.findByEventId(eventId).orElseThrow();
        assertThat(log.getStatus()).isEqualTo("DONE");
        assertThat(log.getOrderId()).isEqualTo(orderId);
    }
}
