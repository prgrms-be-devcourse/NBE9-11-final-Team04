package com.team04.global.config.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        Gateway gateway,
        Toss toss,
        Webhook webhook,
        Pool pool,
        Demo demo,
        Payout payout
) {

    public PaymentProperties {
        if (gateway == null) {
            gateway = new Gateway("mock");
        }
        if (toss == null) {
            toss = new Toss("", "", "https://api.tosspayments.com", "", false);
        }
        if (webhook == null) {
            webhook = new Webhook("dev-webhook-secret");
        }
        if (pool == null) {
            pool = new Pool(true);
        }
        if (demo == null) {
            demo = new Demo(false);
        }
        if (payout == null) {
            payout = new Payout(true, "088", "000000000000", "");
        }
    }

    public record Gateway(String type) {
    }

    public record Toss(String secretKey, String clientKey, String baseUrl, String securityKey, boolean payoutEnabled) {
    }

    public record Webhook(String secret) {
    }

    public record Pool(boolean enabled) {
    }

    /** 테스트 결제창 입력만으로 결제 완료 처리 */
    public record Demo(boolean enabled) {
    }

    /**
     * 지급대행 최소 연동 설정.
     * autoComplete=true이면 PG skipped 응답 시에도 지급 장부를 완료 처리합니다 (로컬·시연용).
     */
    public record Payout(boolean autoComplete, String bankCode, String accountNumber, String destinationSellerId) {
    }
}
