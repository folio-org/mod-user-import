package org.folio.rest.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;

@Getter
public class UserImportData {

  private final List<User> users;
  private final Set<Department> departments;
  private final Map<String, RequestPreference> requestPreferences;
  private final boolean deactivateMissingUsers;
  private final boolean updateOnlyPresentFields;
  private final String sourceType;

  private final UserSystemData systemData;

  private UserImportData(UserdataimportCollection userdataCollection, UserSystemData systemData) {
    this.users = userdataCollection.getUsers();
    this.deactivateMissingUsers = Boolean.TRUE.equals(userdataCollection.getDeactivateMissingUsers());
    this.updateOnlyPresentFields = Boolean.TRUE.equals(userdataCollection.getUpdateOnlyPresentFields());
    this.sourceType = userdataCollection.getSourceType();
    this.departments = fetchDepartments(userdataCollection);
    this.requestPreferences = fetchRequestPreferences(userdataCollection);
    this.systemData = systemData;
  }

  public static UserImportData from(UserdataimportCollection userdataCollection, UserSystemData systemData) {
    return new UserImportData(userdataCollection, systemData);
  }

  private Set<Department> fetchDepartments(UserdataimportCollection userdataCollection) {
    return userdataCollection.getIncluded() == null
      ? Collections.emptySet()
      : userdataCollection.getIncluded().getDepartments();
  }

  private Map<String, RequestPreference> fetchRequestPreferences(UserdataimportCollection userdataCollection) {
    final Map<String, RequestPreference> requestPreferenceMap = new HashMap<>();
    for (User user : userdataCollection.getUsers()) {
      requestPreferenceMap.put(user.getUsername(), user.getRequestPreference());
      user.setRequestPreference(null);
    }
    return requestPreferenceMap;
  }
}
