package org.folio.rest.validator;

import static io.vertx.ext.web.client.predicate.ResponsePredicate.create;

import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

/**
 * The same as {@link ResponsePredicate} but with response body included in the Exception message.
 */
public interface ChattyResponsePredicate {
  ErrorConverter errorConverter = ErrorConverter.createFullBody(result -> {
    String message = result.message() + " - " + result.response().bodyAsString();
    return new NoStackTraceThrowable(message);
  });

  /**
   * 200 OK
   */
  ResponsePredicate SC_OK = create(ResponsePredicate.SC_OK, errorConverter);

  /**
   * 201 Created
   */
  ResponsePredicate SC_CREATED = create(ResponsePredicate.SC_CREATED, errorConverter);

  /**
   * 204 No Content
   */
  ResponsePredicate SC_NO_CONTENT = create(ResponsePredicate.SC_NO_CONTENT, errorConverter);
}
