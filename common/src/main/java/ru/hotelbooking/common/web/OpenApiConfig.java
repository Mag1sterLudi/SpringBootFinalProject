package ru.hotelbooking.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI с кнопкой Authorize для ввода JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI serviceOpenApi(@Value("${spring.application.name}") String applicationName,
                                  @Value("${app.api.description:}") String description) {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(new Info().title(applicationName + " API").version("v1").description(description))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
