package org.folio.rest.util;

import static org.folio.rest.util.CustomFieldsUtil.updateCfOptions;
import static org.folio.rest.util.HttpClientUtil.createHeaders;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_GET_USER_MODULE_ID;
import static org.folio.rest.util.UserImportAPIConstants.GET_CUSTOM_FIELDS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.HTTP_HEADER_VALUE_APPLICATION_JSON;
import static org.folio.rest.util.UserImportAPIConstants.OKAPI_MODULE_ID_HEADER;
import static org.folio.rest.util.UserImportAPIConstants.PUT_CUSTOM_FIELDS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.USERS_INTERFACE_NAME;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import org.folio.rest.jaxrs.model.CustomFields;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

public final class CustomFieldsManager {

  private CustomFieldsManager() {
    throw new UnsupportedOperationException("Util class");
  }

  public static Future<Void> checkAndUpdateCustomFields(HttpClientInterface httpClient, Map<String, String> okapiHeaders,
                                                        UserdataimportCollection userCollection, Vertx vertx) {
    Future<Void> future = Future.future();
    Map<String, Set<String>> customFieldsOptions = getCustomFieldsOptions(userCollection);

    if (customFieldsOptions.isEmpty()) {
      future.complete();
    } else {
      OkapiUtil.getModulesProvidingInterface(USERS_INTERFACE_NAME, okapiHeaders, vertx)
        .compose(moduleIds -> {
          if (moduleIds.size() != 1) {
            return Future.failedFuture(FAILED_TO_GET_USER_MODULE_ID);
          } else {
            okapiHeaders.put(OKAPI_MODULE_ID_HEADER, moduleIds.get(0));
            return requestCustomFieldsDefinitions(httpClient, okapiHeaders);
          }
        })
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

  private static Map<String, Set<String>> getCustomFieldsOptions(UserdataimportCollection userCollection) {
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
    return customFieldsOptions;
  }

  private static Future<JsonObject> requestCustomFieldsDefinitions(HttpClientInterface client, Map<String, String> headers) {
    Future<JsonObject> future = Future.future();
    try {
      client.request(GET_CUSTOM_FIELDS_ENDPOINT, headers)
        .whenComplete((response, ex) -> proceedResponse(future, response, ex, Response::getBody));
    } catch (Exception e) {
      future.fail(e);
    }
    return future;
  }

  private static Future<Void> updateCF(JsonObject cfCollection, Map<String, Set<String>> customFieldsOptions,
                                       HttpClientInterface client, Map<String, String> okapiHeaders) {
    Future<Void> future = Future.future();
    updateCfOptions(cfCollection, customFieldsOptions);
    Map<String, String> headers =
      createHeaders(okapiHeaders, null, HTTP_HEADER_VALUE_APPLICATION_JSON);
    try {
      client.request(HttpMethod.PUT, cfCollection, PUT_CUSTOM_FIELDS_ENDPOINT, headers)
        .whenComplete((response, ex) -> proceedResponse(future, response, ex, r -> null));
    } catch (Exception e) {
      future.fail(e);
    }
    return future;
  }

  private static <T> void proceedResponse(Future<T> future, Response response, Throwable ex,
                                          Function<Response, T> function) {
    if (ex != null) {
      future.fail(ex);
    } else if (response.getException() != null) {
      future.fail(response.getException());
    } else if (isFailedResponseCode(response)) {
      String failureMessage = response.getError() != null ? response.getError().encode() : "Error code" + response.getCode();
      future.fail(failureMessage);
    } else {
      future.complete(function.apply(response));
    }
  }

  private static boolean isFailedResponseCode(Response response) {
    return !Response.isSuccess(response.getCode());
  }
}