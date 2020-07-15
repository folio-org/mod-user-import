package org.folio.rest.validator;

import java.util.Map;
import java.util.Objects;

import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.util.ValidationUtil;

public class UserRequestManagerValidator {

  private static final String DELIVERY_PARAMETER = "delivery";
  private static final String FULFILLMENT_PARAMETER = "fulfillment";
  private static final String ADDRESS_TYPE_ID = "defaultDeliveryAddressTypeId";
  public static final String DEFAULT_SERVICE_POINT_PARAMETER = "defaultServicePointId";

  private UserRequestManagerValidator() { }

  public static void validate(RequestPreference requestPreference, Map<String, String> addressTypes, Map<String, String> servicePoints){

    validateDefaultServicePoint(requestPreference, servicePoints);
    validateDelivery(requestPreference, addressTypes);
  }

  private static void validateDefaultServicePoint(RequestPreference requestPreference, Map<String, String> servicePoints) {
    String defaultServicePointId = requestPreference.getDefaultServicePointId();

    if (Objects.nonNull(defaultServicePointId)) {
      ValidationUtil.checkValueInStringCollection(DEFAULT_SERVICE_POINT_PARAMETER, defaultServicePointId, servicePoints.values());
    }
  }

  private static void validateDelivery(RequestPreference requestPreference, Map<String, String> addressTypes) {
    Boolean hasDelivery = requestPreference.getDelivery();
    ValidationUtil.checkIsNotNull(DELIVERY_PARAMETER, hasDelivery);

    if (Boolean.TRUE.equals(hasDelivery)){
      ValidationUtil.checkIsNotNull(FULFILLMENT_PARAMETER, requestPreference.getFulfillment());
      validateAddressType(requestPreference, addressTypes);
    } else {
      ValidationUtil.checkIsNull(FULFILLMENT_PARAMETER, requestPreference.getFulfillment());
      ValidationUtil.checkIsNull(ADDRESS_TYPE_ID, requestPreference.getDefaultDeliveryAddressTypeId());
    }
  }

  private static void validateAddressType(RequestPreference requestPreference, Map<String, String> addressTypes) {
    String addressTypeId = requestPreference.getDefaultDeliveryAddressTypeId();
    ValidationUtil.checkIsNotNull(ADDRESS_TYPE_ID, addressTypeId);
    ValidationUtil.checkValueInStringCollection(ADDRESS_TYPE_ID, addressTypeId, addressTypes.values());
  }
}
