package com.team04.global.config.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        Gateway gateway,
        Toss toss,
        Webhook webhook,
        Pool pool,
        Demo demo
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
    }

    public record Gateway(String type) {
    }

    public record Toss(String secretKey, String clientKey, String baseUrl, String securityKey, boolean payoutEnabled) {
    }

    public record Webhook(String secret) {
    }

    public record Pool(boolean enabled) {
    }

    /** 포트폴리오·부트캠프 시연 모드 — 테스트 결제창 입력만으로 결제 완료 처리 */
    public record Demo(boolean enabled) {
    }
}
