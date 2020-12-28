package org.folio.util;

import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_ACCEPT;
import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_CONTENT_TYPE;
import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_VALUE_APPLICATION_JSON;
import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_VALUE_TEXT_PLAIN;
import static org.folio.rest.impl.UserImportAPIConstants.OKAPI_MODULE_ID_HEADER;
import static org.folio.rest.impl.UserImportAPIConstants.OKAPI_TENANT_HEADER;
import static org.folio.rest.impl.UserImportAPIConstants.OKAPI_TOKEN_HEADER;
import static org.folio.rest.impl.UserImportAPIConstants.OKAPI_URL_HEADER;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.base.Strings;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

public class HttpClientUtil {

  private static final Logger LOGGER =  LogManager.getLogger(HttpClientUtil.class);

  private HttpClientUtil() {
  }

  public static Future<JsonObject> get(Map<String, String> okapiHeaders, String query,
                                       String failedMessage) {
    Promise<JsonObject> future = Promise.promise();
    Map<String, String> headers = HttpClientUtil.createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, null);
    LOGGER.info("Do GET request: {}", query);
    try {
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      httpClient.request(query, headers)
        .whenComplete((response, ex) -> {
          if (ex != null) {
            LOGGER.error(failedMessage, ex.getMessage());
            LOGGER.debug(ex.getMessage());
            future.fail(failedMessage + ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
            LOGGER.warn(failedMessage + response.getError());
            future.fail(failedMessage + response.getError());
          } else {
            JsonObject resultObject = response.getBody();
            future.complete(resultObject);
          }
        });

    } catch (Exception exc) {
      LOGGER.warn(failedMessage + exc.getMessage());
      future.fail(exc);
    }
    return future.future();
  }

  public static <T> Future<T> post(Map<String, String> okapiHeaders, String query, Class<T> clazz, Object entity,
                                   String failedMessage) {

    Map<String, String> headers =
      createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
    Promise<T> promise = Promise.promise();
    try {
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      httpClient.request(HttpMethod.POST, JsonObject.mapFrom(entity), query, headers)
        .thenApply(response -> {
          try {
            if (Response.isSuccess(response.getCode())) {
              return Json.decodeValue(response.getBody().toString(), clazz);
            } else {
              LOGGER.error(failedMessage + response.getError().toString(), response.getException());
              throw new IllegalStateException(failedMessage + response.getError().toString());
            }
          } finally {
            httpClient.closeClient();
          }
        })
        .thenAccept(promise::complete)
        .exceptionally(e -> {
          promise.fail(e.getCause());
          return null;
        });
    } catch (Exception e) {
      LOGGER.error("Cannot perform a request: {} with body: {}. ", query, JsonObject.mapFrom(entity), e.getMessage(), e);
      promise.fail(e);
    }
    return promise.future();
  }

  public static Future<Void> put(Map<String, String> okapiHeaders, String query, Object entity, String failedMessage) {

    Promise<Void> future = Promise.promise();
    Map<String, String> headers =
      createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
    try {
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      LOGGER.info("Do PUT request: {}", query);
      httpClient.request(HttpMethod.PUT, JsonObject.mapFrom(entity), query, headers)
        .whenComplete(handleResponse(failedMessage, future, httpClient));
    } catch (Exception exc) {
      LOGGER.error(failedMessage, exc.getMessage());
      future.fail(failedMessage + exc.getMessage());
    }
    return future.future();
  }

  public static Future<Void> delete(Map<String, String> okapiHeaders, String query, String id, String failedMessage) {
    Promise<Void> future = Promise.promise();
    Map<String, String> headers =
      createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
    try {
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      LOGGER.info("Do DELETE request: {}", query);
      httpClient.request(HttpMethod.DELETE, id, query, headers)
        .whenComplete(handleResponse(failedMessage, future, httpClient));
    } catch (Exception exc) {
      LOGGER.error(failedMessage, exc.getMessage());
      future.fail(failedMessage + exc.getMessage());
    }
    return future.future();
  }

  public static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = getOkapiUrl(okapiHeaders);
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_TENANT_HEADER));

    return HttpClientFactory.getHttpClient(okapiURL, tenantId, true);
  }

  private static boolean isSuccess(org.folio.rest.tools.client.Response response, Throwable ex) {
    return ex == null && org.folio.rest.tools.client.Response.isSuccess(response.getCode());
  }

  @NotNull
  public static BiConsumer<Response, Throwable> handleResponse(String failedMessage, Promise<Void> future,
                                                               HttpClientInterface httpClient) {
    return (res, ex) -> {
      try {
        if (isSuccess(res, ex)) {
          future.complete();
        } else {
          future.fail(failedMessage + ex.getMessage());
        }
      } catch (Exception e) {
        LOGGER.error(failedMessage, e.getMessage());
        future.fail(failedMessage + e.getMessage());
      } finally {
        httpClient.closeClient();
      }
    };
  }

  public static Map<String, String> createHeaders(Map<String, String> okapiHeaders, String accept, String contentType) {
    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    String moduleId = okapiHeaders.get(OKAPI_MODULE_ID_HEADER);
    if (moduleId != null) {
      headers.put(OKAPI_MODULE_ID_HEADER, moduleId);
    }
    headers.put(HTTP_HEADER_ACCEPT, accept);
    if (!Strings.isNullOrEmpty(contentType)) {
      headers.put(HTTP_HEADER_CONTENT_TYPE, contentType);
    }
    return headers;
  }

  public static String getOkapiUrl(Map<String, String> okapiHeaders) {
    return okapiHeaders.get(OKAPI_URL_HEADER);
  }
}
