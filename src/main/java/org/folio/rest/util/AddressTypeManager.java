package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.*;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AddressTypeManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(AddressTypeManager.class);

  private AddressTypeManager() {
  }

  public static Future<Map<String, String>> getAddressTypes(HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    Promise<Map<String, String>> future = Promise.promise();

    Map<String, String> headers = HttpClientUtil.createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, null);
    final String addressTypeQuery = UriBuilder.fromPath("/addresstypes").build().toString();

    try {
      httpClient.request(addressTypeQuery, headers)
        .whenComplete((addressTypeResponse, ex) -> {
          if (ex != null) {
            LOGGER.error(FAILED_TO_LIST_ADDRESS_TYPES);
            LOGGER.debug(ex.getMessage());
            future.fail(ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(addressTypeResponse.getCode())) {
            LOGGER.warn(FAILED_TO_LIST_ADDRESS_TYPES);
            future.fail("");
          } else {
            JsonObject resultObject = addressTypeResponse.getBody();
            JsonArray addressTypeArray = resultObject.getJsonArray("addressTypes");
            Map<String, String> addressTypes = extractAddressTypes(addressTypeArray);
            future.complete(addressTypes);
          }
        });

    } catch (Exception exc) {
      LOGGER.warn("Failed to list address types", exc.getMessage());
      future.fail(exc);
    }
    return future.future();
  }

  private static Map<String, String> extractAddressTypes(JsonArray addressTypes) {
    Map<String, String> addressTypeMap = new HashMap<>();
    for (int i = 0; i < addressTypes.size(); i++) {
      JsonObject addressType = addressTypes.getJsonObject(i);
      addressTypeMap.put(addressType.getString("addressType"), addressType.getString("id"));
    }
    return addressTypeMap;
  }
}
