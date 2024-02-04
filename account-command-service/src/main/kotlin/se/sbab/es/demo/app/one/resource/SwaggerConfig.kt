package se.sbab.es.demo.app.one.resource

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun api(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Account Service API")
                .description("An event store demo service handling bank accounts")
                .version("1.0")
                .license(License().name("Copyright by SBAB").url("https://www.sbab.se"))
                .contact(Contact().name("Team TNT"))
        )
}
