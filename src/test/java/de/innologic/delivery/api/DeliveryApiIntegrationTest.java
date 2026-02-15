package de.innologic.delivery.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.delivery.domain.DeliveryLogEntity;
import de.innologic.delivery.domain.DeliveryState;
import de.innologic.delivery.persistence.DeliveryEventRepository;
import de.innologic.delivery.persistence.DeliveryLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
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
    private DeliveryLogRepository deliveryLogRepository;

    @Autowired
    private DeliveryEventRepository deliveryEventRepository;

    @AfterEach
    void cleanup() {
        deliveryEventRepository.deleteAll();
        deliveryLogRepository.deleteAll();
    }

    @Test
    void postDeliveriesShouldPersistDeliveryLog() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String requestJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "subject": "Test Subject",
                  "content": {
                    "text": "Hello world",
                    "html": "<p>Hello world</p>"
                  },
                  "attachments": [
                    {
                      "fileId": "f-1",
                      "url": "https://example.com/file.pdf",
                      "filename": "file.pdf",
                      "mimeType": "application/pdf",
                      "size": 1234
                    }
                  ],
                  "correlationId": "corr-123"
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.state").value("ACCEPTED"))
                .andExpect(jsonPath("$.createdAtUtc").exists());

        DeliveryLogEntity log = deliveryLogRepository.findByCompanyIdAndAttemptId("company-a", attemptId).orElseThrow();
        assertThat(log.getState()).isEqualTo(DeliveryState.ACCEPTED);
        assertThat(log.getToAddress()).isEqualTo("user@example.com");
        assertThat(log.getSubject()).isEqualTo("Test Subject");
        assertThat(log.getContentText()).isEqualTo("Hello world");
    }

    @Test
    void sameAttemptIdShouldBeIdempotent() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String requestJson = """
                {
                  "attemptId": "%s",
                  "channel": "WHATSAPP",
                  "to": "+491700000000",
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
        assertThat(second.get("providerMessageId")).isEqualTo(first.get("providerMessageId"));
        assertThat(second.get("errorCode")).isEqualTo(first.get("errorCode"));
        assertThat(second.get("errorMessage")).isEqualTo(first.get("errorMessage"));
        Instant firstCreatedAt = Instant.parse(first.get("createdAtUtc").asText());
        Instant secondCreatedAt = Instant.parse(second.get("createdAtUtc").asText());
        assertThat(secondCreatedAt).isEqualTo(firstCreatedAt.truncatedTo(ChronoUnit.MICROS));
        assertThat(deliveryLogRepository.countByCompanyIdAndAttemptId("company-a", attemptId)).isEqualTo(1);
    }

    @Test
    void providerCallbackShouldPersistEventAndUpdateLog() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String createRequestJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "subject": "Initial",
                  "content": {
                    "text": "Initial text"
                  }
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson))
                .andExpect(status().isAccepted());

        String callbackJson = """
                {
                  "attemptId": "%s",
                  "channel": "EMAIL",
                  "eventType": "FAILED",
                  "eventAtUtc": "2026-02-15T18:30:00Z",
                  "providerMessageId": "pm-123",
                  "rawStatus": "undelivered",
                  "errorCode": "TWILIO-400",
                  "errorMessage": "Destination not reachable"
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/provider-callbacks/twilio")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.attemptId").value(attemptId))
                .andExpect(jsonPath("$.eventType").value("FAILED"));

        assertThat(deliveryEventRepository.countByCompanyIdAndAttemptId("company-a", attemptId)).isEqualTo(1);
        DeliveryLogEntity log = deliveryLogRepository.findByCompanyIdAndAttemptId("company-a", attemptId).orElseThrow();
        assertThat(log.getState()).isEqualTo(DeliveryState.REJECTED);
        assertThat(log.getProviderMessageId()).isEqualTo("pm-123");
        assertThat(log.getErrorCode()).isEqualTo("TWILIO-400");
    }

    @Test
    void missingRequiredFieldsShouldReturn400() throws Exception {
        String invalidJson = """
                {
                  "channel": "EMAIL",
                  "to": "user@example.com",
                  "content": {
                    "text": "x"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void smsWithSubjectShouldReturn400() throws Exception {
        String attemptId = "att-" + UUID.randomUUID();
        String invalidJson = """
                {
                  "attemptId": "%s",
                  "channel": "SMS",
                  "to": "+491700000000",
                  "subject": "forbidden",
                  "content": {
                    "text": "x"
                  }
                }
                """.formatted(attemptId);

        mockMvc.perform(post("/api/v1/deliveries")
                        .with(jwt().jwt(jwt -> jwt.claim("companyId", "company-a")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingJwtShouldReturn401() throws Exception {
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtShouldReturn401() throws Exception {
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
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCompanyIdClaimShouldReturn403() throws Exception {
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
                        .with(jwt().jwt(jwt -> jwt.claims(claims -> claims.remove("companyId"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }
}
