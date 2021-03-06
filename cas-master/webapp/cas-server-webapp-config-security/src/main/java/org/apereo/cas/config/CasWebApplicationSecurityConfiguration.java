package org.apereo.cas.config;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.security.CasJdbcUserDetailsManagerConfigurer;
import org.apereo.cas.web.security.CasLdapUserDetailsManagerConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;

/**
 * This is {@link CasWebApplicationSecurityConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Configuration("casWebApplicationSecurityConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CasWebApplicationSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Override
    public void init(final AuthenticationManagerBuilder auth) throws Exception {
        if (StringUtils.isNotBlank(casProperties.getAdminPagesSecurity().getJdbc().getQuery())) {
            auth.apply(new CasJdbcUserDetailsManagerConfigurer(casProperties.getAdminPagesSecurity()));
        }
        if (StringUtils.isNotBlank(casProperties.getAdminPagesSecurity().getLdap().getBaseDn())
                && StringUtils.isNotBlank(casProperties.getAdminPagesSecurity().getLdap().getLdapUrl())
                && StringUtils.isNotBlank(casProperties.getAdminPagesSecurity().getLdap().getLdapAuthz().getRoleAttribute())
                && StringUtils.isNotBlank(casProperties.getAdminPagesSecurity().getLdap().getUserFilter())) {
            auth.apply(new CasLdapUserDetailsManagerConfigurer<>(casProperties.getAdminPagesSecurity()));
        }
    }

}
