package de.innologic.delivery.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryEventTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"delivered", "DELIVERED", "Delivered"})
    void shouldNormalizeEventTypeIgnoringCaseAndFormatting(String input) {
        assertThat(DeliveryEventType.from(input)).isEqualTo(DeliveryEventType.DELIVERED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"event-nonexistent", "123_EVENT", " "})
    void shouldRejectUnknownEventTypes(String input) {
        assertThatThrownBy(() -> DeliveryEventType.from(input)).isInstanceOf(IllegalArgumentException.class);
    }
}
