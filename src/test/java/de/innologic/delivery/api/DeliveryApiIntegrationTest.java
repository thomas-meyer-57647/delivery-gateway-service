package de.innologic.delivery.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.delivery.domain.DeliveryStatus;
import de.innologic.delivery.persistence.DeliveryAttemptRepository;
import de.innologic.delivery.persistence.DeliveryEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class DeliveryApiIntegrationTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.6")
            .withDatabaseName("deliveries")
            .withUsername("root")
            .withPassword("");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
        registry.add("spring.flyway.url", MARIADB::getJdbcUrl);
        registry.add("spring.flyway.user", MARIADB::getUsername);
        registry.add("spring.flyway.password", MARIADB::getPassword);
    }

    @TestConfiguration
    static class JwtDecoderTestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if ("valid-token".equals(token)) {
                    return new Jwt(
                            token,
                            Instant.now(),
                            Instant.now().plusSeconds(600),
                            Map.of("alg", "none"),
                            Map.of("sub", "test-user", "companyId", "company-a")
                    );
                }
                throw new BadJwtException("Invalid token");
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeliveryAttemptRepository deliveryAttemptRepository;

    @Autowired
    private DeliveryEventRepository deliveryEventRepository;

    @AfterEach
    void cleanup() {
        deliveryEventRepository.deleteAll();
        deliveryAttemptRepository.deleteAll();
    }

    @Test
    void postDeliveriesShouldPersistAttemptAndEvent() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String requestJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "to": "user@example.com; duplicate@example.com;user@example.com",
                  "deliveryMode": "INDIVIDUAL",
                  "subject": "Service Update",
                  "content": {
                    "text": "Hello world"
                  }
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.state").value("QUEUED"))
                .andExpect(jsonPath("$.deliveryMode").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.to[0]").value("user@example.com"))
                .andExpect(jsonPath("$.to[1]").value("duplicate@example.com"));

        assertThat(deliveryAttemptRepository.countByCompanyIdAndAttemptId("company-a", attemptId)).isEqualTo(1);
        assertThat(deliveryEventRepository.countByCompanyIdAndAttemptId("company-a", attemptId)).isEqualTo(1);
        assertThat(deliveryAttemptRepository.findByCompanyIdAndAttemptId("company-a", attemptId))
                .map(attempt -> attempt.getState())
                .hasValue(DeliveryStatus.QUEUED);
    }

    @Test
    void sameAttemptIdShouldStayIdempotent() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String requestJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "content": {
                    "text": "Idempotent message"
                  }
                }
                """.formatted(attemptId);

        String firstResponse = mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstResponse);
        JsonNode second = objectMapper.readTree(secondResponse);

        assertThat(second.get("attemptId")).isEqualTo(first.get("attemptId"));
        assertThat(second.get("state")).isEqualTo(first.get("state"));
        assertThat(second.get("createdAtUtc").asText()).isEqualTo(first.get("createdAtUtc").asText());
        assertThat(deliveryAttemptRepository.countByCompanyIdAndAttemptId("company-a", attemptId)).isEqualTo(1);
    }

    @Test
    void getDeliveriesReturnsTimelineAscending() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String createJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "content": {
                    "text": "Timeline test"
                  }
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isAccepted());

        String callbackJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "eventType": "SENT",
                  "eventAtUtc": "2026-12-31T00:00:00Z",
                  "providerMessageId": "pm-123"
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/providers/twilio/events")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/deliveries/" + attemptId)
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].eventType").value("QUEUED"))
                .andExpect(jsonPath("$.events[1].eventType").value("SENT"));
    }

    @Test
    void providerEventUpdatesStatusAndProviderMessageId() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String createJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "content": {
                    "text": "Status update"
                  }
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isAccepted());

        String callbackJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "eventType": "DELIVERED",
                  "providerMessageId": "pm-abc",
                  "rawStatus": "delivered"
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/providers/twilio/events")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventType").value("DELIVERED"));

        assertThat(deliveryAttemptRepository.findByCompanyIdAndAttemptId("company-a", attemptId))
                .map(attempt -> attempt.getState())
                .hasValue(DeliveryStatus.DELIVERED);
        assertThat(deliveryAttemptRepository.findByCompanyIdAndAttemptId("company-a", attemptId))
                .map(attempt -> attempt.getProviderMessageId())
                .hasValue("pm-abc");
    }

    @Test
    void missingJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCompanyIdClaimReturnsForbidden() throws Exception {
        String requestJson = """
                {
                  "attemptId": "att-403",
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "content": {
                    "text": "x"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claims(claims -> claims.remove("companyId"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherCompanyCannotReadAttempt() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String requestJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "content": {
                    "text": "x"
                  }
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/deliveries/" + attemptId)
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-b"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void providerEventWithoutIdentifiersReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                  "channel": "EMAIL",
                  "eventType": "SENT"
                }
                """;

        mockMvc.perform(post("/api/v1/providers/twilio/events")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }


}
