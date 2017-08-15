
package org.folio.rest.jaxrs.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import io.vertx.core.Context;
import io.vertx.ext.web.RoutingContext;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.UserdataCollection;

@Path("user-import")
public interface UserImportResource {


    /**
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param routingContext
     *     RoutingContext of the request. Note that the RMB framework handles all routing.This should only be used if a third party add-on to vertx needs the RC as input 
     */
    @GET
    @Produces({
        "text/plain"
    })
    @Validate
    void getUserImport(RoutingContext routingContext, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

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
    void postUserImport(UserdataCollection entity, RoutingContext routingContext, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    public class GetUserImportResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetUserImportResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Fake endpoint
         * 
         * @param entity
         *     
         */
        public static UserImportResource.GetUserImportResponse withPlainOK(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new UserImportResource.GetUserImportResponse(responseBuilder.build());
        }

    }

    public class PostUserImportResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


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
