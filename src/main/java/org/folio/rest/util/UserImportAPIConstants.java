package org.folio.rest.util;

public class UserImportAPIConstants {

  public static final String FAILED_TO_PROCESS_USER_SEARCH_RESPONSE = "Failed to process user search response.";
  public static final String FAILED_TO_PROCESS_USERS = "Failed to process users.";
  public static final String FAILED_TO_PROCESS_USER_SEARCH_RESULT = "Failed to process user search result.";
  public static final String FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID =
    "Failed to add permissions for user with externalSystemId: ";
  public static final String FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID =
    "Failed to create new user with externalSystemId: ";
  public static final String FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID = "Failed to update user with externalSystemId: ";
  public static final String FAILED_TO_IMPORT_USERS = "Failed to import users.";
  public static final String FAILED_TO_LIST_ADDRESS_TYPES = "Failed to list address types.";
  public static final String FAILED_TO_LIST_PATRON_GROUPS = "Failed to list patron groups.";
  public static final String FAILED_TO_LIST_SERVICE_POINTS = "Failed to list service points.";
  public static final String FAILED_TO_LIST_DEPARTMENTS = "Failed to list departments.";
  public static final String FAILED_TO_GET_USER_PREFERENCE = "Failed to get user preference.";
  public static final String FAILED_TO_CREATE_USER_PREFERENCE = "Failed to create new user preference.";
  public static final String FAILED_TO_UPDATE_USER_PREFERENCE = "Failed to update user preference.";
  public static final String FAILED_TO_DELETE_USER_PREFERENCE = "Failed to delete user preference.";
  public static final String FAILED_USER_PREFERENCE_VALIDATION = "User Preference validation failed: ";
  public static final String FAILED_TO_GET_USER_MODULE_ID = "Interface 'users' must be provided only by one module";
  public static final String ERROR_MESSAGE = " Error message: ";
  public static final String USERS_WERE_IMPORTED_SUCCESSFULLY = "Users were imported successfully.";
  public static final String USER_DEACTIVATION_SKIPPED = "Users were not deactivated because of import failures.";
  public static final String USER_SCHEMA_MISMATCH = "Failed to map existing users. This could be caused by schema mismatch.";

  public static final String HTTP_HEADER_CONTENT_TYPE = "Content-type";
  public static final String HTTP_HEADER_VALUE_APPLICATION_JSON = "application/json";
  public static final String HTTP_HEADER_VALUE_TEXT_PLAIN = "text/plain";
  public static final String HTTP_HEADER_ACCEPT = "Accept";
  public static final String OKAPI_URL_HEADER = "X-Okapi-URL";
  public static final String OKAPI_TOKEN_HEADER = "X-Okapi-Token";
  public static final String OKAPI_TENANT_HEADER = "X-Okapi-Tenant";
  public static final String OKAPI_MODULE_ID_HEADER = "X-Okapi-Module-Id";

  public static final String PUT_CUSTOM_FIELDS_ENDPOINT = "/custom-fields";
  public static final String GET_CUSTOM_FIELDS_ENDPOINT = "/custom-fields?offset=0&limit=" + Integer.MAX_VALUE;
  public static final String GET_MODULE_ID_ENDPOINT = "/_/proxy/tenants/%s/modules?provide=%s";
  public static final String USERS_INTERFACE_NAME = "users";

  public static final String ADDRESS_TYPES_ENDPOINT = "/addresstypes";
  public static final String PATRON_GROUPS_ENDPOINT = "/groups";
  public static final String SERVICE_POINTS_ENDPOINT = "/service-points";
  public static final String DEPARTMENTS_ENDPOINT = "/departments";
  public static final String REQUEST_PREFERENCES_ENDPOINT = "/request-preference-storage/request-preference";
  public static final String REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT = REQUEST_PREFERENCES_ENDPOINT + "%s";

  public static final int CONN_TO = 5000;
  public static final int IDLE_TO = 10000;

  private UserImportAPIConstants() {

  }
}
