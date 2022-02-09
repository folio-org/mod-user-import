package org.folio.service;

import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_CREATE_USER_PREFERENCE;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_DELETE_USER_PREFERENCE;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_GET_USER_PREFERENCE;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_UPDATE_USER_PREFERENCE;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION;
import static org.folio.rest.impl.UserImportAPIConstants.REQUEST_PREFERENCES_ENDPOINT;
import static org.folio.rest.impl.UserImportAPIConstants.REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT;

import java.util.Map;

import javax.validation.ValidationException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import org.folio.model.UserImportData;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.validator.UserRequestManagerValidator;
import org.folio.util.HttpClientUtil;

public class UserPreferenceService {

  public static final String REQUEST_PREFERENCES_ARRAY_KEY = "requestPreferences";
  private static final Logger LOGGER = LogManager.getLogger(UserPreferenceService.class);

  public Future<RequestPreference> get(Map<String, String> okapiHeaders, WebClient webClient, String userId) {
    String query = String.format(REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT, "?query=userId==" + userId);
    return HttpClientUtil.webClientOkapi(webClient, HttpMethod.GET, okapiHeaders, query)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .map(res -> res.bodyAsJsonObject().getJsonArray(REQUEST_PREFERENCES_ARRAY_KEY).getJsonObject(0).mapTo(RequestPreference.class))
        .otherwiseEmpty();
  }

  public Future<Void> update(Map<String, String> okapiHeaders, WebClient webClient, RequestPreference entity) {
    String query = String.format(REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT, "/" + entity.getId());
    return HttpClientUtil.webClientOkapi(webClient, HttpMethod.PUT, okapiHeaders, query)
        .expect(ResponsePredicate.SC_NO_CONTENT)
        .sendJsonObject(JsonObject.mapFrom(entity))
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_UPDATE_USER_PREFERENCE))
        .mapEmpty();
  }

  public Future<Void> delete(Map<String, String> okapiHeaders, WebClient webClient, String id) {
    String query = String.format(REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT, "/" + id);
    return HttpClientUtil.webClientOkapi(webClient, HttpMethod.DELETE, okapiHeaders, query)
        .expect(ResponsePredicate.SC_NO_CONTENT)
        .send()
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_DELETE_USER_PREFERENCE))
        .mapEmpty();
  }

  public Future<RequestPreference> create(Map<String, String> okapiHeaders, WebClient webClient, RequestPreference entity) {
    return HttpClientUtil.webClientOkapi(webClient, HttpMethod.POST, okapiHeaders, REQUEST_PREFERENCES_ENDPOINT)
        .expect(ResponsePredicate.SC_CREATED)
        .sendJsonObject(JsonObject.mapFrom(entity))
        .map(res -> res.bodyAsJsonObject().mapTo(RequestPreference.class))
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_CREATE_USER_PREFERENCE));
  }

  public Future<Void> validate(@NotNull RequestPreference entity, @NotNull UserImportData importData, User user) {
    Promise<Void> promise = Promise.promise();
    try {
      UserRequestManagerValidator.validate(entity, importData, user);
      promise.complete();
    } catch (ValidationException ex) {
      LOGGER.error(FAILED_USER_PREFERENCE_VALIDATION + ex.getMessage());
      promise.fail(FAILED_USER_PREFERENCE_VALIDATION + ex.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error occurred" + e.getMessage());
      promise.fail(e);
    }
    return promise.future();
  }

}
