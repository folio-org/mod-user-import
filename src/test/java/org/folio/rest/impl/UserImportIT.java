package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.nginx.NginxContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * This integration test runs in the maven integration-test phase and tests that
 * the shaded fat uber jar works, that the Docker container works, and that
 * the interaction with the mod-users container works.
 */
@Testcontainers
class UserImportIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserImportIT.class);
  private static final String POSTGRES_IMAGE_NAME = PostgresTesterContainer.getImageName();
  private static final Network network = Network.newNetwork();
  private static String modUsersUri;

  @Container
  static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"))
    .withNetwork(network)
    .withNetworkAliases("kafka")
    .withListener("kafka:29092")
    .withStartupAttempts(3)
    .withLogConsumer(logConsumer("kafka"));

  @Container
  static final PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE_NAME)
    .withNetwork(network)
    .withNetworkAliases("postgres")
    .withUsername("username")
    .withPassword("password")
    .withDatabaseName("postgres")
    .withStartupAttempts(3)
    .withLogConsumer(logConsumer("pg"));

  @Container
  static final GenericContainer<?> modUserImport =
    new GenericContainer<>(new ImageFromDockerfile("mod-user-import").withFileFromPath(".", Path.of(".")))
      .withNetwork(network)
      .withNetworkAliases("mod-user-import")
      .withExposedPorts(8081)
      .withLogConsumer(logConsumer("mui"));

  @Container
  static final GenericContainer<?> modUsers =
    new GenericContainer<>(DockerImageName.parse("folioorg/mod-users:19.6.0"))
      .withNetwork(network)
      .withNetworkAliases("mod-users")
      .withExposedPorts(8081)
      .withEnv("DB_HOST", "postgres")
      .withEnv("DB_PORT", "5432")
      .withEnv("DB_USERNAME", "username")
      .withEnv("DB_PASSWORD", "password")
      .withEnv("DB_DATABASE", "postgres")
      .withEnv("KAFKA_HOST", "kafka")
      .withEnv("KAFKA_PORT", "29092")
      .dependsOn(postgres, kafka)
      .withLogConsumer(logConsumer("mod-users"));

  @Container
  static final NginxContainer okapi =  // mock okapi and other modules
    new NginxContainer("nginx:alpine-slim")
      .withNetwork(network)
      .withNetworkAliases("okapi")
      .withExposedPorts(9130)
      .withCopyToContainer(Transferable.of("""
          server {
            listen 9130;
            default_type application/json;
            location /_/proxy/tenants/diku/modules {
              return 200 '[{"id": "mod-users"}]';
            }
            location /perms/users {
              return 201;
            }
            location /service-points {
              return 200 '{"servicepoints": []}';
            }
            location / {
              proxy_pass http://mod-users:8081/;
              proxy_redirect default;
            }
          }
          """), "/etc/nginx/conf.d/default.conf")
      .dependsOn(modUsers)
      .withLogConsumer(logConsumer("okapi"));

  @BeforeAll
  static void beforeClass() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + modUserImport.getHost() + ":" + modUserImport.getFirstMappedPort();
    RestAssured.requestSpecification = new RequestSpecBuilder()
      .addHeader("x-okapi-tenant", "diku")
      .addHeader("x-okapi-url", "http://okapi:9130")
      .addHeader("x-okapi-url-to", "http://okapi:9130")
      .addHeader("X-Okapi-User-Id", "2205005b-ca51-4a04-87fd-938eefa8f6de")  // username: rick
      .setContentType(ContentType.JSON)
      .build();
    modUsersUri = "http://" + modUsers.getHost() + ":" + modUsers.getFirstMappedPort();
    enableModUsersTenant();
  }

  private static void enableModUsersTenant() {
    String location =
        given().
          body(new JsonObject()
              .put("module_to", "mod-users-99.99.99")
              .put("parameters", new JsonArray()
                  .add(new JsonObject().put("key", "loadReference").put("value", "true"))
                  .add(new JsonObject().put("key", "loadSample").put("value", "true")))
              .encode()).
        when().
          post(modUsersUri + "/_/tenant").
        then().
          statusCode(201).
        extract().
          header("Location");

    when().
      get(modUsersUri + location + "?wait=90000").
    then().
      statusCode(200).  // getting job record succeeds
      body("complete", is(true)).  // job is complete
      body("error", is(nullValue()));  // job has succeeded without error
  }

  @Test
  void healthTest() {
    when().
      get("/admin/health").
    then().
      statusCode(200);
  }

  @Test
  void userImportWithCustomFields() {
    given().
      body(getResource("/custom-fields.json")).
    when().
      post(modUsersUri + "/custom-fields").
    then().
      statusCode(201);

    given().
      body(getResource("/user-import.json")).
    when().
      post("/user-import").
    then().
      statusCode(200);

    when().
      get(modUsersUri + "/users?query=externalSystemId==10192").
    then().
      statusCode(200).
      body("users[0].username", is("chani")).
      body("users[0].personal.pronouns", is("He/Him")).
      body("users[0].customFields.classYear", startsWith("opt_"));
  }

  private static Consumer<OutputFrame> logConsumer(String prefix) {
    if (LOGGER.isDebugEnabled()) {
      return new Slf4jLogConsumer(LOGGER, true).withPrefix(prefix);
    } else {
      return outputFrame -> {};
    }
  }

  private String getResource(String resource) {
    try {
      return IOUtils.resourceToString(resource, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
