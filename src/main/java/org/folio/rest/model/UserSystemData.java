package org.folio.rest.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

import org.folio.rest.jaxrs.model.Department;

@Getter
@Builder
public class UserSystemData {

  private Map<String, String> patronGroups;
  private Map<String, String> addressTypes;
  private Map<String, String> servicePoints;
  @Builder.Default
  private Set<Department> departments = new HashSet<>();
}
