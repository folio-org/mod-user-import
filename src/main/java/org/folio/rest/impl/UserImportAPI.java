package org.folio.rest.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataCollection;
import org.folio.rest.jaxrs.resource.UserImportResource;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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

  @Override
  public void postUserImport(UserdataCollection entity, RoutingContext routingContext, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) throws Exception {
    if (entity.getTotalRecords() == 0) {
      asyncResultHandler
        .handle(Future.succeededFuture(PostUserImportResponse.withPlainOK("OK")));
    } else {
      Future<JsonArray> addressTypeAsyncRequest = getAddressTypes(okapiHeaders, vertxContext);
      addressTypeAsyncRequest.setHandler(res -> {
        if (res.succeeded()) {
          Future<JsonArray> patronGroupsAsyncRequest = getPatronGroups(okapiHeaders, vertxContext);
          patronGroupsAsyncRequest.setHandler(patronGroupResponse -> {
            if (patronGroupResponse.succeeded()) {

              //TODO: option to check all users for existence + deactivate non-existing users present in FOLIO
              List<List<User>> userPartitions = Lists.partition(entity.getUsers(), 10);

              //TODO: callback
              for (List<User> currentPartition : userPartitions) {
                Future<String> userBatchProcessResponse =
                  processUserBatch(okapiHeaders, vertxContext, currentPartition);
                userBatchProcessResponse.setHandler(batchProcResp -> {
                  if (batchProcResp.failed()) {
                    asyncResultHandler
                      .handle(
                        Future.succeededFuture(PostUserImportResponse.withPlainBadRequest(batchProcResp.result())));
                  }
                });
              }

              //TODO: if all responses were successful:
              asyncResultHandler.handle(
                Future.succeededFuture(PostUserImportResponse.withPlainOK("OK")));

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
      if (res.statusCode() != 200) {
        future.fail("Got status code " + res.statusCode());
      } else {
        res.bodyHandler(buf -> {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject.getJsonArray("addressTypes"));
          } catch (Exception e) {
            future.fail(e);
          }
        });
      }
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
      if (res.statusCode() != 200) {
        future.fail("Got status code " + res.statusCode());
      } else {
        res.bodyHandler(buf -> {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject.getJsonArray("usergroups"));
          } catch (Exception e) {
            future.fail(e);
          }
        });
      }
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
      if (res.statusCode() != 200) {
        future.fail("Got status code " + res.statusCode());
      } else {
        res.bodyHandler(buf -> {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject);
          } catch (Exception e) {
            future.fail(e);
          }
        });
      }
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
    HttpClientRequest request = client.getAbs(okapiURL + "/users");
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Content-type", "application/json")
      .putHeader("Accept", "application/json").setRawMethod("POST");
    request.handler(res -> {
      if (res.statusCode() != 200) {
        future.fail("Got status code " + res.statusCode());
      } else {
        res.bodyHandler(buf -> {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject);
          } catch (Exception e) {
            future.fail(e);
          }
        });
      }
    });
    request.end(user.toString());
    return future;
  }

  private Future<JsonObject> updateUser(Map<String, String> okapiHeaders, Context vertxContext, User user) {
    Future<JsonObject> future = Future.future();

    String okapiURL = okapiHeaders.get(OKAPI_URL_HEADER);

    HttpClientOptions options = new HttpClientOptions();
    options.setConnectTimeout(10);
    options.setIdleTimeout(10);
    HttpClient client = vertxContext.owner().createHttpClient(options);
    HttpClientRequest request = client.getAbs(okapiURL + "/users/" + user.getId());
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Content-type", "application/json")
      .putHeader("Accept", "application/json").setRawMethod("PUT");
    request.handler(res -> {
      if (res.statusCode() != 200) {
        future.fail("Got status code " + res.statusCode());
      } else {
        res.bodyHandler(buf -> {
          try {
            JsonObject resultObject = buf.toJsonObject();
            Future<JsonObject> permissionsFuture = addEmptyPermissionSetForUser(okapiHeaders, vertxContext, user);
            permissionsFuture.setHandler(futurePermissionHandler -> {
              if (futurePermissionHandler.failed()) {
                logger.error("Failed to register permissions for user.");
              }
            });
            future.complete(resultObject);
          } catch (Exception e) {
            future.fail(e);
          }
        });
      }
    });
    request.end(user.toString());
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
    HttpClientRequest request = client.getAbs(okapiURL + "/perms/users");
    request.putHeader(OKAPI_TENANT_HEADER, okapiHeaders.get(OKAPI_TENANT_HEADER))
      .putHeader(OKAPI_TOKEN_HEADER, okapiHeaders.get(OKAPI_TOKEN_HEADER))
      .putHeader("Content-type", "application/json")
      .putHeader("Accept", "application/json").setRawMethod("PUT");
    request.handler(res -> {
      if (res.statusCode() != 200) {
        future.fail("Got status code " + res.statusCode());
      } else {
        res.bodyHandler(buf -> {
          try {
            JsonObject resultObject = buf.toJsonObject();
            future.complete(resultObject);
          } catch (Exception e) {
            future.fail(e);
          }
        });
      }
    });
    request.end("{\"username\": \"" + user.getUsername() + "\", \"permissions\": []}");
    return future;
  }

  private Future<JsonObject> processUserSearchResult(Map<String, String> okapiHeaders, Context vertxContext,
    JsonArray existingUserList, List<User> usersToImport) {
    Future<JsonObject> future = Future.future();

    List<String> externalIds = extractExistingIds(existingUserList);

    for (User user : usersToImport) {
      //TODO handle responses
      //TODO log errors
      //TODO create statistics from number of created/updated + failed users
      if (externalIds.contains(user.getExternalSystemId())) {
        updateUser(okapiHeaders, vertxContext, user);
      } else {
        createNewUser(okapiHeaders, vertxContext, user);
      }
    }

    future.complete();
    return future;
  }

  private Future<String> processUserBatch(Map<String, String> okapiHeaders, Context vertxContext,
    List<User> currentPartition) {
    Future<String> processFuture = Future.future();
    Future<JsonObject> userSearchResponse = listUsers(okapiHeaders, vertxContext, currentPartition);
    userSearchResponse.setHandler(userSearchAsyncResponse -> {
      if (userSearchAsyncResponse.succeeded()) {

        Future<JsonObject> userSearchAsyncResult =
          processUserSearchResult(okapiHeaders, vertxContext, userSearchAsyncResponse.result().getJsonArray("users"),
            currentPartition);
        if (userSearchAsyncResult.succeeded()) {

        } else {
          processFuture.fail("Not OK for user processing");
        }
      } else {
        processFuture.fail("Not OK for user search.");
      }
    });
    return processFuture;
  }

  private List<String> extractExistingIds(JsonArray existingUserList) {
    List<String> externalIds = new ArrayList<>();

    List existingUsers = existingUserList.getList();
    if (existingUsers != null && !existingUsers.isEmpty() && existingUsers.get(0) instanceof JsonObject) {
      List<JsonObject> userList = (List<JsonObject>) existingUserList.getList();
      for (JsonObject user : userList) {
        externalIds.add(user.getString("externalSystemId"));
      }
    }

    return externalIds;
  }

}
