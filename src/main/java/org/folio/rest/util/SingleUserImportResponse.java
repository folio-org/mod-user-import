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
    return new SingleUserImportResponse().withExternalSystemId(externalSystemId).withStatus(UserRecordImportStatus.CREATED);
  }

  public static SingleUserImportResponse updated(String externalSystemId) {
    return new SingleUserImportResponse().withExternalSystemId(externalSystemId).withStatus(UserRecordImportStatus.UPDATED);
  }

  public static SingleUserImportResponse failed(String externalSystemId, String username, int errorCode, String errorMessage) {
    return new SingleUserImportResponse()
      .withExternalSystemId(externalSystemId)
      .withUsername(username)
      .withStatus(UserRecordImportStatus.FAILED)
      .swithErrorCode(errorCode)
      .withErrorMessage(errorMessage);
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

  public SingleUserImportResponse withExternalSystemId(String externalSystemId) {
    this.externalSystemId = externalSystemId;
    return this;
  }

  public SingleUserImportResponse withUsername(String username) {
    this.username = username;
    return this;
  }

  public SingleUserImportResponse withStatus(UserRecordImportStatus status) {
    this.status = status;
    return this;
  }

  public SingleUserImportResponse withErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  public SingleUserImportResponse swithErrorCode(int errorCode) {
    this.errorCode = errorCode;
    return this;
  }

}
