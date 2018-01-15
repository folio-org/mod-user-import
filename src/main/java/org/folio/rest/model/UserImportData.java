package org.folio.rest.model;

import java.util.Map;

import org.folio.rest.jaxrs.model.UserdataCollection;

public class UserImportData {

  private Boolean deactivateMissingUsers;

  private Boolean updateOnlyPresentFields;

  private String sourceType;

  private Map<String, String> patronGroups;

  private Map<String, String> addressTypes;

  public UserImportData(UserdataCollection userdataCollection) {
    this.deactivateMissingUsers = userdataCollection.getDeactivateMissingUsers();
    if (this.deactivateMissingUsers == null) {
      this.deactivateMissingUsers = Boolean.FALSE;
    }
    this.updateOnlyPresentFields = userdataCollection.getUpdateOnlyPresentFields();
    if (this.updateOnlyPresentFields == null) {
      this.updateOnlyPresentFields = Boolean.FALSE;
    }
    this.sourceType = userdataCollection.getSourceType();
  }

  public void setPatronGroups(Map<String, String> patronGroups) {
    this.patronGroups = patronGroups;
  }

  public void setAddressTypes(Map<String, String> addressTypes) {
    this.addressTypes = addressTypes;
  }

  public Boolean getDeactivateMissingUsers() {
    return deactivateMissingUsers;
  }

  public Boolean getUpdateOnlyPresentFields() {
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

}
