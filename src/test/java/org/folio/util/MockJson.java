package org.folio.util;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class MockJson extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger(MockJson.class);

  JsonArray mocks;
  final JsonArray all;

  String resource;

  public MockJson(String resource) {
    all = setMockContent(resource);
  }

  public MockJson() {
    all = new JsonArray();
  }

  private static JsonArray setMockContent(String resource) {
    try {
      String file = IOUtils.toString(MockJson.class.getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8);
      JsonObject config = new JsonObject(file);
      return config.getJsonArray("mocks");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void setMockJsonContent(String resource) {
    this.resource = resource;
    mocks = setMockContent(resource);
  }

  private void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();
    String method = request.method().name();
    String uri = request.uri();
    for (int i = 0; i < all.size(); i++) {
      JsonObject entry = all.getJsonObject(i);
      if (extracted(response, method, uri, entry)) {
        return;
      }
    }
    for (int i = 0; i < mocks.size(); i++) {
      JsonObject entry = mocks.getJsonObject(i);
      if (extracted(response, method, uri, entry)) {
        return;
      }
    }
    log.info("Not found in mock={} method={} uri={}", resource, method, uri);
    response.setStatusCode(404);
    response.putHeader("Content-Type", "text/plain");
    response.end("Not found in mock");
  }

  private boolean extracted(HttpServerResponse response, String method, String uri, JsonObject entry) {
    if (!method.equalsIgnoreCase(entry.getString("method", "get"))
      || !uri.equals(entry.getString("url"))) {
      return false;
    }
    response.setStatusCode(entry.getInteger("status", 200));
    JsonArray headers = entry.getJsonArray("headers");
    if (headers != null) {
      for (int j = 0; j < headers.size(); j++) {
        JsonObject headObject = headers.getJsonObject(j);
        response.putHeader(headObject.getString("name"), headObject.getString("value"));
      }
    }
    Object responseData = entry.getValue("receivedData");
    if (responseData instanceof JsonObject) {
      response.putHeader("Content-Type", "application/json");
      response.end(((JsonObject) responseData).encodePrettily());
    } else if (responseData instanceof JsonArray) {
      response.putHeader("Content-Type", "application/json");
      response.end(((JsonArray) responseData).encodePrettily());
    } else if (responseData instanceof String) {
      response.putHeader("Content-Type", "text/plain");
      response.end((String) responseData);
    } else {
      response.end();
    }
    return true;
  }

  public void start(Promise<Void> promise) {
    final int port = context.config().getInteger("http.port");

    log.info("Running Mock JSON on port {}", port);

    Router router = Router.router(vertx);
    router.routeWithRegex("/.*").handler(this::handle);
    vertx.createHttpServer().requestHandler(router).listen(port).<Void>mapEmpty().onComplete(promise);
  }
}
