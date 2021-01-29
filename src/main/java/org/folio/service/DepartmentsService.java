package org.folio.service;

import static io.vertx.core.Future.succeededFuture;

import static org.folio.rest.impl.UserImportAPIConstants.DEPARTMENTS_ENDPOINT;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_DEPARTMENTS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import org.folio.model.UserImportData;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.jaxrs.model.Department;
import org.folio.util.HttpClientUtil;

public class DepartmentsService {

  private static final String DEPARTMENTS_ARRAY_KEY = "departments";
  private static final String FAILED_TO_CREATE_DEPARTMENT_MESSAGE = "Failed to create department";
  private static final String FAILED_TO_UPDATE_DEPARTMENT_MESSAGE = "Failed to update department";


  public Future<Set<Department>> prepareDepartments(UserImportData importData, Map<String, String> okapiHeaders) {
    return getDepartments(okapiHeaders)
      .compose(systemDepartments -> {
        Set<Department> importDepartments = importData.getDepartments();
        if (!importDepartments.isEmpty()) {
          return updateSystemDepartments(importDepartments, systemDepartments, okapiHeaders);
        } else {
          return succeededFuture(systemDepartments);
        }
      });
  }

  public Optional<Department> findDepartmentByName(Set<Department> departments, String name) {
    return departments.stream()
        .filter(department -> department.getName().equals(name))
        .findFirst();
  }

  public Optional<Department> findDepartmentByCode(Set<Department> departments, String code) {
    return departments.stream()
        .filter(department -> department.getCode().equals(code))
        .findFirst();
  }

  private Future<Set<Department>> updateSystemDepartments(Set<Department> importDepartments,
                                                                 Set<Department> systemDepartments,
                                                                 Map<String, String> okapiHeaders) {
    List<Future<Void>> futures = new ArrayList<>();
    for (Department importDepartment : importDepartments) {
      Optional<Department> existedDepartmentByName = findDepartmentByName(systemDepartments, importDepartment.getName());
      if (existedDepartmentByName.isEmpty()) {
        Optional<Department> existedDepartmentByCode =
          findDepartmentByCode(systemDepartments, importDepartment.getCode());
        if (existedDepartmentByCode.isPresent()) {
          Department systemDepartment = existedDepartmentByCode.get();
          futures.add(updateDepartment(systemDepartment, importDepartment, okapiHeaders));
          systemDepartment.setName(importDepartment.getName());
        } else {
          futures.add(createDepartment(importDepartment, okapiHeaders)
            .onSuccess(systemDepartments::add)
            .map(department -> null));
        }
      }
    }
    return GenericCompositeFuture.all(futures).map(o -> systemDepartments);
  }

  private Future<Set<Department>> getDepartments(Map<String, String> okapiHeaders) {
    return HttpClientUtil.get(okapiHeaders, DEPARTMENTS_ENDPOINT, FAILED_TO_LIST_DEPARTMENTS)
      .map(this::extractDepartments);
  }

  private Future<Department> createDepartment(Department department, Map<String, String> okapiHeaders) {
    if (StringUtils.isBlank(department.getCode())) {
      department.setCode(generateCode(department.getName()));
    }
    return HttpClientUtil
      .post(okapiHeaders, DEPARTMENTS_ENDPOINT, Department.class, department, FAILED_TO_CREATE_DEPARTMENT_MESSAGE);
  }

  private Future<Void> updateDepartment(Department existed, Department updated, Map<String, String> okapiHeaders) {
    return HttpClientUtil
      .put(okapiHeaders, DEPARTMENTS_ENDPOINT + "/" + existed.getId(), updated, FAILED_TO_UPDATE_DEPARTMENT_MESSAGE);
  }

  private String generateCode(String name) {
    return StringUtils.replaceChars(name.toUpperCase(), ' ', '_');
  }

  private Set<Department> extractDepartments(JsonObject result) {
    return result.getJsonArray(DEPARTMENTS_ARRAY_KEY)
      .stream()
      .map(o -> Json.decodeValue(o.toString(), Department.class))
      .collect(Collectors.toSet());
  }

}
