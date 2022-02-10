package org.folio.rest.impl;

import static org.folio.rest.impl.UserImportAPIConstants.ERROR_MESSAGE;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_IMPORT_USERS;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_PROCESS_USERS;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESPONSE;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID;
import static org.folio.rest.impl.UserImportAPIConstants.PERMS_USERS_ENDPOINT;
import static org.folio.rest.impl.UserImportAPIConstants.USERS_ENDPOINT;
import static org.folio.rest.impl.UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY;
import static org.folio.rest.impl.UserImportAPIConstants.USER_DEACTIVATION_SKIPPED;
import static org.folio.rest.impl.UserImportAPIConstants.USER_SCHEMA_MISMATCH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.folio.model.SingleUserImportResponse;
import org.folio.model.UserImportData;
import org.folio.model.UserRecordImportStatus;
import org.folio.model.UserSystemData;
import org.folio.model.exception.UserMappingFailedException;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.FailedUser;
import org.folio.rest.jaxrs.model.ImportResponse;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;
import org.folio.rest.jaxrs.resource.UserImport;
import org.folio.service.AddressTypeService;
import org.folio.service.CustomFieldsService;
import org.folio.service.DepartmentsService;
import org.folio.service.PatronGroupService;
import org.folio.service.ServicePointsService;
import org.folio.service.UserDataProcessingService;
import org.folio.service.UserPreferenceService;
import org.folio.util.HttpClientUtil;

public class UserImportAPI implements UserImport {

  private static final Logger LOGGER = LogManager.getLogger(UserImportAPI.class);

  private final CustomFieldsService cfService;
  private final UserDataProcessingService udpService;
  private final UserPreferenceService prefService;
  private final AddressTypeService addressService;
  private final DepartmentsService depService;
  private final PatronGroupService pgService;
  private final ServicePointsService spService;


  public UserImportAPI() {
    cfService = new CustomFieldsService();
    depService = new DepartmentsService();
    udpService = new UserDataProcessingService(depService, cfService);
    prefService = new UserPreferenceService();
    addressService = new AddressTypeService();
    pgService = new PatronGroupService();
    spService = new ServicePointsService();
  }



  /**
   * User import entry point.
   */
  @Override
  @Validate
  public void postUserImport(UserdataimportCollection userCollection, RoutingContext routingContext,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    if (userCollection.getTotalRecords() == 0) {
      ImportResponse emptyResponse = new ImportResponse()
          .withMessage("No users to import.")
          .withTotalRecords(0);
      asyncResultHandler
          .handle(Future.succeededFuture(PostUserImportResponse.respond200WithApplicationJson(emptyResponse)));
      return;
    }
    prepareUserImportData(userCollection, okapiHeaders, vertxContext.owner())
        .compose(importData -> startUserImport(importData, okapiHeaders))
        .otherwise(throwable -> processErrorResponse(userCollection.getUsers(), throwable.getMessage()))
        .onComplete(handler -> {
          if (handler.succeeded() && handler.result() != null && handler.result().getError() == null) {
            asyncResultHandler
                .handle(Future.succeededFuture(PostUserImportResponse.respond200WithApplicationJson(handler.result())));
          } else {
            asyncResultHandler
                .handle(Future.succeededFuture(PostUserImportResponse.respond500WithApplicationJson(handler.result())));
          }
        });
  }

