package org.folio.rest.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import static org.folio.TestUtils.CREATED_RECORDS;
import static org.folio.TestUtils.FAILED_RECORDS;
import static org.folio.TestUtils.FAILED_USERS;
import static org.folio.TestUtils.JSON_CONTENT_TYPE_HEADER;
import static org.folio.TestUtils.MESSAGE;
import static org.folio.TestUtils.TENANT_HEADER;
import static org.folio.TestUtils.TOKEN_HEADER;
import static org.folio.TestUtils.TOTAL_RECORDS;
import static org.folio.TestUtils.UPDATED_RECORDS;
import static org.folio.TestUtils.USER_IMPORT;
import static org.folio.TestUtils.generateUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.CustomFields;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;
import org.folio.rest.tools.client.test.HttpClientMock2;


@RunWith(VertxUnitRunner.class)
public class CustomFieldsManagerTest {

  public static final int PORT = 8081;
  public static final String HOST = "http://127.0.0.1";

  @Rule public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

  private Vertx vertx;
  private HttpClientMock2 mock;

  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", PORT)
        .put(HttpClientMock2.MOCK_MODE, "true"));

    vertx.deployVerticle(new RestVerticle(),
      options,
      context.asyncAssertSuccess());

    RestAssured.port = PORT;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    mock = new HttpClientMock2("http://localhost:9130", "import-test");

    stubFor(
      get(urlEqualTo("/_/proxy/tenants/diku/modules?provide=users"))
        .willReturn(aResponse().withBody("[\n"
          + "    {\n"
          + "        \"id\": \"mod-users\"\n"
          + "    }\n"
          + "]")));
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testImportUsersWithExistedCustomFieldOptions() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_custom_fields.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null)
      .withCustomFields(new CustomFields().withAdditionalProperty("department_1", "Design"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getWiremockUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportUsersWithNotExistedCustomFieldOptions() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_custom_fields.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null)
      .withCustomFields(new CustomFields().withAdditionalProperty("department_1", "Development"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getWiremockUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .content(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  private String getWiremockUrl() {
    return HOST + ":" + wireMockRule.port();
  }
}
