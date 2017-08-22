package org.folio.rest.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataCollection;
import org.folio.rest.jaxrs.resource.UserImportResource;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class UserImportAPI implements UserImportResource {

  public static String OKAPI_URL_HEADER = "X-Okapi-URL";
  public static String OKAPI_TOKEN_HEADER = "X-Okapi-Token";
  public static String OKAPI_TENANT_HEADER = "X-Okapi-Tenant";
  public static String OKAPI_PERMISSIONS_HEADER = "X-Okapi-Permissions";

  private final Logger logger = LoggerFactory.getLogger(UserImportAPI.class);

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
      .handle(Future.succeededFuture(GetUserImportResponse.withPlainOK("OK")));
  }

  @Override
  public void postUserImport(UserdataCollection userCollection, RoutingContext routingContext,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {
    if (userCollection.getTotalRecords() == 0) {
      asyncResultHandler
        .handle(Future.succeededFuture(PostUserImportResponse.withPlainOK("OK")));
    } else {
      Future<JsonArray> addressTypeAsyncRequest = getAddressTypes(okapiHeaders, vertxContext);
      addressTypeAsyncRequest.setHandler(res -> {
        if (res.succeeded()) {
          Map<String, String> addressTypes = extractAddressTypes(res.result());
          Future<JsonArray> patronGroupsAsyncRequest = getPatronGroups(okapiHeaders, vertxContext);
          patronGroupsAsyncRequest.setHandler(patronGroupResponse -> {
            if (patronGroupResponse.succeeded()) {

              Map<String, String> patronGroups = extractPatronGroups(patronGroupResponse.result());

              Boolean updateOnlyPresentData = userCollection.getUpdateOnlyPresentFields();

              if (userCollection.getDeactivateMissingUsers() != null && userCollection.getDeactivateMissingUsers()) {

                Future<JsonObject> usersResponse = listAllUsersWithExternalSystemId(okapiHeaders, vertxContext);

                usersResponse.setHandler(handler -> {

                  if (handler.failed()) {
                    asyncResultHandler
                      .handle(
                        Future.succeededFuture(PostUserImportResponse
                          .withPlainBadRequest("Failed to import users. Function not yet implemented.")));
                  } else {
                    logger.info("response: " + handler.result());
                    JsonArray existingUsers = handler.result().getJsonArray("users");
                    Map<String, User> existingUserMap = extractExistingUsers(existingUsers);

                    List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);

                    List<Future> futures = new ArrayList<>();

                    for (List<User> currentPartition : userPartitions) {
                      Future<JsonObject> userSearchAsyncResult =
                        processUserSearchResult(okapiHeaders, vertxContext, existingUserMap,
                          currentPartition, patronGroups, addressTypes, updateOnlyPresentData);
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

              } else {

                List<List<User>> userPartitions = Lists.partition(userCollection.getUsers(), 10);

                List<Future> futures = new ArrayList<>();

                for (List<User> currentPartition : userPartitions) {
                  Future<String> userBatchProcessResponse =
                    processUserBatch(okapiHeaders, vertxContext, currentPartition, patronGroups, addressTypes,
                      updateOnlyPresentData);
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

            } else {
              asyncResultHandler
                .handle(
                  Future.succeededFuture(PostUserImportResponse.withPlainBadRequest("Not OK for the patron groups")));
            }
          });

        } else {
          asyncResultHandler
            .handle(Future.succeededFuture(PostUserImportResponse.withPlainBadRequest("Not OK for address types")));
        }
      });
    }
  }

  private Map<String, String> extractAddressTypes(JsonArray addressTypes) {
    Map<String, String> addressTypeMap = new HashMap<>();
    for (int i = 0; i < addressTypes.size(); i++) {
      JsonObject addressType = addressTypes.getJsonObject(i);
      addressTypeMap.put(addressType.getString("addressType"), addressType.getString("id"));
    }
    return addressTypeMap;
  }

  private Map<String, String> extractPatronGroups(JsonArray patronGroups) {
    Map<String, String> patronGroupMap = new HashMap<>();
    for (int i = 0; i < patronGroups.size(); i++) {
      JsonObject patronGroup = patronGroups.getJsonObject(i);
      patronGroupMap.put(patronGroup.getString("group"), patronGroup.getString("id"));
    }
    return patronGroupMap;
  }

  private Future<JsonArray> getAddressTypes(Map<String, String> okapiHeaders, Context vertxContext) {
    Future<JsonArray> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.getAbs(okapiURL + "/addresstypes");
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Content-type", "application/json")
      .putHeader("Accept", "application/json");
    request.handler(res -> {
      res.bodyHandler(buf -> {
        if (res.statusCode() != 200) {
          future.fail("Response is: " + buf + " " + res.statusCode());
        } else {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject.getJsonArray("addressTypes"));
          } catch (Exception e) {
            future.fail(e);
          }
        }
      });
    });
    request.end();
    return future;
  }

  private Future<JsonArray> getPatronGroups(Map<String, String> okapiHeaders, Context vertxContext) {
    Future<JsonArray> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.getAbs(okapiURL + "/groups");
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Accept", "application/json");
    request.handler(res -> {
      res.bodyHandler(buf -> {
        if (res.statusCode() != 200) {
          future.fail("Response is: " + buf + " " + res.statusCode());
        } else {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject.getJsonArray("usergroups"));
          } catch (Exception e) {
            future.fail(e);
          }
        }
      });
    });
    request.end();
    return future;
  }

  private Future<JsonObject> listUsers(Map<String, String> okapiHeaders, Context vertxContext, List<User> users) {
    Future<JsonObject> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);

    StringBuilder userQueryBuilder = new StringBuilder("(");

    for (int i = 0; i < users.size(); i++) {
      userQueryBuilder.append("externalSystemId==\"").append(users.get(i).getExternalSystemId() + "\"");
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
      logger.warn("Could not encode request URL.");
    }

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.getAbs(okapiURL + "/users?query=" + url);
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Accept", "application/json");
    request.handler(res -> {
      res.bodyHandler(buf -> {
        if (res.statusCode() != 200) {
          future.fail("Response is: " + buf + " " + res.statusCode());
        } else {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject);
          } catch (Exception e) {
            future.fail(e);
          }
        }
      });
    });
    request.end();
    return future;
  }

  private Future<JsonObject> createNewUser(Map<String, String> okapiHeaders, Context vertxContext, User user) {
    Future<JsonObject> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);

    user.setId(UUID.randomUUID().toString());

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.postAbs(okapiURL + "/users", res -> {
      res.bodyHandler(buf -> {
        if (res.statusCode() != 201) {
          future.fail("Response is: " + buf + " " + res.statusCode());
        } else {
          try {
            Future<JsonObject> permissionsFuture = addEmptyPermissionSetForUser(okapiHeaders, vertxContext, user);
            permissionsFuture.setHandler(futurePermissionHandler -> {
              if (futurePermissionHandler.failed()) {
                logger.error("Failed to register permissions for user.");
              }
              future.complete(futurePermissionHandler.result());
            });
          } catch (Exception e) {
            future.fail(e);
          }
        }
      });
    });
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Content-type", "application/json")
      .putHeader("Accept", "application/json");
    request.end(JsonObject.mapFrom(user).encode());
    return future;
  }

  private Future<Buffer> updateUser(Map<String, String> okapiHeaders, Context vertxContext, User user) {
    Future<Buffer> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.putAbs(okapiURL + "/users/" + user.getId(), res -> {
      res.bodyHandler(buf -> {
        if (res.statusCode() != 204) {
          future.fail("Response is: " + buf + " " + res.statusCode());
        } else {
          future.complete(buf);
        }
      });
    });
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Content-type", "application/json")
      .putHeader("Accept", "text/plain");
    request.end(JsonObject.mapFrom(user).encode());
    return future;
  }

  private Future<JsonObject> addEmptyPermissionSetForUser(Map<String, String> okapiHeaders, Context vertxContext,
    User user) {
    Future<JsonObject> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.postAbs(okapiURL + "/perms/users");
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Content-type", "application/json")
      .putHeader("Accept", "application/json").setRawMethod("PUT");
    request.handler(res -> {
      res.bodyHandler(buf -> {
        if (res.statusCode() != 201) {
          future.fail("Response is: " + buf + " " + res.statusCode());
        } else {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject);
          } catch (Exception e) {
            future.fail(e);
          }
        }
      });
    });
    request.end("{\"username\": \"" + user.getUsername() + "\", \"permissions\": []}");
    return future;
  }

  private Future<JsonObject> processUserSearchResult(Map<String, String> okapiHeaders, Context vertxContext,
    Map<String, User> existingUsers, List<User> usersToImport, Map<String, String> patronGroups,
    Map<String, String> addressTypes, boolean updateOnlyPresentData) {
    Future<JsonObject> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : usersToImport) {
      //TODO log errors
      //TODO create statistics from number of created/updated + failed users
      updateUserData(user, patronGroups, addressTypes);
      if (existingUsers.containsKey(user.getExternalSystemId())) {
        if (updateOnlyPresentData) {
          user = updateExistingUserWithIncomingFields(user, existingUsers.get(user.getExternalSystemId()));
        } else {
          user.setId(existingUsers.get(user.getExternalSystemId()).getId());
        }
        Future<Buffer> userUpdateResponse = updateUser(okapiHeaders, vertxContext, user);
        futures.add(userUpdateResponse);
        existingUsers.remove(user.getExternalSystemId());
      } else {
        Future<JsonObject> userCreationResponse = createNewUser(okapiHeaders, vertxContext, user);
        futures.add(userCreationResponse);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        future.fail("Failed to import users.");
      }
    });

    return future;
  }

  private Future<String> processUserBatch(Map<String, String> okapiHeaders, Context vertxContext,
    List<User> currentPartition, Map<String, String> patronGroups, Map<String, String> addressTypes,
    boolean updateOnlyPresentData) {
    Future<String> processFuture = Future.future();
    Future<JsonObject> userSearchResponse = listUsers(okapiHeaders, vertxContext, currentPartition);
    userSearchResponse.setHandler(userSearchAsyncResponse -> {
      if (userSearchAsyncResponse.succeeded()) {

        Map<String, User> existingUsers = extractExistingUsers(userSearchAsyncResponse.result().getJsonArray("users"));

        Future<JsonObject> userSearchAsyncResult =
          processUserSearchResult(okapiHeaders, vertxContext, existingUsers,
            currentPartition, patronGroups, addressTypes, updateOnlyPresentData);
        userSearchAsyncResult.setHandler(response -> {
          if (response.succeeded()) {
            processFuture.complete();
          } else {
            processFuture.fail("Not OK for user processing");
          }
        });

      } else {
        processFuture.fail("Not OK for user search.");
      }
    });
    return processFuture;
  }

  private Map<String, User> extractExistingUsers(JsonArray existingUserList) {
    Map<String, User> existingUsers = new HashMap<>();
    for (int i = 0; i < existingUserList.size(); i++) {
      JsonObject existingUser = existingUserList.getJsonObject(i);
      User mappedUser = existingUser.mapTo(User.class);
      existingUsers.put(mappedUser.getExternalSystemId(), mappedUser);
    }

    return existingUsers;
  }

  private void updateUserData(User user, Map<String, String> patronGroups, Map<String, String> addressTypes) {
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

  private Future<JsonObject> listAllUsersWithExternalSystemId(Map<String, String> okapiHeaders, Context vertxContext) {
    Future<JsonObject> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);
    String url = "externalSystemId <> ''";
    try {
      url = URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException exc) {
      logger.warn("Could not encode request URL.");
    }

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.getAbs(okapiURL + "/users?query=" + url);
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Accept", "application/json");
    request.handler(res -> {
      res.bodyHandler(buf -> {
        if (res.statusCode() != 200) {
          future.fail("Response is: " + buf + " " + res.statusCode());
        } else {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject);
          } catch (Exception e) {
            future.fail(e);
          }
        }
      });
    });
    request.end();
    return future;
  }

  private Future<Void> deactivateUsers(Map<String, String> okapiHeaders, Context vertxContext,
    Map<String, User> existingUserMap) {
    Future<Void> future = Future.future();

    List<Future> futures = new ArrayList<>();

    for (User user : existingUserMap.values()) {
      if (user.getActive()) {
        user.setActive(Boolean.FALSE);
        Future<Buffer> userDeactivateAsyncResult =
          updateUser(okapiHeaders, vertxContext, user);
        futures.add(userDeactivateAsyncResult);
      }
    }

    CompositeFuture.all(futures).setHandler(ar -> {
      if (ar.succeeded()) {
        future.complete();
      } else {
        future.fail("Failed to deactivate users!");
      }
    });

    return future;
  }

}
