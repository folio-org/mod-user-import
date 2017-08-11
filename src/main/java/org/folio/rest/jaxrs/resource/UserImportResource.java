
package org.folio.rest.jaxrs.resource;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.UserdataCollection;
import org.folio.rest.jaxrs.resource.support.ResponseWrapper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Path("user-import")
public interface UserImportResource {

  /**
   * Create or update a list of users
   * 
   * @param vertxContext
   *      The Vertx Context Object <code>io.vertx.core.Context</code> 
   * @param asyncResultHandler
   *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
   * @param routingContext
   *     RoutingContext of the request. Note that the RMB framework handles all routing.This should only be used if a third party add-on to vertx needs the RC as input 
   * @param entity
   *     
   */
  @POST
  @Consumes("application/json")
  @Produces({
      "text/plain"
  })
  @Validate
  void postUserImport(UserdataCollection entity, RoutingContext routingContext, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)
    throws Exception;

  public class PostUserImportResponse
    extends ResponseWrapper {

    private PostUserImportResponse(Response delegate) {
      super(delegate);
    }

    /**
     * Return OK
     * 
     * @param entity
     *     
     */
    public static UserImportResource.PostUserImportResponse withPlainOK(String entity) {
      Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "text/plain");
      responseBuilder.entity(entity);
      return new UserImportResource.PostUserImportResponse(responseBuilder.build());
    }

    /**
     * Bad request, possibly error in user data
     * 
     * @param entity
     *     
     */
    public static UserImportResource.PostUserImportResponse withPlainBadRequest(String entity) {
      Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
      responseBuilder.entity(entity);
      return new UserImportResource.PostUserImportResponse(responseBuilder.build());
    }

    /**
     * Internal server error
     * 
     * @param entity
     *     
     */
    public static UserImportResource.PostUserImportResponse withPlainInternalServerError(String entity) {
      Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
      responseBuilder.entity(entity);
      return new UserImportResource.PostUserImportResponse(responseBuilder.build());
    }

  }

}
