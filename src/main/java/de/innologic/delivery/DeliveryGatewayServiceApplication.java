package de.innologic.delivery;

import de.innologic.delivery.config.CreditsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CreditsProperties.class)
public class DeliveryGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryGatewayServiceApplication.class, args);
    }
}
