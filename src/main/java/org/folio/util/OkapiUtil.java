package org.folio.util;

import static org.folio.rest.impl.UserImportAPIConstants.GET_MODULE_ID_ENDPOINT;
import static org.folio.util.HttpClientUtil.getOkapiUrl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

import org.folio.okapi.common.OkapiClient;
import org.folio.rest.tools.utils.TenantTool;

public final class OkapiUtil {

  private OkapiUtil() {
  }

  public static Future<List<String>> getModulesProvidingInterface(String interfaceName, Map<String, String> okapiHeaders,
                                                                  Vertx vertx) {
    Promise<List<String>> promise = Promise.promise();
    OkapiClient okapiClient = new OkapiClient(getOkapiUrl(okapiHeaders), vertx, okapiHeaders);
    String requestUri = String.format(GET_MODULE_ID_ENDPOINT, TenantTool.tenantId(okapiHeaders), interfaceName);
    okapiClient.get(requestUri, response -> {
      AsyncResult<List<String>> asyncResult = response.map(s -> extractModuleIds(response.result()));
      completeFutureWithResult(promise, asyncResult);
    });
    return promise.future();
  }

  private static List<String> extractModuleIds(String json) {
    JsonArray jsonArray = new JsonArray(json);
    return IntStream.range(0, jsonArray.size())
      .mapToObj(jsonArray::getJsonObject)
      .map(o -> o.getString("id"))
      .distinct()
      .collect(Collectors.toList());
  }

  private static void completeFutureWithResult(Promise<List<String>> promise, AsyncResult<List<String>> result) {
    if (result.succeeded()) {
      promise.complete(result.result());
    } else {
      promise.fail(result.cause());
    }
  }
}
