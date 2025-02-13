import org.flywaydb.core.Flyway
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.codegen.gradle.CodegenTask
import org.jooq.meta.jaxb.MatcherTransformType
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.utility.DockerImageName
import java.time.Duration

buildscript {
    dependencies {
        classpath(platform(libs.spring.boot.dependencies))
        classpath(libs.testcontainers.postgresql)
        classpath(libs.flyway.core)
        classpath(libs.flyway.postgresql)
        classpath(libs.postgresql)
    }
}

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.jooq.codegen)
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.jooq)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.jooq)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.postgresql)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.embedded.database.spring.test)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

jooq {
    configuration {
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
                excludes = "flyway_.*"
                schemaVersionProvider =
                    """
                        SELECT 'v' || version || '/' || checksum
                        FROM flyway_schema_history
                        ORDER BY version DESC LIMIT 1
                    """.trimIndent()
            }
            generate {
                isDaos = true
                isImmutablePojos = true
                isDeprecationOnUnknownTypes = false
                isKotlinNotNullPojoAttributes = true
            }
            target {
                packageName = "org.example"
                directory = "build/generated/jooq/main"
            }
            strategy {
                matchers {
                    enums {
                        enum_ {
                            enumClass {
                                transform = MatcherTransformType.PASCAL
                                expression = "DB_$0"
                            }
                        }
                    }
                    tables {
                        table {
                            pojoClass {
                                transform = MatcherTransformType.PASCAL
                                expression = "$0_Pojo"
                            }
                            tableClass {
                                transform = MatcherTransformType.PASCAL
                                expression = "$0_Table"
                            }
                        }
                    }
                }
            }
        }

        jdbc {
            url = "jdbc:postgresql://localhost:53948/postgres"
            username = "postgres"
            password = "postgres"
        }
    }

    val imageName = DockerImageName.parse("postgres:17-alpine")

    val container = WaitingForPortPostgresContainer(imageName)
        .withDatabaseName("postgres")
        .withUsername("postgres")
        .withPassword("postgres")
        .withLogConsumer(Slf4jLogConsumer(logger).withSeparateOutputStreams().withPrefix("postgres"))
        .apply { portBindings = listOf("53948:${PostgreSQLContainer.POSTGRESQL_PORT}") }

    val jooqCodegenCleanup = tasks.register("jooqCodegenCleanup") {
        doLast { container.stop() }
    }

    tasks.withType<CodegenTask> {
        val schemaLocation = file("src/main/resources/db/migration")

        inputs.dir(schemaLocation)

        doFirst {
            container.start()

            Flyway
                .configure()
                .dataSource(container.jdbcUrl, container.username, container.password)
                .locations("filesystem:${schemaLocation.absolutePath}")
                .load()
                .migrate()
        }

        doLast { container.stop() }

        finalizedBy(jooqCodegenCleanup)
    }

    tasks.withType<KotlinCompile> {
        dependsOn(tasks.withType(CodegenTask::class))
    }
}

class WaitingForPortPostgresContainer(imageName: DockerImageName) :
    PostgreSQLContainer<WaitingForPortPostgresContainer>(imageName) {

    init {
        this.waitStrategy = WaitAllStrategy()
            .withStrategy(this.waitStrategy)
            .withStrategy(HostPortWaitStrategy())
            .withStartupTimeout(Duration.ofSeconds(60))
    }
}
