package org.folio.rest.validator;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.model.UserImportData;
import org.folio.rest.util.ValidationUtil;

public class UserRequestManagerValidator {

  private static final String DELIVERY_PARAMETER = "delivery";
  private static final String FULFILLMENT_PARAMETER = "fulfillment";
  private static final String ADDRESS_TYPE_ID = "defaultDeliveryAddressTypeId";
  private static final String DEFAULT_SERVICE_POINT_PARAMETER = "defaultServicePointId";
  private static final String SERVICE_POINT_COLLECTION_NAME = "service point collection";
  private static final String ADDRESSES_COLLECTION_NAME = "addresses collection";
  private static final String USER_ADDRESSES_COLLECTION_NAME = "user addresses collection";

  private UserRequestManagerValidator() { }

  public static void validate(RequestPreference requestPreference, UserImportData importData, User user){

    validateDefaultServicePoint(requestPreference, importData.getSystemData().getServicePoints());
    validateDelivery(requestPreference, importData.getSystemData().getAddressTypes(), user);
  }

  private static void validateDefaultServicePoint(RequestPreference requestPreference, Map<String, String> servicePoints) {
    String defaultServicePointId = requestPreference.getDefaultServicePointId();

    if (Objects.nonNull(defaultServicePointId)) {
      ValidationUtil.checkValueInStringCollection(DEFAULT_SERVICE_POINT_PARAMETER, SERVICE_POINT_COLLECTION_NAME,
        defaultServicePointId, servicePoints.values());
    }
  }

  private static void validateDelivery(RequestPreference requestPreference, Map<String, String> addressTypes, User user) {
    Boolean hasDelivery = requestPreference.getDelivery();
    ValidationUtil.checkIsNotNull(DELIVERY_PARAMETER, hasDelivery);

    if (Boolean.TRUE.equals(hasDelivery)){
      ValidationUtil.checkIsNotNull(FULFILLMENT_PARAMETER, requestPreference.getFulfillment());
      validateAddressType(requestPreference, addressTypes, user);
    } else {
      ValidationUtil.checkIsNull(FULFILLMENT_PARAMETER, requestPreference.getFulfillment());
      ValidationUtil.checkIsNull(ADDRESS_TYPE_ID, requestPreference.getDefaultDeliveryAddressTypeId());
    }
  }

  private static void validateAddressType(RequestPreference requestPreference, Map<String, String> addressTypes, User user) {
    final String addressTypeName = requestPreference.getDefaultDeliveryAddressTypeId();
    ValidationUtil.checkIsNotNull(ADDRESS_TYPE_ID, addressTypeName);
    ValidationUtil.checkValueInStringCollection(ADDRESS_TYPE_ID, ADDRESSES_COLLECTION_NAME, addressTypeName, addressTypes.keySet());
    final String addressTypeId = addressTypes.get(addressTypeName);
    ValidationUtil.checkValueInStringCollection(ADDRESS_TYPE_ID, USER_ADDRESSES_COLLECTION_NAME, addressTypeId,
      user.getPersonal().getAddresses()
        .stream()
        .map(Address::getAddressTypeId)
        .collect(Collectors.toList()));
  }
}
