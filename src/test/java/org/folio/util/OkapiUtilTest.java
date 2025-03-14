package org.folio.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.okapi.common.XOkapiHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.RunTestOnContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class OkapiUtilTest {

  static String okapiUrl;

  @RegisterExtension
  RunTestOnContext runTestOnContext = new RunTestOnContext();

  @BeforeAll
  static void setUp(Vertx vertx, VertxTestContext vtc) {
    vertx.createHttpServer()
    .requestHandler(request -> {
      switch (request.path() + "?" + request.query()) {
        case "/_/proxy/tenants/one/modules?provide=custom-fields":
          request.response().setStatusCode(200).end("""
                          [{"id": "mod-orders-storage-13.8.0"}, {"id": "mod-users-1.2.3"}]
                          """);
          break;
        case "/_/proxy/tenants/zero/modules?provide=custom-fields":
          request.response().setStatusCode(200).end("""
                          []
                          """);
          break;
        case "/_/proxy/tenants/two/modules?provide=custom-fields":
          request.response().setStatusCode(200).end("""
                          [{"id": "mod-users-0.0.1"}, {"id": "mod-users-0.0.2"}]
                          """);
          break;
        case "/_/proxy/tenants/diku/modules?provide=500":
          request.response().setStatusCode(500).end("big fail");
          break;
        default:
          request.response().setStatusCode(404).end();
      }

    })
    .listen(0)
    .onComplete(vtc.succeeding(httpServer -> {
      okapiUrl = "http://localhost:" + httpServer.actualPort();
      vtc.completeNow();
    }));
  }

  Future<String> setModuleIdForMultipleInterface(String tenantId, String interfaceName) {
    var headers = new CaseInsensitiveMap<>(Map.of(
        XOkapiHeaders.TENANT, tenantId,
        XOkapiHeaders.URL, okapiUrl));
    return OkapiUtil.setModuleIdForMultipleInterface(interfaceName, "mod-users", headers)
        .map(x -> headers.get(XOkapiHeaders.MODULE_ID));
  }

  @Test
  void one(VertxTestContext vtc) {
    setModuleIdForMultipleInterface("one", "custom-fields")
    .onComplete(vtc.succeeding(moduleId -> {
      assertThat(moduleId, is("mod-users-1.2.3"));
      vtc.completeNow();
    }));
  }

  @Test
  void zero(VertxTestContext vtc) {
    setModuleIdForMultipleInterface("zero", "custom-fields")
    .onComplete(vtc.failing(e -> {
      assertThat(e.getMessage(), containsString("No module found"));
      vtc.completeNow();
    }));
  }

  @Test
  void two(VertxTestContext vtc) {
    setModuleIdForMultipleInterface("two", "custom-fields")
    .onComplete(vtc.failing(e -> {
      assertThat(e.getMessage(), containsString("Multiple modules found: [mod-users-0.0.1, mod-users-0.0.2]"));
      vtc.completeNow();
    }));
  }

  @Test
  void status500(VertxTestContext vtc) {
    setModuleIdForMultipleInterface("diku", "500")
    .onComplete(vtc.failing(e -> {
      assertThat(e.getMessage(), containsString("Response status code 500"));
      assertThat(e.getMessage(), containsString("big fail"));
      vtc.completeNow();
    }));
  }

  @Test
  void status404(VertxTestContext vtc) {
    setModuleIdForMultipleInterface("diku", "404")
    .onComplete(vtc.failing(e -> {
      assertThat(e.getMessage(), containsString("Response status code 404"));
      vtc.completeNow();
    }));
  }

}
