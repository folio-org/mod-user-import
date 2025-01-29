package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 * This integration test runs in the maven integration-test phase and tests that
 * the shaded fat uber jar works, that the Docker container works, and that
 * the interaction with the mod-users container works.
 */
public class UserImportIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserImportIT.class);
  private static final String POSTGRES_IMAGE_NAME = PostgresTesterContainer.getImageName();
  private static final Network network = Network.newNetwork();
  /** set true for debugging. */
  public static final boolean IS_LOG_ENABLED = false;
  private static String modUsersUri;

  @ClassRule
  public static final GenericContainer<?> module =
    new GenericContainer<>(new ImageFromDockerfile("mod-user-import").withFileFromPath(".", Path.of(".")))
      .withNetwork(network)
      .withNetworkAliases("module")
      .withExposedPorts(8081);

  @ClassRule
  public static final GenericContainer<?> modUsers =
    new GenericContainer<>(DockerImageName.parse("folioorg/mod-users:19.0.0"))
      .withNetwork(network)
      .withNetworkAliases("mod-users")
      .withExposedPorts(8081)
      .withEnv("DB_HOST", "postgres")
      .withEnv("DB_PORT", "5432")
      .withEnv("DB_USERNAME", "username")
      .withEnv("DB_PASSWORD", "password")
      .withEnv("DB_DATABASE", "postgres");

  @ClassRule
  public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE_NAME)
    .withNetwork(network)
    .withNetworkAliases("postgres")
    .withExposedPorts(5432)
    .withUsername("username")
    .withPassword("password")
    .withDatabaseName("postgres");

  @ClassRule
  public static final WireMockRule OKAPI_MOCK =
      new WireMockRule(WireMockConfiguration.wireMockConfig()
        .notifier(new ConsoleNotifier(IS_LOG_ENABLED))
        .dynamicPort());

  @BeforeClass
  public static void beforeClass() {
    Testcontainers.exposeHostPorts(OKAPI_MOCK.port());

    if (IS_LOG_ENABLED) {
      module.followOutput(new Slf4jLogConsumer(LOGGER)
          .withSeparateOutputStreams().withPrefix("mod-user-import"));
      modUsers.followOutput(new Slf4jLogConsumer(LOGGER)
          .withSeparateOutputStreams().withPrefix("mod-users"));
    }

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + module.getHost() + ":" + module.getFirstMappedPort();

    stubFor(get(urlEqualTo("/_/proxy/tenants/diku/modules?provide=custom-fields"))
        .willReturn(okJson("[{\"id\": \"mod-users\"}]")));
    stubFor(post(urlPathEqualTo("/perms/users"))
        .willReturn(aResponse().withStatus(201)));
    stubFor(get(urlPathEqualTo("/service-points"))
        .willReturn(okJson("{\"servicepoints\": []}")));
    forwardToModUsers("/addresstypes.*");
    forwardToModUsers("/custom-fields.*");
    forwardToModUsers("/departments.*");
    forwardToModUsers("/groups.*");
    forwardToModUsers("/proxiesfor.*");
    forwardToModUsers("/users.*");

    var okapiUrl = "http://host.testcontainers.internal:" + OKAPI_MOCK.port();
    RestAssured.requestSpecification = new RequestSpecBuilder()
      .addHeader("x-okapi-tenant", "diku")
      .addHeader("x-okapi-url", okapiUrl)
      .addHeader("x-okapi-url-to", okapiUrl)
      .addHeader("X-Okapi-User-Id", "2205005b-ca51-4a04-87fd-938eefa8f6de")  // username: rick
      .setContentType(ContentType.JSON)
      .build();

    modUsersUri = "http://" + modUsers.getHost() + ":" + modUsers.getFirstMappedPort();
    enableTenant();
  }

  private static void forwardToModUsers(String pathRegex) {
    stubFor(any(urlPathMatching(pathRegex))
        .willReturn(aResponse().proxiedFrom("http://mod-users:8081")));
  }

  private static void enableTenant() {
    String location =
        given().
          body(new JsonObject()
              .put("module_to", "99.99.99")
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
      get(modUsersUri + location + "?wait=30000").
    then().
      statusCode(200).  // getting job record succeeds
      body("complete", is(true)).  // job is complete
      body("error", is(nullValue()));  // job has succeeded without error
  }

  @Test
  public void healthTest() {
    when().
      get("/admin/health").
    then().
      statusCode(200);
  }

  @Test
  public void userImportWithCustomFields() {
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
      body("users[0].customFields.classYear", startsWith("opt_"));
  }

  private String getResource(String resource) {
    try {
      return IOUtils.resourceToString(resource, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

