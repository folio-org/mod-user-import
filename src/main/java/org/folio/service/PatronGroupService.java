package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS;
import static org.folio.rest.impl.UserImportAPIConstants.LIMIT_ALL;
import static org.folio.rest.impl.UserImportAPIConstants.PATRON_GROUPS_ENDPOINT;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import org.folio.util.HttpClientUtil;
import org.folio.util.JsonObjectUtil;

public class PatronGroupService {

  private static final String USER_GROUPS_ARRAY_KEY = "usergroups";
  private static final String USER_GROUP_NAME_OBJECT_KEY = "group";


  public Future<Map<String, String>> getPatronGroups(Map<String, String> okapiHeaders) {
    final String query = PATRON_GROUPS_ENDPOINT + LIMIT_ALL;
    return HttpClientUtil.get(okapiHeaders, query, FAILED_TO_LIST_PATRON_GROUPS)
      .map(this::extractPatronGroups);
  }

  private Map<String, String> extractPatronGroups(JsonObject result) {
    return JsonObjectUtil.extractMap(result, USER_GROUPS_ARRAY_KEY, USER_GROUP_NAME_OBJECT_KEY);
  }
}
