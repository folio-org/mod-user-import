package org.folio.rest.impl;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataCollection;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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
  public void testFakeEndpoint() {

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .get("/user-import")
      .then()
      .body(equalTo("This is a fake endpoint."))
      .statusCode(200);
  }

  @Test
  public void testImportWithoutUsers() {

    UserdataCollection collection = new UserdataCollection();
    collection.setUsers(new ArrayList<>());
    collection.setTotalRecords(0);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("No users to import."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserCreation() {

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithMoreUserCreation() {

    List<User> users = new ArrayList<>();
    users.add(generateUser("1", "11", "12", null));
    users.add(generateUser("2", "21", "22", null));
    users.add(generateUser("3", "31", "32", null));
    users.add(generateUser("4", "41", "42", null));
    users.add(generateUser("5", "51", "52", null));
    users.add(generateUser("6", "61", "62", null));
    users.add(generateUser("7", "71", "72", null));
    users.add(generateUser("8", "81", "82", null));
    users.add(generateUser("9", "91", "92", null));
    users.add(generateUser("10", "101", "102", null));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(10);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdate() {
    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3"));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithMoreUserUpdateAndDeactivation() {

    List<User> users = new ArrayList<>();
    users.add(generateUser("11", "111", "112", null));
    users.add(generateUser("12", "121", "122", null));
    users.add(generateUser("13", "131", "132", null));
    users.add(generateUser("14", "141", "142", null));
    users.add(generateUser("15", "151", "152", null));
    users.add(generateUser("16", "161", "162", null));
    users.add(generateUser("17", "171", "172", null));
    users.add(generateUser("18", "181", "182", null));
    users.add(generateUser("19", "191", "192", null));
    users.add(generateUser("110", "1101", "1102", null));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(10)
      .withDeactivateMissingUsers(true)
      .withUpdateOnlyPresentFields(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Deactivated missing users."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressUpdate() {
    List<User> users = new ArrayList<>();
    User user = generateUser("30313233", "User", "Address", "2cbf64a1-5904-4748-ae77-3d0605e911e7");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Home")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    users.add(user);

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressRewrite() {
    List<User> users = new ArrayList<>();
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
    users.add(user);

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserCreation() {

    List<User> users = new ArrayList<>();
    users.add(generateUser("17181920", "Test", "User", null));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withSourceType("test");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserUpdate() {

    List<User> users = new ArrayList<>();
    users.add(generateUser("21222324", "User2", "Update2", "a3436a5f-707a-4005-804d-303220dd035b"));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withSourceType("test2");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Users were imported successfully."))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivateInSourceType() {
    List<User> users = new ArrayList<>();
    users.add(generateUser("2526272829", "User2", "Deactivate2", null));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true)
      .withSourceType("test3");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo("Deactivated missing users."))
      .statusCode(200);
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
