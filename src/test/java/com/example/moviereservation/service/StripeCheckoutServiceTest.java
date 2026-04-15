package com.example.moviereservation.service;

import com.example.moviereservation.config.StripeProperties;
import com.example.moviereservation.dto.StripeCheckoutCompletedEvent;
import com.example.moviereservation.dto.StripeCheckoutExpiredEvent;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeCheckoutServiceTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";

    private StripeCheckoutService stripeCheckoutService;

    @BeforeEach
    void setUp() {
        StripeProperties stripeProperties = new StripeProperties();
        stripeProperties.setSecretKey("sk_test_fake");
        stripeProperties.setWebhookSecret(WEBHOOK_SECRET);
        stripeProperties.setSuccessUrl("http://localhost:3000/checkout/success?checkoutReference={CHECKOUT_REFERENCE}");
        stripeProperties.setCancelUrl("http://localhost:3000/checkout/cancel?checkoutReference={CHECKOUT_REFERENCE}");
        stripeProperties.setCurrency("gbp");

        stripeCheckoutService = new StripeCheckoutService(stripeProperties);
    }

    @Test
    void parseCheckoutCompletedEventReturnsCompletedEventForValidSignature() throws Exception {
        String payload = """
                {
                  "id": "evt_test_completed",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_completed",
                      "object": "checkout.session",
                      "payment_intent": "pi_test_completed"
                    }
                  }
                }
                """;

        String signatureHeader = buildSignatureHeader(payload);

        StripeCheckoutCompletedEvent event =
                stripeCheckoutService.parseCheckoutCompletedEvent(payload, signatureHeader);

        assertThat(event).isNotNull();
        assertThat(event.getStripeCheckoutSessionId()).isEqualTo("cs_test_completed");
        assertThat(event.getStripePaymentIntentId()).isEqualTo("pi_test_completed");
    }

    @Test
    void parseCheckoutCompletedEventReturnsNullForDifferentEventType() throws Exception {
        String payload = """
                {
                  "id": "evt_test_expired",
                  "object": "event",
                  "type": "checkout.session.expired",
                  "data": {
                    "object": {
                      "id": "cs_test_expired",
                      "object": "checkout.session"
                    }
                  }
                }
                """;

        String signatureHeader = buildSignatureHeader(payload);

        StripeCheckoutCompletedEvent event =
                stripeCheckoutService.parseCheckoutCompletedEvent(payload, signatureHeader);

        assertThat(event).isNull();
    }

    @Test
    void parseCheckoutExpiredEventReturnsExpiredEventForValidSignature() throws Exception {
        String payload = """
                {
                  "id": "evt_test_expired",
                  "object": "event",
                  "type": "checkout.session.expired",
                  "data": {
                    "object": {
                      "id": "cs_test_expired",
                      "object": "checkout.session"
                    }
                  }
                }
                """;

        String signatureHeader = buildSignatureHeader(payload);

        StripeCheckoutExpiredEvent event =
                stripeCheckoutService.parseCheckoutExpiredEvent(payload, signatureHeader);

        assertThat(event).isNotNull();
        assertThat(event.getStripeCheckoutSessionId()).isEqualTo("cs_test_expired");
    }

    @Test
    void parseCheckoutExpiredEventReturnsNullForDifferentEventType() throws Exception {
        String payload = """
                {
                  "id": "evt_test_completed",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_completed",
                      "object": "checkout.session",
                      "payment_intent": "pi_test_completed"
                    }
                  }
                }
                """;

        String signatureHeader = buildSignatureHeader(payload);

        StripeCheckoutExpiredEvent event =
                stripeCheckoutService.parseCheckoutExpiredEvent(payload, signatureHeader);

        assertThat(event).isNull();
    }

    @Test
    void parseCheckoutCompletedEventRejectsInvalidSignature() {
        String payload = """
                {
                  "id": "evt_test_completed",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_completed",
                      "object": "checkout.session",
                      "payment_intent": "pi_test_completed"
                    }
                  }
                }
                """;

        String invalidSignatureHeader = "t=123,v1=invalid";

        assertThatThrownBy(() ->
                stripeCheckoutService.parseCheckoutCompletedEvent(payload, invalidSignatureHeader)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Stripe webhook payload");
    }

    private String buildSignatureHeader(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String signedPayload = timestamp + "." + payload;
        String signature = Webhook.Util.computeHmacSha256(WEBHOOK_SECRET, signedPayload);

        return "t=" + timestamp + ",v1=" + signature;
    }
}
