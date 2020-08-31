package org.folio.model.exception;

import static java.util.Collections.emptyMap;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomFieldMappingFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String CF_MISSING_MESSAGE = "Custom fields do not exist in the system: [%s].";
  private static final String CF_OPTIONS_MISSING_MESSAGE = "Custom field's options do not exist in the system: [%s].";
  private static final String MISSING_OPTIONS_SUB_MESSAGE = "refId = %s, options: [%s]";

  public CustomFieldMappingFailedException(Set<String> missingRefIds) {
    this(missingRefIds, emptyMap());
  }

  public CustomFieldMappingFailedException(Set<String> missingRefIds, Map<String, Set<String>> missingOptions) {
    super(createMessage(missingRefIds, missingOptions));
  }

  private static String createMessage(Set<String> customFieldRefIds, Map<String, Set<String>> customFieldOptions) {
    StringBuilder sb = new StringBuilder();
    if (!customFieldRefIds.isEmpty()) {
      String missingFieldsMessage = String.format(CF_MISSING_MESSAGE, String.join(", ", customFieldRefIds));
      sb.append(missingFieldsMessage);
    }
    if (!customFieldOptions.isEmpty()) {
      String missingOptions = customFieldOptions.entrySet().stream()
        .map(CustomFieldMappingFailedException::createMissingOptionMessage)
        .collect(Collectors.joining("; "));
      String missingOptionsMessage = String.format(CF_OPTIONS_MISSING_MESSAGE, missingOptions);
      sb.append(" ").append(missingOptionsMessage);
    }
    return sb.toString();
  }

  private static String createMissingOptionMessage(Map.Entry<String, Set<String>> entry) {
    return String.format(MISSING_OPTIONS_SUB_MESSAGE, entry.getKey(), String.join(", ", entry.getValue())).trim();
  }
}
