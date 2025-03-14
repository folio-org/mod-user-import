package org.folio.util;

import static org.folio.rest.validator.ChattyResponsePredicate.SC_OK;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import org.folio.HttpStatus;
import org.folio.okapi.common.ChattyHttpResponseExpectation;
import org.folio.okapi.common.ModuleId;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.tools.utils.TenantTool;

public final class OkapiUtil {

  private OkapiUtil() {
  }

  /**
   * Set the module id header for the interface.
   *
   * <p>See <a href="https://github.com/folio-org/okapi/blob/master/doc/guide.md#multiple-interfaces">
   * multiple interfaces</a> documentation.
   *
   * @param interfaceName the interface with "interfaceType": "multiple", for example custom-fields
   * @param moduleName the module name (product name) without version, for example mod-users
   * @param okapiHeaders where to get the "X-Okapi-Tenant" header and where to write the "X-Okapi-Module-Id" header
   * @return
   */
  public static Future<Void> setModuleIdForMultipleInterface(
      String interfaceName, String moduleName, Map<String, String> okapiHeaders) {

    String requestUri = "/_/proxy/tenants/%s/modules?provide=%s"
        .formatted(TenantTool.tenantId(okapiHeaders), PercentCodec.encode(interfaceName));
    return HttpClientUtil.getRequestOkapi(HttpMethod.GET, okapiHeaders, requestUri)
        .send()
        .expecting(ChattyHttpResponseExpectation.SC_OK)
        .compose(res -> {
          var list = extractModuleIds(res.bodyAsJsonArray(), moduleName);
          if (list.isEmpty()) {
            return Future.failedFuture("No module found.");
          }
          if (list.size() != 1) {
            return Future.failedFuture("Multiple modules found: " + list.toString());
          }
          okapiHeaders.put(XOkapiHeaders.MODULE_ID, list.get(0));
          return Future.<Void>succeededFuture();
        })
        .recover(e -> HttpClientUtil.errorManagement(e,
            "Failed to get module id for " + moduleName + " module providing interface " + interfaceName));
  }

  private static List<String> extractModuleIds(JsonArray jsonArray, String moduleName) {
    return IntStream.range(0, jsonArray.size())
        .mapToObj(jsonArray::getJsonObject)
        .map(o -> o.getString("id"))
        .filter(moduleId -> new ModuleId(moduleId).getProduct().equals(moduleName))
        .distinct()
        .toList();
  }

}
