package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.ADDRESS_TYPES_ENDPOINT;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import org.folio.util.HttpClientUtil;
import org.folio.util.JsonObjectUtil;

public class AddressTypeService {

  private static final String ADDRESS_TYPES_ARRAY_KEY = "addressTypes";
  private static final String ADDRESS_TYPE_NAME_OBJECT_KEY = "addressType";

  private AddressTypeService() {}

  public static Future<Map<String, String>> getAddressTypes(Map<String, String> okapiHeaders) {
    return HttpClientUtil.get(okapiHeaders, ADDRESS_TYPES_ENDPOINT, FAILED_TO_LIST_ADDRESS_TYPES)
      .map(AddressTypeService::extractAddressTypes);
  }

  private static Map<String, String> extractAddressTypes(JsonObject result) {
    return JsonObjectUtil.extractMap(result, ADDRESS_TYPES_ARRAY_KEY, ADDRESS_TYPE_NAME_OBJECT_KEY);
  }
}
