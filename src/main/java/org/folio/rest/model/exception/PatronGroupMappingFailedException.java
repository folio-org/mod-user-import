package org.folio.rest.model.exception;

public class PatronGroupMappingFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String MESSAGE_PATTERN = "Patron group does not exist in the system: [%s]";
  private final String patronGroupName;

  public PatronGroupMappingFailedException(String patronGroupName) {
    super(String.format(MESSAGE_PATTERN, patronGroupName));
    this.patronGroupName = patronGroupName;
  }

  public String getPatronGroupName() {
    return patronGroupName;
  }
}
