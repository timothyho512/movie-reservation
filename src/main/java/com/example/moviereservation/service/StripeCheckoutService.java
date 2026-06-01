package com.example.moviereservation.service;

import com.example.moviereservation.config.StripeProperties;
import com.example.moviereservation.dto.StripeCheckoutSessionResult;
import com.example.moviereservation.entity.CheckoutSession;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import com.example.moviereservation.dto.StripeCheckoutCompletedEvent;
import com.example.moviereservation.dto.StripeCheckoutExpiredEvent;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



import java.math.BigDecimal;
import java.util.Map;
@Service
public class StripeCheckoutService {

    private final StripeProperties stripeProperties;

    public StripeCheckoutService(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    public StripeCheckoutSessionResult createHostedCheckoutSession(CheckoutSession checkoutSession) {
        return createHostedCheckoutSession(checkoutSession, null);
    }

    public StripeCheckoutSessionResult createHostedCheckoutSession(
            CheckoutSession checkoutSession,
            String stripeIdempotencyKey
    ) {
        try {
            Stripe.apiKey = stripeProperties.getSecretKey();

            SessionCreateParams.LineItem.PriceData.ProductData productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Movie tickets")
                            .putMetadata("checkoutReference", checkoutSession.getCheckoutReference())
                            .build();

            SessionCreateParams.LineItem.PriceData priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(stripeProperties.getCurrency())
                            .setUnitAmount(toMinorUnits(checkoutSession.getTotalAmount()))
                            .setProductData(productData)
                            .build();

            SessionCreateParams.LineItem lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(priceData)
                            .build();

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl(buildRedirectUrl(stripeProperties.getSuccessUrl(), checkoutSession))
                            .setCancelUrl(buildRedirectUrl(stripeProperties.getCancelUrl(), checkoutSession))
                            .setCustomerEmail(checkoutSession.getStripeCustomerEmail())
                            .addLineItem(lineItem)
                            .putAllMetadata(buildMetadata(checkoutSession))
                            .build();

            Session session;
            if (stripeIdempotencyKey == null || stripeIdempotencyKey.isBlank()) {
                session = Session.create(params);
            } else {
                RequestOptions requestOptions = RequestOptions.builder()
                        .setIdempotencyKey(stripeIdempotencyKey)
                        .build();
                session = Session.create(params, requestOptions);
            }

            return new StripeCheckoutSessionResult(
                    session.getId(),
                    session.getUrl(),
                    session.getPaymentIntent()
            );
        } catch (StripeException e) {
            throw new IllegalStateException("Could not create Stripe checkout session", e);
        }
    }

    private String buildRedirectUrl(String template, CheckoutSession checkoutSession) {
        return template.replace("{CHECKOUT_REFERENCE}", checkoutSession.getCheckoutReference());
    }

    private Map<String, String> buildMetadata(CheckoutSession checkoutSession) {
        return Map.of(
                "checkoutReference", checkoutSession.getCheckoutReference(),
                "checkoutSessionId", checkoutSession.getId() != null ? checkoutSession.getId().toString() : "",
                "showtimeId", checkoutSession.getShowtime().getId().toString()
        );
    }

    private Long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    // 
    public StripeCheckoutCompletedEvent parseCheckoutCompletedEvent(String payload, String signatureHeader) {
        try {
            Event event = Webhook.constructEvent(
                    payload,
                    signatureHeader,
                    stripeProperties.getWebhookSecret()
            );

            if (!"checkout.session.completed".equals(event.getType())) {
                return null;
            }

            return new StripeCheckoutCompletedEvent(
                    extractRequiredStringFromDataObject(payload, "id"),
                    extractOptionalStringFromDataObject(payload, "payment_intent")
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Stripe webhook payload", e);
        }
    }


    public StripeCheckoutExpiredEvent parseCheckoutExpiredEvent(String payload, String signatureHeader) {
        try {
            Event event = Webhook.constructEvent(
                    payload,
                    signatureHeader,
                    stripeProperties.getWebhookSecret()
            );

            if (!"checkout.session.expired".equals(event.getType())) {
                return null;
            }

            return new StripeCheckoutExpiredEvent(
                    extractRequiredStringFromDataObject(payload, "id")
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Stripe webhook payload", e);
        }
    }

    private String extractRequiredStringFromDataObject(String payload, String fieldName) {
    String value = extractOptionalStringFromDataObject(payload, fieldName);

    if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("Missing Stripe checkout session field: " + fieldName);
    }

    return value;
}

    private String extractOptionalStringFromDataObject(String payload, String fieldName) {
        JsonObject dataObject = JsonParser.parseString(payload)
                .getAsJsonObject()
                .getAsJsonObject("data")
                .getAsJsonObject("object");

        JsonElement field = dataObject.get(fieldName);

        if (field == null || field.isJsonNull()) {
            return null;
        }

        if (field.isJsonPrimitive()) {
            return field.getAsString();
        }

        if (field.isJsonObject()) {
            JsonElement id = field.getAsJsonObject().get("id");
            return id == null || id.isJsonNull() ? null : id.getAsString();
        }

        throw new IllegalArgumentException("Unsupported Stripe checkout session field: " + fieldName);
    }
}
