package org.folio.rest.impl;

import static org.folio.rest.util.AddressTypeManager.*;
import static org.folio.rest.util.HttpClientUtil.*;
import static org.folio.rest.util.PatronGroupManager.*;
import static org.folio.rest.util.UserDataUtil.*;
import static org.folio.rest.util.UserImportAPIConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.folio.rest.jaxrs.model.FailedUser;
import org.folio.rest.jaxrs.model.ImportResponse;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;
import org.folio.rest.jaxrs.resource.UserImportResource;
import org.folio.rest.model.UserImportData;
import org.folio.rest.model.UserMappingFailedException;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.util.SingleUserImportResponse;
import org.folio.rest.util.UserRecordImportStatus;

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
  private static final int CONN_TO = 5000;
  private static final int IDLE_TO = 10000;

  /*
   * Fake endpoint. Workaround for raml-module-builder.
   */
  @Override
  public void getUserImport(RoutingContext routingContext, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    asyncResultHandler
      .handle(Future.succeededFuture(GetUserImportResponse.withPlainBadRequest("This is a fake endpoint.")));
  }

  /**
   * User import entry point.
   */
  @Override
  public void postUserImport(UserdataimportCollection userCollection, RoutingContext routingContext,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {
    if (userCollection.getTotalRecords() == 0) {
      ImportResponse emptyResponse = new ImportResponse()
        .withMessage("No users to import.")
        .withTotalRecords(0);
      asyncResultHandler
        .handle(Future.succeededFuture(PostUserImportResponse.withJsonOK(emptyResponse)));
    } else {

      HttpClientInterface httpClient = HttpClientFactory.getHttpClient(getOkapiUrl(okapiHeaders), -1, okapiHeaders.get(OKAPI_TENANT_HEADER), true, CONN_TO, IDLE_TO,false,30L);
      startUserImport(httpClient, okapiHeaders, userCollection).setHandler(handler -> {
        if (handler.succeeded() && handler.result() != null && handler.result().getError() == null) {
          asyncResultHandler
            .handle(Future.succeededFuture(PostUserImportResponse.withJsonOK(handler.result())));
        } else {
          asyncResultHandler
            .handle(Future.succeededFuture(PostUserImportResponse.withJsonInternalServerError(handler.result())));
        }

      });
    }
  }

  /**
   * Start user import by getting address types and patron groups from the system.
   */
  private Future<ImportResponse> startUserImport(HttpClientInterface httpClient, Map<String, String> okapiHeaders, UserdataimportCollection userCollection) {

    Future<ImportResponse> future = Future.future();

    getAddressTypes(httpClient, okapiHeaders).setHandler(addressTypeResultHandler -> {
      if (addressTypeResultHandler.failed()) {
        LOGGER.error(FAILED_TO_LIST_ADDRESS_TYPES + extractErrorMessage(addressTypeResultHandler));
        ImportResponse addressTypeListingFailureResponse = processErrorResponse(userCollection, FAILED_TO_LIST_ADDRESS_TYPES + extractErrorMessage(addressTypeResultHandler));
        future.complete(addressTypeListingFailureResponse);
      } else {
        getPatronGroups(httpClient, okapiHeaders).setHandler(patronGroupResultHandler -> {

          if (patronGroupResultHandler.succeeded()) {
            UserImportData userImportData = new UserImportData(userCollection);
            userImportData.setAddressTypes(addressTypeResultHandler.result());
            userImportData.setPatronGroups(patronGroupResultHandler.result());

            if (userImportData.getDeactivateMissingUsers()) {
              startImportWithDeactivatingUsers(httpClient, okapiHeaders, userCollection, userImportData).setHandler(
                future.completer());
            } else {
              startImport(httpClient, userCollection, userImportData, okapiHeaders).setHandler(future.completer());
            }

          } else {
            LOGGER.error(FAILED_TO_LIST_PATRON_GROUPS + extractErrorMessage(patronGroupResultHandler));
            ImportResponse patronGroupListingFailureResponse = processErrorResponse(userCollection, FAILED_TO_LIST_PATRON_GROUPS + extractErrorMessage(patronGroupResultHandler));
            future.complete(patronGroupListingFailureResponse);
          }
        });
      }
    });
    return future;

  }

  /**
   * Start importing users if deactivation is needed. In this case all users should be queried to be able to tell which ones need to be deactivated after the import.
   */
  private Future<ImportResponse> startImportWithDeactivatingUsers(HttpClientInterface httpClient, Map<String, String> okapiHeaders, UserdataimportCollection userCollection,
    UserImportData userImportData) {
    Future<ImportResponse> future = Future.future();
    listAllUsersWithExternalSystemId(httpClient, okapiHeaders, userCollection.getSourceType()).setHandler(handler -> {

      if (handler.failed()) {
        LOGGER.error("Failed to list users with externalSystemId (and specific sourceType)");
        ImportResponse userListingFailureResponse = processErrorResponse(userCollection, FAILED_TO_IMPORT_USERS + extractErrorMessage(handler));
        future.complete(userListingFailureResponse);
      } else {
        List<Map> existingUsers = handler.result();
        try {
          final Map<String, User> existingUserMap = extractExistingUsers(existingUsers);

          List<Future> futures = processAllUsersInPartitions(httpClient, userCollection, userImportData, existingUserMap, okapiHeaders);

          CompositeFuture.all(futures).setHandler(ar -> {
            if (ar.succeeded()) {
              LOGGER.info("Processing user search result.");
              ImportResponse compositeResponse = processFutureResponses(futures);

              if (existingUserMap.isEmpty()) {
                compositeResponse.setMessage(USERS_WERE_IMPORTED_SUCCESSFULLY);
                future.complete(compositeResponse);
              } else if (compositeResponse.getFailedRecords() > 0) {
                LOGGER.warn("Failed to import all users, skipping deactivation.");
                compositeResponse.setMessage(USERS_WERE_IMPORTED_SUCCESSFULLY + " " + USER_DEACTIVATION_SKIPPED);
                future.complete(compositeResponse);
              } else {
                deactivateUsers(httpClient, okapiHeaders, existingUserMap).setHandler(deactivateHandler -> {
                  compositeResponse.setMessage("Deactivated missing users.");
                  future.complete(compositeResponse);
                });
              }
            } else {
              ImportResponse userProcessFailureResponse = processErrorResponse(userCollection, FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
              future.complete(userProcessFailureResponse);
            }
          });
        } catch (UserMappingFailedException exc) {
          ImportResponse userMappingFailureResponse = processErrorResponse(userCollection, USER_SCHEMA_MISMATCH);
          future.complete(userMappingFailureResponse);
        }
      }
    });
    return future;
  }

  /**
   * Create partitions from all users, process them and return the list of Futures of the partition processing.
   */
  private List<Future> processAllUsersInPartitions(HttpClientInterface httpClient, UserdataimportCollection userCollection, UserImportData userImportData, Map<String, User> existingUserMap, Map<String, String> okapiHeaders) {
    List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);
    List<Future> futures = new ArrayList<>();

    for (List<User> currentPartition : userPartitions) {
      Future<ImportResponse> userSearchAsyncResult =
        processUserSearchResult(httpClient, okapiHeaders, existingUserMap,
          currentPartition, userImportData);
      futures.add(userSearchAsyncResult);
    }
    return futures;
  }

  /**
   * Start user import. Partition and process users in batches of 10.
   */
  private Future<ImportResponse> startImport(HttpClientInterface httpClient, UserdataimportCollection userCollection, UserImportData userImportData, Map<String, String> okapiHeaders) {
    Future<ImportResponse> future = Future.future();
    List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);

    List<Future> futures = new ArrayList<>();

    for (List<User> currentPartition : userPartitions) {
      Future<ImportResponse> userBatchProcessResponse =
        processUserBatch(httpClient, okapiHeaders, currentPartition, userImportData);
      futures.add(userBatchProcessResponse);
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        LOGGER.info("Aggregating user import result.");
        ImportResponse successResponse = processFutureResponses(futures);
        successResponse.setMessage(USERS_WERE_IMPORTED_SUCCESSFULLY);
        future.complete(successResponse);
      } else {
        ImportResponse userProcessFailureResponse = processErrorResponse(userCollection, FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
        future.complete(userProcessFailureResponse);
      }
    });
    return future;
  }

  /**
   * Process a batch of users. Extract existing users from the user list and process the result (create non-existing, update existing users).
   * @param userSearchClient
   */
  private Future<ImportResponse> processUserBatch(HttpClientInterface httpClient, Map<String, String> okapiHeaders,
    List<User> currentPartition, UserImportData userImportData) {
    Future<ImportResponse> processFuture = Future.future();
    listUsers(httpClient, currentPartition, userImportData.getSourceType()).setHandler(userSearchAsyncResponse -> {
      if (userSearchAsyncResponse.succeeded()) {
        try {
          Map<String, User> existingUsers = extractExistingUsers(userSearchAsyncResponse.result());

          processUserSearchResult(httpClient, okapiHeaders, existingUsers, currentPartition, userImportData)
            .setHandler(response -> {
              if (response.succeeded()) {
                processFuture.complete(response.result());
              } else {
                LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(response));
                UserdataimportCollection userCollection = new UserdataimportCollection();
                userCollection.setTotalRecords(currentPartition.size());
                userCollection.setUsers(currentPartition);
                ImportResponse userSearchFailureResponse = processErrorResponse(userCollection, FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(response));
                processFuture.complete(userSearchFailureResponse);
              }
            });
        } catch (UserMappingFailedException exc) {
          UserdataimportCollection userCollection = new UserdataimportCollection();
          userCollection.setTotalRecords(currentPartition.size());
          userCollection.setUsers(currentPartition);
          ImportResponse userMappingFailureResponse = processErrorResponse(userCollection, FAILED_TO_PROCESS_USER_SEARCH_RESULT + USER_SCHEMA_MISMATCH);
          processFuture.complete(userMappingFailureResponse);
        }

      } else {
        LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(userSearchAsyncResponse));
        UserdataimportCollection userCollection = new UserdataimportCollection();
        userCollection.setTotalRecords(currentPartition.size());
        userCollection.setUsers(currentPartition);
        ImportResponse userSearchFailureResponse = processErrorResponse(userCollection, FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(userSearchAsyncResponse));
        processFuture.complete(userSearchFailureResponse);
      }
    });
    return processFuture;
  }

  /**
   * List a batch of users.
   */
  private Future<List<Map>> listUsers(HttpClientInterface userSearchClient, List<User> users, String sourceType) {
    Future<List<Map>> future = Future.future();

    StringBuilder userQueryBuilder = new StringBuilder("externalSystemId==(");
    for (int i = 0; i < users.size(); i++) {
      if (!Strings.isNullOrEmpty(sourceType)) {
        userQueryBuilder.append(sourceType).append("_");
      }
      userQueryBuilder.append(users.get(i).getExternalSystemId());
      if (i < users.size() - 1) {
        userQueryBuilder.append(" or ");
      } else {
        userQueryBuilder.append(")");
      }
    }

    String query = userQueryBuilder.toString();

    final String userSearchQuery = generateUserSearchQuery(query, users.size() * 2, 0);

    try {
      userSearchClient.request(userSearchQuery)
        .whenComplete((userSearchQueryResponse, ex) -> {
          if (isSuccess(userSearchQueryResponse, ex)) {
            JsonObject resultObject = userSearchQueryResponse.getBody();
            future.complete(getUsersFromResult(resultObject));
          } else {
            errorManagement(userSearchQueryResponse, ex, future, FAILED_TO_PROCESS_USER_SEARCH_RESPONSE);
          }
        });
    } catch (Exception exc) {
      LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE, exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  /**
   * Process batch of users. Decide if current user exists, if it does, updates it, otherwise creates a new one.
   */
  private Future<ImportResponse> processUserSearchResult(HttpClientInterface httpClient, Map<String, String> okapiHeaders,
    Map<String, User> existingUsers, List<User> usersToImport, UserImportData userImportData) {
    Future<ImportResponse> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : usersToImport) {
      updateUserData(user, userImportData);
      if (existingUsers.containsKey(user.getExternalSystemId())) {
        if (userImportData.getUpdateOnlyPresentFields()) {
          user = updateExistingUserWithIncomingFields(user, existingUsers.get(user.getExternalSystemId()));
        } else {
          user.setId(existingUsers.get(user.getExternalSystemId()).getId());
        }
        Future<SingleUserImportResponse> userUpdateResponse = updateUser(httpClient, okapiHeaders, user);
        futures.add(userUpdateResponse);
        existingUsers.remove(user.getExternalSystemId());
      } else {
        Future<SingleUserImportResponse> userCreationResponse = createNewUser(httpClient, okapiHeaders, user);
        futures.add(userCreationResponse);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        LOGGER.info("User creation and update has finished for the current batch.");
        ImportResponse successResponse = processSuccessfulImportResponse(futures);
        future.complete(successResponse);
      } else {
        LOGGER.error(FAILED_TO_IMPORT_USERS);
        future.fail(FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
      }
    });

    return future;
  }

  /**
   * Aggregate SingleUserImportResponses to an ImportResponse.
   */
  private ImportResponse processSuccessfulImportResponse(List<Future> futures) {
    List<FailedUser> failedUsers = new ArrayList<>();
    int created = 0;
    int updated = 0;
    int failed = 0;
    for (Future currentFuture : futures) {
      if (currentFuture.result() instanceof SingleUserImportResponse) {
        SingleUserImportResponse resp = (SingleUserImportResponse) currentFuture.result();
        if (resp.getStatus() == UserRecordImportStatus.CREATED) {
          created++;
        } else if (resp.getStatus() == UserRecordImportStatus.UPDATED) {
          updated++;
        } else {
          failed++;
          failedUsers.add(new FailedUser().withExternalSystemId(resp.getExternalSystemId()).withUsername(resp.getUsername()).withErrorMessage(resp.getErrorMessage()));
        }
      }
    }
    return new ImportResponse()
      .withMessage("")
      .withTotalRecords(futures.size())
      .withCreatedRecords(created)
      .withUpdatedRecords(updated)
      .withFailedRecords(failed)
      .withFailedUsers(failedUsers);
  }

  /**
   * Update a single user.
   */
  private Future<SingleUserImportResponse> updateUser(HttpClientInterface httpClient, Map<String, String> okapiHeaders, final User user) {
    Future<SingleUserImportResponse> future = Future.future();

    try {
      final String userUpdateQuery = UriBuilder.fromPath("/users/" + user.getId()).build().toString();

      Map<String, String> headers = createHeaders(okapiHeaders, "text/plain", HTTP_HEADER_VALUE_APPLICATION_JSON);

      httpClient.request(HttpMethod.PUT, JsonObject.mapFrom(user), userUpdateQuery, headers)
        .whenComplete((res, ex) -> {
          if (isSuccess(res, ex)) {
            try {
              future.complete(SingleUserImportResponse.updated(user.getExternalSystemId()));
            } catch (Exception e) {
              LOGGER.warn(FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), e.getMessage());
              future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), user.getUsername(), -1, e.getMessage()));
            }
          } else {
            errorManagement(res, ex, future, FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(),
              SingleUserImportResponse.failed(user.getExternalSystemId(), user.getUsername(), res.getCode(), FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId()));
          }
        });
    } catch (Exception exc) {
      LOGGER.error(FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), exc.getMessage());
      future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), user.getUsername(), -1, exc.getMessage()));
    }

    return future;
  }

  /**
   * Create a new user.
   */
  private Future<SingleUserImportResponse> createNewUser(HttpClientInterface httpClient, Map<String, String> okapiHeaders, User user) {
    Future<SingleUserImportResponse> future = Future.future();

    user.setId(UUID.randomUUID().toString());

    final String userCreationQuery = UriBuilder.fromPath("/users").build().toString();
    Map<String, String> headers = createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, HTTP_HEADER_VALUE_APPLICATION_JSON);

    try {
      httpClient.request(HttpMethod.POST, JsonObject.mapFrom(user), userCreationQuery, headers)
        .whenComplete((userCreationResponse, ex) -> {
          if (isSuccess(userCreationResponse, ex)) {
            try {
              addEmptyPermissionSetForUser(httpClient, okapiHeaders, user).setHandler(futurePermissionHandler -> {
                if (futurePermissionHandler.failed()) {
                  LOGGER.error("Failed to register permissions for user with externalSystemId: " + user.getExternalSystemId());
                }
                future.complete(SingleUserImportResponse.created(user.getExternalSystemId()));
              });
            } catch (Exception e) {
              LOGGER.warn("Failed to register permission for user with externalSystemId: " + user.getExternalSystemId());
              future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), user.getUsername(), -1, e.getMessage()));
            }
          } else {
            errorManagement(userCreationResponse, ex, future, FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(),
              SingleUserImportResponse.failed(user.getExternalSystemId(), user.getUsername(), userCreationResponse.getCode(), FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId()));
          }
        });
    } catch (Exception exc) {
      LOGGER.error(FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), exc.getMessage());
      future.complete(SingleUserImportResponse.failed(user.getExternalSystemId(), user.getUsername(), -1, exc.getMessage()));
    }

    return future;
  }

  private Future<JsonObject> addEmptyPermissionSetForUser(HttpClientInterface httpClient, Map<String, String> okapiHeaders, User user) {
    Future<JsonObject> future = Future.future();

    Map<String, String> headers = createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, HTTP_HEADER_VALUE_APPLICATION_JSON);

    try {
      JsonObject object = new JsonObject();
      object.put("userId", user.getId());
      object.put("permissions", new JsonArray());

      final String permissionAddQuery = UriBuilder.fromPath("/perms/users").build().toString();

      httpClient.request(HttpMethod.POST, object, permissionAddQuery, headers)
        .whenComplete((response, ex) -> {
          if (isSuccess(response, ex)) {
            try {
              future.complete(response.getBody());
            } catch (Exception e) {
              LOGGER.error(FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), e.getMessage());
              future.fail(e);
            }
          } else {
            errorManagement(response, ex, future, FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId());
          }
        });
    } catch (Exception exc) {
      LOGGER.error(FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId(), exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  /**
   * List all users (in a sourceType if given).
   */
  private Future<List<Map>> listAllUsersWithExternalSystemId(HttpClientInterface httpClient, Map<String, String> okapiHeaders, String sourceType) {
    Future<List<Map>> future = Future.future();

    StringBuilder queryBuilder = new StringBuilder("externalSystemId");
    if (!Strings.isNullOrEmpty(sourceType)) {
      queryBuilder.append("=^").append(sourceType).append("_*");
    } else {
      queryBuilder.append("<>''");
    }

    final String query = queryBuilder.toString();
    int limit = 10;
    Map<String, String> headers = createHeaders(okapiHeaders, HTTP_HEADER_VALUE_APPLICATION_JSON, null);

    try {

      final String userSearchQuery = generateUserSearchQuery(query, limit, 0);
      httpClient.request(HttpMethod.GET, userSearchQuery, headers)
        .whenComplete((response, ex) -> {
          if (isSuccess(response, ex)) {
            listAllUsers(future, response.getBody(), httpClient, okapiHeaders, query, limit);
          } else {
            errorManagement(response, ex, future, FAILED_TO_PROCESS_USER_SEARCH_RESULT);
          }
        });
    } catch (Exception exc) {
      LOGGER.error(FAILED_TO_PROCESS_USERS, exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  /**
   * List all users.
   */
  private void listAllUsers(Future<List<Map>> future, JsonObject resultObject, HttpClientInterface userSearchClient, Map<String, String> okapiHeaders, String query, int limit) {
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
          Future subFuture = processResponse(existingUserList, userSearchClient, okapiHeaders, query, limit, offset * limit);
          futures.add(subFuture);
        }

        CompositeFuture.all(futures).setHandler(ar -> {
          if (ar.succeeded()) {
            LOGGER.info("Listed all users.");
            future.complete(existingUserList);
          } else {
            LOGGER.error(FAILED_TO_PROCESS_USERS);
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

  /**
   * Process user search response.
   */
  private Future processResponse(List<Map> existingUserList, HttpClientInterface userSearchClient, Map<String, String> okapiHeaders, String query, int limit, int offset) {
    Future future = Future.future();

    try {
      final String userSearchQuery = generateUserSearchQuery(query, limit, offset);
      userSearchClient.request(HttpMethod.GET, userSearchQuery, okapiHeaders)
        .whenComplete((subResponse, subEx) -> {
          if (isSuccess(subResponse, subEx)) {
            try {
              List<Map> users = getUsersFromResult(subResponse.getBody());
              existingUserList.addAll(users);
              future.complete();
            } catch (Exception e) {
              LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE, e.getMessage());
              future.fail(e);
            }
          } else {
            errorManagement(subResponse, subEx, future, FAILED_TO_PROCESS_USER_SEARCH_RESPONSE);
          }
        });

    } catch (Exception exc) {
      LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESPONSE, exc.getMessage());
      future.fail(exc);
    }

    return future;
  }

  /**
   * Deactivate users
   * @param okapiHeaders the Okapi headers
   * @param existingUserMap the existing users that were not updated in the request
   * @return  a completed future if users were deactivated
   *          a failed future if not all users could be deactivated
   */
  private Future<Void> deactivateUsers(HttpClientInterface httpClient, Map<String, String> okapiHeaders,
    Map<String, User> existingUserMap) {
    Future<Void> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : existingUserMap.values()) {
      if (user.getActive()) {
        user.setActive(Boolean.FALSE);
        Future<SingleUserImportResponse> userDeactivateAsyncResult =
          updateUser(httpClient, okapiHeaders, user);
        futures.add(userDeactivateAsyncResult);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        LOGGER.info("Deactivated missing users.");
        future.complete();
      } else {
        LOGGER.error("Failed to deactivate users.");
        future.fail("Failed to deactivate users." + extractErrorMessage(ar));
      }
    });

    return future;
  }

  /**
   * Build query for user search.
   * @param query the query string
   * @param limit maximum number of retrieved users
   * @param offset page number
   * @return the build query string
   */
  private String generateUserSearchQuery(String query, int limit, int offset) {
    return UriBuilder.fromPath("/users")
      .queryParam("query", query)
      .queryParam("limit", limit)
      .queryParam("offset", offset)
      .queryParam("orderBy", "externalSystemId")
      .queryParam("order", "asc").build().toString();
  }

  /**
   * Extract users from JSONObject.
   * @param result the JSONObject containing the users
   * @return the users from the JSONObject
   */
  private List getUsersFromResult(JsonObject result) {
    JsonArray array = result.getJsonArray("users");
    if (array == null) {
      return new ArrayList();
    }
    return array.getList();
  }

  /**
   * Extract error message from result.
   * @param asyncResult the result
   * @return the extracted error message
   */
  private String extractErrorMessage(AsyncResult asyncResult) {
    if (asyncResult.cause() != null && !Strings.isNullOrEmpty(asyncResult.cause().getMessage())) {
      return ERROR_MESSAGE + asyncResult.cause().getMessage();
    } else {
      return "";
    }
  }

  /**
   * Create import response from sub-responses.
   * @param futures the ImportResponse list with the successful/failed user creation/update
   * @return the aggregated ImportResponse
   */
  private ImportResponse processFutureResponses(List<Future> futures) {
    int created = 0;
    int updated = 0;
    int failed = 0;
    int totalRecords = 0;
    List<FailedUser> failedUsers = new ArrayList<>();
    for (Future currentFuture : futures) {
      if (currentFuture.result() instanceof ImportResponse) {
        ImportResponse currentResponse = (ImportResponse) currentFuture.result();
        created += currentResponse.getCreatedRecords();
        updated += currentResponse.getUpdatedRecords();
        failed += currentResponse.getFailedRecords();
        totalRecords += currentResponse.getTotalRecords();
        failedUsers.addAll(currentResponse.getFailedUsers());
      }
    }
    return new ImportResponse().withCreatedRecords(created)
      .withUpdatedRecords(updated)
      .withFailedRecords(failed)
      .withTotalRecords(totalRecords)
      .withFailedUsers(failedUsers);
  }

  /**
   * Helper function to create ImportResponse.
   * @param userCollection the users which were failed to import
   * @param errorMessage the reason of the failure
   * @return the assembled ImportResponse object
   */
  private ImportResponse processErrorResponse(UserdataimportCollection userCollection, String errorMessage) {
    List<FailedUser> failedUsers = new ArrayList<>();
    for (User user : userCollection.getUsers()) {
      FailedUser failedUser = new FailedUser()
        .withExternalSystemId(user.getExternalSystemId())
        .withUsername(user.getUsername())
        .withErrorMessage(errorMessage);
      failedUsers.add(failedUser);
    }
    return new ImportResponse()
      .withMessage(FAILED_TO_IMPORT_USERS)
      .withError(errorMessage)
      .withTotalRecords(userCollection.getTotalRecords())
      .withCreatedRecords(0)
      .withUpdatedRecords(0)
      .withFailedRecords(userCollection.getTotalRecords())
      .withFailedUsers(failedUsers);
  }

  private boolean isSuccess(org.folio.rest.tools.client.Response response, Throwable ex) {
    return ex == null && org.folio.rest.tools.client.Response.isSuccess(response.getCode());
  }

  private <T> void errorManagement(org.folio.rest.tools.client.Response response, Throwable ex, Future<T> future, String errorMessage) {
    errorManagement(response, ex, future, errorMessage, null);
  }

  private <T> void errorManagement(org.folio.rest.tools.client.Response response, Throwable ex, Future<T> future, String errorMessage, T completeObj) {
    if (ex != null) {
      LOGGER.error(errorMessage);
      LOGGER.error(ex.getMessage());
      future.fail(ex.getMessage());
    } else {
      LOGGER.error(errorMessage);
      StringBuilder errorBuilder = new StringBuilder(errorMessage);
      if (response.getError() != null) {
        errorBuilder.append(" " + response.getError().toString());
        LOGGER.error(response.getError());
      }
      if (completeObj == null) {
        future.fail(errorBuilder.toString());
      } else {
        future.complete(completeObj);
      }
    }
  }

}
