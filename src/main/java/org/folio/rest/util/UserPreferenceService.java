package org.folio.rest.util;

import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_CREATE_USER_PREFERENCE;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_DELETE_USER_PREFERENCE;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_GET_USER_PREFERENCE;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_TO_UPDATE_USER_PREFERENCE;
import static org.folio.rest.util.UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION;
import static org.folio.rest.util.UserImportAPIConstants.REQUEST_PREFERENCES_ENDPOINT;
import static org.folio.rest.util.UserImportAPIConstants.REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT;

import java.util.Map;
import java.util.function.Function;

import javax.validation.ValidationException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.folio.rest.jaxrs.model.User;
import org.jetbrains.annotations.NotNull;

import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.model.UserImportData;
import org.folio.rest.validator.UserRequestManagerValidator;

public class UserPreferenceService {

  public static final String REQUEST_PREFERENCES_ARRAY_KEY = "requestPreferences";

  private UserPreferenceService() {
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferenceService.class);

  public static Future<RequestPreference> get(Map<String, String> okapiHeaders, String userId){
    String query = String.format(REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT , "?query=userId=="+ userId );
    return RequestManager.get(RequestManager.getHttpClient(okapiHeaders), okapiHeaders, query, FAILED_TO_GET_USER_PREFERENCE)
      .map(mapToRequestPreference())
      .otherwiseEmpty();
  }

  @NotNull
  private static Function<JsonObject, RequestPreference> mapToRequestPreference() {
    return object -> object.getJsonArray(REQUEST_PREFERENCES_ARRAY_KEY)
      .getJsonObject(0)
      .mapTo(RequestPreference.class);
  }

  public static Future<Void> update(Map<String, String> okapiHeaders, RequestPreference entity){
    String query = String.format(REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT , "/" + entity.getId() );
    return RequestManager.put(okapiHeaders, query, entity, FAILED_TO_UPDATE_USER_PREFERENCE);
  }

  public static Future<Void> delete(Map<String, String> okapiHeaders, String id){
    String query = String.format(REQUEST_PREFERENCES_SEARCH_QUERY_ENDPOINT , "/" + id);
    return RequestManager.delete(okapiHeaders, query, id, FAILED_TO_DELETE_USER_PREFERENCE);
  }

  public static Future<RequestPreference> create(Map<String, String> okapiHeaders, RequestPreference entity){
    return RequestManager.post(okapiHeaders, REQUEST_PREFERENCES_ENDPOINT, RequestPreference.class, entity, FAILED_TO_CREATE_USER_PREFERENCE);
  }

  public static Future<Void> validate(@NotNull RequestPreference entity, @NotNull UserImportData importData, User user) {
    Promise<Void> promise = Promise.promise();
    try {
      UserRequestManagerValidator.validate(entity, importData, user);
      promise.complete();
    } catch (ValidationException ex){
      LOGGER.error(FAILED_USER_PREFERENCE_VALIDATION + ex.getMessage());
      promise.fail(FAILED_USER_PREFERENCE_VALIDATION + ex.getMessage());
    } catch (Exception e){
      LOGGER.error("Error occurred" + e.getMessage());
      promise.fail(e);
    }
    return promise.future();
  }

}
