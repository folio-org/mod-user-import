package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import static org.folio.TestUtils.CREATED_RECORDS;
import static org.folio.TestUtils.ERROR;
import static org.folio.TestUtils.EXTERNAL_SYSTEM_ID;
import static org.folio.TestUtils.FAILED_RECORDS;
import static org.folio.TestUtils.FAILED_USERS;
import static org.folio.TestUtils.JSON_CONTENT_TYPE_HEADER;
import static org.folio.TestUtils.MESSAGE;
import static org.folio.TestUtils.OKAPI_URL_HEADER;
import static org.folio.TestUtils.TENANT_HEADER;
import static org.folio.TestUtils.TOKEN_HEADER;
import static org.folio.TestUtils.TOTAL_RECORDS;
import static org.folio.TestUtils.UPDATED_RECORDS;
import static org.folio.TestUtils.USERNAME;
import static org.folio.TestUtils.USER_ERROR_MESSAGE;
import static org.folio.TestUtils.USER_IMPORT;
import static org.folio.TestUtils.generateUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import org.folio.rest.jaxrs.model.RequestPreference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.folio.rest.util.UserImportAPIConstants;

@RunWith(VertxUnitRunner.class)
public class UserImportAPITest {

  public static final int PORT = 8081;
  private Vertx vertx;
  private HttpClientMock2 mock;

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

