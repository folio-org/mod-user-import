package org.folio.rest.util;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class CustomFieldsUtil {

  private static final String CF_ARRAY_KEY = "customFields";
  private static final String CF_REF_ID_KEY = "refId";
  private static final String CF_OPTIONS_KEY = "options";
  private static final String CF_SELECT_FIELD_KEY = "selectField";
  private static final String CF_TOTAL_RECORDS_KEY = "totalRecords";
  private static final String CF_OPTIONS_VALUES_KEY = "values";
  private static final String CF_OPTION_VALUE_KEY = "value";

  private CustomFieldsUtil() {
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
      List<JsonObject> cfOptions =
        jsonObjectsStream(cfOptionsArray, cfOptionsArray != null ? cfOptionsArray::getJsonObject : null)
          .collect(Collectors.toList());
      cfOptions
        .forEach(entries -> expectedOptions.removeIf(s -> s.equalsIgnoreCase(entries.getString(CF_OPTION_VALUE_KEY))));
      if (cfOptionsArray != null && isNotEmpty(expectedOptions)) {
        isUpdated = true;
        for (String newOptionValue : expectedOptions) {
          JsonObject newOption = new JsonObject().put(CF_OPTION_VALUE_KEY, newOptionValue);
          cfOptionsArray.add(newOption);
        }
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
    return jsonArray != null && !jsonArray.isEmpty();
  }

  public static <T> Stream<T> jsonObjectsStream(JsonArray jsonArray, IntFunction<T> mappingFunction) {
    return isNotEmptyJsonArray(jsonArray)
      ? IntStream.range(0, jsonArray.size()).mapToObj(mappingFunction)
      : Stream.empty();
  }
}
