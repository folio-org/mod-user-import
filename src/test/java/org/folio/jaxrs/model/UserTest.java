package org.folio.jaxrs.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.junit.Test;

public class UserTest {

  @Test
  public void testUserCreation() {
    User user = createUser();

    User otherUser = new User()
      .withId(user.getId())
      .withBarcode(user.getBarcode())
      .withExternalSystemId(user.getExternalSystemId())
      .withUsername(user.getUsername())
      .withActive(user.getActive())
      .withPatronGroup(user.getPatronGroup());

    assertEquals(user.getId(), otherUser.getId());
    assertEquals(user.getBarcode(), otherUser.getBarcode());
    assertEquals(user.getExternalSystemId(), otherUser.getExternalSystemId());
    assertEquals(user.getUsername(), otherUser.getUsername());
    assertEquals(user.getActive(), otherUser.getActive());
    assertEquals(user.getPatronGroup(), otherUser.getPatronGroup());

  }

  @Test
  public void testPersonalCreation() {
    User user = createUser();
    Personal personal = createPersonal();
    user.setPersonal(personal);

    Personal otherPersonal = new Personal()
      .withFirstName(personal.getFirstName())
      .withLastName(personal.getLastName())
      .withEmail(personal.getEmail())
      .withPreferredContactTypeId(personal.getPreferredContactTypeId());

    assertEquals(user.getPersonal().getFirstName(), otherPersonal.getFirstName());
    assertEquals(user.getPersonal().getLastName(), otherPersonal.getLastName());
    assertEquals(user.getPersonal().getEmail(), otherPersonal.getEmail());
    assertEquals(user.getPersonal().getPreferredContactTypeId(), otherPersonal.getPreferredContactTypeId());
  }

  @Test
  public void testAddressCreation() {
    User user = createUser();
    Personal personal = createPersonal();

    Address address = new Address();
    address.setAddressLine1(UUID.randomUUID().toString());
    address.setAddressLine2(UUID.randomUUID().toString());
    address.setAddressTypeId(UUID.randomUUID().toString());
    address.setCity(UUID.randomUUID().toString());
    address.setCountryId(UUID.randomUUID().toString());
    address.setId(UUID.randomUUID().toString());
    address.setPostalCode(UUID.randomUUID().toString());
    address.setPrimaryAddress(true);
    address.setRegion(UUID.randomUUID().toString());
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);

    personal.setAddresses(addresses);
    user.setPersonal(personal);

    Address otherAddress = new Address()
      .withAddressLine1(address.getAddressLine1())
      .withAddressLine2(address.getAddressLine2())
      .withAddressTypeId(address.getAddressTypeId())
      .withCity(address.getCity())
      .withCountryId(address.getCountryId())
      .withId(address.getId())
      .withPostalCode(address.getPostalCode())
      .withPrimaryAddress(address.getPrimaryAddress())
      .withRegion(address.getRegion());

    assertEquals(user.getPersonal().getAddresses().get(0).getAddressLine1(), otherAddress.getAddressLine1());
    assertEquals(user.getPersonal().getAddresses().get(0).getAddressLine2(), otherAddress.getAddressLine2());
    assertEquals(user.getPersonal().getAddresses().get(0).getAddressTypeId(), otherAddress.getAddressTypeId());
    assertEquals(user.getPersonal().getAddresses().get(0).getCity(), otherAddress.getCity());
    assertEquals(user.getPersonal().getAddresses().get(0).getCountryId(), otherAddress.getCountryId());
    assertEquals(user.getPersonal().getAddresses().get(0).getId(), otherAddress.getId());
    assertEquals(user.getPersonal().getAddresses().get(0).getPostalCode(), otherAddress.getPostalCode());
    assertEquals(user.getPersonal().getAddresses().get(0).getPrimaryAddress(), otherAddress.getPrimaryAddress());
    assertEquals(user.getPersonal().getAddresses().get(0).getRegion(), otherAddress.getRegion());

  }

  private User createUser() {
    User user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setBarcode(UUID.randomUUID().toString());
    user.setExternalSystemId(UUID.randomUUID().toString());
    user.setUsername(UUID.randomUUID().toString());
    user.setActive(true);
    user.setPatronGroup("undergrad");
    return user;
  }

  private Personal createPersonal() {
    Personal personal = new Personal();
    personal.setFirstName(UUID.randomUUID().toString());
    personal.setLastName(UUID.randomUUID().toString());
    personal.setEmail(UUID.randomUUID().toString() + "@user.org");
    personal.setPreferredContactTypeId("email");
    return personal;
  }

}
