package org.folio.util;

import io.vertx.core.Future;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class HttpClientUtilTest {

  @Test
  public void errorManagement() {
    Future<Void> f = HttpClientUtil.errorManagement(new RuntimeException("x"), "lead");
    Assert.assertTrue(f.failed());
    Assert.assertEquals("lead: x", f.cause().getMessage());
  }
}
