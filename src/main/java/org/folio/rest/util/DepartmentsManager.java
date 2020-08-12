package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.DEPARTMENTS_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_LIST_DEPARTMENTS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import io.vertx.core.Future;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;

public class DepartmentsManager {

  private static final String DEPARTMENTS_ARRAY_KEY = "departments";

  private DepartmentsManager() {}

  public static Future<Void> checkAndUpdateDepartments(UserdataimportCollection userCollection,
                                                       Map<String, String> okapiHeaders) {
    Set<String> importDepartmentsIds = userCollection.getUsers().stream()
      .map(User::getDepartments)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());

    Set<Department> includedDepartmentsData =
      userCollection.getIncluded() == null ? Collections.emptySet() : userCollection.getIncluded().getDepartments();

    return getDepartments(okapiHeaders)
      .compose(existedDepartments -> updateDepartments(importDepartmentsIds, includedDepartmentsData, existedDepartments,
        okapiHeaders));
  }

  private static Future<Void> updateDepartments(Set<String> importDepartmentIds, Set<Department> includedDepartmentsData,
                                                Set<Department> existedDepartments, Map<String, String> okapiHeaders) {
    List<Future<Void>> futures = new ArrayList<>();
    for (String departmentId : importDepartmentIds) {
      Optional<Department> existedDepartment = findDepartmentById(existedDepartments, departmentId);
      Optional<Department> includedDepartment = findDepartmentById(includedDepartmentsData, departmentId);
      if (existedDepartment.isPresent()) {
        includedDepartment.ifPresent(department -> futures.add(updateDepartment(department, okapiHeaders)));
      } else {
        if (includedDepartment.isPresent()) {
          futures.add(createDepartment(includedDepartment.get(), okapiHeaders));
        } else {
          return Future.failedFuture("Data for department with id '" + departmentId + "' is not included.");
        }
      }
    }
    return CompositeFutureImpl.all(futures.toArray(new Future[0]))
      .map(compositeFuture -> null);
  }

  private static Future<Void> createDepartment(Department department,
                                               Map<String, String> okapiHeaders) {
    department.setId(null);
    if (StringUtils.isBlank(department.getCode())) {
      department.setCode(generateCode(department.getName()));
    }
    return RequestManager
      .post(okapiHeaders, DEPARTMENTS_ENDPOINT, Department.class, department, "Failed to create department")
      .map(department1 -> null);
  }

  private static String generateCode(String name) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
  }

  private static Optional<Department> findDepartmentById(Set<Department> departments, String id) {
    return departments.stream()
      .filter(department -> department.getId().equalsIgnoreCase(id))
      .findFirst();
  }

  public static Future<Void> updateDepartment(Department department,
                                              Map<String, String> okapiHeaders) {
    return RequestManager
      .put(okapiHeaders, DEPARTMENTS_ENDPOINT + department.getId(), department, "Failed to update department");
  }

  private static Future<Set<Department>> getDepartments(Map<String, String> okapiHeaders) {
    return RequestManager.get(okapiHeaders, DEPARTMENTS_ENDPOINT, FAILED_TO_LIST_DEPARTMENTS)
      .map(DepartmentsManager::extractDepartments);
  }

  private static Set<Department> extractDepartments(JsonObject result) {
    return result.getJsonArray(DEPARTMENTS_ARRAY_KEY)
      .stream()
      .map(o -> new Gson().fromJson(o.toString(), Department.class))
      .collect(Collectors.toSet());
  }

}
