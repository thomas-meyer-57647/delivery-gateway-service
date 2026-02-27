package de.innologic.delivery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "credits")
public class CreditsProperties {

    private boolean enabled = true;

    private long smsCost = 1;

    private long whatsappCost = 1;

    private long emailCost = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getSmsCost() {
        return smsCost;
    }

    public void setSmsCost(long smsCost) {
        this.smsCost = smsCost;
    }

    public long getWhatsappCost() {
        return whatsappCost;
    }

    public void setWhatsappCost(long whatsappCost) {
        this.whatsappCost = whatsappCost;
    }

    public long getEmailCost() {
        return emailCost;
    }

    public void setEmailCost(long emailCost) {
        this.emailCost = emailCost;
    }
}
