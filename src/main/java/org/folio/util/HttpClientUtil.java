package org.folio.util;

import java.util.HashMap;
import java.util.Map;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.XOkapiHeaders;

public class HttpClientUtil {

  private static final Logger LOGGER =  LogManager.getLogger(HttpClientUtil.class);

  private HttpClientUtil() {
  }

  static Map<Vertx, WebClient> clients = new HashMap<>();

  private static synchronized WebClient getWebClient() {
    Vertx vertx  = Vertx.currentContext().owner();
    if (clients.containsKey(vertx)) {
      return clients.get(vertx);
    }
    WebClient webClient = WebClient.create(vertx);
    clients.put(vertx, webClient);
    return webClient;
  }

  public static <T> Future<T> errorManagement(Throwable cause, String msg) {
    LOGGER.error("{} {}", msg, cause.getMessage(), cause);
    return Future.failedFuture(msg + ": " + cause.getMessage());
  }

  public static HttpRequest<Buffer> getRequestOkapi(HttpMethod method, Map<String,String> okapiHeaders, String path) {
    HttpRequest<Buffer> bufferHttpRequest = getWebClient().requestAbs(method, okapiHeaders.get(XOkapiHeaders.URL) + path);
    for (Map.Entry<String,String> entry : okapiHeaders.entrySet()) {
      bufferHttpRequest.putHeader(entry.getKey(), entry.getValue());
    }
    // Content-Type already set by sendJsonObject
    bufferHttpRequest.putHeader("Accept", "*/*"); // For pre RMB 32.0.0 RMB-519
    return bufferHttpRequest;
  }
}
