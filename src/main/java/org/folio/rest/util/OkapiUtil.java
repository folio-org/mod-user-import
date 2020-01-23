package org.folio.rest.util;

import static org.folio.rest.util.HttpClientUtil.getOkapiUrl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.core.UriBuilder;

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
    String requestUri = UriBuilder.fromPath("/_/proxy/tenants/")
      .segment(TenantTool.tenantId(okapiHeaders))
      .segment("modules")
      .queryParam("provide", interfaceName)
      .build().toString();
    okapiClient.get(requestUri, response -> {
      AsyncResult<List<String>> asyncResult = response.map(s -> extractModuleIds(response.result()));
      completeFutureWithResult(future, asyncResult);
    });
    return future;
  }

  private static List<String> extractModuleIds(String json) {
    JsonArray jsonArray = new JsonArray(json);
    return IntStream.of(0, jsonArray.size() - 1)
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
