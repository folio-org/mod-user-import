package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.ADDRESS_TYPES_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import org.folio.rest.tools.client.interfaces.HttpClientInterface;

public class AddressTypeManager {

  private static final String ADDRESS_TYPES_ARRAY_KEY = "addressTypes";
  private static final String ADDRESS_TYPE_NAME_OBJECT_KEY = "addressType";

  private AddressTypeManager() {}

  public static Future<Map<String, String>> getAddressTypes(HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    return RequestManager.get(httpClient, okapiHeaders, ADDRESS_TYPES_ENDPOINT, FAILED_TO_LIST_ADDRESS_TYPES)
      .map(AddressTypeManager::extractAddressTypes);
  }

  private static Map<String, String> extractAddressTypes(JsonObject result) {
    return JsonObjectUtil.extractMap(result, ADDRESS_TYPES_ARRAY_KEY, ADDRESS_TYPE_NAME_OBJECT_KEY);
  }
}
