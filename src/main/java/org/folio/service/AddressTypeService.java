package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.ADDRESS_TYPES_ENDPOINT;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES;
import static org.folio.rest.impl.UserImportAPIConstants.LIMIT_ALL;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.folio.util.HttpClientUtil;
import org.folio.util.JsonObjectUtil;

public class AddressTypeService {

  private static final String ADDRESS_TYPES_ARRAY_KEY = "addressTypes";
  private static final String ADDRESS_TYPE_NAME_OBJECT_KEY = "addressType";

  public Future<Map<String, String>> getAddressTypes(WebClient webClient, Map<String, String> okapiHeaders) {
    return HttpClientUtil.webClientOkapi(webClient, HttpMethod.GET, okapiHeaders, ADDRESS_TYPES_ENDPOINT + LIMIT_ALL)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .map(res -> extractAddressTypes(res.bodyAsJsonObject()))
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_LIST_ADDRESS_TYPES));
  }

  private Map<String, String> extractAddressTypes(JsonObject result) {
    return JsonObjectUtil.extractMap(result, ADDRESS_TYPES_ARRAY_KEY, ADDRESS_TYPE_NAME_OBJECT_KEY);
  }
}
