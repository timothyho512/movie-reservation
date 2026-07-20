package com.example.moviereservation.controller;

import com.example.moviereservation.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LegacyCheckoutControllerConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CheckoutService.class, () -> mock(CheckoutService.class))
            .withUserConfiguration(LegacyCheckoutController.class);

    @Test
    void legacyCheckoutControllerIsDisabledByDefault() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(LegacyCheckoutController.class)
        );
    }

    @Test
    void legacyCheckoutControllerCanBeEnabledForDevelopmentAndTests() {
        contextRunner
                .withPropertyValues("app.legacy-checkout.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(LegacyCheckoutController.class)
                );
    }
}
