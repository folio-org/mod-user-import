package org.folio.util;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.jaxrs.model.Department;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(VertxUnitRunner.class)
public class HttpClientUtilTest {

  @Test
  public void postDoubleException(TestContext context) {
    String msg = null;
    try {
      HttpClientUtil.post(new HashMap<>(), "/a", Integer.class, "{}", "myFail");
    } catch (Exception ex) {
      msg = ex.getMessage();
    }
    context.assertTrue(msg.startsWith("Cannot construct instance"), msg);
  }

  @Test
  public void postNormalException(TestContext context) {
    HttpClientUtil.post(new HashMap<>(), "/a", Integer.class, null, "myFail")
        .onComplete(context.asyncAssertFailure(
            cause -> context.assertEquals("Entity can not be null", cause.getMessage())));
  }

  @Test
  public void postInvalidUrl(TestContext context) {
      HttpClientUtil.post(new HashMap<>(), "/a", Department.class, new Department(), "myFail")
          .onComplete(context.asyncAssertFailure());
  }
}
