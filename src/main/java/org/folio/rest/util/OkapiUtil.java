package org.folio.rest.util;

import static org.folio.rest.util.HttpClientUtil.getOkapiUrl;
import static org.folio.rest.util.UserImportAPIConstants.GET_MODULE_ID_ENDPOINT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

import org.folio.okapi.common.OkapiClient;
import org.folio.rest.tools.utils.TenantTool;

public final class OkapiUtil {

  private OkapiUtil() {
    throw new UnsupportedOperationException("Util class");
  }

  public static Future<List<String>> getModulesProvidingInterface(String interfaceName, Map<String, String> okapiHeaders,
                                                                  Vertx vertx) {
    Future<List<String>> future = Future.future();
    OkapiClient okapiClient = new OkapiClient(getOkapiUrl(okapiHeaders), vertx, okapiHeaders);
    String requestUri = String.format(GET_MODULE_ID_ENDPOINT, TenantTool.tenantId(okapiHeaders), interfaceName);
    okapiClient.get(requestUri, response -> {
      AsyncResult<List<String>> asyncResult = response.map(s -> extractModuleIds(response.result()));
      completeFutureWithResult(future, asyncResult);
    });
    return future;
  }

  private static List<String> extractModuleIds(String json) {
    JsonArray jsonArray = new JsonArray(json);
    return IntStream.range(0, jsonArray.size())
      .mapToObj(jsonArray::getJsonObject)
      .map(o -> o.getString("id"))
      .distinct()
      .collect(Collectors.toList());
  }

  private static void completeFutureWithResult(Future<List<String>> future, AsyncResult<List<String>> result) {
    if (result.succeeded()) {
      future.complete(result.result());
    } else {
      future.fail(result.cause());
    }
  }
}
