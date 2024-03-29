package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.CUSTOM_FIELDS_ENDPOINT;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_GET_USER_MODULE_ID;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_CUSTOM_FIELDS;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_UPDATE_CUSTOM_FIELD;
import static org.folio.rest.impl.UserImportAPIConstants.LIMIT_ALL;
import static org.folio.rest.impl.UserImportAPIConstants.CUSTOM_FIELDS_INTERFACE_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import org.folio.model.UserImportData;
import org.folio.model.exception.CustomFieldMappingFailedException;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.CheckboxField;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.SelectFieldOption;
import org.folio.rest.jaxrs.model.SelectFieldOptions;
import org.folio.util.HttpClientUtil;
import org.folio.util.OkapiUtil;

public class CustomFieldsService {

  public Future<Set<CustomField>> prepareCustomFields(UserImportData importData, Map<String, String> okapiHeaders) {
    Map<String, String> headers = new CaseInsensitiveMap<>(okapiHeaders);

    return OkapiUtil.getModulesProvidingInterface(CUSTOM_FIELDS_INTERFACE_NAME, headers)
      .compose(moduleIds -> updateHeaders(moduleIds, headers))
      .compose(o -> getCustomFields(headers))
      .compose(systemCustomFields -> {
        Set<CustomField> importCustomFields = importData.getCustomFields();
        if (importCustomFields.isEmpty()) {
          return Future.succeededFuture(systemCustomFields);
        }
        return updateCustomFields(importCustomFields, systemCustomFields, headers);
      })
      .recover(e -> HttpClientUtil.errorManagement(e, "Failed to prepare custom fields"));
  }

  public Optional<CustomField> findCustomFieldByRefId(Set<CustomField> customFields, String refId) {
    return customFields.stream()
        .filter(customField -> customField.getRefId().equals(refId))
        .findAny();
  }

  private Future<Set<CustomField>> updateCustomFields(Set<CustomField> importCustomFields,
      Set<CustomField> systemCustomFields, Map<String, String> okapiHeaders) {

    List<Future<Void>> futures = new ArrayList<>();
    for (CustomField importCustomField : importCustomFields) {
      CustomField systemCustomField = findCustomFieldByRefId(systemCustomFields, importCustomField.getRefId())
          .orElseThrow(() -> new CustomFieldMappingFailedException(Set.of(importCustomField.getRefId())));

      updateValues(systemCustomField, importCustomField);
      futures.add(updateCustomField(systemCustomField, okapiHeaders));
    }
    return GenericCompositeFuture.all(futures)
        .map(systemCustomFields)
        .recover(e -> HttpClientUtil.errorManagement(e, "Failed to update custom fields"));
  }

  private void updateValues(CustomField target, CustomField source) {
    updateCommonValues(target, source);
    switch (target.getType()) {
      case SINGLE_CHECKBOX:
        updateCheckboxFieldValues(target, source);
        break;
      case RADIO_BUTTON:
      case MULTI_SELECT_DROPDOWN:
      case SINGLE_SELECT_DROPDOWN:
        updateSelectableFieldValues(target, source);
        break;
      default:
        break;
    }
  }

  private void updateCommonValues(CustomField target, CustomField source) {
    target.setMetadata(null);
    target.setName(extractValue(source, target, CustomField::getName));
    target.setVisible(extractValue(source, target, CustomField::getVisible));
    target.setIsRepeatable(extractValue(source, target, CustomField::getIsRepeatable));
    target.setHelpText(extractValue(source, target, CustomField::getHelpText));
  }

  private void updateCheckboxFieldValues(CustomField target, CustomField source) {
    CheckboxField checkboxField = target.getCheckboxField();
    checkboxField.setDefault(extractValue(checkboxField, source.getCheckboxField(), CheckboxField::getDefault));
  }

  private void updateSelectableFieldValues(CustomField target, CustomField source) {
    if (source.getSelectField() != null && source.getSelectField().getOptions() != null) {
      SelectFieldOptions targetOptions = target.getSelectField().getOptions();
      SelectFieldOptions sourceOptions = source.getSelectField().getOptions();
      targetOptions.setSortingOrder(extractValue(targetOptions, sourceOptions, SelectFieldOptions::getSortingOrder));

      for (SelectFieldOption value : sourceOptions.getValues()) {
        targetOptions.getValues().stream()
          .filter(opt -> opt.getValue().equals(value.getValue()))
          .findFirst()
          .ifPresentOrElse(opt -> opt.setDefault(value.getDefault()), () -> addNewOption(targetOptions, value));
      }
    }
  }

  private void addNewOption(SelectFieldOptions options, SelectFieldOption newOption) {
    newOption.setId(createOptionId(options));
    options.getValues().add(newOption);
  }

  private String createOptionId(SelectFieldOptions options) {
    int maxOptId = options.getValues().stream()
      .map(SelectFieldOption::getId)
      .map(s -> StringUtils.substringAfter(s, "_"))
      .mapToInt(Integer::parseInt)
      .max().orElse(0);
    return "opt_" + ++maxOptId;
  }

  private Future<Void> updateCustomField(CustomField customField, Map<String, String> okapiHeaders) {
    String query = CUSTOM_FIELDS_ENDPOINT + "/" + customField.getId();
    return HttpClientUtil.getRequestOkapi(HttpMethod.PUT, okapiHeaders, query)
        .expect(ResponsePredicate.SC_NO_CONTENT)
        .sendJson(customField)
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_UPDATE_CUSTOM_FIELD))
        .mapEmpty();
  }

  private <E, T> T extractValue(E o1, E o2, Function<E, T> extractFunc) {
    if (o2 == null) {
      return extractFunc.apply(o1);
    }
    return ObjectUtils.defaultIfNull(extractFunc.apply(o1), extractFunc.apply(o2));
  }

  private Future<Void> updateHeaders(List<String> moduleIds, Map<String, String> headers) {
    if (moduleIds.size() != 1) {
      return Future.failedFuture(FAILED_TO_GET_USER_MODULE_ID);
    } else {
      headers.put(XOkapiHeaders.MODULE_ID, moduleIds.get(0));
      return Future.succeededFuture();
    }
  }

  private Future<Set<CustomField>> getCustomFields(Map<String, String> headers) {
    return HttpClientUtil.getRequestOkapi(HttpMethod.GET, headers, CUSTOM_FIELDS_ENDPOINT + LIMIT_ALL)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .map(res -> extractCustomFields(res.bodyAsJsonObject()))
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_LIST_CUSTOM_FIELDS));
  }

  private Set<CustomField> extractCustomFields(JsonObject json) {
    return json.getJsonArray("customFields").stream()
      .map(o -> Json.decodeValue(o.toString(), CustomField.class))
      .collect(Collectors.toSet());
  }

}
