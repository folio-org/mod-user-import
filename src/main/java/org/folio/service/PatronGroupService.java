package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS;
import static org.folio.rest.impl.UserImportAPIConstants.LIMIT_ALL;
import static org.folio.rest.impl.UserImportAPIConstants.PATRON_GROUPS_ENDPOINT;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.folio.util.HttpClientUtil;
import org.folio.util.JsonObjectUtil;

public class PatronGroupService {

  private static final String USER_GROUPS_ARRAY_KEY = "usergroups";
  private static final String USER_GROUP_NAME_OBJECT_KEY = "group";

  public Future<Map<String, String>> getPatronGroups(WebClient webClient, Map<String, String> okapiHeaders) {
    final String query = PATRON_GROUPS_ENDPOINT + LIMIT_ALL;

    return HttpClientUtil.webClientOkapi(webClient, HttpMethod.GET, okapiHeaders, query)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .map(res -> extractPatronGroups(res.bodyAsJsonObject()))
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_LIST_PATRON_GROUPS));
  }

  private Map<String, String> extractPatronGroups(JsonObject result) {
    return JsonObjectUtil.extractMap(result, USER_GROUPS_ARRAY_KEY, USER_GROUP_NAME_OBJECT_KEY);
  }
}
