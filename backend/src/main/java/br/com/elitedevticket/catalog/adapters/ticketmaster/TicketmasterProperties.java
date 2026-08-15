package br.com.elitedevticket.catalog.adapters.ticketmaster;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "catalog.ticketmaster")
public final class TicketmasterProperties {
    private String apiKey = "";
    private String baseUrl = "https://app.ticketmaster.com/discovery/v2";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofMillis(2500);
    private int maxRetries = 1;
    private Duration totalBudget = Duration.ofSeconds(5);

    @PostConstruct
    public void validate() {
        if (maxRetries < 0 || maxRetries > 1) {
            throw new IllegalStateException("catalog.ticketmaster.max-retries deve ser 0 ou 1.");
        }
        if (totalBudget == null || totalBudget.isZero() || totalBudget.isNegative() || totalBudget.toMillis() > 5000) {
            throw new IllegalStateException("catalog.ticketmaster.total-budget deve ser positivo e no máximo 5 segundos.");
        }
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalStateException("catalog.ticketmaster.connect-timeout deve ser positivo.");
        }
        if (readTimeout == null || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalStateException("catalog.ticketmaster.read-timeout deve ser positivo.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("catalog.ticketmaster.base-url é obrigatório.");
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(Duration totalBudget) {
        this.totalBudget = totalBudget;
    }
}
