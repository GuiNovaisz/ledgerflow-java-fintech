package com.ledgerflow.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
        return api -> {
            api.getComponents().addSchemas("ApiProblem", new ObjectSchema()
                    .addProperty("type", new StringSchema()).addProperty("title", new StringSchema())
                    .addProperty("status", new IntegerSchema()).addProperty("code", new StringSchema())
                    .addProperty("detail", new StringSchema()));
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                boolean session = path.startsWith("/api/v1/session/");
                boolean publicRoute = path.equals("/api/v1/session/csrf") || path.equals("/api/v1/session/login")
                        || path.equals("/api/v1/session/register") || path.equals("/api/v1/auth/login")
                        || path.equals("/api/v1/auth/register");
                if (publicRoute) operation.setSecurity(List.of());
                else if (session) operation.setSecurity(List.of(new SecurityRequirement().addList("cookieAuth")));
                if (!publicRoute) operation.getResponses().addApiResponse("401", problem("Authentication is required"));
                if (path.endsWith("/login")) operation.getResponses().addApiResponse("401", problem("Invalid credentials"));
                if (path.endsWith("/register")) operation.getResponses().addApiResponse("409", problem("Email is already registered"));
                if (path.startsWith("/api/v1/")) {
                    operation.getResponses().addApiResponse("400", problem("Invalid request fields, formats or limits"));
                    if (Set.of("POST", "PUT", "PATCH", "DELETE").contains(method.name()) && !path.startsWith("/api/v1/auth/"))
                        operation.getResponses().addApiResponse("403", problem("CSRF token or access denied"));
                }
                if (path.contains("/{id}") || path.contains("/by-key/"))
                    operation.getResponses().addApiResponse("404", problem("Resource is not visible to this user or operation is not committed"));
                if (path.startsWith("/api/v1/goals/") && Set.of("PUT", "DELETE").contains(method.name()))
                    operation.getResponses().addApiResponse("409", problem("Goal version changed"));
                if (path.startsWith("/api/v1/transactions/") && method.name().equals("POST"))
                    operation.getResponses().addApiResponse("409", problem("Insufficient funds, inactive account or idempotency conflict"));
                if (path.startsWith("/api/") && !path.startsWith("/api/v1/auth/")
                        && Set.of("POST", "PUT", "PATCH", "DELETE").contains(method.name())) {
                    operation.addParametersItem(new Parameter().in("header").name("X-CSRF-TOKEN").required(session)
                            .schema(new StringSchema()).description("Required with browser cookies. Fetch token and headerName from GET /api/v1/session/csrf; fetch again after login, registration or logout. Omitted for stateless bearer API requests."));
                }
            }));
        };
    }

    private ApiResponse problem(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))));
    }
}
