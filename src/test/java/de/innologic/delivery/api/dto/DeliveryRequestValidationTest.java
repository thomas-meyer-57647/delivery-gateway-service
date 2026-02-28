package de.innologic.delivery.api.dto;

import de.innologic.delivery.api.dto.MetaDto;
import de.innologic.delivery.domain.Channel;
import de.innologic.delivery.domain.DeliveryMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldFailWhenRequiredFieldsAreMissing() {
        DeliveryRequest request = new DeliveryRequest(
                "",
                null,
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<DeliveryRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("attemptId", "channel", "to", "content");
    }

    @Test
    void shouldPassForValidRequest() {
        DeliveryRequest request = new DeliveryRequest(
                "att-123",
                Channel.EMAIL,
                "user@example.com",
                "provider-a",
                "noreply@example.com",
                DeliveryMode.SINGLE,
                "Subject",
                new DeliveryContentDto("Hello", "<p>Hello</p>"),
                Collections.emptyList(),
                new MetaDto("ref", List.of("tag")),
                "corr-1"
        );

        Set<ConstraintViolation<DeliveryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
