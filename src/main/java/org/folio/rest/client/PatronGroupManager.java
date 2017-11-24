package org.folio.rest.client;

import static org.folio.rest.client.HttpClientUtil.*;
import static org.folio.rest.client.UserImportAPIConstants.*;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PatronGroupManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PatronGroupManager.class);

  private PatronGroupManager() {

  }

  public static Future<Map<String, String>> getPatronGroups(Map<String, String> okapiHeaders) {
    Future<Map<String, String>> future = Future.future();

    HttpClientInterface patronGroupClient = createClientWithHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, null);
    final String patronGroupQuery = UriBuilder.fromPath("/groups").build().toString();

    try {
      patronGroupClient.request(patronGroupQuery)
        .whenComplete((patronGroupResponse, ex) -> {
          if (ex != null) {
            LOGGER.error("Failed to list patron groups");
            LOGGER.debug(ex.getMessage());
            future.fail(ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(patronGroupResponse.getCode())) {
            LOGGER.warn("Failed to list patron groups");
            future.fail(patronGroupResponse.getError().toString());
          } else {
            JsonObject resultObject = patronGroupResponse.getBody();
            JsonArray patronGroupArray = resultObject.getJsonArray("usergroups");
            Map<String, String> patronGroups = extractPatronGroups(patronGroupArray);
            future.complete(patronGroups);
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to list patron groups", exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private static Map<String, String> extractPatronGroups(JsonArray patronGroups) {
    Map<String, String> patronGroupMap = new HashMap<>();
    for (int i = 0; i < patronGroups.size(); i++) {
      JsonObject patronGroup = patronGroups.getJsonObject(i);
      patronGroupMap.put(patronGroup.getString("group"), patronGroup.getString("id"));
    }
    return patronGroupMap;
  }
}
