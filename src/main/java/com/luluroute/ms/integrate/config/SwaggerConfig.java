package com.luluroute.ms.integrate.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * @author MANDALAKARTHIK1
 */
@Configuration
public class SwaggerConfig {

    @Value("${config.swagger.base.path}")
    private String basePath;

    @Value("${config.swagger.base.server.name}")
    private String serverUrl;

    @Value("${config.security.lulurouteOAuthToken.url}")
    private String oauthurl;


    @Bean
    public OpenAPI myOpenAPI() throws IOException {
        Info info = new Info().title("LULUROUTE Integrate carrier API").version("2.0")
                .contact(new Contact().name("integrate-api").email("")).description("LULUROUTE Integrate API");

        OpenAPI openAPI = new OpenAPI().info(info)
                .components(new Components().addSecuritySchemes("OAuth2",
                        new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().clientCredentials(new OAuthFlow().tokenUrl(oauthurl) //
                                        /*
                                         * .scopes(new Scopes().addString(appConfig.getScope(),
                                         * appConfig.getScopeDescription()))
                                         */
                                )))).security(getSecurityByName());

        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url(serverUrl ));
        openAPI.setServers(servers);
        return openAPI;
    }

    private List<SecurityRequirement> getSecurityByName() {
        List<SecurityRequirement> securityRequirementList = new ArrayList<>();
        securityRequirementList.add(new SecurityRequirement().addList("OAuth2"));
        return securityRequirementList;
    }

}