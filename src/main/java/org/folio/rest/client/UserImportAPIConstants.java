package org.folio.rest.client;

public interface UserImportAPIConstants {

  String FAILED_TO_PROCESS_USER_SEARCH_RESPONSE = "Failed to process user search response.";
  String FAILED_TO_PROCESS_USERS = "Failed to process users.";
  String FAILED_TO_PROCESS_USER_SEARCH_RESULT = "Failed to process user search result.";
  String FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID = "Failed to add permissions for user with externalSystemId: ";
  String FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID = "Failed to update user with externalSystemId: ";
  String FAILED_TO_IMPORT_USERS = "Failed to import users.";
  String FAILED_TO_LIST_ADDRESS_TYPES = "Failed to list address types.";

  String HTTP_HEADER_CONTENT_TYPE = "Content-type";
  String HTTP_HEADER_VALUE_APPLICATION_JSON = "application/json";
  String HTTP_HEADER_ACCEPT = "Accept";
  String OKAPI_URL_HEADER = "X-Okapi-URL";
  String OKAPI_TOKEN_HEADER = "X-Okapi-Token";
  String OKAPI_TENANT_HEADER = "X-Okapi-Tenant";

}
