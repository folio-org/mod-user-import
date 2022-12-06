package org.folio.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Set;
import org.folio.rest.jaxrs.model.User;
import org.junit.Test;

public class UserDataProcessingServiceTest {
  @Test
  public void mergeUserDefault() {
    assertMergeUser(new User(), new User(), new User());
  }

  @Test
  public void mergeUserActive() {
    assertMergeUser(
        new User().withActive(true),
        new User().withActive(false),
        new User().withActive(true));
  }

  @Test
  public void mergeUserBarcodeIdPatronGroup() {
    assertMergeUser(
        new User().withBarcode("123").withPatronGroup("staff"),
        new User().withBarcode("456").withId("foo"),
        new User().withBarcode("123").withId("foo").withPatronGroup("staff"));
  }

  @Test
  public void mergeUserArraySet1() {
    assertMergeUser(
        new User(),
        new User().withProxyFor(List.of("x", "y")).withDepartments(Set.of("d1", "d2")),
        new User());
  }

  @Test
  public void mergeUserArraySet2() {
    assertMergeUser(
        new User().withProxyFor(List.of("x", "y")).withDepartments(Set.of("d1", "d2")),
        new User(),
        new User().withProxyFor(List.of("x", "y")).withDepartments(Set.of("d1", "d2")));
  }

  @Test
  public void mergeUserArraySet3() {
    assertMergeUser(
        new User().withProxyFor(List.of("x", "y")).withDepartments(Set.of("d1", "d2")),
        new User().withProxyFor(List.of("a", "b")).withDepartments(Set.of("d2", "d3")),
        new User().withProxyFor(List.of("x", "y")).withDepartments(Set.of("d1", "d2")));
  }

  private static void assertMergeUser(User user, User existingUser, User expectedUser) {
    var actual = JsonObject.mapFrom(UserDataProcessingService.updateExistingUserWithIncomingFields(user, existingUser));
    var expected = JsonObject.mapFrom(expectedUser);
    assertThat(actual, is(expected));
  }
}
