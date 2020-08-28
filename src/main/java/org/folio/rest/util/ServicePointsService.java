package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_LIST_SERVICE_POINTS;
import static org.folio.rest.util.UserImportAPIConstants.SERVICE_POINTS_ENDPOINT;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class ServicePointsService {

  public static final String SERVICE_POINTS_ARRAY_KEY = "servicepoints";
  public static final String SERVICE_POINT_NAME_OBJECT_KEY = "name";

  private ServicePointsService(){}

  public static Future<Map<String, String>> getServicePoints(Map<String, String> okapiHeaders) {
    return RequestManager.get(okapiHeaders, SERVICE_POINTS_ENDPOINT, FAILED_TO_LIST_SERVICE_POINTS)
      .map(ServicePointsService::extractServicePoints);
  }

  private static Map<String, String> extractServicePoints(JsonObject result){
    return JsonObjectUtil.extractMap(result, SERVICE_POINTS_ARRAY_KEY, SERVICE_POINT_NAME_OBJECT_KEY);
  }
}