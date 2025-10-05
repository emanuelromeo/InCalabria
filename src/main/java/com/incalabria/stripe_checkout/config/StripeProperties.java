package com.incalabria.stripe_checkout.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {
    private Api api = new Api();
    private Webhook webhook = new Webhook();

    public static class Api {
        private String secretKey;
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    }

    public static class Webhook {
        private String secret;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }

    public Api getApi() { return api; }
    public Webhook getWebhook() { return webhook; }
    public void setApi(Api api) { this.api = api; }
    public void setWebhook(Webhook webhook) { this.webhook = webhook; }
}
