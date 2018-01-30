package org.folio.rest.model;

public class UserMappingFailedException extends Exception {
  private static final long serialVersionUID = 1L;

  public UserMappingFailedException() {
    super();
  }

  public UserMappingFailedException(String message) {
    super(message);
  }

}
