package org.folio.rest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Strings;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.model.UserImportData;
import org.folio.rest.model.UserMappingFailedException;

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

  public static Map<String, User> extractExistingUsers(List<Map> existingUserList) throws UserMappingFailedException {
    Map<String, User> existingUsers = new HashMap<>();
    for (Map existingUser : existingUserList) {
      JsonObject user = JsonObject.mapFrom(existingUser);
      try {
        User mappedUser = user.mapTo(User.class);
        LOGGER.trace("The external system id of the user is: " + mappedUser.getExternalSystemId());
        existingUsers.put(mappedUser.getExternalSystemId(), mappedUser);
      } catch (Exception ex) {
        LOGGER.error("Failed to map user ", user);
        throw new UserMappingFailedException("Failed to map user " + user.toString());
      }
    }

    return existingUsers;
  }

  public static void updateUserData(User user, UserImportData userImportData) {
    if (StringUtils.isNotEmpty(userImportData.getSourceType())) {
      user.setExternalSystemId(userImportData.getSourceType() + "_" + user.getExternalSystemId());
    }
    setPatronGroup(user, userImportData);
    setPersonalData(user, userImportData);
    setDepartments(user, userImportData);
  }

  public static void setPersonalData(User user, UserImportData userImportData) {
    if (user.getPersonal() != null) {
      setAddressTypes(user, userImportData);
      setPreferredContactType(user);
    }
  }

  private static void setPreferredContactType(User user) {
    if (user.getPersonal().getPreferredContactTypeId() != null
      && preferredContactTypeIds.containsKey(user.getPersonal().getPreferredContactTypeId().toLowerCase())) {
      user.getPersonal()
        .setPreferredContactTypeId(
          preferredContactTypeIds.get(user.getPersonal().getPreferredContactTypeId().toLowerCase()));
    } else {
      user.getPersonal().setPreferredContactTypeId(null);
    }
  }

  private static void setAddressTypes(User user, UserImportData userImportData) {
    Map<String, String> addressTypes = userImportData.getAddressTypes();
    List<Address> addressList = user.getPersonal().getAddresses();
    if (CollectionUtils.isNotEmpty(addressList)) {
      List<Address> updatedAddresses = new ArrayList<>();
      addressList.stream()
        .filter(address -> address.getAddressTypeId() != null && addressTypes.containsKey(address.getAddressTypeId()))
        .forEach(address -> {
          address.setAddressTypeId(addressTypes.get(address.getAddressTypeId()));
          updatedAddresses.add(address);
        });
      user.getPersonal().setAddresses(updatedAddresses);
    }
  }

  private static void setPatronGroup(User user, UserImportData userImportData) {
    if (user.getPatronGroup() != null && userImportData.getPatronGroups().containsKey(user.getPatronGroup())) {
      user.setPatronGroup(userImportData.getPatronGroups().get(user.getPatronGroup()));
    } else {
      user.setPatronGroup(null);
    }
  }

  private static void setDepartments(User user, UserImportData userImportData) {
    Set<String> departments = user.getDepartments();
    Map<String, String> existingDepartments = userImportData.getDepartments();
    if (CollectionUtils.isNotEmpty(departments)){
      Set<String> updatedDepartments = new HashSet<>();
      departments.stream()
        .filter(existingDepartments::containsKey)
        .forEach(department -> updatedDepartments.add(existingDepartments.get(department)));
      user.setDepartments(updatedDepartments);
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

        addresses = new ArrayList<>(addressMap.values());
      }
    }

    if (StringUtils.isBlank(user.getPersonal().getPreferredFirstName())) {
      response.getPersonal().setPreferredFirstName(existingUser.getPersonal().getPreferredFirstName());
    }

    if (addresses != null) {
      response.getPersonal().setAddresses(addresses);
    }

    return response;
  }

}
