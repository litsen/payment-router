package com.company.payrouter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentRouterOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Router API")
                        .description("Multi merchant account payment routing backend APIs")
                        .version("v1"));
    }
}
