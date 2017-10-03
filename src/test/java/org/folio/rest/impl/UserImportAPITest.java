package org.folio.rest.impl;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UserImportAPITest {

  private static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", "import-test");
  private static final Header TOKEN_HEADER = new Header("X-Okapi-Token", "import-test");
  private static final Header OKAPI_URL_HEADER = new Header("X-Okapi-Url", "http://localhost:9130");
  private static final Header JSON_CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");

  public static final int PORT = 8081;
  private Vertx vertx;

  @Before
  public void setUp(TestContext context) throws Exception {
    vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", PORT)
        .put(HttpClientMock2.MOCK_MODE, "true"));

    vertx.deployVerticle(new RestVerticle(),
      options,
      context.asyncAssertSuccess());

    RestAssured.port = PORT;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

  }

  @After
  public void tearDown(TestContext context) throws Exception {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testImportWithoutUsers() {

    JsonObject obj = new JsonObject();
    obj.put("users", new JsonArray());
    obj.put("totalRecords", 0);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("No users to import."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserCreation() {

    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    users.add(generateUserObject("1234567", "Amy", "Cabble", null));
    obj.put("users", users);
    obj.put("totalRecords", 1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdate() {
    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    users.add(generateUserObject("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3"));
    obj.put("users", users);
    obj.put("totalRecords", 1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressUpdate() {
    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    User user = generateUser("30313233", "User", "Address", "2cbf64a1-5904-4748-ae77-3d0605e911e7");
    Address address = new Address();
    address.setAddressLine1("Test first line");
    address.setCity("Test city");
    address.setRegion("Test region");
    address.setPostalCode("12345");
    address.setAddressTypeId("Home");
    address.setPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    users.add(JsonObject.mapFrom(user));
    obj.put("users", users);
    obj.put("totalRecords", 1);
    obj.put("updateOnlyPresentFields", true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressRewrite() {
    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    User user = generateUser("34353637", "User2", "Address2", "da4106eb-ec94-49ce-8019-9cc89281091c");
    Address address = new Address();
    address.setAddressLine1("Test first line");
    address.setCity("Test city");
    address.setRegion("Test region");
    address.setPostalCode("12345");
    address.setAddressTypeId("Home");
    address.setPrimaryAddress(Boolean.TRUE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    users.add(JsonObject.mapFrom(user));
    obj.put("users", users);
    obj.put("totalRecords", 1);
    obj.put("updateOnlyPresentFields", false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivate() {
    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    users.add(generateUserObject("13141516", "User", "Deactivate", null));
    obj.put("users", users);
    obj.put("totalRecords", 1);
    obj.put("deactivateMissingUsers", true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Deactivated missing users."))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserCreation() {

    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    users.add(generateUserObject("17181920", "Test", "User", null));
    obj.put("users", users);
    obj.put("totalRecords", 1);
    obj.put("sourceType", "test");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserUpdate() {

    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    users.add(generateUserObject("21222324", "User2", "Update2", "a3436a5f-707a-4005-804d-303220dd035b"));
    obj.put("users", users);
    obj.put("totalRecords", 1);
    obj.put("sourceType", "test2");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivateInSourceType() {
    JsonObject obj = new JsonObject();
    JsonArray users = new JsonArray();
    users.add(generateUserObject("2526272829", "User2", "Deactivate2", null));
    obj.put("users", users);
    obj.put("totalRecords", 1);
    obj.put("deactivateMissingUsers", true);
    obj.put("sourceType", "test3");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(obj.encode())
      .post("/user-import")
      .then()
      .body(equalTo("Deactivated missing users."))
      .statusCode(200);
  }

  private JsonObject generateUserObject(String barcode, String firstName, String lastName, String id) {
    User user = generateUser(barcode, firstName, lastName, id);
    return JsonObject.mapFrom(user);
  }

  private User generateUser(String barcode, String firstName, String lastName, String id) {
    String username = firstName.toLowerCase() + "_" + lastName.toLowerCase();
    User user = new User();
    if (id != null) {
      user.setId(id);
    }
    user.setBarcode(barcode);
    user.setExternalSystemId(username);
    user.setUsername(username);
    user.setActive(true);
    user.setPatronGroup("undergrad");
    Personal personal = new Personal();
    personal.setFirstName(firstName);
    personal.setLastName(lastName);
    personal.setEmail(username + "@user.org");
    personal.setPreferredContactTypeId("email");
    user.setPersonal(personal);
    return user;
  }
}
