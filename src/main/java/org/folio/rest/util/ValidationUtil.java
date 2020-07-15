package org.folio.rest.util;

import java.util.Collection;
import java.util.Objects;

import javax.validation.ValidationException;

public class ValidationUtil {

  private ValidationUtil(){}

  private static final String MUST_NOT_BE_NULL_FORMAT = "%s must not be null";
  private static final String MUST_BE_NULL_FORMAT = "%s must be not specified";
  private static final String MUST_CONTAINS_IN_LIST_FORMAT = "Provided %s value not in collection";

  public static void checkIsNotNull(String paramName, Object value) {
    if (Objects.isNull(value)) {
      throw new ValidationException(
        String.format(MUST_NOT_BE_NULL_FORMAT, paramName));
    }
  }

  public static void checkIsNull(String paramName, Object value) {
    if (Objects.nonNull(value)) {
      throw new ValidationException(
        String.format(MUST_BE_NULL_FORMAT, paramName));
    }
  }

  public static void checkValueInStringCollection(String paramName, String value, Collection<String> collection) {
    if (!collection.contains(value)) {
      throw new ValidationException(
        String.format(MUST_CONTAINS_IN_LIST_FORMAT, paramName));
    }
  }
}
