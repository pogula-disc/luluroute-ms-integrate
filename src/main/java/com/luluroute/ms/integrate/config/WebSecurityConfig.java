package com.luluroute.ms.integrate.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * author: subramanian venkatraman
 **/

@Slf4j
@Configuration
@EnableWebSecurity
@EnableCaching
@RequiredArgsConstructor
public class WebSecurityConfig {

    private static final String[] AUTH_WHITELIST = { "/swagger-resources/**", "/swagger-ui.html", "/swagger-ui/*",
            "/swagger-ui/index.html", "/v2/api-docs/**", "/v3/api-docs/**", "/webjars/**", "/configuration/ui",
            "/configuration/security", "/v1/api/integrate/**",
            "/dev/luluroute-ms-integrate/swagger-ui/index.html", "/favicon.ico", "**/v3/**" };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeHttpRequests(authorize -> authorize.requestMatchers(AUTH_WHITELIST).permitAll()
                        .requestMatchers("/actuator/**", "/v1/api/integrate/**").permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        AtomicReference<Collection<GrantedAuthority>> grantedAuthorities1 = null;
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            try {
                List<String> authorities = jwt.getClaimAsStringList("authorities");

                if (authorities == null) {
                    authorities = Collections.emptyList();
                }

                Collection<GrantedAuthority> userAuthorities = authorities.stream().map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                JwtGrantedAuthoritiesConverter scopesAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
                Collection<GrantedAuthority> scopeAuthorities = scopesAuthoritiesConverter.convert(jwt);

                Collection<GrantedAuthority> grantedAuthorities = userAuthorities;
                grantedAuthorities.addAll(scopeAuthorities);
                log.info("APP_MESSAGE=\"Granted authorities :: \"" + grantedAuthorities);
                return grantedAuthorities;

            } catch (Exception e) {
                log.error("Error while validating the request : " + e.getMessage());
                throw e;
            }
        });

        return jwtAuthenticationConverter;
    }

}

