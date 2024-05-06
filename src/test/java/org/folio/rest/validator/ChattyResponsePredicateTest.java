package org.folio.rest.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
class ChattyResponsePredicateTest {

  static Stream<Arguments> predicate() {
    return Stream.of(
        arguments(ChattyResponsePredicate.SC_OK, 200),
        arguments(ChattyResponsePredicate.SC_CREATED, 201),
        arguments(ChattyResponsePredicate.SC_NO_CONTENT, 204)
        );
  }

  @ParameterizedTest
  @MethodSource
  void predicate(ResponsePredicate predicate, Integer status, Vertx vertx, VertxTestContext vtc) {
    vertx.createHttpServer()
    .requestHandler(request -> request.response().setStatusCode(555).end("Body and Soul"))
    .listen(0)
    .compose(httpServer -> WebClient.create(vertx)
        .post(httpServer.actualPort(), "localhost", "/")
        .expect(predicate)
        .send())
    .onComplete(vtc.failing(e -> {
      var expected = "Response status code 555 is not equal to " + status + " - Body and Soul";
      assertThat(e.getMessage(), is(expected));
      vtc.completeNow();
    }));
  }

}
