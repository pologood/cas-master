package org.apereo.cas.web.report.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.audit.spi.DelegatingAuditTrailManager;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.monitor.HealthStatus;
import org.apereo.cas.monitor.Monitor;
import org.apereo.cas.support.events.dao.CasEventRepository;
import org.apereo.cas.trusted.authentication.api.MultifactorAuthenticationTrustStorage;
import org.apereo.cas.web.report.AuthenticationEventsController;
import org.apereo.cas.web.report.DashboardController;
import org.apereo.cas.web.report.HealthCheckController;
import org.apereo.cas.web.report.ConfigurationStateController;
import org.apereo.cas.web.report.LoggingConfigController;
import org.apereo.cas.web.report.PersonDirectoryAttributeResolutionController;
import org.apereo.cas.web.report.SingleSignOnSessionsReportController;
import org.apereo.cas.web.report.StatisticsController;
import org.apereo.cas.web.report.TrustedDevicesController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * This is {@link CasReportsConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("casReportsConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableWebSocketMessageBroker
public class CasReportsConfiguration extends AbstractWebSocketMessageBrokerConfigurer {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    @Qualifier("healthCheckMonitor")
    private Monitor<HealthStatus> healthCheckMonitor;

    @Autowired
    @Qualifier("centralAuthenticationService")
    private CentralAuthenticationService centralAuthenticationService;
    
    @Autowired
    @Qualifier("metrics")
    private MetricRegistry metricsRegistry;

    @Autowired
    @Qualifier("healthCheckMetrics")
    private HealthCheckRegistry healthCheckRegistry;

    @RefreshScope
    @Bean
    public DashboardController dashboardController() {
        return new DashboardController();
    }

    @Bean
    public PersonDirectoryAttributeResolutionController personDirectoryAttributeResolutionController() {
        return new PersonDirectoryAttributeResolutionController();
    }
    
    @RefreshScope
    @Bean
    public ConfigurationStateController internalConfigController() {
        return new ConfigurationStateController();
    }

    @Bean
    public HealthCheckController healthCheckController() {
        return new HealthCheckController(healthCheckMonitor, casProperties.getHttpClient().getAsyncTimeout());
    }

    @Bean
    public SingleSignOnSessionsReportController singleSignOnSessionsReportController() {
        return new SingleSignOnSessionsReportController(centralAuthenticationService);
    }

    @RefreshScope
    @Bean
    @Autowired
    public LoggingConfigController loggingConfigController(@Qualifier("auditTrailManager") final DelegatingAuditTrailManager auditTrailManager) {
        return new LoggingConfigController(auditTrailManager);
    }

    @Bean
    public StatisticsController statisticsController() {
        return new StatisticsController(centralAuthenticationService, metricsRegistry, healthCheckRegistry, casProperties.getHost().getName());
    }

    @Override
    public void configureMessageBroker(final MessageBrokerRegistry config) {
        config.enableSimpleBroker("/logs");
        if (StringUtils.isNotBlank(serverProperties.getContextPath())) {
            config.setApplicationDestinationPrefixes(serverProperties.getContextPath());
        }
    }

    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        registry.addEndpoint("/logoutput")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
    }

    /**
     * The Trusted devices configuration for the UI.
     */
    @ConditionalOnClass(value = MultifactorAuthenticationTrustStorage.class)
    @Configuration("trustedDevicesConfiguration")
    public static class TrustedDevicesConfiguration {

        @Autowired
        @Bean
        public TrustedDevicesController trustedDevicesController(@Qualifier("mfaTrustEngine") final MultifactorAuthenticationTrustStorage mfaTrustEngine) {
            return new TrustedDevicesController(mfaTrustEngine);
        }
    }

    /**
     * The type Authentication events configuration.
     */
    @ConditionalOnClass(value = CasEventRepository.class)
    @Configuration("authenticationEventsConfiguration")
    public static class AuthenticationEventsConfiguration {

        @Autowired
        @Bean
        public AuthenticationEventsController authenticationEventsController(@Qualifier("casEventRepository") final CasEventRepository eventRepository) {
            return new AuthenticationEventsController(eventRepository);
        }
    }
}
