package org.folio.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;

@Getter
public class UserImportData {

  private final List<User> users;
  private final Set<Department> departments;
  private final Set<CustomField> customFields;
  private final Map<String, RequestPreference> requestPreferences;
  private final boolean deactivateMissingUsers;
  private final boolean updateOnlyPresentFields;
  private final String sourceType;

  private UserSystemData systemData;

  public UserImportData(UserdataimportCollection userdataCollection) {
    this.users = userdataCollection.getUsers();
    this.deactivateMissingUsers = Boolean.TRUE.equals(userdataCollection.getDeactivateMissingUsers());
    this.updateOnlyPresentFields = Boolean.TRUE.equals(userdataCollection.getUpdateOnlyPresentFields());
    this.sourceType = userdataCollection.getSourceType();
    this.requestPreferences = fetchRequestPreferences(userdataCollection);

    if (userdataCollection.getIncluded() == null) {
      this.departments = Collections.emptySet();
      this.customFields = Collections.emptySet();
    } else {
      this.departments = userdataCollection.getIncluded().getDepartments();
      this.customFields = userdataCollection.getIncluded().getCustomFields();
    }
  }

  private Map<String, RequestPreference> fetchRequestPreferences(UserdataimportCollection userdataCollection) {
    final Map<String, RequestPreference> requestPreferenceMap = new HashMap<>();
    for (User user : userdataCollection.getUsers()) {
      requestPreferenceMap.put(user.getUsername(), user.getRequestPreference());
      user.setRequestPreference(null);
    }
    return requestPreferenceMap;
  }

  public UserImportData withSystemData(UserSystemData systemData) {
    this.systemData = systemData;
    return this;
  }
}
