package br.com.elitedevticket.shared.configuration;

import java.util.Set;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

public final class RequiredDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final Set<String> EXTERNALLY_CONFIGURED_PROFILES = Set.of("demo", "prod");
    private static final String[] REQUIRED_CONFIGURATIONS = {
        "DATABASE_URL",
        "DATABASE_USERNAME",
        "DATABASE_PASSWORD"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!requiresExternalDatasource(environment)) {
            return;
        }

        for (String configurationName : REQUIRED_CONFIGURATIONS) {
            if (!StringUtils.hasText(environment.getProperty(configurationName))) {
                throw new MissingDatasourceConfigurationException(configurationName);
            }
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    private boolean requiresExternalDatasource(ConfigurableEnvironment environment) {
        for (String activeProfile : environment.getActiveProfiles()) {
            if (EXTERNALLY_CONFIGURED_PROFILES.contains(activeProfile)) {
                return true;
            }
        }

        if (environment.getActiveProfiles().length > 0) {
            return false;
        }

        for (String defaultProfile : environment.getDefaultProfiles()) {
            if (EXTERNALLY_CONFIGURED_PROFILES.contains(defaultProfile)) {
                return true;
            }
        }
        return false;
    }
}
