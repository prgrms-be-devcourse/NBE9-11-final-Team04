package com.team04.domain.payment.client;

import com.team04.domain.payment.client.toss.TossApiDtos.TossErrorResponse;
import com.team04.domain.payment.client.toss.TossApiDtos.TossPaymentResponse;
import com.team04.domain.payment.client.toss.TossApiDtos.TossVirtualAccount;
import com.team04.domain.payment.dto.request.PayoutRequest;
import com.team04.domain.payment.dto.response.PaymentConfirmResult;
import com.team04.domain.payment.dto.response.PaymentRefundResult;
import com.team04.domain.payment.dto.response.PaymentSessionResult;
import com.team04.domain.payment.dto.response.PaymentVerifyResult;
import com.team04.domain.payment.dto.response.PayoutResult;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.global.config.payment.PaymentProperties;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * 토스페이먼츠 REST API 연동 구현체.
 *
 * @see <a href="https://docs.tosspayments.com/reference">Toss Payments API</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.gateway.type", havingValue = "toss")
@RequiredArgsConstructor
public class TossPaymentGateway implements PaymentGateway {

    private final RestClient restClient;
    private final PaymentProperties paymentProperties;

    @Override
    public boolean issuesVirtualAccountAtCreateTime() {
        return false;
    }

    @Override
    public PaymentSessionResult createSession(String orderId, long amount, PaymentMethod method) {
        log.info("[TossPG] 세션 생성 orderId={}, amount={}, method={}", orderId, amount, method);
        return new PaymentSessionResult(paymentProperties.toss().clientKey(), null);
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount) {
        log.info("[TossPG] 결제 승인 paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);

        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        try {
            TossPaymentResponse response = post("/v1/payments/confirm", body, TossPaymentResponse.class);
            return mapConfirmResponse(response);
        } catch (Exception e) {
            log.error("[TossPG] 결제 승인 실패 orderId={}", orderId, e);
            return PaymentConfirmResult.failure(resolveErrorMessage(e));
        }
    }

    @Override
    public VirtualAccountIssueResult issueVirtualAccount(String orderId, long amount) {
        throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
    }

    @Override
    public PaymentVerifyResult verifyVirtualAccountDeposit(String orderId, long amount) {
        log.info("[TossPG] 가상계좌 입금 검증 orderId={}, amount={}", orderId, amount);

        try {
            TossPaymentResponse response = get(
                    "/v1/payments/orders/{orderId}",
                    orderId,
                    TossPaymentResponse.class
            );

            if (response == null) {
                return PaymentVerifyResult.failure("토스 결제 조회 응답이 비어 있습니다");
            }
            if (!"DONE".equalsIgnoreCase(response.status())) {
                return PaymentVerifyResult.failure("입금 완료 상태가 아닙니다: " + response.status());
            }
            if (response.totalAmount() == null || !response.totalAmount().equals(amount)) {
                return PaymentVerifyResult.failure("입금 금액이 일치하지 않습니다");
            }
            return PaymentVerifyResult.success();
        } catch (Exception e) {
            log.error("[TossPG] 가상계좌 검증 실패 orderId={}", orderId, e);
            return PaymentVerifyResult.failure(resolveErrorMessage(e));
        }
    }

    @Override
    public PaymentRefundResult refund(String paymentKey, String orderId, long amount, String cancelReason) {
        log.info("[TossPG] 환불 paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);

        Map<String, Object> body = Map.of(
                "cancelReason", cancelReason,
                "cancelAmount", amount
        );

        try {
            TossPaymentResponse response = post(
                    "/v1/payments/{paymentKey}/cancel",
                    paymentKey,
                    body,
                    TossPaymentResponse.class
            );

            if (response == null) {
                return PaymentRefundResult.failure("토스 환불 응답이 비어 있습니다");
            }
            String cancelKey = response.transactionKey() != null
                    ? response.transactionKey()
                    : response.paymentKey();
            return PaymentRefundResult.success(cancelKey);
        } catch (Exception e) {
            log.error("[TossPG] 환불 실패 paymentKey={}, orderId={}", paymentKey, orderId, e);
            return PaymentRefundResult.failure(resolveErrorMessage(e));
        }
    }

    @Override
    public PayoutResult payout(PayoutRequest request) {
        PaymentProperties.Toss toss = paymentProperties.toss();
        if (!toss.payoutEnabled()) {
            log.info("[TossPG] 지급대행 비활성 — type={}, targetId={}, amount={}",
                    request.payoutTargetType(), request.payoutTargetId(), request.amount());
            return PayoutResult.skipped("toss-payout-disabled");
        }

        if (toss.securityKey() == null || toss.securityKey().isBlank()) {
            return PayoutResult.failure("지급대행 securityKey가 설정되지 않았습니다");
        }

        String destination = paymentProperties.payout().destinationSellerId();
        if (destination == null || destination.isBlank()) {
            return PayoutResult.failure("지급대행 destinationSellerId가 설정되지 않았습니다");
        }

        // 토스 지급대행(/v2/payouts)은 JWE 암호화 연동이 필요합니다 — 추후 구현
        log.warn("[TossPG] 지급대행 미구현 — type={}, targetId={}, destination={}, amount={}",
                request.payoutTargetType(), request.payoutTargetId(), destination, request.amount());
        return PayoutResult.skipped("toss-payout-not-implemented");
    }

    private PaymentConfirmResult mapConfirmResponse(TossPaymentResponse response) {
        if (response == null || response.paymentKey() == null) {
            return PaymentConfirmResult.failure("토스 결제 승인 응답이 비어 있습니다");
        }

        if ("WAITING_FOR_DEPOSIT".equalsIgnoreCase(response.status())) {
            TossVirtualAccount virtualAccount = response.virtualAccount();
            if (virtualAccount == null) {
                return PaymentConfirmResult.failure("가상계좌 정보가 응답에 없습니다");
            }
            VirtualAccountIssueResult issued = new VirtualAccountIssueResult(
                    null,
                    virtualAccount.bankCode(),
                    virtualAccount.accountNumber(),
                    virtualAccount.parseDueDate()
            );
            return PaymentConfirmResult.awaitingDeposit(
                    response.paymentKey(),
                    response.secret(),
                    issued
            );
        }

        if ("DONE".equalsIgnoreCase(response.status())) {
            return PaymentConfirmResult.success(response.paymentKey());
        }

        return PaymentConfirmResult.failure("지원하지 않는 결제 상태입니다: " + response.status());
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return restClient.post()
                .uri(baseUrl() + path)
                .header("Authorization", authorizationHeader())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    private <T> T post(String path, String pathVariable, Object body, Class<T> responseType) {
        return restClient.post()
                .uri(baseUrl() + path, pathVariable)
                .header("Authorization", authorizationHeader())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    private <T> T get(String path, String orderId, Class<T> responseType) {
        return restClient.get()
                .uri(baseUrl() + path, orderId)
                .header("Authorization", authorizationHeader())
                .retrieve()
                .body(responseType);
    }

    private String baseUrl() {
        String baseUrl = paymentProperties.toss().baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.tosspayments.com";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String authorizationHeader() {
        String secretKey = paymentProperties.toss().secretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        String encoded = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String resolveErrorMessage(Exception e) {
        if (e instanceof RestClientResponseException responseException) {
            HttpStatusCode status = responseException.getStatusCode();
            try {
                TossErrorResponse error = responseException.getResponseBodyAs(TossErrorResponse.class);
                if (error != null && error.message() != null) {
                    return error.message();
                }
            } catch (Exception ignored) {
                // fall through
            }
            return "토스 API 오류 (" + status.value() + ")";
        }
        return e.getMessage() != null ? e.getMessage() : "토스 API 호출 실패";
    }
}
