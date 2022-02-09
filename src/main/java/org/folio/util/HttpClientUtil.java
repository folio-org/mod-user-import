package org.folio.util;

import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_ACCEPT;
import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_CONTENT_TYPE;
import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_VALUE_APPLICATION_JSON;
import static org.folio.rest.impl.UserImportAPIConstants.HTTP_HEADER_VALUE_TEXT_PLAIN;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.base.Strings;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;

public class HttpClientUtil {

  private static final Logger LOGGER =  LogManager.getLogger(HttpClientUtil.class);

  private HttpClientUtil() {
  }

  public static <T> Future<T> errorManagement(Throwable cause, String msg) {
    LOGGER.error("{} {}", msg, cause.getMessage(), cause);
    return Future.failedFuture(msg + ": " + cause.getMessage());
  }

  public static HttpRequest<Buffer> webClientOkapi(WebClient webClient, HttpMethod method, Map<String,String> okapiHeaders, String path) {
    HttpRequest<Buffer> bufferHttpRequest = webClient.requestAbs(method, okapiHeaders.get(XOkapiHeaders.URL) + path);
    for (Map.Entry<String,String> entry : okapiHeaders.entrySet()) {
      bufferHttpRequest.putHeader(entry.getKey(), entry.getValue());
    }
    return bufferHttpRequest;
  }

  public static Future<JsonObject> get(Map<String, String> okapiHeaders, String query,
                                       String failedMessage) {
    Promise<JsonObject> future = Promise.promise();
    try {
      Map<String, String> headers = HttpClientUtil.createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, null);
      LOGGER.info("Do GET request: {}", query);
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      httpClient.request(query, headers).whenComplete((response, ex) -> {
            if (ex != null) {
              LOGGER.error(failedMessage, ex.getMessage());
              future.fail(failedMessage + ex.getMessage());
            } else if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
              LOGGER.warn(failedMessage + response.getError());
              future.fail(failedMessage + response.getError());
            } else {
              JsonObject resultObject = response.getBody();
              future.complete(resultObject);
            }
          });
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      future.fail(e);
    }
    return future.future();
  }

  public static <T> Future<T> post(Map<String, String> okapiHeaders, String query, Class<T> clazz, Object entity,
                                   String failedMessage) {

    try {
      Map<String, String> headers =
          createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      Promise<T> promise = Promise.promise();
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
      return promise.future();
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return Future.failedFuture(e);
    }
  }

  public static Future<Void> put(Map<String, String> okapiHeaders, String query, Object entity, String failedMessage) {

    Promise<Void> future = Promise.promise();
    try {
      Map<String, String> headers =
          createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      LOGGER.info("Do PUT request: {}", query);
      httpClient.request(HttpMethod.PUT, JsonObject.mapFrom(entity), query, headers)
          .whenComplete(handleResponse(failedMessage, future, httpClient));
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      future.fail(e);
    }
    return future.future();
  }

  public static Future<Void> delete(Map<String, String> okapiHeaders, String query, String failedMessage) {
    Promise<Void> future = Promise.promise();
    try {
      Map<String, String> headers =
          createHeaders(okapiHeaders, HTTP_HEADER_VALUE_TEXT_PLAIN, HTTP_HEADER_VALUE_APPLICATION_JSON);
      final HttpClientInterface httpClient = getHttpClient(okapiHeaders);
      LOGGER.info("Do DELETE request: {}", query);
      httpClient.request(HttpMethod.DELETE, query, headers)
          .whenComplete(handleResponse(failedMessage, future, httpClient));
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      future.fail(e);
    }
    return future.future();
  }

  public static HttpClientInterface getHttpClient(Map<String, String> okapiHeaders) {
    final String okapiURL = getOkapiUrl(okapiHeaders);
    final String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(XOkapiHeaders.TENANT));

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
    Map<String, String> headers = new CaseInsensitiveMap<>();
    headers.put(XOkapiHeaders.URL, okapiHeaders.get(XOkapiHeaders.URL));
    headers.put(XOkapiHeaders.TOKEN, okapiHeaders.get(XOkapiHeaders.TOKEN));
    String moduleId = okapiHeaders.get(XOkapiHeaders.MODULE_ID);
    if (moduleId != null) {
      headers.put(XOkapiHeaders.MODULE_ID, moduleId);
    }
    headers.put(HTTP_HEADER_ACCEPT, accept);
    if (!Strings.isNullOrEmpty(contentType)) {
      headers.put(HTTP_HEADER_CONTENT_TYPE, contentType);
    }
    return headers;
  }

  public static String getOkapiUrl(Map<String, String> okapiHeaders) {
    return okapiHeaders.get(XOkapiHeaders.URL);
  }
}
