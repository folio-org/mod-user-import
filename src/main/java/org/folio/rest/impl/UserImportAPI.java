package org.folio.rest.impl;

import static org.folio.rest.util.AddressTypeManager.*;
import static org.folio.rest.util.HttpClientUtil.*;
import static org.folio.rest.util.PatronGroupManager.*;
import static org.folio.rest.util.UserDataUtil.*;
import static org.folio.rest.util.UserImportAPIConstants.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.folio.rest.jaxrs.model.ImportResponse;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataCollection;
import org.folio.rest.jaxrs.resource.UserImportResource;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.SingleUserImportResponse;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class UserImportAPI implements UserImportResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserImportAPI.class);

  /*
   * Fake endpoint. Workaround for raml-module-builder.
   */
  @Override
  public void getUserImport(RoutingContext routingContext, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    asyncResultHandler
      .handle(Future.succeededFuture(GetUserImportResponse.withPlainBadRequest("This is a fake endpoint.")));
  }

  @Override
  public void postUserImport(UserdataCollection userCollection, RoutingContext routingContext,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {
    if (userCollection.getTotalRecords() == 0) {
      ImportResponse emptyResponse = new ImportResponse();
      emptyResponse.setMessage("No users to import.");
      emptyResponse.setTotalRecords(0);
      asyncResultHandler
        .handle(Future.succeededFuture(PostUserImportResponse.withJsonOK(emptyResponse)));
    } else {
      importUsers(okapiHeaders, userCollection).setHandler(handler -> {
        if (handler.succeeded()) {
          asyncResultHandler
            .handle(Future.succeededFuture(PostUserImportResponse.withJsonOK(handler.result())));
        } else {
          asyncResultHandler
            .handle(Future.succeededFuture(PostUserImportResponse.withPlainBadRequest(handler.cause().getMessage())));
        }

      });
    }
  }

  private Future<ImportResponse> importUsers(Map<String, String> okapiHeaders, UserdataCollection userCollection) {

    Future<ImportResponse> future = Future.future();

    getAddressTypes(okapiHeaders).setHandler(addressTypeResultHandler -> {
      if (addressTypeResultHandler.failed()) {
        LOGGER.warn(FAILED_TO_LIST_ADDRESS_TYPES);
        future.fail(FAILED_TO_LIST_ADDRESS_TYPES + extractErrorMessage(addressTypeResultHandler));
      } else {
        getPatronGroups(okapiHeaders).setHandler(patronGroupResultHandler -> {

          if (patronGroupResultHandler.succeeded()) {

            if (userCollection.getDeactivateMissingUsers() != null && userCollection.getDeactivateMissingUsers()) {
              processWithDeactivatingUsers(okapiHeaders, userCollection, patronGroupResultHandler.result(), addressTypeResultHandler.result()).setHandler(
                future.completer());
            } else {
              processUserImport(userCollection, patronGroupResultHandler.result(), addressTypeResultHandler.result(), okapiHeaders).setHandler(future.completer());
            }

          } else {
            future.fail("Failed to list patron groups." + extractErrorMessage(patronGroupResultHandler));
          }
        });
      }
    });
    return future;

  }

  private Future<ImportResponse> processWithDeactivatingUsers(Map<String, String> okapiHeaders, UserdataCollection userCollection,
    Map<String, String> patronGroups, Map<String, String> addressTypes) {
    Future<ImportResponse> future = Future.future();
    listAllUsersWithExternalSystemId(okapiHeaders, userCollection.getSourceType()).setHandler(handler -> {

      if (handler.failed()) {
        LOGGER.warn("Failed to list users with externalSystemId (and specific sourceType)");
        future.fail(FAILED_TO_IMPORT_USERS + extractErrorMessage(handler));
      } else {
        LOGGER.info("response: " + handler.result());
        List<Map> existingUsers = handler.result();
        final Map<String, User> existingUserMap = extractExistingUsers(existingUsers);

        List<Future> futures = processUsers(userCollection, addressTypes, patronGroups, existingUserMap, okapiHeaders);

        CompositeFuture.all(futures).setHandler(ar -> {
          if (ar.succeeded()) {
            ImportResponse compositeResponse = processFutureResponses(futures);

            if (existingUserMap.isEmpty()) {
              compositeResponse.setMessage("Users were imported successfully.");
              future.complete(compositeResponse);
            } else {
              deactivateUsers(okapiHeaders, existingUserMap).setHandler(deactivateHandler -> {
                compositeResponse.setMessage("Deactivated missing users.");
                future.complete(compositeResponse);
              });
            }
          } else {
            future.fail(FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
          }
        });

      }
    });
    return future;
  }

  private List<Future> processUsers(UserdataCollection userCollection, Map<String, String> addressTypes, Map<String, String> patronGroups, Map<String, User> existingUserMap, Map<String, String> okapiHeaders) {
    List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);
    List<Future> futures = new ArrayList<>();

    Boolean updateOnlyPresentData = userCollection.getUpdateOnlyPresentFields();
    if (updateOnlyPresentData == null) {
      updateOnlyPresentData = Boolean.FALSE;
    }

    for (List<User> currentPartition : userPartitions) {
      Future<ImportResponse> userSearchAsyncResult =
        processUserSearchResult(okapiHeaders, existingUserMap,
          currentPartition, patronGroups, addressTypes, updateOnlyPresentData,
          userCollection.getSourceType());
      futures.add(userSearchAsyncResult);
    }
    return futures;
  }

  private Future<ImportResponse> processUserImport(UserdataCollection userCollection, Map<String, String> patronGroups, Map<String, String> addressTypes, Map<String, String> okapiHeaders) {
    Future<ImportResponse> future = Future.future();
    List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);

    List<Future> futures = new ArrayList<>();
    Boolean updateOnlyPresentData = userCollection.getUpdateOnlyPresentFields();
    if (updateOnlyPresentData == null) {
      updateOnlyPresentData = Boolean.FALSE;
    }

    for (List<User> currentPartition : userPartitions) {
      Future<ImportResponse> userBatchProcessResponse =
        processUserBatch(okapiHeaders, currentPartition, patronGroups, addressTypes,
          updateOnlyPresentData, userCollection.getSourceType());
      futures.add(userBatchProcessResponse);
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        ImportResponse successResponse = processFutureResponses(futures);
        successResponse.setMessage("Users were imported successfully.");
        future.complete(successResponse);
      } else {
        future.fail(FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
      }
    });
    return future;
  }

  private Future<ImportResponse> processUserBatch(Map<String, String> okapiHeaders,
    List<User> currentPartition, Map<String, String> patronGroups, Map<String, String> addressTypes,
    Boolean updateOnlyPresentData, String sourceType) {
    Future<ImportResponse> processFuture = Future.future();
    listUsers(okapiHeaders, currentPartition, sourceType).setHandler(userSearchAsyncResponse -> {
      if (userSearchAsyncResponse.succeeded()) {

        Map<String, User> existingUsers = extractExistingUsers(userSearchAsyncResponse.result());

        processUserSearchResult(okapiHeaders, existingUsers, currentPartition, patronGroups, addressTypes, updateOnlyPresentData, sourceType)
          .setHandler(response -> {
            if (response.succeeded()) {
              processFuture.complete(response.result());
            } else {
              LOGGER.warn(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
              processFuture.fail(FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(response));
            }
          });

      } else {
        LOGGER.warn(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
        processFuture.fail(FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(userSearchAsyncResponse));
      }
    });
    return processFuture;
  }

  private Future<List<Map>> listUsers(Map<String, String> okapiHeaders, List<User> users, String sourceType) {
    Future<List<Map>> future = Future.future();

    StringBuilder userQueryBuilder = new StringBuilder("(");

    for (int i = 0; i < users.size(); i++) {
      userQueryBuilder.append("externalSystemId==\"");
      if (!Strings.isNullOrEmpty(sourceType)) {
        userQueryBuilder.append(sourceType).append("_");
      }
      userQueryBuilder.append(users.get(i).getExternalSystemId()).append("\"");
      if (i < users.size() - 1) {
        userQueryBuilder.append(" or ");
      } else {
        userQueryBuilder.append(")");
      }
    }

    String url = userQueryBuilder.toString();
    try {
      url = URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException exc) {
      LOGGER.warn("Could not encode request URL.");
    }

    HttpClientInterface userSearchClient = createClientWithHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, null);
    final String userSearchQuery = generateUserSearchQuery(url, users.size() * 2, 0);

    try {
      userSearchClient.request(userSearchQuery)
        .whenComplete((userSearchQueryResponse, ex) -> {
          if (ex != null) {
            LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
            LOGGER.debug(ex.getMessage());
            future.fail(ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(userSearchQueryResponse.getCode())) {
            LOGGER.warn(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
            future.fail(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
          } else {
            JsonObject resultObject = userSearchQueryResponse.getBody();
            future.complete(getUsersFromResult(resultObject));
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to process user search result", exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private Future<ImportResponse> processUserSearchResult(Map<String, String> okapiHeaders,
    Map<String, User> existingUsers, List<User> usersToImport, Map<String, String> patronGroups,
    Map<String, String> addressTypes, Boolean updateOnlyPresentData, String sourceType) {
    Future<ImportResponse> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : usersToImport) {
      updateUserData(user, patronGroups, addressTypes, sourceType);
      if (existingUsers.containsKey(user.getExternalSystemId())) {
        if (updateOnlyPresentData) {
          user = updateExistingUserWithIncomingFields(user, existingUsers.get(user.getExternalSystemId()));
        } else {
          user.setId(existingUsers.get(user.getExternalSystemId()).getId());
        }
        Future<SingleUserImportResponse> userUpdateResponse = updateUser(okapiHeaders, user);
        futures.add(userUpdateResponse);
        existingUsers.remove(user.getExternalSystemId());
      } else {
        Future<SingleUserImportResponse> userCreationResponse = createNewUser(okapiHeaders, user);
        futures.add(userCreationResponse);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        ImportResponse successResponse = new ImportResponse();
        successResponse.setMessage("");
        successResponse.setTotalRecords(futures.size());
        List<String> failedExternalSystemIds = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int failed = 0;
        for (Future currentFuture : futures) {
          if (currentFuture.result() instanceof SingleUserImportResponse) {
            SingleUserImportResponse resp = (SingleUserImportResponse) currentFuture.result();
            switch (resp.getStatus()) {
              case CREATED: {
                created++;
                break;
              }
              case UPDATED: {
                updated++;
                break;
              }
              case FAILED: {
                failed++;
                failedExternalSystemIds.add(resp.getExternalSystemId());
                break;
              }
            }
          }
        }
        successResponse.setCreatedRecords(created);
        successResponse.setUpdatedRecords(updated);
        successResponse.setFailedRecords(failed);
        successResponse.setFailedExternalSystemIds(failedExternalSystemIds);

        future.complete(successResponse);
      } else {
        LOGGER.warn(FAILED_TO_IMPORT_USERS);
        future.fail(FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
      }
    });

    return future;
  }

  private Future<SingleUserImportResponse> updateUser(Map<String, String> okapiHeaders, final User user) {
    Future<SingleUserImportResponse> future = Future.future();

    HttpClientInterface userUpdateClient = createClient(okapiHeaders);
    Map<String, String> headers = createHeaders(okapiHeaders, "text/plain", HTTP_HEADER_VALUE_APPLICATION_JSON);

    try {
      final String userUpdateQuery = UriBuilder.fromPath("/users/" + user.getId()).build().toString();

      userUpdateClient.request(HttpMethod.PUT, JsonObject.mapFrom(user), userUpdateQuery, headers)
        .whenComplete((res, ex) -> {
          if (ex != null) {
            LOGGER.error(FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId());
            LOGGER.debug(ex.getMessage());
            future.fail(ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(res.getCode())) {
            LOGGER.warn(FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId());
            future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), res.getCode(), FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId()));
          } else {
            try {
              future.complete(SingleUserImportResponse.updated(user.getExternalSystemId()));
            } catch (Exception e) {
              LOGGER.warn(FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), e.getMessage());
              future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), -1, e.getMessage()));
            }
          }
        });
    } catch (Exception exc) {
      LOGGER.warn(FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), exc.getMessage());
      future.fail(exc);
    }

    return future;
  }

  private Future<SingleUserImportResponse> createNewUser(Map<String, String> okapiHeaders, User user) {
    Future<SingleUserImportResponse> future = Future.future();

    user.setId(UUID.randomUUID().toString());

    HttpClientInterface userCreationClient = createClient(okapiHeaders);
    final String userCreationQuery = UriBuilder.fromPath("/users").build().toString();
    Map<String, String> headers = createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, HTTP_HEADER_VALUE_APPLICATION_JSON);

    try {
      userCreationClient.request(HttpMethod.POST, JsonObject.mapFrom(user), userCreationQuery, headers)
        .whenComplete((userCreationResponse, ex) -> {
          if (ex != null) {
            LOGGER.error("Failed to create new user with externalSystemId: " + user.getExternalSystemId());
            LOGGER.debug(ex.getMessage());
            future.fail(ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(userCreationResponse.getCode())) {
            LOGGER.warn("Failed to create new user with externalSystemId: " + user.getExternalSystemId());
            future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), userCreationResponse.getCode(), "Failed to create new user with externalSystemId: " + user.getExternalSystemId()));
          } else {
            try {
              addEmptyPermissionSetForUser(okapiHeaders, user).setHandler(futurePermissionHandler -> {
                if (futurePermissionHandler.failed()) {
                  LOGGER.error("Failed to register permissions for user with externalSystemId: " + user.getExternalSystemId());
                }
                future.complete(SingleUserImportResponse.created(user.getExternalSystemId()));
              });
            } catch (Exception e) {
              LOGGER.warn("Failed to register permission for user with externalSystemId: " + user.getExternalSystemId());
              future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), -1, e.getMessage()));
            }
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to create new user with externalSystemId: " + user.getExternalSystemId(), exc.getMessage());
      future.fail(exc);
    }

    return future;
  }

  private Future<JsonObject> addEmptyPermissionSetForUser(Map<String, String> okapiHeaders, User user) {
    Future<JsonObject> future = Future.future();

    HttpClientInterface permissionsClient = createClient(okapiHeaders);
    Map<String, String> headers = createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, HTTP_HEADER_VALUE_APPLICATION_JSON);

    try {
      JsonObject object = new JsonObject();
      object.put("userId", user.getId());
      object.put("permissions", new JsonArray());

      final String permissionAddQuery = UriBuilder.fromPath("/perms/users").build().toString();

      permissionsClient.request(HttpMethod.POST, object, permissionAddQuery, headers)
        .whenComplete((response, ex) -> {
          if (ex != null) {
            LOGGER.error(FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId());
            LOGGER.debug(ex.getMessage());
            future.fail(ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
            LOGGER.warn(FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId());
            future.fail(FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId());
          } else {
            try {
              future.complete(response.getBody());
            } catch (Exception e) {
              LOGGER.warn(FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), e.getMessage());
              future.fail(e);
            }
          }
        });
    } catch (Exception exc) {
      LOGGER.warn(FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private Future<List<Map>> listAllUsersWithExternalSystemId(Map<String, String> okapiHeaders, String prefix) {
    Future<List<Map>> future = Future.future();

    final String url = createUrl(prefix);

    int limit = 5;

    HttpClientInterface userSearchClient = createClient(okapiHeaders);
    Map<String, String> headers = createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, null);

    try {

      final String userSearchQuery =
        generateUserSearchQuery(url, limit, 0);
      userSearchClient.request(HttpMethod.GET, userSearchQuery, headers)
        .whenComplete((response, ex) -> {
          if (ex != null) {
            LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
            LOGGER.debug(ex.getMessage());
            future.fail(ex.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
            LOGGER.warn(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
            future.fail(FAILED_TO_PROCESS_USER_SEARCH_RESULT);
          } else {
            processAllUsersResult(future, response.getBody(), userSearchClient, okapiHeaders, url, limit);
          }

        });
    } catch (Exception exc) {
      LOGGER.warn(FAILED_TO_PROCESS_USERS, exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private void processAllUsersResult(Future<List<Map>> future, JsonObject resultObject, HttpClientInterface userSearchClient, Map<String, String> okapiHeaders, String url, int limit) {
    try {
      List<Future> futures = new ArrayList<>();
      List<Map> existingUserList = new ArrayList<>();

      List<Map> users = getUsersFromResult(resultObject);
      existingUserList.addAll(users);
      int totalRecords = resultObject.getInteger("totalRecords");
      if (totalRecords > limit) {

        int numberOfPages = totalRecords / limit;
        if (totalRecords % limit != 0) {
          numberOfPages++;
        }
        for (int offset = 1; offset < numberOfPages; offset++) {
          Future subFuture = processResponse(existingUserList, userSearchClient, okapiHeaders, url, limit, offset * limit);
          futures.add(subFuture);
        }

        CompositeFuture.all(futures).setHandler(ar -> {
          if (ar.succeeded()) {
            future.complete(existingUserList);
          } else {
            LOGGER.warn(FAILED_TO_PROCESS_USERS);
            future.fail(FAILED_TO_PROCESS_USERS + extractErrorMessage(ar));
          }
        });
      } else {
        future.complete(existingUserList);
      }
    } catch (Exception e) {
      LOGGER.warn(FAILED_TO_PROCESS_USERS, e.getMessage());
      future.fail(e);
    }
  }

  private Future processResponse(List<Map> existingUserList, HttpClientInterface userSearchClient, Map<String, String> okapiHeaders, String url, int limit, int offset) {
    Future future = Future.future();

    try {
      final String userSearchQuery = generateUserSearchQuery(url, limit, offset);
      userSearchClient.request(HttpMethod.GET, userSearchQuery, okapiHeaders)
        .whenComplete((subResponse, subEx) -> {
          if (subEx != null) {
            LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE);
            LOGGER.debug(subEx.getMessage());
            future.fail(subEx.getMessage());
          } else if (!org.folio.rest.tools.client.Response.isSuccess(subResponse.getCode())) {
            LOGGER.warn(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE);
            future.fail(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE);
          } else {
            try {
              List<Map> users = getUsersFromResult(subResponse.getBody());
              existingUserList.addAll(users);
              future.complete();
            } catch (Exception e) {
              LOGGER.warn(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE, e.getMessage());
              future.fail(e);
            }
          }

        });

    } catch (Exception exc) {
      LOGGER.warn(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE, exc.getMessage());
      future.fail(exc);
    }

    return future;
  }

  private Future<Void> deactivateUsers(Map<String, String> okapiHeaders,
    Map<String, User> existingUserMap) {
    Future<Void> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : existingUserMap.values()) {
      if (user.getActive()) {
        user.setActive(Boolean.FALSE);
        Future<SingleUserImportResponse> userDeactivateAsyncResult =
          updateUser(okapiHeaders, user);
        futures.add(userDeactivateAsyncResult);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        LOGGER.warn("Failed to deactivate users.");
        future.fail("Failed to deactivate users." + extractErrorMessage(ar));
      }
    });

    return future;
  }

  private String createUrl(String prefix) {
    String url;
    if (!Strings.isNullOrEmpty(prefix)) {
      url = "externalSystemId=^" + prefix + "_*";
    } else {
      url = "externalSystemId<>''";
    }
    return url;
  }

  private String generateUserSearchQuery(String query, int limit, int offset) {
    return UriBuilder.fromPath("/users")
      .queryParam("query", query)
      .queryParam("limit", limit)
      .queryParam("offset", offset)
      .queryParam("orderBy", "externalSystemId")
      .queryParam("order", "asc").build().toString();
  }

  private List getUsersFromResult(JsonObject result) {
    JsonArray array = result.getJsonArray("users");
    if (array == null) {
      return new ArrayList();
    }
    return array.getList();
  }

  private String extractErrorMessage(AsyncResult asyncResult) {
    if (asyncResult.cause() != null && asyncResult.cause().getMessage() != null) {
      return "Error message: " + asyncResult.cause().getMessage();
    } else {
      return "";
    }
  }

  private ImportResponse processFutureResponses(List<Future> futures) {
    ImportResponse response = new ImportResponse();

    int created = 0;
    int updated = 0;
    int failed = 0;
    int totalRecords = 0;
    List<String> failedExternalSystemIds = new ArrayList<>();
    for (Future currentFuture : futures) {
      if (currentFuture.result() instanceof ImportResponse) {
        ImportResponse currentResponse = (ImportResponse) currentFuture.result();
        created += currentResponse.getCreatedRecords();
        updated += currentResponse.getUpdatedRecords();
        failed += currentResponse.getFailedRecords();
        totalRecords += currentResponse.getTotalRecords();
        failedExternalSystemIds.addAll(currentResponse.getFailedExternalSystemIds());
      }
    }
    response.setCreatedRecords(created);
    response.setUpdatedRecords(updated);
    response.setFailedRecords(failed);
    response.setTotalRecords(totalRecords);
    response.setFailedExternalSystemIds(failedExternalSystemIds);
    return response;
  }

}
