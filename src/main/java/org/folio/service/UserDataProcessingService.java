package org.folio.service;

import static org.folio.rest.jaxrs.model.CustomField.Type.MULTI_SELECT_DROPDOWN;
import static org.folio.rest.jaxrs.model.CustomField.Type.RADIO_BUTTON;
import static org.folio.rest.jaxrs.model.CustomField.Type.SINGLE_SELECT_DROPDOWN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import org.folio.model.UserImportData;
import org.folio.model.exception.CustomFieldMappingFailedException;
import org.folio.model.exception.DepartmentMappingFailedException;
import org.folio.model.exception.PatronGroupMappingFailedException;
import org.folio.model.exception.UserMappingFailedException;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFields;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.SelectFieldOption;
import org.folio.rest.jaxrs.model.User;

public class UserDataProcessingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserDataProcessingService.class);

  private static final Map<String, String> preferredContactTypeIds = new CaseInsensitiveMap<>();

  static {
    preferredContactTypeIds.put("mail", "001");
    preferredContactTypeIds.put("email", "002");
    preferredContactTypeIds.put("text", "003");
    preferredContactTypeIds.put("phone", "004");
    preferredContactTypeIds.put("mobile", "005");
  }

  private final CustomFieldsService customFieldsService;
  private final DepartmentsService departmentService;


  public UserDataProcessingService(DepartmentsService departmentService,
      CustomFieldsService customFieldsService) {
    this.departmentService = departmentService;
    this.customFieldsService = customFieldsService;
  }

  public Map<String, User> extractExistingUsers(List<Map> existingUserList) throws UserMappingFailedException {
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

  public void updateUserData(User user, UserImportData userImportData) {
    if (StringUtils.isNotEmpty(userImportData.getSourceType())) {
      user.setExternalSystemId(userImportData.getSourceType() + "_" + user.getExternalSystemId());
    }
    setPatronGroup(user, userImportData);
    setPersonalData(user, userImportData);
    setDepartments(user, userImportData);
    setCustomFields(user, userImportData);
  }

  public void updateUserPreference(RequestPreference preference, UserImportData userImportData) {
    setPreferenceAddressType(preference, userImportData);
  }

  /*
   * Currently this deep copy only works for addresses.
   * If more embedded fields will raise a need for this feature this function needs to be updated.
   */
  public User updateExistingUserWithIncomingFields(User user, User existingUser) {
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

    if (user.getPersonal() != null && StringUtils.isBlank(user.getPersonal().getPreferredFirstName())) {
      response.getPersonal().setPreferredFirstName(existingUser.getPersonal().getPreferredFirstName());
    }

    if (addresses != null) {
      response.getPersonal().setAddresses(addresses);
    }

    return response;
  }

  private void setPersonalData(User user, UserImportData userImportData) {
    if (user.getPersonal() != null) {
      setAddressTypes(user, userImportData);
      setPreferredContactType(user);
    }
  }

  private void setPreferredContactType(User user) {
    String preferredContactTypeName = user.getPersonal().getPreferredContactTypeId();
    user.getPersonal()
      .setPreferredContactTypeId(preferredContactTypeIds.getOrDefault(preferredContactTypeName, null));
  }

  private void setAddressTypes(User user, UserImportData userImportData) {
    Map<String, String> addressTypes = userImportData.getSystemData().getAddressTypes();
    List<Address> addressList = user.getPersonal().getAddresses();
    if (CollectionUtils.isNotEmpty(addressList)) {
      List<Address> updatedAddresses = getExistingAddresses(addressTypes, addressList);
      user.getPersonal().setAddresses(updatedAddresses);
    }
  }

  private void setPreferenceAddressType(RequestPreference preference, UserImportData userImportData) {
    Map<String, String> addressTypes = userImportData.getSystemData().getAddressTypes();
    String addressTypeName = preference.getDefaultDeliveryAddressTypeId();
    String addressTypeId = addressTypes.getOrDefault(addressTypeName, null);
    preference.setDefaultDeliveryAddressTypeId(addressTypeId);
  }

  @NotNull
  private List<Address> getExistingAddresses(Map<String, String> addressTypes, List<Address> addressList) {
    List<Address> updatedAddresses = new ArrayList<>();
    addressList.stream()
      .filter(address -> address.getAddressTypeId() != null && addressTypes.containsKey(address.getAddressTypeId()))
      .forEach(address -> {
        address.setAddressTypeId(addressTypes.get(address.getAddressTypeId()));
        updatedAddresses.add(address);
      });
    return updatedAddresses;
  }

  private void setPatronGroup(User user, UserImportData userImportData) {
    Map<String, String> patronGroups = userImportData.getSystemData().getPatronGroups();
    String patronGroupName = user.getPatronGroup();
    String patronGroupId = patronGroups.get(patronGroupName);
    if (patronGroupId == null) {
      throw new PatronGroupMappingFailedException(patronGroupName);
    }
    user.setPatronGroup(patronGroupId);
  }

  private void setDepartments(User user, UserImportData userImportData) {
    var departments = user.getDepartments();
    if (CollectionUtils.isNotEmpty(departments)) {
      var systemDepartments = userImportData.getSystemData().getDepartments();
      Set<String> departmentIds = new HashSet<>();
      Set<String> missedDepartmentNames = new TreeSet<>();

      for (String departmentName : departments) {
        departmentService.findDepartmentByName(systemDepartments, departmentName)
          .ifPresentOrElse(department -> departmentIds.add(department.getId()),
            () -> missedDepartmentNames.add(departmentName)
          );
      }

      if (missedDepartmentNames.isEmpty()) {
        user.setDepartments(departmentIds);
      } else {
        throw new DepartmentMappingFailedException(missedDepartmentNames);
      }
    }
  }

  private void setCustomFields(User user, UserImportData userImportData) {
    CustomFields customFields = user.getCustomFields();
    if (customFields == null)
      return;
    var systemCustomFields = userImportData.getSystemData().getCustomFields();
    var userCustomFields = customFields.getAdditionalProperties();

    Set<String> missingCustomFieldsRefIds = new TreeSet<>();
    Map<String, Set<String>> missingOptions = new TreeMap<>();
    userCustomFields.entrySet().forEach(customFieldEntry -> {
        String refId = customFieldEntry.getKey();
        customFieldsService.findCustomFieldByRefId(systemCustomFields, refId)
          .ifPresentOrElse(customFieldDefinition ->
              setOptionIds(userCustomFields, customFieldDefinition, customFieldEntry, missingOptions),
            () -> missingCustomFieldsRefIds.add(refId)
          );
      }
    );
    if (!missingCustomFieldsRefIds.isEmpty()) {
      throw new CustomFieldMappingFailedException(missingCustomFieldsRefIds, missingOptions);
    } else if (!missingOptions.isEmpty()) {
      throw new CustomFieldMappingFailedException(Collections.emptySet(), missingOptions);
    }
  }

  private void setOptionIds(Map<String, Object> userCustomFields, CustomField definition,
                                   Map.Entry<String, Object> customFieldEntry,
                                   Map<String, Set<String>> missingOptions) {
    if (isSelectableField(definition)) {
      String refId = customFieldEntry.getKey();
      Object value = customFieldEntry.getValue();
      if (value instanceof String) {
        setOptionId(value, refId, definition, opt -> userCustomFields.put(refId, opt.getId()), missingOptions);
      } else if (value instanceof List) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) value;
        List<String> optIds = new ArrayList<>();
        for (String v : values) {
          setOptionId(v, refId, definition, opt -> optIds.add(opt.getId()), missingOptions);
        }
        userCustomFields.put(refId, optIds);
      }
    }
  }

  private void setOptionId(Object value, String refId, CustomField definition,
                                  Consumer<SelectFieldOption> optionConsumer,
                                  Map<String, Set<String>> missingOptions) {
    definition.getSelectField().getOptions().getValues().stream()
      .filter(opt -> opt.getValue().equals(value))
      .findFirst()
      .ifPresentOrElse(optionConsumer, () -> getEntrySet(missingOptions, refId).add((String) value)
      );
  }

  private Set<String> getEntrySet(Map<String, Set<String>> missingCustomFieldOptions, String refId) {
    return missingCustomFieldOptions.computeIfAbsent(refId, s -> new TreeSet<>());
  }

  private boolean isSelectableField(CustomField customField) {
    return customField.getType() == RADIO_BUTTON || customField.getType() == MULTI_SELECT_DROPDOWN || customField
      .getType() == SINGLE_SELECT_DROPDOWN;
  }

}
