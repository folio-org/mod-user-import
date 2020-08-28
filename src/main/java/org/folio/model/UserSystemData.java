package org.folio.model;

import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.Department;

@Getter
@Builder
public class UserSystemData {

  private final Map<String, String> patronGroups;
  private final Map<String, String> addressTypes;
  private final Map<String, String> servicePoints;
  private final Set<Department> departments;
  private final Set<CustomField> customFields;
}
