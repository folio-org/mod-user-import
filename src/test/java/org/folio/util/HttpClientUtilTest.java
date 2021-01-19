package org.folio.util;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.Department;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(VertxUnitRunner.class)
public class HttpClientUtilTest {

  @Test
  public void getException(TestContext context) {
    HttpClientUtil.get(null, "/a", "fail")
        .onComplete(context.asyncAssertFailure(cause ->
            context.assertNull(cause.getMessage(), cause.getMessage())));
  }

  @Test
  public void getError(TestContext context) {
    Map<String, String> headers = new HashMap<>();
    headers.put(XOkapiHeaders.URL, "http://localhost:9230");
    headers.put(XOkapiHeaders.TENANT, "tenant");
    HttpClientUtil.get(headers, "/a", "fail")
        .onComplete(context.asyncAssertFailure(cause ->
            context.assertTrue(cause.getMessage().startsWith("fail"), cause.getMessage())));
  }

  @Test
  public void deleteException(TestContext context) {
    HttpClientUtil.delete(null, "/a", "1234", "fail")
        .onComplete(context.asyncAssertFailure(cause ->
            context.assertNull(cause.getMessage(), cause.getMessage())));
  }

  @Test
  public void postPutException(TestContext context) {
    HttpClientUtil.put(null, "/a", null, "fail")
        .onComplete(context.asyncAssertFailure(cause ->
            context.assertNull(cause.getMessage(), cause.getMessage())));
  }

  @Test
  public void postBadEntityException(TestContext context) {
    HttpClientUtil.post(new HashMap<>(), "/a", Integer.class, "{}", "myFail")
        .onComplete(context.asyncAssertFailure(cause ->
            context.assertTrue(cause.getMessage().startsWith("Cannot construct instance of"), cause.getMessage())
        ));
  }

  @Test
  public void postNullEntityException(TestContext context) {
    HttpClientUtil.post(new HashMap<>(), "/a", Integer.class, null, "myFail")
        .onComplete(context.asyncAssertFailure()); // mock and real http client returns different messages!
  }

  @Test
  public void postInvalidUrl(TestContext context) {
      HttpClientUtil.post(new HashMap<>(), "/a", Department.class, new Department(), "myFail")
          .onComplete(context.asyncAssertFailure()); // mock and real http client returns different messages!
  }
}
