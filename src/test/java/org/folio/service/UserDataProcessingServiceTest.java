package org.folio.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.folio.model.UserImportData;
import org.folio.model.UserSystemData;
import org.folio.model.exception.PatronGroupMappingFailedException;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;
import org.junit.Test;

public class UserDataProcessingServiceTest {
  private static final String staffUuid = "0b7593ac-da2c-4539-b99a-e45d41652b0d";
  private static final Map<String,String> patronGroups = Map.of("staff", staffUuid);
  private static final UserImportData userImportData = new UserImportData(new UserdataimportCollection())
      .withSystemData(UserSystemData.builder().patronGroups(patronGroups).build());

  @Test
  public void setPatronGroup() {
    var user = new User().withPatronGroup("staff");
    UserDataProcessingService.setPatronGroup(user, userImportData);
    assertThat(user.getPatronGroup(), is(staffUuid));
  }

  @Test
  public void setPatronGroupNull() {
    var user = new User();
    UserDataProcessingService.setPatronGroup(user, userImportData);
    assertThat(user.getPatronGroup(), is(nullValue()));
  }

  @Test(expected=PatronGroupMappingFailedException.class)
  public void setPatronGroupInvalid() {
    var user = new User().withPatronGroup("bugs");
    UserDataProcessingService.setPatronGroup(user, userImportData);
  }

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
