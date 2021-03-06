package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_SERVICE_POINTS;
import static org.folio.rest.impl.UserImportAPIConstants.SERVICE_POINTS_ENDPOINT;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import org.folio.util.HttpClientUtil;
import org.folio.util.JsonObjectUtil;

public class ServicePointsService {

  public static final String SERVICE_POINTS_ARRAY_KEY = "servicepoints";
  public static final String SERVICE_POINT_NAME_OBJECT_KEY = "name";


  public Future<Map<String, String>> getServicePoints(Map<String, String> okapiHeaders) {
    return HttpClientUtil.get(okapiHeaders, SERVICE_POINTS_ENDPOINT, FAILED_TO_LIST_SERVICE_POINTS)
      .map(this::extractServicePoints);
  }

  private Map<String, String> extractServicePoints(JsonObject result){
    return JsonObjectUtil.extractMap(result, SERVICE_POINTS_ARRAY_KEY, SERVICE_POINT_NAME_OBJECT_KEY);
  }
}
