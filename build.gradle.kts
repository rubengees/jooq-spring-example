import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.core.Flyway
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType
import org.jooq.meta.jaxb.Matchers
import org.jooq.meta.jaxb.MatchersEnumType
import org.jooq.meta.jaxb.MatchersTableType
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.WaitAllStrategy
import org.testcontainers.utility.DockerImageName
import java.nio.file.Paths
import java.time.Duration

buildscript {
    dependencies {
        classpath(platform("org.springframework.boot:spring-boot-dependencies:3.2.3"))
        classpath("org.testcontainers:postgresql")
        classpath("org.flywaydb:flyway-core")
        classpath("org.postgresql:postgresql")
    }
}

plugins {
    id("org.springframework.boot") version "3.2.3"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("nu.studer.jooq") version "9.0"
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.3"))
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    jooqGenerator(platform("org.springframework.boot:spring-boot-dependencies:3.2.3"))
    jooqGenerator("org.postgresql:postgresql")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.zonky.test:embedded-database-spring-test:2.5.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/postgres"
                    user = "postgres"
                    password = "postgres"
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"

                        isIncludeIndexes = false
                        isIncludeSequences = false
                        isIncludeForeignKeys = false
                        isIncludeUniqueKeys = false

                        generate.apply {
                            isDaos = true
                            isImmutablePojos = true
                            isDeprecationOnUnknownTypes = false
                            isKotlinNotNullPojoAttributes = true
                        }
                        target.apply {
                            packageName = "org.example"
                            directory = "${layout.buildDirectory.get()}/generated/jooq/main"
                        }
                        strategy.apply {
                            matchers = Matchers().apply {
                                enums = listOf(
                                    MatchersEnumType().apply {
                                        enumClass = MatcherRule().apply {
                                            transform = MatcherTransformType.PASCAL
                                            expression = "DB_$0"
                                        }
                                    },
                                )
                                tables = listOf(
                                    MatchersTableType().apply {
                                        pojoClass = MatcherRule().apply {
                                            transform = MatcherTransformType.PASCAL
                                            expression = "$0_Pojo"
                                        }
                                        tableClass = MatcherRule().apply {
                                            transform = MatcherTransformType.PASCAL
                                            expression = "$0_Table"
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


class KPostgresContainer(imageName: DockerImageName) : PostgreSQLContainer<KPostgresContainer>(imageName) {
    init {
        this.waitStrategy = WaitAllStrategy()
            .withStrategy(this.waitStrategy)
            .withStrategy(HostPortWaitStrategy())
            .withStartupTimeout(Duration.ofSeconds(60))
    }
}

tasks.withType(JooqGenerate::class.java).configureEach {
    val container = KPostgresContainer(DockerImageName.parse("postgres:16-alpine"))
        .withDatabaseName("postgres")
        .withUsername("postgres")
        .withPassword("postgres")
        .withLogConsumer(Slf4jLogConsumer(logger).withSeparateOutputStreams().withPrefix("postgres"))

    val migrationPath = Paths
        .get(projectDir.path, "src/main/resources/db/migration")
        .normalize().toAbsolutePath()

    doFirst {
        container.start()

        Flyway.configure()
            .dataSource(container.jdbcUrl, container.username, container.password)
            .locations("filesystem:$migrationPath")
            .load()
            .migrate()

        // Hack to get the private jooqConfiguration of the generate task.
        // Needed to dynamically set jdbc connection parameters.
        val jooqConfiguration = this::class.java.superclass.getDeclaredField("jooqConfiguration")
            .apply { isAccessible = true }
            .get(this) as Configuration

        jooqConfiguration.jdbc.apply {
            url = container.jdbcUrl
            username = container.username
            password = container.password
        }
    }

    doLast {
        container.stop()
    }

    inputs.files(fileTree(migrationPath))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    allInputsDeclared.set(true)
}
