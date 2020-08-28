package org.folio.rest.util;

import static org.folio.rest.util.CustomFieldsUtil.updateCfOptions;
import static org.folio.rest.util.HttpClientUtil.createHeaders;
import static org.folio.rest.util.HttpClientUtil.getOkapiUrl;
import static org.folio.rest.util.UserImportAPIConstants.CONN_TO;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_GET_USER_MODULE_ID;
import static org.folio.rest.util.UserImportAPIConstants.GET_CUSTOM_FIELDS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.HTTP_HEADER_VALUE_APPLICATION_JSON;
import static org.folio.rest.util.UserImportAPIConstants.HTTP_HEADER_VALUE_TEXT_PLAIN;
import static org.folio.rest.util.UserImportAPIConstants.IDLE_TO;
import static org.folio.rest.util.UserImportAPIConstants.OKAPI_MODULE_ID_HEADER;
import static org.folio.rest.util.UserImportAPIConstants.PUT_CUSTOM_FIELDS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.USERS_INTERFACE_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import org.folio.rest.jaxrs.model.CustomFields;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.model.UserImportData;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

public final class CustomFieldsManager {

  private CustomFieldsManager() {
  }

  public static Future<Void> prepareCustomFields(UserImportData importData, Map<String, String> okapiHeaders,
                                                 Vertx vertx) {
    Promise<Void> promise = Promise.promise();
    Map<String, Set<String>> customFieldsOptions = getCustomFieldsOptions(importData);

    Map<String, String> headers = new CaseInsensitiveMap<>(okapiHeaders);
    HttpClientInterface httpClient = HttpClientFactory
      .getHttpClient(getOkapiUrl(okapiHeaders), -1, TenantTool.tenantId(headers),
        true, CONN_TO, IDLE_TO, false, 30L);
    if (customFieldsOptions.isEmpty()) {
      promise.complete();
    } else {
      OkapiUtil.getModulesProvidingInterface(USERS_INTERFACE_NAME, headers, vertx)
        .compose(moduleIds -> updateHeaders(moduleIds, headers))
        .compose(o -> requestCustomFieldsDefinitions(httpClient, headers))
        .compose(jsonObjects -> updateCF(jsonObjects, customFieldsOptions, httpClient, headers))
        .onComplete(o -> {
          httpClient.closeClient();
          if (o.succeeded()) {
            promise.complete();
          } else {
            promise.fail(o.cause());
          }
        });
    }
    return promise.future();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Set<String>> getCustomFieldsOptions(UserImportData importData) {
    Map<String, Set<String>> customFieldsOptions = new HashMap<>();
    importData.getUsers()
      .stream()
      .map(User::getCustomFields)
      .filter(Objects::nonNull)
      .map(CustomFields::getAdditionalProperties)
      .map(Map::entrySet)
      .flatMap(Collection::stream)
      .forEach(entry -> {
        Set<String> options = customFieldsOptions.computeIfAbsent(entry.getKey(), s1 -> new HashSet<>());
        Object value = entry.getValue();
        if (value instanceof String) {
          options.add((String) value);
        } else if (value instanceof List) {
          options.addAll((List<String>) value);
        }
      });
    return customFieldsOptions;
  }

  private static Future<Void> updateHeaders(List<String> moduleIds, Map<String, String> headers) {
    if (moduleIds.size() != 1) {
      return Future.failedFuture(FAILED_TO_GET_USER_MODULE_ID);
    } else {
      headers.put(OKAPI_MODULE_ID_HEADER, moduleIds.get(0));
      return Future.succeededFuture();
    }
  }

  private static Future<JsonObject> requestCustomFieldsDefinitions(HttpClientInterface client, Map<String, String> headers) {
    Promise<JsonObject> promise = Promise.promise();
    try {
      client.request(GET_CUSTOM_FIELDS_ENDPOINT, headers)
        .whenComplete((response, ex) -> processResponse(promise, response, ex, Response::getBody));
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  private static Future<Void> updateCF(JsonObject cfCollection, Map<String, Set<String>> customFieldsOptions,
                                       HttpClientInterface client, Map<String, String> okapiHeaders) {
    Promise<Void> promise = Promise.promise();
    boolean isUpdated = updateCfOptions(cfCollection, customFieldsOptions);
    if (isUpdated) {
      Map<String, String> headers =
        createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
      try {
        client.request(HttpMethod.PUT, cfCollection, PUT_CUSTOM_FIELDS_ENDPOINT, headers)
          .whenComplete((response, ex) -> processResponse(promise, response, ex, r -> null));
      } catch (Exception e) {
        promise.fail(e);
      }
    } else {
      promise.complete();
    }
    return promise.future();
  }

  private static <T> void processResponse(Promise<T> promise, Response response, Throwable ex,
                                          Function<Response, T> completeFunction) {
    if (ex != null) {
      promise.fail(ex);
    } else if (response.getException() != null) {
      promise.fail(response.getException());
    } else if (isFailedResponseCode(response)) {
      String failureMessage =
        response.getError() != null ? response.getError().encode() : "Error code: " + response.getCode();
      promise.fail(failureMessage);
    } else {
      promise.complete(completeFunction.apply(response));
    }
  }

  private static boolean isFailedResponseCode(Response response) {
    return !Response.isSuccess(response.getCode());
  }
}
