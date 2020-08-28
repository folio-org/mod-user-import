package org.folio.util;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonObjectUtil {

  private JsonObjectUtil() {}

  public static Map<String, String> extractMap(JsonObject result, String arrayKey, String objectKey) {
    Map<String, String> resultMap = new HashMap<>();
    JsonArray jsonArray = result.getJsonArray(arrayKey);
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonObject type = jsonArray.getJsonObject(i);
      resultMap.put(type.getString(objectKey), type.getString("id"));
    }
    return resultMap;
  }
}