    mock = new HttpClientMock2("http://localhost:9130", "diku");

  }

  @After
  public void tearDown(TestContext context) throws Exception {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testImportWithoutUsers() throws IOException {

    mock.setMockJsonContent("mock_content.json");

    UserdataimportCollection collection = new UserdataimportCollection();
    collection.setUsers(new ArrayList<>());
    collection.setTotalRecords(0);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("No users to import."))
      .body(TOTAL_RECORDS, equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithAddressTypeResponseError() throws IOException {

    mock.setMockJsonContent("mock_address_types_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithPatronGroupResponseError() throws IOException {

    mock.setMockJsonContent("mock_patron_groups_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserCreation() throws IOException {

    mock.setMockJsonContent("mock_user_creation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
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
  public void testImportWithUserCreationWithoutPersonalData() throws IOException {

    mock.setMockJsonContent("mock_user_creation_without_personal_data.json");

    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setPersonal(null);
    List<User> users = new ArrayList<>();
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
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
  public void testImportWithUserCreationWithNonExistingPatronGroup() throws IOException {

    mock.setMockJsonContent("mock_user_creation_with_non_existing_patron_group.json");

    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setPatronGroup("nonExistingTestPatronGroup");
    List<User> users = new ArrayList<>();
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(
        UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + users.get(0).getExternalSystemId()))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserWithoutExternalSystemId() throws IOException {

    mock.setMockJsonContent("mock_user_creation_without_externalsystemid.json");

    List<User> users = new ArrayList<>();
    User testUser = generateUser("1234567", "Amy", "Cabble", null);
    testUser.setExternalSystemId(null);
    users.add(testUser);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  /*
   * This test does not reflect real-time environment currently.
   */
  //  @Test
  public void testImportWithUserWithEmptyExternalSystemId() throws IOException {

    mock.setMockJsonContent("mock_user_creation_with_empty_externalsystemid.json");

    List<User> users = new ArrayList<>();
    User testUser = generateUser("1234567", "Amy", "Cabble", null);
    testUser.setExternalSystemId("");
    users.add(testUser);
    users.add(testUser);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(2))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(testUser.getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(testUser.getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        equalTo(UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + testUser.getExternalSystemId()))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserWithoutUsername() throws IOException {

    mock.setMockJsonContent("mock_user_creation.json");

    List<User> users = new ArrayList<>();
    User testUser = generateUser("1234567", "Amy", "Cabble", null);
    testUser.setUsername(null);
    users.add(testUser);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  @Test
  public void testImportWithUserCreationAndPermissionError() throws IOException {

    mock.setMockJsonContent("mock_user_creation_with_permission_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
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
  public void testImportWithUserSearchError() throws IOException {

    mock.setMockJsonContent("mock_user_search_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(
        UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT + UserImportAPIConstants.ERROR_MESSAGE
          + UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESPONSE))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserCreationError() throws IOException {

    mock.setMockJsonContent("mock_user_creation_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("0000", "Error", "Error", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + users.get(0).getExternalSystemId()))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  /*
   * This test does not work as expected because the user creation endpoint can only be mocked once in a JSON file.
   * The solution couldtestImportWithUserAddressUpdate be to check the body of the input and decide if the response should be success or failure.
   */
  @Test
  public void testImportWithMoreUserCreation() throws IOException {

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

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(10);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(10))
      .body(CREATED_RECORDS, equalTo(10))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdate() throws IOException {

    mock.setMockJsonContent("mock_user_update.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    user.getPersonal().setPreferredFirstName("Preferred User");
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndWrongSchemaInUserSearchResult() throws IOException {

    mock.setMockJsonContent("mock_user_update_with_wrong_user_schema_in_search_result.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(false);

    StringBuilder resultMessageBuilder = new StringBuilder(UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT);
    resultMessageBuilder.append(UserImportAPIConstants.USER_SCHEMA_MISMATCH);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(resultMessageBuilder.toString()))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndWrongSchemaInUserSearchResultWithDeactivation() throws IOException {

    mock.setMockJsonContent("mock_user_update_with_wrong_user_schema_in_search_result_with_deactivation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(UserImportAPIConstants.USER_SCHEMA_MISMATCH))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserUpdateError() throws IOException {

    mock.setMockJsonContent("mock_user_update_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "228f3e79-9ebf-47a4-acaa-e8ffdff81ace"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        equalTo(UserImportAPIConstants.FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + users.get(0).getExternalSystemId()))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithMoreUserUpdateAndDeactivation() throws IOException {

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

    UserdataimportCollection collection = new UserdataimportCollection()
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
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("Deactivated missing users."))
      .body(TOTAL_RECORDS, equalTo(10))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(10))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithMoreUserUpdate() throws IOException {

    mock.setMockJsonContent("mock_more_user_update.json");

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
    users.add(generateUser("11x", "111x", "112x", null));
    users.add(generateUser("12x", "121x", "122x", null));
    users.add(generateUser("13x", "131x", "132x", null));
    users.add(generateUser("14x", "141x", "142x", null));
    users.add(generateUser("15x", "151x", "152x", null));
    users.add(generateUser("16x", "161x", "162x", null));
    users.add(generateUser("17x", "171x", "172x", null));
    users.add(generateUser("18x", "181x", "182x", null));
    users.add(generateUser("19x", "191x", "192x", null));
    users.add(generateUser("110x", "1101x", "1102x", null));

    UserdataimportCollection collection = new UserdataimportCollection()
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
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(20))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(20))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressUpdate() throws IOException {

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

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithExistingUserAddress() throws IOException {

    mock.setMockJsonContent("mock_import_with_existing_address.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("30313233", "User", "Address", "2cbf64a1-5904-4748-ae77-3d0605e911e7");
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressAdd() throws IOException {

    mock.setMockJsonContent("mock_import_with_address_add.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("30313233", "User", "Address", "2cbf64a1-5904-4748-ae77-3d0605e911e7");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Home")
      .withPrimaryAddress(Boolean.FALSE);

    Address address2 = new Address()
      .withAddressLine1("Test first line2")
      .withCity("Test city2")
      .withRegion("Test region2")
      .withPostalCode("123452")
      .withAddressTypeId("Home2")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    addresses.add(address2);
    user.getPersonal().setAddresses(addresses);
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressRewrite() throws IOException {

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

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserCreation() throws IOException {

    mock.setMockJsonContent("mock_prefixed_user_creation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("17181920", "Test", "User", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withSourceType("test");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
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
  public void testImportWithPrefixedUserUpdate() throws IOException {

    mock.setMockJsonContent("mock_prefixed_user_update.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("21222324", "User2", "Update2", "a3436a5f-707a-4005-804d-303220dd035b"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(false)
      .withSourceType("test2");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivateInSourceType() throws IOException {

    mock.setMockJsonContent("mock_deactivate_in_source_type.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("2526272829", "User2", "Deactivate2", null));

    UserdataimportCollection collection = new UserdataimportCollection()
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
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("Deactivated missing users."))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivateInSourceTypeWithDeactivationError() throws IOException {

    mock.setMockJsonContent("mock_deactivate_in_source_type_with_deactivation_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("2526272829", "User2", "Deactivate2", null));

    UserdataimportCollection collection = new UserdataimportCollection()
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
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("Deactivated missing users."))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithNoNeedToDeactivate() throws IOException {

    mock.setMockJsonContent("mock_no_need_to_deactivate.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("987654321", "User3", "Deactivate3", null));

    UserdataimportCollection collection = new UserdataimportCollection()
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
  public void testImportWithUserSearchErrorWhenDeactivating() throws IOException {

    mock.setMockJsonContent("mock_deactivate_search_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("987612345", "User4", "Deactivate4", null));

    UserdataimportCollection collection = new UserdataimportCollection()
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
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS + UserImportAPIConstants.ERROR_MESSAGE
        + UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(
        UserImportAPIConstants.FAILED_TO_IMPORT_USERS + UserImportAPIConstants.ERROR_MESSAGE
          + UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserCreationErrorWhenDeactivating() throws IOException {

    mock.setMockJsonContent("mock_user_creation_error_when_deactivating.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("0000", "Error", "Error", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(
        UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY + " " + UserImportAPIConstants.USER_DEACTIVATION_SKIPPED))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(
        UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + users.get(0).getExternalSystemId()))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithServicePointsResponseError() throws IOException {

    mock.setMockJsonContent("mock_service_points_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_LIST_SERVICE_POINTS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_LIST_SERVICE_POINTS))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithNewPreferenceCreation() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
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
  public void testImportWithUserPreferenceDeliveryIsFalseAndFulfillmentSpecified () throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "fulfillment must be not specified"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsFalseAndAddressTypeSpecified () throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "defaultDeliveryAddressTypeId must be not specified"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceInvalidDefaultServicePoint () throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717d")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsTrueAndInvalidAddressType() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717d3")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532c")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  @Test
  public void testImportWithUserPreferenceDefaultServicePointNotFound () throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("00000000-0000-1000-a000-000000000000")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "Provided defaultServicePointId value does not exist"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsTrueAndAddressTypeNotFound () throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("11111111-1111-1111-b111-111111111111")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "Provided defaultDeliveryAddressTypeId value does not exist"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsTrueAndFulfillmentIsNull () throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
        .withFulfillment(null)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "fulfillment must not be null"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndNewPreferenceCreation() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_new_preference_creation.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndExistingPreferenceUpdate() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_preference_update.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateWithNoPreference() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_no_user_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(OKAPI_URL_HEADER)
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);

  }
}
