package org.folio.rest.util;

import static io.vertx.core.Future.succeededFuture;

import static org.folio.rest.util.UserImportAPIConstants.DEPARTMENTS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_LIST_DEPARTMENTS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.model.UserImportData;

public class DepartmentsManager {

  private static final String DEPARTMENTS_ARRAY_KEY = "departments";
  private static final String DEPARTMENTS_NOT_EXIST_MESSAGE = "Departments do not exist in the system: [%s]";
  private static final String FAILED_TO_CREATE_DEPARTMENT_MESSAGE = "Failed to create department";
  private static final String FAILED_TO_UPDATE_DEPARTMENT_MESSAGE = "Failed to update department";

  private DepartmentsManager() {}

  public static Future<Void> prepareDepartments(UserImportData importData,
                                                Map<String, String> okapiHeaders) {
    return getDepartments(okapiHeaders)
      .compose(systemDepartments -> {
        Set<Department> importDepartments = importData.getDepartments();
        if (!importDepartments.isEmpty()) {
          return updateSystemDepartments(importDepartments, systemDepartments, okapiHeaders);
        } else {
          return succeededFuture(systemDepartments);
        }
      })
      .compose(systemDepartments -> checkDepartmentsExistence(importData, systemDepartments));
  }

  private static Future<Void> checkDepartmentsExistence(UserImportData importData, Set<Department> systemDepartments) {
    Set<String> usersDepartmentNames = fetchUsersDepartmentNames(importData);
    Set<String> missedDepartmentNames = findMissedDepartments(systemDepartments, usersDepartmentNames);

    if (!missedDepartmentNames.isEmpty()) {
      String errorMessage = String.format(DEPARTMENTS_NOT_EXIST_MESSAGE, String.join(", ", missedDepartmentNames));
      return Future.failedFuture(errorMessage);
    }
    importData.getSystemData().getDepartments().addAll(systemDepartments);
    return succeededFuture();
  }

  private static Set<String> findMissedDepartments(Set<Department> systemDepartments, Set<String> usersDepartmentNames) {
    Set<String> missedDepartmentNames = new TreeSet<>();
    usersDepartmentNames.forEach(departmentName -> {
      if (findDepartmentByName(systemDepartments, departmentName).isEmpty()) {
        missedDepartmentNames.add(departmentName);
      }
    });
    return missedDepartmentNames;
  }

  private static Set<String> fetchUsersDepartmentNames(UserImportData importData) {
    return importData.getUsers().stream()
      .map(User::getDepartments)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private static Future<Set<Department>> updateSystemDepartments(Set<Department> importDepartments,
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
    return CompositeFutureImpl.all(futures.toArray(new Future[0])).map(o -> systemDepartments);
  }

  private static Future<Set<Department>> getDepartments(Map<String, String> okapiHeaders) {
    return RequestManager.get(okapiHeaders, DEPARTMENTS_ENDPOINT, FAILED_TO_LIST_DEPARTMENTS)
      .map(DepartmentsManager::extractDepartments);
  }

  private static Future<Department> createDepartment(Department department, Map<String, String> okapiHeaders) {
    if (StringUtils.isBlank(department.getCode())) {
      department.setCode(generateCode(department.getName()));
    }
    return RequestManager
      .post(okapiHeaders, DEPARTMENTS_ENDPOINT, Department.class, department, FAILED_TO_CREATE_DEPARTMENT_MESSAGE);
  }

  private static Future<Void> updateDepartment(Department existed, Department updated, Map<String, String> okapiHeaders) {
    return RequestManager
      .put(okapiHeaders, DEPARTMENTS_ENDPOINT + "/" + existed.getId(), updated, FAILED_TO_UPDATE_DEPARTMENT_MESSAGE);
  }

  private static String generateCode(String name) {
    return StringUtils.replaceChars(name.toUpperCase(), ' ', '_');
  }

  public static Optional<Department> findDepartmentByName(Set<Department> departments, String name) {
    return departments.stream()
      .filter(department -> department.getName().equals(name))
      .findFirst();
  }

  public static Optional<Department> findDepartmentByCode(Set<Department> departments, String code) {
    return departments.stream()
      .filter(department -> department.getCode().equals(code))
      .findFirst();
  }

  private static Set<Department> extractDepartments(JsonObject result) {
    return result.getJsonArray(DEPARTMENTS_ARRAY_KEY)
      .stream()
      .map(o -> Json.decodeValue(o.toString(), Department.class))
      .collect(Collectors.toSet());
  }

}
