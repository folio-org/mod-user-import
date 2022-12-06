package org.folio.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
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
}
