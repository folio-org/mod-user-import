package org.folio.rest.util;

import java.util.Collection;
import java.util.Objects;

import javax.validation.ValidationException;

public class ValidationUtil {

  private ValidationUtil(){}

  private static final String MUST_NOT_BE_NULL_FORMAT = "%s must not be null";
  private static final String MUST_BE_NULL_FORMAT = "%s must be not specified";
  private static final String VALUE_DOES_NOT_EXIST_FORMAT = "Provided %s value does not exist in %s";

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

  public static void checkValueInStringCollection(String paramName, String collectionName, String value, Collection<String> collection) {
    if (!collection.contains(value)) {
      throw new ValidationException(
        String.format(VALUE_DOES_NOT_EXIST_FORMAT, paramName, collectionName));
    }
  }
}