  private Future<UserImportData> prepareUserImportData(UserdataimportCollection userCollection,
      Map<String, String> okapiHeaders, Vertx vertx) {

    UserImportData importData = new UserImportData(userCollection);
    UserSystemData.UserSystemDataBuilder systemDataBuilder = UserSystemData.builder();

    Future<Map<String, String>> addressTypesFuture = addressService.getAddressTypes(okapiHeaders)
        .onSuccess(systemDataBuilder::addressTypes);

    Future<Map<String, String>> patronGroupsFuture = pgService.getPatronGroups(okapiHeaders)
        .onSuccess(systemDataBuilder::patronGroups);

    Future<Map<String, String>> servicePointsFuture = spService.getServicePoints(okapiHeaders)
        .onSuccess(systemDataBuilder::servicePoints);

    Future<Set<CustomField>> customFieldsFuture = cfService.prepareCustomFields(importData, okapiHeaders)
        .onSuccess(systemDataBuilder::customFields);

    Future<Set<Department>> departmentsFuture = depService.prepareDepartments(importData, okapiHeaders)
        .onSuccess(systemDataBuilder::departments);

    return CompositeFuture
        .all(addressTypesFuture, patronGroupsFuture, servicePointsFuture, customFieldsFuture, departmentsFuture)
        .map(o -> importData.withSystemData(systemDataBuilder.build()));
  }

  /**
   * Start user import by getting address types and patron groups from the
   * system.
   */
  private Future<ImportResponse> startUserImport(UserImportData importData, Map<String, String> okapiHeaders) {
    return importUsers(importData, okapiHeaders);
  }

  private Future<ImportResponse> importUsers(UserImportData importData, Map<String, String> okapiHeaders) {

    if (importData.isDeactivateMissingUsers()) {
      return startImportWithDeactivatingUsers(importData, okapiHeaders);
    } else {
      return startImport(importData, okapiHeaders);
    }
  }

