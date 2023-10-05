package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpForward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This integration test runs in the maven integration-test phase and tests that
 * the shaded fat uber jar works, that the Docker container works, and that
 * the interaction with the mod-users container works.
 */
public class UserImportIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserImportIT.class);
  private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
    .parse("mockserver/mockserver")
    .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());
  private static final Network network = Network.newNetwork();
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
  public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12-alpine")
    .withNetwork(network)
    .withNetworkAliases("postgres")
    .withExposedPorts(5432)
    .withUsername("username")
    .withPassword("password")
    .withDatabaseName("postgres");

  @ClassRule
  public static final MockServerContainer okapi =
    new MockServerContainer(MOCKSERVER_IMAGE)
      .withNetwork(network)
      .withNetworkAliases("okapi")
      .withExposedPorts(1080);

  @BeforeClass
  public static void beforeClass() {
    module.followOutput(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams());
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.baseURI = "http://" + module.getHost() + ":" + module.getFirstMappedPort();

    var mockServerClient = new MockServerClient(okapi.getHost(), okapi.getServerPort());
    mockServerClient.when(request("/_/proxy/tenants/diku/interfaces/custom-fields"))
      .respond(response().withStatusCode(200).withBody("[{\"id\": \"mod-users\"}]"));
    mockServerClient.when(request("/perms/users").withMethod("POST"))
      .respond(response().withStatusCode(201));
    mockServerClient.when(request("/service-points"))
      .respond(response().withStatusCode(200).withBody("{\"servicepoints\": []}"));
    forwardToModUsers(mockServerClient, "/addresstypes.*");
    forwardToModUsers(mockServerClient, "/custom-fields.*");
    forwardToModUsers(mockServerClient, "/departments.*");
    forwardToModUsers(mockServerClient, "/groups.*");
    forwardToModUsers(mockServerClient, "/proxiesfor.*");
    forwardToModUsers(mockServerClient, "/users.*");

    RestAssured.requestSpecification = new RequestSpecBuilder()
      .addHeader("x-okapi-tenant", "diku")
      .addHeader("x-okapi-url", "http://okapi:1080")
      .addHeader("x-okapi-url-to", "http://okapi:1080")
      .addHeader("X-Okapi-User-Id", "2205005b-ca51-4a04-87fd-938eefa8f6de")  // username: rick
      .setContentType(ContentType.JSON)
      .build();

    modUsersUri = "http://" + modUsers.getHost() + ":" + modUsers.getFirstMappedPort();
    enableTenant();
  }

  private static void forwardToModUsers(MockServerClient mockServerClient, String path) {
    mockServerClient.when(request(path))
      .forward(HttpForward.forward().withHost("mod-users").withPort(8081));
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

