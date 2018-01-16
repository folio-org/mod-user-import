package org.folio.rest.util;

public class SingleUserImportResponse {

  private String externalSystemId;
  private String username;
  private UserRecordImportStatus status;
  private String errorMessage;
  private int errorCode;

  private SingleUserImportResponse() {
  }

  public static SingleUserImportResponse created(String externalSystemId) {
    SingleUserImportResponse response = new SingleUserImportResponse();
    response.externalSystemId = externalSystemId;
    response.status = UserRecordImportStatus.CREATED;
    return response;
  }

  public static SingleUserImportResponse updated(String externalSystemId) {
    SingleUserImportResponse response = new SingleUserImportResponse();
    response.externalSystemId = externalSystemId;
    response.status = UserRecordImportStatus.UPDATED;
    return response;
  }

  public static SingleUserImportResponse failed(String externalSystemId, String username, int errorCode, String errorMessage) {
    SingleUserImportResponse response = new SingleUserImportResponse();
    response.externalSystemId = externalSystemId;
    response.username = username;
    response.status = UserRecordImportStatus.FAILED;
    response.errorCode = errorCode;
    response.errorMessage = errorMessage;
    return response;
  }

  public String getExternalSystemId() {
    return externalSystemId;
  }

  public String getUsername() {
    return username;
  }

  public UserRecordImportStatus getStatus() {
    return status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public int getErrorCode() {
    return errorCode;
  }

}
