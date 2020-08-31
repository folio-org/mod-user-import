package org.folio.model.exception;

import java.util.Set;

public class DepartmentMappingFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String MESSAGE_PATTERN = "Departments do not exist in the system: [%s]";

  public DepartmentMappingFailedException(Set<String> departmentNames) {
    super(String.format(MESSAGE_PATTERN, String.join(", ", departmentNames)));
  }

}
