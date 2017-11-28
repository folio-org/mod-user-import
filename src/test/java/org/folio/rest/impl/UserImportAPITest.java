package org.folio.rest.impl;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataCollection;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.folio.rest.util.UserImportAPIConstants;
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
  public void testFakeEndpoint() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_content.json");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .get("/user-import")
      .then()
      .body(equalTo("This is a fake endpoint."))
      .statusCode(400);
  }

  @Test
  public void testImportWithoutUsers() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_content.json");

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
      .body("message", equalTo("No users to import."))
      .body("totalRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithAddressTypeResponseError() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_address_types_error.json");

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
      .body(equalTo(UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES))
      .statusCode(500);
  }

  @Test
  public void testImportWithPatronGroupResponseError() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_patron_groups_error.json");

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
      .body(equalTo(UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserCreation() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_user_creation.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(1))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserCreationAndPermissionError() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_user_creation_with_permission_error.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(1))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserSearchError() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_user_search_error.json");

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
      .body(equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS + UserImportAPIConstants.ERROR_MESSAGE + UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserCreationError() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_user_creation_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("0000", "Error", "Error", null));

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(0))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(1))
      .statusCode(200);
  }

  /*
   * This test does not work as expected because the user creation endpoint can only be mocked once in a JSON file.
   * The solution could be to check the body of the input and decide if the response should be success or failure.
   */
  @Test
  public void testImportWithMoreUserCreation() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_multiple_user_creation.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(10))
      .body("createdRecords", equalTo(10))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdate() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_user_update.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(0))
      .body("updatedRecords", equalTo(1))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateError() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_user_update_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "228f3e79-9ebf-47a4-acaa-e8ffdff81ace"));

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(0))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithMoreUserUpdateAndDeactivation() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_user_update_and_deactivation.json");

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
      .body("message", equalTo("Deactivated missing users."))
      .body("totalRecords", equalTo(10))
      .body("createdRecords", equalTo(0))
      .body("updatedRecords", equalTo(10))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressUpdate() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_import_with_address_update.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(0))
      .body("updatedRecords", equalTo(1))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressRewrite() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_import_with_address_rewrite.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(0))
      .body("updatedRecords", equalTo(1))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserCreation() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_prefixed_user_creation.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(1))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserUpdate() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_prefixed_user_update.json");

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
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(0))
      .body("updatedRecords", equalTo(1))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivateInSourceType() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_deactivate_in_source_type.json");

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
      .body("message", equalTo("Deactivated missing users."))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(1))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithNoNeedToDeactivate() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_no_need_to_deactivate.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("987654321", "User3", "Deactivate3", null));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true)
      .withSourceType("test4");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body("message", equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body("totalRecords", equalTo(1))
      .body("createdRecords", equalTo(1))
      .body("updatedRecords", equalTo(0))
      .body("failedRecords", equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserSearchErrorWhenDeactivating() throws IOException {

    HttpClientMock2 mock = new HttpClientMock2("http://localhost:9130", "diku");
    mock.setMockJsonContent("mock_deactivate_search_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("987612345", "User4", "Deactivate4", null));

    UserdataCollection collection = new UserdataCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true)
      .withSourceType("test5");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post("/user-import")
      .then()
      .body(equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .statusCode(500);
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
