package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_SERVICE_POINTS;
import static org.folio.rest.impl.UserImportAPIConstants.LIMIT_ALL;
import static org.folio.rest.impl.UserImportAPIConstants.SERVICE_POINTS_ENDPOINT;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.folio.util.HttpClientUtil;
import org.folio.util.JsonObjectUtil;

public class ServicePointsService {

  public static final String SERVICE_POINTS_ARRAY_KEY = "servicepoints";
  public static final String SERVICE_POINT_NAME_OBJECT_KEY = "name";


  public Future<Map<String, String>> getServicePoints(WebClient webClient, Map<String, String> okapiHeaders) {
    return HttpClientUtil.webClientOkapi(webClient, HttpMethod.GET, okapiHeaders, SERVICE_POINTS_ENDPOINT + LIMIT_ALL)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .map(res -> extractServicePoints(res.bodyAsJsonObject()))
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_LIST_SERVICE_POINTS));
  }

  private Map<String, String> extractServicePoints(JsonObject result){
    return JsonObjectUtil.extractMap(result, SERVICE_POINTS_ARRAY_KEY, SERVICE_POINT_NAME_OBJECT_KEY);
  }
}
