package org.folio.rest.util;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import java.util.Map;

import static org.folio.rest.util.UserImportAPIConstants.DEPARTMENTS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_LIST_DEPARTMENTS;

public class DepartmentsManager {

  private static final String DEPARTMENTS_ARRAY_KEY = "departments";
  private static final String DEPARTMENT_NAME_OBJECT_KEY = "name";

  private DepartmentsManager() {}

  public static Future<Map<String, String>> getDepartments(HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    return RequestManager.get(httpClient, okapiHeaders, DEPARTMENTS_ENDPOINT, FAILED_TO_LIST_DEPARTMENTS)
      .map(DepartmentsManager::extractDepartments);
  }

  private static Map<String, String> extractDepartments(JsonObject result) {
    return JsonObjectUtil.extractMap(result, DEPARTMENTS_ARRAY_KEY, DEPARTMENT_NAME_OBJECT_KEY);
  }
}
