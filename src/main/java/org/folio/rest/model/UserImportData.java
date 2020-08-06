package org.folio.rest.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;

public class UserImportData {

  private boolean deactivateMissingUsers;
  private boolean updateOnlyPresentFields;
  private String sourceType;
  private Map<String, String> patronGroups;
  private Map<String, String> addressTypes;
  private Map<String, String> servicePoints;
  private Map<String, String> departments;
  private List<User> users;
  private Map<String, RequestPreference> requestPreferences;

  public UserImportData(UserdataimportCollection userdataCollection) {
    this.deactivateMissingUsers = Boolean.TRUE.equals(userdataCollection.getDeactivateMissingUsers());
    this.updateOnlyPresentFields = Boolean.TRUE.equals(userdataCollection.getUpdateOnlyPresentFields());
    this.sourceType = userdataCollection.getSourceType();
    parseUsersAndRequestPreferences(userdataCollection.getUsers());
  }

  private void parseUsersAndRequestPreferences(List<User> users) {
    this.requestPreferences = new HashMap<>();
    for (User user : users) {
      this.requestPreferences.put(user.getUsername(), user.getRequestPreference());
      user.setRequestPreference(null);
    }
    this.users = users;
  }

  public void setPatronGroups(Map<String, String> patronGroups) {
    this.patronGroups = new CaseInsensitiveMap<>(patronGroups);
  }

  public void setAddressTypes(Map<String, String> addressTypes) {
    this.addressTypes = new CaseInsensitiveMap<>(addressTypes);
  }

  public boolean getDeactivateMissingUsers() {
    return deactivateMissingUsers;
  }

  public boolean getUpdateOnlyPresentFields() {
    return updateOnlyPresentFields;
  }

  public String getSourceType() {
    return sourceType;
  }

  public Map<String, String> getPatronGroups() {
    return patronGroups;
  }

  public Map<String, String> getAddressTypes() {
    return addressTypes;
  }

  public Map<String, String> getServicePoints() {
    return servicePoints;
  }

  public void setServicePoints(Map<String, String> servicePoints) {
    this.servicePoints = new CaseInsensitiveMap<>(servicePoints);
  }

  public List<User> getUsers() {
    return users;
  }

  public Map<String, RequestPreference> getRequestPreference() {
    return requestPreferences;
  }

  public Map<String, String> getDepartments() {
    return departments;
  }

  public void setDepartments(Map<String, String> departments) {
    this.departments = new CaseInsensitiveMap<>(departments);
  }
}
