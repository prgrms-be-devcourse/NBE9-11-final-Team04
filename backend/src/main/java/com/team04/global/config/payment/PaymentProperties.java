package com.team04.global.config.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(
        Gateway gateway,
        Toss toss,
        Webhook webhook
) {

    public PaymentProperties {
        if (gateway == null) {
            gateway = new Gateway("mock");
        }
        if (toss == null) {
            toss = new Toss("", "", "https://api.tosspayments.com");
        }
        if (webhook == null) {
            webhook = new Webhook("dev-webhook-secret");
        }
    }

    public record Gateway(String type) {
    }

    public record Toss(String secretKey, String clientKey, String baseUrl) {
    }

    public record Webhook(String secret) {
    }
}
