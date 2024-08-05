package org.folio.rest.validator;

import static io.vertx.ext.web.client.predicate.ResponsePredicate.create;

import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

/**
 * The same as {@link ResponsePredicate} but with response body included in the Exception message.
 */
public final class ChattyResponsePredicate {
  private static final ErrorConverter CONVERTER = ErrorConverter.createFullBody(result -> {
    String message = result.message() + " - " + result.response().bodyAsString();
    return new NoStackTraceThrowable(message);
  });

  /**
   * 200 OK
   */
  public static final ResponsePredicate SC_OK = create(ResponsePredicate.SC_OK, CONVERTER);

  /**
   * 201 Created
   */
  public static final ResponsePredicate SC_CREATED = create(ResponsePredicate.SC_CREATED, CONVERTER);

  /**
   * 204 No Content
   */
  public static final ResponsePredicate SC_NO_CONTENT = create(ResponsePredicate.SC_NO_CONTENT, CONVERTER);

  private ChattyResponsePredicate() {
  }
}
