package org.folio.rest.util;

import static org.folio.rest.util.CustomFieldsUtil.updateCfOptions;
import static org.folio.rest.util.HttpClientUtil.createHeaders;
import static org.folio.rest.util.UserImportAPIConstants.GET_CUSTOM_FIELDS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.PUT_CUSTOM_FIELDS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.HTTP_HEADER_VALUE_APPLICATION_JSON;
import static org.folio.rest.util.UserImportAPIConstants.HTTP_HEADER_VALUE_TEXT_PLAIN;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import org.folio.rest.jaxrs.model.CustomFields;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

public final class CustomFieldsManager {

  private CustomFieldsManager() {
    throw new UnsupportedOperationException("Util class");
  }

  public static Future<Void> checkAndUpdateCustomFields(HttpClientInterface httpClient, Map<String, String> okapiHeaders,
                                                        UserdataimportCollection userCollection) {
    Future<Void> future = Future.future();
    Map<String, Set<String>> customFieldsOptions = new HashMap<>();
    List<CustomFields> customFields = userCollection.getUsers()
      .stream()
      .map(User::getCustomFields)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    for (CustomFields customField : customFields) {
      customField.getAdditionalProperties().forEach((s, o) -> {
        Set<String> options = customFieldsOptions.computeIfAbsent(s, s1 -> new HashSet<>());
        options.add((String) o);
      });
    }

    if (customFieldsOptions.isEmpty()) {
      future.complete();
    } else {
      requestCustomFieldsDefinitions(httpClient, okapiHeaders)
        .compose(jsonObjects -> updateCF(jsonObjects, customFieldsOptions, httpClient, okapiHeaders))
        .setHandler(o -> {
          if (o.succeeded()) {
            future.complete();
          } else {
            future.fail(o.cause());
          }
        });
    }
    return future;
  }

  private static Future<Void> updateCF(JsonObject cfCollection, Map<String, Set<String>> customFieldsOptions,
                                       HttpClientInterface httpClient, Map<String, String> okapiHeaders) {
    Future<Void> future = Future.future();
    updateCfOptions(cfCollection, customFieldsOptions);
    Map<String, String> headers =
      createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
    try {
      httpClient.request(HttpMethod.PUT, cfCollection, PUT_CUSTOM_FIELDS_ENDPOINT, headers)
        .whenComplete((response, ex) -> {
          if (ex != null) {
            future.fail(ex);
          } else if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
            future.fail(future.cause());
          } else {
            future.complete();
          }
        });
    } catch (Exception e) {
      future.fail(e);
    }
    return future;
  }

  private static Future<JsonObject> requestCustomFieldsDefinitions(HttpClientInterface client, Map<String, String> headers) {
    Future<JsonObject> future = Future.future();
    try {
      client.request(GET_CUSTOM_FIELDS_ENDPOINT, headers)
        .whenComplete((cfListResponse, ex) -> {
          if (ex != null) {
            future.fail(ex);
          } else if (!org.folio.rest.tools.client.Response.isSuccess(cfListResponse.getCode())) {
            future.fail(future.cause());
          } else {
            future.complete(cfListResponse.getBody());
          }
        });
    } catch (Exception e) {
      future.fail(e);
    }
    return future;
  }

}
