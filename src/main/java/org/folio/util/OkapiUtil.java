package org.folio.util;

import static org.folio.rest.impl.UserImportAPIConstants.GET_MODULE_ID_ENDPOINT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.folio.rest.tools.utils.TenantTool;

public final class OkapiUtil {

  private OkapiUtil() {
  }

  public static Future<List<String>> getModulesProvidingInterface(String interfaceName, Map<String, String> okapiHeaders) {

    String requestUri = String.format(GET_MODULE_ID_ENDPOINT, TenantTool.tenantId(okapiHeaders), interfaceName);
    return HttpClientUtil.getRequestOkapi(HttpMethod.GET, okapiHeaders, requestUri)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .map(res -> extractModuleIds(res.bodyAsJsonArray()));
  }

  private static List<String> extractModuleIds(JsonArray jsonArray) {
    return IntStream.range(0, jsonArray.size())
        .mapToObj(jsonArray::getJsonObject)
        .map(o -> o.getString("id"))
        .distinct()
        .collect(Collectors.toList());
  }

}