  /**
   * Start importing users if deactivation is needed. In this case all users
   * should be queried to be able to tell which ones need to be deactivated
   * after the import.
   */
  private Future<ImportResponse> startImportWithDeactivatingUsers(UserImportData userImportData,
      Map<String, String> okapiHeaders) {

    Promise<ImportResponse> future = Promise.promise();
    listAllUsersWithExternalSystemId(okapiHeaders, userImportData.getSourceType()).onComplete(handler -> {
      if (handler.failed()) {
        LOGGER.error("Failed to list users with externalSystemId (and specific sourceType)");
        ImportResponse userListingFailureResponse =
            processErrorResponse(userImportData.getUsers(), FAILED_TO_IMPORT_USERS + extractErrorMessage(handler));
        future.complete(userListingFailureResponse);
      } else {
        List<Map> existingUsers = handler.result();
        try {
          final Map<String, User> existingUserMap = udpService.extractExistingUsers(existingUsers);

          List<Future<ImportResponse>> futures = processAllUsersInPartitions(userImportData, existingUserMap, okapiHeaders);

          GenericCompositeFuture.all(futures).onComplete(ar -> {
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
                deactivateUsers(okapiHeaders, existingUserMap).onComplete(deactivateHandler -> {
                  compositeResponse.setMessage("Deactivated missing users.");
                  future.complete(compositeResponse);
                });
              }
            } else {
              ImportResponse userProcessFailureResponse =
                  processErrorResponse(userImportData.getUsers(), FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
              future.complete(userProcessFailureResponse);
            }
          });
        } catch (UserMappingFailedException exc) {
          ImportResponse userMappingFailureResponse = processErrorResponse(userImportData.getUsers(), USER_SCHEMA_MISMATCH);
          future.complete(userMappingFailureResponse);
        }
      }
    });
    return future.future();
  }

  /**
   * Create partitions from all users, process them and return the list of
   * Futures of the partition processing.
   */
  private List<Future<ImportResponse>> processAllUsersInPartitions(UserImportData userImportData,
      Map<String, User> existingUserMap, Map<String, String> okapiHeaders) {

    List<List<User>> userPartitions = Lists.partition(userImportData.getUsers(), 10);
    List<Future<ImportResponse>> futures = new ArrayList<>();
    for (List<User> currentPartition : userPartitions) {
      Future<ImportResponse> userSearchAsyncResult
          = processUserSearchResult(okapiHeaders, existingUserMap, currentPartition, userImportData);
      futures.add(userSearchAsyncResult);
    }
    return futures;
  }

  /**
   * Start user import. Partition and process users in batches of 10.
   */
  private Future<ImportResponse> startImport(UserImportData userImportData, Map<String, String> okapiHeaders) {

    List<List<User>> userPartitions = Lists.partition(userImportData.getUsers(), 10);

    List<Future<ImportResponse>> futures = new ArrayList<>();

    for (List<User> currentPartition : userPartitions) {
      Future<ImportResponse> userBatchProcessResponse
          = processUserBatch(okapiHeaders, currentPartition, userImportData);
      futures.add(userBatchProcessResponse);
    }
    return GenericCompositeFuture.all(futures)
        .map(res -> {
          ImportResponse successResponse = processFutureResponses(futures);
          successResponse.setMessage(USERS_WERE_IMPORTED_SUCCESSFULLY);
          return successResponse;
        })
        .recover(e -> {
          ImportResponse userProcessFailureResponse =
              processErrorResponse(userImportData.getUsers(), FAILED_TO_IMPORT_USERS + extractErrorMessage(e));
          return Future.succeededFuture(userProcessFailureResponse);
        });
  }

  /**
   * Process a batch of users. Extract existing users from the user list and
   * process the result (create non-existing, update existing users).
   */
  private Future<ImportResponse> processUserBatch(Map<String, String> okapiHeaders,
      List<User> currentPartition, UserImportData userImportData) {

    Promise<ImportResponse> future = Promise.promise();
    listUsers(okapiHeaders, currentPartition, userImportData.getSourceType()).onComplete(userSearchAsyncResponse -> {
      if (userSearchAsyncResponse.succeeded()) {
        try {
          Map<String, User> existingUsers = udpService.extractExistingUsers(userSearchAsyncResponse.result());

          processUserSearchResult(okapiHeaders, existingUsers, currentPartition, userImportData)
            .onComplete(response -> {
              if (response.succeeded()) {
                future.complete(response.result());
              } else {
                LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(response));
                UserdataimportCollection userCollection = new UserdataimportCollection();
                userCollection.setTotalRecords(currentPartition.size());
                userCollection.setUsers(currentPartition);
                ImportResponse userSearchFailureResponse = processErrorResponse(userImportData.getUsers(),
                  FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(response));
                future.complete(userSearchFailureResponse);
              }
            });
        } catch (UserMappingFailedException exc) {
          UserdataimportCollection userCollection = new UserdataimportCollection();
          userCollection.setTotalRecords(currentPartition.size());
          userCollection.setUsers(currentPartition);
          ImportResponse userMappingFailureResponse =
            processErrorResponse(userImportData.getUsers(), FAILED_TO_PROCESS_USER_SEARCH_RESULT + USER_SCHEMA_MISMATCH);
          future.complete(userMappingFailureResponse);
        }

      } else {
        LOGGER.error(FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(userSearchAsyncResponse));
        UserdataimportCollection userCollection = new UserdataimportCollection();
        userCollection.setTotalRecords(currentPartition.size());
        userCollection.setUsers(currentPartition);
        ImportResponse userSearchFailureResponse = processErrorResponse(userImportData.getUsers(),
          FAILED_TO_PROCESS_USER_SEARCH_RESULT + extractErrorMessage(userSearchAsyncResponse));
        future.complete(userSearchFailureResponse);
      }
    });
    return future.future();
  }

  /**
   * List a batch of users.
   */
  private Future<List> listUsers(Map<String, String> okapiHeaders,
      List<User> users, String sourceType) {

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
    final String userSearchQuery = generateUserSearchQuery(userQueryBuilder.toString(), users.size() * 2, 0);
    return HttpClientUtil.getRequestOkapi(HttpMethod.GET, okapiHeaders, userSearchQuery)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .map(res -> getUsersFromResult(res.bodyAsJsonObject()))
        .recover(e -> HttpClientUtil.errorManagement(e, FAILED_TO_PROCESS_USER_SEARCH_RESPONSE));
  }

  /**
   * Process batch of users. Decide if current user exists, if it does, updates
   * it, otherwise creates a new one.
   */
  private Future<ImportResponse> processUserSearchResult(Map<String, String> okapiHeaders,
      Map<String, User> existingUsers, List<User> usersToImport, UserImportData userImportData) {

    Promise<ImportResponse> future = Promise.promise();
    List<Future<SingleUserImportResponse>> futures = usersToImport.stream()
      .map(user -> processUser(user, userImportData, existingUsers, okapiHeaders))
      .collect(Collectors.toList());

    GenericCompositeFuture.all(futures).onComplete(ar -> {
      if (ar.succeeded()) {
        LOGGER.info("User creation and update has finished for the current batch.");
        ImportResponse successResponse = processSuccessfulImportResponse(futures);
        future.complete(successResponse);
      } else {
        LOGGER.error(FAILED_TO_IMPORT_USERS);
        future.fail(FAILED_TO_IMPORT_USERS + extractErrorMessage(ar));
      }
    });
    return future.future();
  }

  private Future<SingleUserImportResponse> processUser(User user, UserImportData userImportData,
      Map<String, User> existingUsers, Map<String, String> okapiHeaders) {

    try {
      udpService.updateUserData(user, userImportData);
    } catch (RuntimeException e) {
      SingleUserImportResponse failed = getFailedUserResponse(user, e);
      return Future.succeededFuture(failed);
    }

    if (existingUsers.containsKey(user.getExternalSystemId())) {
      if (userImportData.isUpdateOnlyPresentFields()) {
        user = udpService.updateExistingUserWithIncomingFields(user, existingUsers.get(user.getExternalSystemId()));
      } else {
        user.setId(existingUsers.get(user.getExternalSystemId()).getId());
      }
      existingUsers.remove(user.getExternalSystemId());
      User finalUser = user;
      return updateUser(okapiHeaders, user)
        .compose(singleUserImportResponse -> updateUserPreference(finalUser, userImportData, okapiHeaders)
          .map(o -> singleUserImportResponse)
          .otherwise(e -> getFailedUserResponse(finalUser, e))
        );
    } else {
      User finalUser = user;
      return createNewUser(okapiHeaders, user)
        .compose(singleUserImportResponse -> createUserPreference(finalUser, userImportData, okapiHeaders)
          .map(o -> singleUserImportResponse)
          .otherwise(e -> getFailedUserResponse(finalUser, e))
        );
    }
  }

  private SingleUserImportResponse getFailedUserResponse(User finalUser, Throwable e) {
    return SingleUserImportResponse
      .failed(finalUser.getExternalSystemId(), finalUser.getUsername(), -1, e.getMessage());
  }

  /**
   * Aggregate SingleUserImportResponses to an ImportResponse.
   */
  private ImportResponse processSuccessfulImportResponse(List<Future<SingleUserImportResponse>> futures) {
    List<FailedUser> failedUsers = new ArrayList<>();
    int created = 0;
    int updated = 0;
    int failed = 0;
    for (Future<SingleUserImportResponse> currentFuture : futures) {
      SingleUserImportResponse resp = currentFuture.result();
      if (resp.getStatus() == UserRecordImportStatus.CREATED) {
        created++;
      } else if (resp.getStatus() == UserRecordImportStatus.UPDATED) {
        updated++;
      } else {
        failed++;
        failedUsers.add(new FailedUser().withExternalSystemId(resp.getExternalSystemId()).withUsername(resp.getUsername())
          .withErrorMessage(resp.getErrorMessage()));
      }
    }
    return new ImportResponse()
      .withMessage("")
      .withTotalRecords(created + updated + failed)
      .withCreatedRecords(created)
      .withUpdatedRecords(updated)
      .withFailedRecords(failed)
      .withFailedUsers(failedUsers);
  }

  /**
   * Update a single user.
   */
  private Future<SingleUserImportResponse> updateUser(Map<String, String> okapiHeaders,
      final User user) {

    final String userUpdateQuery = UriBuilder.fromPath(USERS_ENDPOINT + "/" + user.getId()).build().toString();

    return HttpClientUtil.getRequestOkapi(HttpMethod.PUT, okapiHeaders, userUpdateQuery)
        .expect(ResponsePredicate.SC_NO_CONTENT)
        .sendJson(JsonObject.mapFrom(user))
        .compose(
            res -> Future.succeededFuture(SingleUserImportResponse.updated(user.getExternalSystemId()))
            , e -> HttpClientUtil.errorManagement(e, FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId()));
  }

  /**
   * Create a new user.
   */
  private Future<SingleUserImportResponse> createNewUser(Map<String, String> okapiHeaders, User user) {

    if (user.getId() == null) {
      user.setId(UUID.randomUUID().toString());
    }

    final String userCreationQuery = UriBuilder.fromPath(USERS_ENDPOINT).build().toString();

    // create permission before user object to avoid user without permission
    // a dangling permission user object does not hurt.
    return addEmptyPermissionSetForUser(okapiHeaders, user).compose(x ->
        HttpClientUtil.getRequestOkapi(HttpMethod.POST, okapiHeaders, userCreationQuery)
            .expect(ResponsePredicate.SC_CREATED)
            .sendJsonObject(JsonObject.mapFrom(user))
            .compose(
                res -> Future.succeededFuture(SingleUserImportResponse.created(user.getExternalSystemId())),
                e -> Future.succeededFuture(SingleUserImportResponse.failed(user.getExternalSystemId(), user.getUsername(),
                    500, FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId()))));
    // e.getMessage() not used
  }

  private Future<RequestPreference> createUserPreference(User user, UserImportData userImportData,
      Map<String, String> okapiHeaders) {

    RequestPreference requestPreference = userImportData.getRequestPreferences().get(user.getUsername());
    if (Objects.nonNull(requestPreference)) {
      requestPreference.setUserId(user.getId());
      return prefService.validate(requestPreference, userImportData, user)
          .compose(o -> {
            udpService.updateUserPreference(requestPreference, userImportData);
            return prefService.create(okapiHeaders, requestPreference);
          });
    } else {
      return Future.succeededFuture().mapEmpty();
    }
  }

  private Future<RequestPreference> updateUserPreference(User user, UserImportData userImportData,
      Map<String, String> okapiHeaders) {

    return prefService.get(okapiHeaders, user.getId())
        .compose(result -> {
          if (Objects.nonNull(result)) {
            RequestPreference requestPreference = userImportData.getRequestPreferences().get(user.getUsername());
            if (Objects.nonNull(requestPreference)) {
              requestPreference.setId(result.getId());
              requestPreference.setUserId(result.getUserId());
              return prefService.validate(requestPreference, userImportData, user)
                  .compose(o -> {
                    udpService.updateUserPreference(requestPreference, userImportData);
                    return prefService.update(okapiHeaders, requestPreference).mapEmpty();
                  });
            } else if (!userImportData.isUpdateOnlyPresentFields()) {
              return prefService.delete(okapiHeaders, result.getId()).mapEmpty();
            } else {
              return Future.succeededFuture();
            }
          } else {
            return createUserPreference(user, userImportData,  okapiHeaders);
          }
        });
  }

  private Future<JsonObject> addEmptyPermissionSetForUser(Map<String, String> okapiHeaders,
      User user) {

    JsonObject object = new JsonObject();
    object.put("userId", user.getId());
    object.put("permissions", new JsonArray());

    final String permissionAddQuery = UriBuilder.fromPath(PERMS_USERS_ENDPOINT).build().toString();

    return HttpClientUtil.getRequestOkapi(HttpMethod.POST, okapiHeaders, permissionAddQuery)
        .expect(ResponsePredicate.SC_CREATED)
        .sendJsonObject(object)
        .compose(
            res -> Future.succeededFuture(res.bodyAsJsonObject())
            , e -> HttpClientUtil.errorManagement(e, FAILED_TO_ADD_PERMISSIONS_FOR_USER_WITH_EXTERNAL_SYSTEM_ID + user.getExternalSystemId())
        );
  }

  /**
   * List all users (in a sourceType if given).
   */
  private Future<List<Map>> listAllUsersWithExternalSystemId(Map<String, String> okapiHeaders, String sourceType) {
    StringBuilder queryBuilder = new StringBuilder("externalSystemId");
    if (!Strings.isNullOrEmpty(sourceType)) {
      queryBuilder.append("=^").append(sourceType).append("_*");
    } else {
      queryBuilder.append("<>''");
    }

    final String query = queryBuilder.toString();
    int limit = 10;
    final String userSearchQuery = generateUserSearchQuery(query, limit, 0);
    return HttpClientUtil.getRequestOkapi(HttpMethod.GET, okapiHeaders, userSearchQuery)
      .expect(ResponsePredicate.SC_OK)
      .send()
      .compose(res -> listAllUsers(res.bodyAsJsonObject(), okapiHeaders, query, limit),
        ex -> HttpClientUtil.errorManagement(ex, FAILED_TO_PROCESS_USER_SEARCH_RESULT));
  }

  /**
   * List all users.
   */
  private Future<List<Map>> listAllUsers(JsonObject resultObject,
      Map<String, String> okapiHeaders, String query, int limit) {

    List<Map> users = getUsersFromResult(resultObject);
    List<Map> existingUserList = new ArrayList<>(users);
    int totalRecords = resultObject.getInteger("totalRecords");
    if (totalRecords <= limit) {
      return Future.succeededFuture(existingUserList);
    }
    int numberOfPages = totalRecords / limit;
    if (totalRecords % limit != 0) {
      numberOfPages++;
    }
    List<Future<Void>> futures = new ArrayList<>();
    for (int offset = 1; offset < numberOfPages; offset++) {
      futures.add(processResponse(existingUserList, okapiHeaders, query, limit, offset * limit));
    }
    return GenericCompositeFuture.all(futures)
        .compose(
            x -> Future.succeededFuture(existingUserList),
            e -> Future.failedFuture(FAILED_TO_PROCESS_USERS + extractErrorMessage(e)));
  }


  /**
     * Process user search response.
     */
  private Future<Void> processResponse(List<Map> existingUserList, Map<String, String> okapiHeaders,
      String query, int limit, int offset) {

    final String userSearchQuery = generateUserSearchQuery(query, limit, offset);
    return HttpClientUtil.getRequestOkapi(HttpMethod.GET, okapiHeaders, userSearchQuery)
        .expect(ResponsePredicate.SC_OK)
        .send()
        .compose(
            res -> {
              List<Map> users = getUsersFromResult(res.bodyAsJsonObject());
              existingUserList.addAll(users);
              return Future.succeededFuture();
              },
            e -> HttpClientUtil.errorManagement(e,FAILED_TO_PROCESS_USER_SEARCH_RESPONSE));
  }

  /**
   * Deactivate users
   *
   * @param okapiHeaders    the Okapi headers
   * @param existingUserMap the existing users that were not updated in the
   *                        request
   * @return a completed future if users were deactivated a failed future if not
   * all users could be deactivated
   */
  private Future<Void> deactivateUsers(Map<String, String> okapiHeaders, Map<String, User> existingUserMap) {

    List<Future<SingleUserImportResponse>> futures = new ArrayList<>();
    for (User user : existingUserMap.values()) {
      if (Boolean.TRUE.equals(user.getActive())) {
        user.setActive(Boolean.FALSE);
        futures.add(updateUser(okapiHeaders, user));
      }
    }
    return GenericCompositeFuture.all(futures).compose(
        res -> Future.succeededFuture(),
        e -> HttpClientUtil.errorManagement(e, "Failed to deactivate users"));
  }

  /**
   * Build query for user search.
   *
   * @param query  the query string
   * @param limit  maximum number of retrieved users
   * @param offset page number
   * @return the build query string
   */
  private String generateUserSearchQuery(String query, int limit, int offset) {
    return UriBuilder.fromPath(USERS_ENDPOINT)
      .queryParam("query", query)
      .queryParam("limit", limit)
      .queryParam("offset", offset)
      .queryParam("orderBy", "externalSystemId")
      .queryParam("order", "asc").build().toString();
  }

  /**
   * Extract users from JSONObject.
   *
   * @param result the JSONObject containing the users
   * @return the users from the JSONObject
   */
  private List getUsersFromResult(JsonObject result) {
    JsonArray array = result.getJsonArray("users");
    if (array == null) {
      return Collections.EMPTY_LIST;
    }
    return array.getList();
  }

  /**
   * Extract error message from result.
   *
   * @param asyncResult the result
   * @return the extracted error message
   */
  private String extractErrorMessage(AsyncResult asyncResult) {
    return extractErrorMessage(asyncResult.cause());
  }

  private String extractErrorMessage(Throwable cause) {
    if (cause != null && !Strings.isNullOrEmpty(cause.getMessage())) {
      return ERROR_MESSAGE + cause.getMessage();
    } else {
      return "";
    }
  }

  /**
   * Create import response from sub-responses.
   *
   * @param futures the ImportResponse list with the successful/failed user
   *                creation/update
   * @return the aggregated ImportResponse
   */
  private ImportResponse processFutureResponses(List<Future<ImportResponse>> futures) {
    int created = 0;
    int updated = 0;
    int failed = 0;
    int totalRecords = 0;
    List<FailedUser> failedUsers = new ArrayList<>();
    for (Future<ImportResponse> currentFuture : futures) {
      ImportResponse currentResponse = currentFuture.result();
      created += currentResponse.getCreatedRecords();
      updated += currentResponse.getUpdatedRecords();
      failed += currentResponse.getFailedRecords();
      totalRecords += currentResponse.getTotalRecords();
      failedUsers.addAll(currentResponse.getFailedUsers());
    }
    return new ImportResponse().withCreatedRecords(created)
        .withUpdatedRecords(updated)
        .withFailedRecords(failed)
        .withTotalRecords(totalRecords)
        .withFailedUsers(failedUsers);
  }

  /**
   * Helper function to create ImportResponse.
   *
   * @param userCollection the users which were failed to import
   * @param errorMessage   the reason of the failure
   * @return the assembled ImportResponse object
   */
  private ImportResponse processErrorResponse(List<User> userCollection, String errorMessage) {
    List<FailedUser> failedUsers = new ArrayList<>();
    for (User user : userCollection) {
      FailedUser failedUser = new FailedUser()
        .withExternalSystemId(user.getExternalSystemId())
        .withUsername(user.getUsername())
        .withErrorMessage(errorMessage);
      failedUsers.add(failedUser);
    }
    return new ImportResponse()
      .withMessage(FAILED_TO_IMPORT_USERS)
      .withError(errorMessage)
      .withTotalRecords(userCollection.size())
      .withCreatedRecords(0)
      .withUpdatedRecords(0)
      .withFailedRecords(userCollection.size())
      .withFailedUsers(failedUsers);
  }

  private boolean isSuccess(org.folio.rest.tools.client.Response response, Throwable ex) {
    return ex == null && org.folio.rest.tools.client.Response.isSuccess(response.getCode());
  }

  private <T> void errorManagement(org.folio.rest.tools.client.Response response, Throwable ex, Promise<T> future,
                                   String errorMessage) {
    errorManagement(response, ex, future, errorMessage, null);
  }

  private <T> void errorManagement(org.folio.rest.tools.client.Response response, Throwable ex, Promise<T> future,
                                   String errorMessage, T completeObj) {
    if (ex != null) {
      LOGGER.error(errorMessage);
      LOGGER.error(ex.getMessage());
      future.fail(ex.getMessage());
    } else {
      LOGGER.error(errorMessage);
      StringBuilder errorBuilder = new StringBuilder(errorMessage);
      if (response.getError() != null) {
        errorBuilder.append(" ").append(response.getError().toString());
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
