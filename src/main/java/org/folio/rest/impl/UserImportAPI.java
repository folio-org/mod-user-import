package org.folio.rest.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataCollection;
import org.folio.rest.jaxrs.resource.UserImportResource;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class UserImportAPI implements UserImportResource {

  public static final String OKAPI_URL_HEADER = "X-Okapi-URL";
  public static final String OKAPI_TOKEN_HEADER = "X-Okapi-Token";
  public static final String OKAPI_TENANT_HEADER = "X-Okapi-Tenant";
  public static final String OKAPI_PERMISSIONS_HEADER = "X-Okapi-Permissions";

  private static final Logger LOGGER = LoggerFactory.getLogger(UserImportAPI.class);

  private static final Map<String, String> preferredContactTypeIds = new HashMap<>();

  static {
    preferredContactTypeIds.put("mail", "001");
    preferredContactTypeIds.put("email", "002");
    preferredContactTypeIds.put("text", "003");
    preferredContactTypeIds.put("phone", "004");
    preferredContactTypeIds.put("mobile", "005");
  }

  @Override
  public void getUserImport(RoutingContext routingContext, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    asyncResultHandler
      .handle(Future.succeededFuture(GetUserImportResponse.withPlainOK("This is a fake endpoint.")));
  }

  @Override
  public void postUserImport(UserdataCollection userCollection, RoutingContext routingContext,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {
    if (userCollection.getTotalRecords() == 0) {
      asyncResultHandler
        .handle(Future.succeededFuture(PostUserImportResponse.withPlainOK("No users to import.")));
    } else {
      importUsers(asyncResultHandler, okapiHeaders, vertxContext, userCollection);
    }
  }

  private void importUsers(Handler<AsyncResult<Response>> asyncResultHandler, Map<String, String> okapiHeaders, Context vertxContext, UserdataCollection userCollection) {

    Future<Map<String, String>> addressTypeAsyncRequest = getAddressTypes(okapiHeaders, vertxContext);

    addressTypeAsyncRequest.setHandler(addressTypeResultHandler -> {
      if (addressTypeResultHandler.failed()) {
        LOGGER.warn("Failed to list address types.");
        asyncResultHandler
          .handle(
            Future.succeededFuture(PostUserImportResponse.withPlainBadRequest("Failed to list address types.")));
      } else {
        Future<Map<String, String>> patronGroupsAsyncRequest = getPatronGroups(okapiHeaders, vertxContext);
        patronGroupsAsyncRequest.setHandler(patronGroupResultHandler -> {

          if (patronGroupResultHandler.succeeded()) {

            if (userCollection.getDeactivateMissingUsers() != null && userCollection.getDeactivateMissingUsers()) {
              processWithDeactivatingUsers(asyncResultHandler, okapiHeaders, vertxContext, userCollection, patronGroupsAsyncRequest.result(), addressTypeAsyncRequest.result());
            } else {
              processUserImport(userCollection, patronGroupsAsyncRequest.result(), addressTypeAsyncRequest.result(), okapiHeaders, vertxContext, asyncResultHandler);
            }

          } else {
            asyncResultHandler
              .handle(
                Future.succeededFuture(PostUserImportResponse.withPlainBadRequest("Failed to list patron groups.")));
          }
        });
      }
    });

  }

  private void processWithDeactivatingUsers(Handler<AsyncResult<Response>> asyncResultHandler, Map<String, String> okapiHeaders, Context vertxContext, UserdataCollection userCollection,
    Map<String, String> patronGroups, Map<String, String> addressTypes) {
    Future<List<Map>> usersResponse =
      listAllUsersWithExternalSystemId(okapiHeaders, vertxContext, userCollection.getSourceType());

    usersResponse.setHandler(handler -> {

      if (handler.failed()) {
        LOGGER.warn("Failed to list users with externalSystemId (and specific sourceType)");
        asyncResultHandler
          .handle(
            Future.succeededFuture(PostUserImportResponse
              .withPlainBadRequest("Failed to import users.")));
      } else {
        LOGGER.info("response: " + handler.result());
        List<Map> existingUsers = handler.result();
        Map<String, User> existingUserMap = extractExistingUsers(existingUsers);

        List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);

        List<Future> futures = new ArrayList<>();

        Boolean updateOnlyPresentData = userCollection.getUpdateOnlyPresentFields();
        if (updateOnlyPresentData == null) {
          updateOnlyPresentData = Boolean.FALSE;
        }

        for (List<User> currentPartition : userPartitions) {
          Future<JsonObject> userSearchAsyncResult =
            processUserSearchResult(okapiHeaders, vertxContext, existingUserMap,
              currentPartition, patronGroups, addressTypes, updateOnlyPresentData,
              userCollection.getSourceType());
          futures.add(userSearchAsyncResult);
        }

        CompositeFuture.all(futures).setHandler(ar -> {
          if (ar.succeeded()) {
            if (existingUserMap.isEmpty()) {
              asyncResultHandler
                .handle(
                  Future
                    .succeededFuture(
                      PostUserImportResponse.withPlainOK("Users were imported successfully.")));
            } else {
              Future<Void> deactivateResult = deactivateUsers(okapiHeaders, vertxContext, existingUserMap);

              deactivateResult.setHandler(deactivateHandler -> {
                asyncResultHandler
                  .handle(
                    Future.succeededFuture(PostUserImportResponse
                      .withPlainOK("Deactivated missing users.")));

              });
            }
          } else {
            asyncResultHandler
              .handle(
                Future
                  .succeededFuture(PostUserImportResponse.withPlainBadRequest("Failed to import users.")));
          }
        });

      }
    });
  }

  private void processUserImport(UserdataCollection userCollection, Map<String, String> patronGroups, Map<String, String> addressTypes, Map<String, String> okapiHeaders, Context vertxContext, Handler<AsyncResult<Response>> asyncResultHandler) {
    List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);

    List<Future> futures = new ArrayList<>();
    Boolean updateOnlyPresentData = userCollection.getUpdateOnlyPresentFields();
    if (updateOnlyPresentData == null) {
      updateOnlyPresentData = Boolean.FALSE;
    }

    for (List<User> currentPartition : userPartitions) {
      Future<String> userBatchProcessResponse =
        processUserBatch(okapiHeaders, vertxContext, currentPartition, patronGroups, addressTypes,
          updateOnlyPresentData, userCollection.getSourceType());
      futures.add(userBatchProcessResponse);
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        asyncResultHandler
          .handle(
            Future
              .succeededFuture(PostUserImportResponse.withPlainOK("Users were imported successfully.")));
      } else {
        asyncResultHandler
          .handle(
            Future.succeededFuture(PostUserImportResponse.withPlainBadRequest("Failed to import users.")));
      }
    });
  }

  private Future<Map<String, String>> getAddressTypes(Map<String, String> okapiHeaders, Context vertxContext) {
    Future<Map<String, String>> future = Future.future();

    String okapiURL = getOkapiUrl(okapiHeaders);

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "application/json");

    final String addressTypeQuery = UriBuilder.fromPath("/addresstypes").build().toString();

    HttpClientInterface addressTypeClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));
    addressTypeClient.setDefaultHeaders(headers);
    try {
      addressTypeClient.request(addressTypeQuery)
        .whenComplete((addressTypeResponse, ex) -> {
          if (!org.folio.rest.tools.client.Response.isSuccess(addressTypeResponse.getCode())) {
            LOGGER.warn("Failed to list address types.");
            future.fail(addressTypeResponse.getError().toString());
          } else {
            JsonObject resultObject = addressTypeResponse.getBody();
            JsonArray addressTypeArray = resultObject.getJsonArray("addressTypes");
            Map<String, String> addressTypes = extractAddressTypes(addressTypeArray);
            future.complete(addressTypes);
          }
        });

    } catch (Exception exc) {
      LOGGER.warn("Failed to list address types", exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private Map<String, String> extractAddressTypes(JsonArray addressTypes) {
    Map<String, String> addressTypeMap = new HashMap<>();
    for (int i = 0; i < addressTypes.size(); i++) {
      JsonObject addressType = addressTypes.getJsonObject(i);
      addressTypeMap.put(addressType.getString("addressType"), addressType.getString("id"));
    }
    return addressTypeMap;
  }

  private Future<Map<String, String>> getPatronGroups(Map<String, String> okapiHeaders, Context vertxContext) {
    Future<Map<String, String>> future = Future.future();

    String okapiURL = getOkapiUrl(okapiHeaders);

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "application/json");

    final String patronGroupQuery = UriBuilder.fromPath("/groups").build().toString();

    HttpClientInterface patronGroupClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));
    patronGroupClient.setDefaultHeaders(headers);

    try {
      patronGroupClient.request(patronGroupQuery)
        .whenComplete((patronGroupResponse, ex) -> {
          if (!org.folio.rest.tools.client.Response.isSuccess(patronGroupResponse.getCode())) {
            LOGGER.warn("Failed to list patron groups");
            future.fail(patronGroupResponse.getError().toString());
          } else {
            JsonObject resultObject = patronGroupResponse.getBody();
            JsonArray patronGroupArray = resultObject.getJsonArray("usergroups");
            Map<String, String> patronGroups = extractPatronGroups(patronGroupArray);
            future.complete(patronGroups);
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to list patron groups", exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private Map<String, String> extractPatronGroups(JsonArray patronGroups) {
    Map<String, String> patronGroupMap = new HashMap<>();
    for (int i = 0; i < patronGroups.size(); i++) {
      JsonObject patronGroup = patronGroups.getJsonObject(i);
      patronGroupMap.put(patronGroup.getString("group"), patronGroup.getString("id"));
    }
    return patronGroupMap;
  }

  private Future<Void> createNewUser(Map<String, String> okapiHeaders, Context vertxContext, User user) {
    Future<Void> future = Future.future();

    String okapiURL = getOkapiUrl(okapiHeaders);

    user.setId(UUID.randomUUID().toString());

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");

    final String userCreationQuery = UriBuilder.fromPath("/users").build().toString();

    HttpClientInterface userCreationClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));

    try {
      userCreationClient.request(HttpMethod.POST, user, userCreationQuery, headers)
        .whenComplete((userCreationResponse, ex) -> {
          if (!org.folio.rest.tools.client.Response.isSuccess(userCreationResponse.getCode())) {
            LOGGER.warn("Failed to create new user with externalSystemId: {}", user.getExternalSystemId());
            future.fail(userCreationResponse.getError().toString());
          } else {
            try {
              Future<JsonObject> permissionsFuture = addEmptyPermissionSetForUser(okapiHeaders, vertxContext, user);
              permissionsFuture.setHandler(futurePermissionHandler -> {
                if (futurePermissionHandler.failed()) {
                  LOGGER.error("Failed to register permissions for user with externalSystemId: {}", user.getExternalSystemId());
                }
                future.complete();
              });
            } catch (Exception e) {
              LOGGER.warn("Failed to register permission for user with externalSystemId: {}", user.getExternalSystemId());
              future.complete();
            }
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to create new user with externalSystemId: {}", user.getExternalSystemId(), exc.getMessage());
      future.fail(exc);
    }

    return future;
  }

  private Future<Object> updateUser(Map<String, String> okapiHeaders, Context vertxContext, User user) {
    Future<Object> future = Future.future();

    String okapiURL = getOkapiUrl(okapiHeaders);

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "text/plain");
    headers.put("Content-type", "application/json");

    HttpClientInterface userUpdateClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));

    try {
      final String userUpdateQuery = UriBuilder.fromPath("/users/" + user.getId()).build().toString();

      userUpdateClient.request(HttpMethod.PUT, user, userUpdateQuery, headers)
        .whenComplete((res, ex) -> {
          if (!org.folio.rest.tools.client.Response.isSuccess(res.getCode())) {
            LOGGER.warn("Failed to update user with externalSystemId: {}", user.getExternalSystemId());
            future.fail(res.getError().toString());
          } else {
            try {
              future.complete(user);
            } catch (Exception e) {
              LOGGER.warn("Failed to update user with externalSystemId: {}", user.getExternalSystemId(), e.getMessage());
              future.fail(e);
            }
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to update user with externalSystemId: {}", user.getExternalSystemId(), exc.getMessage());
      future.fail(exc);
    }

    return future;
  }

  private Future<JsonObject> addEmptyPermissionSetForUser(Map<String, String> okapiHeaders, Context vertxContext,
    User user) {
    Future<JsonObject> future = Future.future();

    String okapiURL = getOkapiUrl(okapiHeaders);

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");

    HttpClientInterface permissionsClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));
    permissionsClient.setDefaultHeaders(headers);

    try {
      JsonObject object = new JsonObject();
      object.put("userId", user.getId());
      object.put("permissions", new JsonArray());

      final String permissionAddQuery = UriBuilder.fromPath("/perms/users").build().toString();

      permissionsClient.request(HttpMethod.POST, object, permissionAddQuery, headers)
        .whenComplete((response, ex) -> {
          if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
            LOGGER.warn("Failed to add permission for user with externalSystemId: {}", user.getExternalSystemId());
            future.fail(response.getError().toString());
          } else {
            try {
              future.complete(response.getBody());
            } catch (Exception e) {
              LOGGER.warn("Failed to add permissions for user with externalSystemId: {}", user.getExternalSystemId(), e.getMessage());
              future.fail(e);
            }
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to add permissions for user with externalSystemId: {}", user.getExternalSystemId(), exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private Future<JsonObject> processUserSearchResult(Map<String, String> okapiHeaders, Context vertxContext,
    Map<String, User> existingUsers, List<User> usersToImport, Map<String, String> patronGroups,
    Map<String, String> addressTypes, Boolean updateOnlyPresentData, String sourceType) {
    Future<JsonObject> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : usersToImport) {
      //TODO create statistics from number of created/updated + failed users
      updateUserData(user, patronGroups, addressTypes, sourceType);
      if (existingUsers.containsKey(user.getExternalSystemId())) {
        if (updateOnlyPresentData) {
          user = updateExistingUserWithIncomingFields(user, existingUsers.get(user.getExternalSystemId()));
        } else {
          user.setId(existingUsers.get(user.getExternalSystemId()).getId());
        }
        Future<Object> userUpdateResponse = updateUser(okapiHeaders, vertxContext, user);
        futures.add(userUpdateResponse);
        existingUsers.remove(user.getExternalSystemId());
      } else {
        Future<Void> userCreationResponse = createNewUser(okapiHeaders, vertxContext, user);
        futures.add(userCreationResponse);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        LOGGER.warn("Failed to import users.");
        future.fail("Failed to import users.");
      }
    });

    return future;
  }

  private Future<String> processUserBatch(Map<String, String> okapiHeaders, Context vertxContext,
    List<User> currentPartition, Map<String, String> patronGroups, Map<String, String> addressTypes,
    Boolean updateOnlyPresentData, String sourceType) {
    Future<String> processFuture = Future.future();
    Future<List<Map>> userSearchResponse = listUsers(okapiHeaders, vertxContext, currentPartition, sourceType);
    userSearchResponse.setHandler(userSearchAsyncResponse -> {
      if (userSearchAsyncResponse.succeeded()) {

        Map<String, User> existingUsers = extractExistingUsers(userSearchAsyncResponse.result());

        Future<JsonObject> userSearchAsyncResult =
          processUserSearchResult(okapiHeaders, vertxContext, existingUsers,
            currentPartition, patronGroups, addressTypes, updateOnlyPresentData, sourceType);
        userSearchAsyncResult.setHandler(response -> {
          if (response.succeeded()) {
            processFuture.complete();
          } else {
            LOGGER.warn("Failed to process user search result.");
            processFuture.fail("Not OK for user processing");
          }
        });

      } else {
        LOGGER.warn("Failed to process user search result.");
        processFuture.fail("Not OK for user search.");
      }
    });
    return processFuture;
  }

  private Future<List<Map>> listUsers(Map<String, String> okapiHeaders, Context vertxContext, List<User> users, String sourceType) {
    Future<List<Map>> future = Future.future();

    String okapiURL = getOkapiUrl(okapiHeaders);

    StringBuilder userQueryBuilder = new StringBuilder("(");

    for (int i = 0; i < users.size(); i++) {
      userQueryBuilder.append("externalSystemId==\"");
      if (!Strings.isNullOrEmpty(sourceType)) {
        userQueryBuilder.append(sourceType).append("_");
      }
      userQueryBuilder.append(users.get(i).getExternalSystemId() + "\"");
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

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "application/json");

    final String userSearchQuery = UriBuilder.fromPath("/users?query=" + url + "&limit=" + users.size() * 2).build().toString();

    HttpClientInterface userSearchClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));
    userSearchClient.setDefaultHeaders(headers);

    try {
      userSearchClient.request(userSearchQuery)
        .whenComplete((userSearchQueryResponse, ex) -> {
          if (!org.folio.rest.tools.client.Response.isSuccess(userSearchQueryResponse.getCode())) {
            LOGGER.warn("Failed to process user search result.");
            future.fail(userSearchQueryResponse.getError().toString());
          } else {
            JsonObject resultObject = userSearchQueryResponse.getBody();
            JsonArray array = resultObject.getJsonArray("users");
            future.complete(array.getList());
          }
        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to process user search result", exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private Map<String, User> extractExistingUsers(List<Map> existingUserList) {
    Map<String, User> existingUsers = new HashMap<>();
    for (int i = 0; i < existingUserList.size(); i++) {
      Map existingUser = existingUserList.get(i);
      JsonObject user = JsonObject.mapFrom(existingUser);
      User mappedUser = user.mapTo(User.class);
      LOGGER.info("The external system id of the user is: " + mappedUser.getExternalSystemId());
      existingUsers.put(mappedUser.getExternalSystemId(), mappedUser);
    }

    return existingUsers;
  }

  private void updateUserData(User user, Map<String, String> patronGroups, Map<String, String> addressTypes,
    String sourceType) {
    if (!Strings.isNullOrEmpty(sourceType)) {
      user.setExternalSystemId(sourceType + "_" + user.getExternalSystemId());
    }
    if (user.getPatronGroup() != null && patronGroups.containsKey(user.getPatronGroup())) {
      user.setPatronGroup(patronGroups.get(user.getPatronGroup()));
    }
    if (user.getPersonal() != null) {
      if (user.getPersonal().getAddresses() != null
        && !user.getPersonal().getAddresses().isEmpty()) {
        for (Address address : user.getPersonal().getAddresses()) {
          if (address.getAddressTypeId() != null && addressTypes.containsKey(address.getAddressTypeId())) {
            address.setAddressTypeId(addressTypes.get(address.getAddressTypeId()));
          }
        }
      }
      if (user.getPersonal().getPreferredContactTypeId() != null
        && preferredContactTypeIds.containsKey(user.getPersonal().getPreferredContactTypeId().toLowerCase())) {
        user.getPersonal()
          .setPreferredContactTypeId(
            preferredContactTypeIds.get(user.getPersonal().getPreferredContactTypeId().toLowerCase()));
      }
    }
  }

  //TODO: fix for deep copy and not just addresses.
  private User updateExistingUserWithIncomingFields(User user, User existingUser) {
    JsonObject current = JsonObject.mapFrom(user);
    JsonObject existing = JsonObject.mapFrom(existingUser);

    List<Address> addresses = null;

    existing.mergeIn(current);

    User response = existing.mapTo(User.class);

    if (existingUser.getPersonal() != null) {
      List<Address> currentAddresses = null;
      List<Address> existingAddresses = existingUser.getPersonal().getAddresses();
      if (user.getPersonal() != null) {
        currentAddresses = user.getPersonal().getAddresses();
      }
      if (currentAddresses == null) {
        addresses = existingAddresses;
      } else {
        Map<String, Address> currentAddressMap = new HashMap<>();
        Map<String, Address> existingAddressMap = new HashMap<>();
        currentAddresses.stream().forEach(object -> {
          currentAddressMap.put(((Address) object).getAddressTypeId(), (Address) object);
        });
        existingAddresses.stream().forEach(object -> {
          existingAddressMap.put(((Address) object).getAddressTypeId(), (Address) object);
        });

        existingAddressMap.putAll(currentAddressMap);
        addresses = new ArrayList<>();
        for (Address address : existingAddressMap.values()) {
          addresses.add(address);
        }
      }
    }

    if (addresses != null) {
      response.getPersonal().setAddresses(addresses);
    }

    return response;
  }

  private Future<List<Map>> listAllUsersWithExternalSystemId(Map<String, String> okapiHeaders, Context vertxContext,
    String prefix) {
    Future<List<Map>> future = Future.future();

    List<Map> existingUserList = new ArrayList<>();

    final String url = createUrl(prefix);

    int limit = 5;

    String okapiURL = getOkapiUrl(okapiHeaders);

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "application/json");

    HttpClientInterface userSearchClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));
    userSearchClient.setDefaultHeaders(headers);

    try {

      final String userSearchQuery = UriBuilder.fromPath("/users?query=" + url + "&limit=" + limit + "&offset=" + 0 + "&orderBy=externalSystemId&order=asc").build().toString();
      userSearchClient.request(HttpMethod.GET, userSearchQuery, headers)
        .whenComplete((response, ex) -> {

          if (!org.folio.rest.tools.client.Response.isSuccess(response.getCode())) {
            LOGGER.warn("Failed to process user search result.");
            future.fail(response.getError().toString());
          } else {

            try {
              List<Future> futures = new ArrayList<>();

              JsonObject resultObject = response.getBody();
              JsonArray array = resultObject.getJsonArray("users");
              List<Map> users = array.getList();
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
                    LOGGER.warn("Failed to process users.");
                    future.fail("Failed to process users.");
                  }
                });
              } else {
                future.complete(existingUserList);
              }
            } catch (Exception e) {
              LOGGER.warn("Failed to process users.", e.getMessage());
              future.fail(e);
            }
          }

        });
    } catch (Exception exc) {
      LOGGER.warn("Failed to process users.", exc.getMessage());
      future.fail(exc);
    }
    return future;
  }

  private Future processResponse(List<Map> existingUserList, HttpClientInterface userSearchClient, Map<String, String> okapiHeaders, String url, int limit, int offset) {
    Future future = Future.future();

    try {
      userSearchClient.request(HttpMethod.GET, "/users?query=" + url + "&limit=" + limit + "&offset=" + offset + "&orderBy=externalSystemId&order=asc", okapiHeaders)
        .whenComplete((subResponse, subEx) -> {

          if (!org.folio.rest.tools.client.Response.isSuccess(subResponse.getCode())) {
            LOGGER.warn("Failed to process user search response.");
            future.fail(subResponse.getError().toString());
          } else {
            try {
              List<Map> users = subResponse.getBody().getJsonArray("users").getList();
              existingUserList.addAll(users);
              future.complete();
            } catch (Exception e) {
              LOGGER.warn("Failed to process user search response.", e.getMessage());
              future.fail(e);
            }
          }

        });

    } catch (Exception exc) {
      LOGGER.warn("Failed to process user search response.", exc.getMessage());
      future.fail(exc);
    }

    return future;
  }

  private String createUrl(String prefix) {
    String url;
    if (!Strings.isNullOrEmpty(prefix)) {
      url = "externalSystemId = ^" + prefix + "_*";
    } else {
      url = "externalSystemId <> ''";
    }
    try {
      url = URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException exc) {
      LOGGER.warn("Could not encode request URL.");
    }
    return url;
  }

  private HttpClientInterface assembleUserSearchRequest(HttpClient client, Map<String, String> okapiHeaders, String url, int limit, int offset) {
    String okapiURL = getOkapiUrl(okapiHeaders);

    Map<String, String> headers = new HashMap<>();
    headers.put(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER));
    headers.put("Accept", "application/json");

    HttpClientInterface userSearchClient = HttpClientFactory.getHttpClient(okapiURL, okapiHeaders.get(OKAPI_TENANT_HEADER));
    userSearchClient.setDefaultHeaders(headers);

    return userSearchClient;
  }

  private Future<Void> deactivateUsers(Map<String, String> okapiHeaders, Context vertxContext,
    Map<String, User> existingUserMap) {
    Future<Void> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : existingUserMap.values()) {
      if (user.getActive()) {
        user.setActive(Boolean.FALSE);
        Future<Object> userDeactivateAsyncResult =
          updateUser(okapiHeaders, vertxContext, user);
        futures.add(userDeactivateAsyncResult);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        LOGGER.warn("Failed to deactivate users.");
        future.fail("Failed to deactivate users!");
      }
    });

    return future;
  }

  private String getOkapiUrl(Map<String, String> okapiHeaders) {
    return okapiHeaders.get(OKAPI_URL_HEADER);
  }

}
