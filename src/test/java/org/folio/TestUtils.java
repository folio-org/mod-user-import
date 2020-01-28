package org.folio;

import io.restassured.http.Header;

import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;

public class TestUtils {

  public static final String USER_IMPORT = "/user-import";
  public static final String FAILED_USERS = "failedUsers";
  public static final String FAILED_RECORDS = "failedRecords";
  public static final String UPDATED_RECORDS = "updatedRecords";
  public static final String CREATED_RECORDS = "createdRecords";
  public static final String TOTAL_RECORDS = "totalRecords";
  public static final String EXTERNAL_SYSTEM_ID = "externalSystemId";
  public static final String USERNAME = "username";
  public static final String USER_ERROR_MESSAGE = "errorMessage";

  public static final String ERROR = "error";
  public static final String MESSAGE = "message";
  public static final Header TENANT_HEADER = new Header("X-Okapi-Tenant", "diku");
  public static final Header TOKEN_HEADER = new Header("X-Okapi-Token", "import-test");
  public static final Header OKAPI_URL_HEADER = new Header("X-Okapi-Url", "http://localhost:9130");
  public static final Header JSON_CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");

  public static User generateUser(String barcode, String firstName, String lastName, String id) {
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
