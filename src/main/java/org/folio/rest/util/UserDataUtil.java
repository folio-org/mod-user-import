package org.folio.rest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.User;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UserDataUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserDataUtil.class);

  private static final Map<String, String> preferredContactTypeIds = new HashMap<>();

  static {
    preferredContactTypeIds.put("mail", "001");
    preferredContactTypeIds.put("email", "002");
    preferredContactTypeIds.put("text", "003");
    preferredContactTypeIds.put("phone", "004");
    preferredContactTypeIds.put("mobile", "005");
  }

  private UserDataUtil() {
  }

  public static Map<String, User> extractExistingUsers(List<Map> existingUserList) {
    Map<String, User> existingUsers = new HashMap<>();
    for (Map existingUser : existingUserList) {
      JsonObject user = JsonObject.mapFrom(existingUser);
      User mappedUser = user.mapTo(User.class);
      LOGGER.info("The external system id of the user is: " + mappedUser.getExternalSystemId());
      existingUsers.put(mappedUser.getExternalSystemId(), mappedUser);
    }

    return existingUsers;
  }

  public static void updateUserData(User user, Map<String, String> patronGroups, Map<String, String> addressTypes,
    String sourceType) {
    if (!Strings.isNullOrEmpty(sourceType)) {
      user.setExternalSystemId(sourceType + "_" + user.getExternalSystemId());
    }
    if (user.getPatronGroup() != null && patronGroups.containsKey(user.getPatronGroup())) {
      user.setPatronGroup(patronGroups.get(user.getPatronGroup()));
    }
    if (user.getPersonal() == null) {
      return;
    }
    if (user.getPersonal().getAddresses() != null
      && !user.getPersonal().getAddresses().isEmpty()) {
      for (Address address : user.getPersonal().getAddresses()) {
        if (address.getAddressTypeId() != null && addressTypes.containsKey(address.getAddressTypeId())) {
          address.setAddressTypeId(addressTypes.get(address.getAddressTypeId()));
        }
      }
    }
    if (user.getPersonal().getPreferredContactTypeId() != null
      && preferredContactTypeIds.containsKey(user.getPersonal().getPreferredContactTypeId().toLowerCase())) {
      user.getPersonal()
        .setPreferredContactTypeId(
          preferredContactTypeIds.get(user.getPersonal().getPreferredContactTypeId().toLowerCase()));
    }
  }

  /*
   * Currently this deep copy only works for addresses. 
   * If more embedded fields will raise a need for this feature this function needs to be updated.
   */
  public static User updateExistingUserWithIncomingFields(User user, User existingUser) {
    JsonObject current = JsonObject.mapFrom(user);
    JsonObject existing = JsonObject.mapFrom(existingUser);

    List<Address> addresses = null;

    existing.mergeIn(current);

    User response = existing.mapTo(User.class);

    if (existingUser.getPersonal() != null) {
      List<Address> currentAddresses = null;
      List<Address> existingAddresses = existingUser.getPersonal().getAddresses();
      if (user.getPersonal() != null) {
        currentAddresses = user.getPersonal().getAddresses();
      }
      if (currentAddresses == null) {
        addresses = existingAddresses;
      } else {
        Map<String, Address> addressMap = new HashMap<>();
        existingAddresses.forEach(address -> addressMap.put(address.getAddressTypeId(), address));
        currentAddresses.forEach(address -> addressMap.put(address.getAddressTypeId(), address));

        addresses = new ArrayList<>();
        addresses.addAll(addressMap.values());
      }
    }

    if (addresses != null) {
      response.getPersonal().setAddresses(addresses);
    }

    return response;
  }

}
