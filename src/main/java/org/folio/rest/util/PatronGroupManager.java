package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS;
import static org.folio.rest.util.UserImportAPIConstants.PATRON_GROUPS_ENDPOINT;

import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import org.folio.rest.tools.client.interfaces.HttpClientInterface;

public class PatronGroupManager {

  private static final String USER_GROUPS_ARRAY_KEY = "usergroups";
  private static final String USER_GROUP_NAME_OBJECT_KEY = "group";

  private PatronGroupManager() {}

  public static Future<Map<String, String>> getPatronGroups(HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    final String query = UriBuilder.fromPath(PATRON_GROUPS_ENDPOINT).queryParam("limit",  "2147483647").build().toString();
    return RequestManager.get(httpClient, okapiHeaders, query, FAILED_TO_LIST_PATRON_GROUPS)
      .map(PatronGroupManager::extractPatronGroups);
  }

  private static Map<String, String> extractPatronGroups(JsonObject result) {
    return JsonObjectUtil.extractMap(result, USER_GROUPS_ARRAY_KEY, USER_GROUP_NAME_OBJECT_KEY);
  }
}
