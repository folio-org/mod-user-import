package org.folio.rest.util;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class CustomFieldsUtil {

  public static final String CF_ARRAY_KEY = "customFields";
  public static final String CF_REF_ID_KEY = "refId";
  public static final String CF_OPTIONS_KEY = "options";
  public static final String CF_SELECT_FIELD_KEY = "selectField";
  public static final String CF_TOTAL_RECORDS_KEY = "totalRecords";
  public static final String CF_OPTIONS_VALUES_KEY = "values";

  private CustomFieldsUtil() {
    throw new UnsupportedOperationException("Util class");
  }

  public static boolean updateCfOptions(JsonObject cfCollection, Map<String, Set<String>> customFieldsOptions) {
    cfCollection.remove(CF_TOTAL_RECORDS_KEY);
    JsonArray cfArray = getCfArray(cfCollection);

    AtomicBoolean isUpdated = new AtomicBoolean(false);
    jsonObjectsStream(cfArray, cfArray::getJsonObject)
      .forEach(cfObject -> isUpdated.compareAndSet(false, updateCfObjectOptions(cfObject, customFieldsOptions)));
    return isUpdated.get();
  }

  private static boolean updateCfObjectOptions(JsonObject cfObject, Map<String, Set<String>> customFieldsOptions) {
    boolean isUpdated = false;
    Set<String> expectedOptions = getExpectedOptions(cfObject, customFieldsOptions);
    if (isNotEmpty(expectedOptions) && isNotEmptyJsonObject(cfObject.getJsonObject(CF_SELECT_FIELD_KEY))) {
      JsonArray cfOptionsArray = getCfOptionsArray(cfObject);
      jsonObjectsStream(cfOptionsArray, isNotNull(cfOptionsArray) ? cfOptionsArray::getString : null)
        .forEach(v -> expectedOptions.removeIf(s -> s.equalsIgnoreCase(v)));
      if (isNotNull(cfOptionsArray) && isNotEmpty(expectedOptions)) {
        isUpdated = true;
        expectedOptions.forEach(cfOptionsArray::add);
      }
    }
    return isUpdated;
  }

  private static JsonArray getCfOptionsArray(JsonObject cfObject) {
    JsonObject cfOptionsObject = cfObject.getJsonObject(CF_SELECT_FIELD_KEY).getJsonObject(CF_OPTIONS_KEY);
    if (isNotEmptyJsonObject(cfOptionsObject)) {
      JsonArray cfOptionsArray = cfOptionsObject.getJsonArray(CF_OPTIONS_VALUES_KEY);
      if (cfOptionsArray == null) {
        cfOptionsArray = new JsonArray();
        cfOptionsObject.put(CF_OPTIONS_VALUES_KEY, cfOptionsArray);
      }
      return cfOptionsArray;
    }
    return null;
  }

  private static Set<String> getExpectedOptions(JsonObject cfObject, Map<String, Set<String>> customFieldsOptions) {
    String refId = cfObject.getString(CF_REF_ID_KEY);
    return customFieldsOptions.get(refId);
  }

  public static JsonArray getCfArray(JsonObject cfCollection) {
    return cfCollection.getJsonArray(CF_ARRAY_KEY);
  }

  private static boolean isNotEmptyJsonObject(JsonObject jsonObject) {
    return jsonObject != null && !jsonObject.isEmpty();
  }

  private static boolean isNotEmptyJsonArray(JsonArray jsonArray) {
    return isNotNull(jsonArray) && !jsonArray.isEmpty();
  }

  private static boolean isNotNull(JsonArray jsonArray) {
    return jsonArray != null;
  }

  public static <T> Stream<T> jsonObjectsStream(JsonArray jsonArray, IntFunction<T> mappingFunction) {
    return isNotEmptyJsonArray(jsonArray)
      ? IntStream.range(0, jsonArray.size()).mapToObj(mappingFunction)
      : Stream.empty();
  }
}
