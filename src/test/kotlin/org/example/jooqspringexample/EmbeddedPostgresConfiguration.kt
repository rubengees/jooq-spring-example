package org.example.jooqspringexample

import io.zonky.test.db.provider.postgres.PostgreSQLContainerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmbeddedPostgresConfiguration {
    @Bean
    fun postgresContainerCustomizer(): PostgreSQLContainerCustomizer {
        return PostgreSQLContainerCustomizer { container ->
            container.withReuse(true)
        }
    }
}
