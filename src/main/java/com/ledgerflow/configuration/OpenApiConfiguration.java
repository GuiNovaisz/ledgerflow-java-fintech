package com.ledgerflow.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI ledgerFlowOpenApi() {
        return new OpenAPI().info(new Info().title("LedgerFlow API").version("v1")
                        .description("Simulated personal finance ledger API"))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSecuritySchemes("cookieAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("LEDGERFLOW_SESSION")
                                .description("HttpOnly browser session. Obtain a CSRF token before mutations; do not send Authorization headers with this authentication mode.")));
    }

    @Bean
    OpenApiCustomizer browserSessionContracts() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean session = path.startsWith("/api/v1/session/");
            if (session) operation.setSecurity(path.endsWith("/csrf") ? List.of()
                    : List.of(new SecurityRequirement().addList("cookieAuth")));
            if (path.startsWith("/api/") && !path.startsWith("/api/v1/auth/")
                    && Set.of("POST", "PUT", "PATCH", "DELETE").contains(method.name())) {
                operation.addParametersItem(new Parameter().in("header").name("X-CSRF-TOKEN").required(session)
                        .schema(new StringSchema()).description("Required with browser cookies. Fetch token and headerName from GET /api/v1/session/csrf; fetch again after login, registration or logout. Omitted for stateless bearer API requests."));
            }
        }));
    }
}
