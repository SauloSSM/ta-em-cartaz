package br.com.elitedevticket.shared.configuration;

public final class MissingDatasourceConfigurationException extends IllegalStateException {
    public MissingDatasourceConfigurationException(String configurationName) {
        super("Missing required configuration: " + configurationName);
    }
}
