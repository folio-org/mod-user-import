
package org.folio.rest.jaxrs.model;

import java.util.Date;

import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * User Schema
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "username",
    "id",
    "barcode",
    "active",
    "type",
    "patronGroup",
    "meta",
    "externalSystemId",
    "personal",
    "openDate",
    "enrollmentDate",
    "expirationDate",
    "lastUpdateDate"
})
public class User {

  /**
   * 
   * (Required)
   * 
   */
  @JsonProperty("username")
  @NotNull
  private String username;
  /**
   * 
   * (Required)
   * 
   */
  @JsonProperty("id")
  private String id;
  @JsonProperty("barcode")
  private String barcode;
  @JsonProperty("active")
  private Boolean active;
  @JsonProperty("type")
  private String type;
  @JsonProperty("patronGroup")
  private String patronGroup;
  @JsonProperty("meta")
  @Valid
  private Meta meta;
  @JsonProperty("externalSystemId")
  private String externalSystemId;
  /**
   * 
   */
  @JsonProperty("personal")
  @Valid
  private Personal personal;
  @JsonProperty("openDate")
  private Date openDate;
  @JsonProperty("enrollmentDate")
  private Date enrollmentDate;
  @JsonProperty("expirationDate")
  private Date expirationDate;
  @JsonProperty("lastUpdateDate")
  private Date lastUpdateDate;

  /**
   * 
   * (Required)
   * 
   * @return
   *     The username
   */
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }

  /**
   * 
   * (Required)
   * 
   * @param username
   *     The username
   */
  @JsonProperty("username")
  public void setUsername(String username) {
    this.username = username;
  }

  public User withUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * 
   * (Required)
   * 
   * @return
   *     The id
   */
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  /**
   * 
   * (Required)
   * 
   * @param id
   *     The id
   */
  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  public User withId(String id) {
    this.id = id;
    return this;
  }

  /**
   * 
   * @return
   *     The barcode
   */
  @JsonProperty("barcode")
  public String getBarcode() {
    return barcode;
  }

  /**
   * 
   * @param barcode
   *     The barcode
   */
  @JsonProperty("barcode")
  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public User withBarcode(String barcode) {
    this.barcode = barcode;
    return this;
  }

  /**
   * 
   * @return
   *     The active
   */
  @JsonProperty("active")
  public Boolean getActive() {
    return active;
  }

  /**
   * 
   * @param active
   *     The active
   */
  @JsonProperty("active")
  public void setActive(Boolean active) {
    this.active = active;
  }

  public User withActive(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * 
   * @return
   *     The type
   */
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  /**
   * 
   * @param type
   *     The type
   */
  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  public User withType(String type) {
    this.type = type;
    return this;
  }

  /**
   * 
   * @return
   *     The patronGroup
   */
  @JsonProperty("patronGroup")
  public String getPatronGroup() {
    return patronGroup;
  }

  /**
   * 
   * @param patronGroup
   *     The patronGroup
   */
  @JsonProperty("patronGroup")
  public void setPatronGroup(String patronGroup) {
    this.patronGroup = patronGroup;
  }

  public User withPatronGroup(String patronGroup) {
    this.patronGroup = patronGroup;
    return this;
  }

  /**
   * 
   * @return
   *     The meta
   */
  @JsonProperty("meta")
  public Meta getMeta() {
    return meta;
  }

  /**
   * 
   * @param meta
   *     The meta
   */
  @JsonProperty("meta")
  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  public User withMeta(Meta meta) {
    this.meta = meta;
    return this;
  }

  /**
   * 
   * @return
   *     The externalSystemId
   */
  @JsonProperty("externalSystemId")
  public String getExternalSystemId() {
    return externalSystemId;
  }

  /**
   * 
   * @param externalSystemId
   *     The externalSystemId
   */
  @JsonProperty("externalSystemId")
  public void setExternalSystemId(String externalSystemId) {
    this.externalSystemId = externalSystemId;
  }

  public User withExternalSystemId(String externalSystemId) {
    this.externalSystemId = externalSystemId;
    return this;
  }

  /**
   * 
   * @return
   *     The personal
   */
  @JsonProperty("personal")
  public Personal getPersonal() {
    return personal;
  }

  /**
   * 
   * @param personal
   *     The personal
   */
  @JsonProperty("personal")
  public void setPersonal(Personal personal) {
    this.personal = personal;
  }

  public User withPersonal(Personal personal) {
    this.personal = personal;
    return this;
  }

  /**
   * 
   * @return
   *     The openDate
   */
  @JsonProperty("openDate")
  public Date getOpenDate() {
    return openDate;
  }

  /**
   * 
   * @param openDate
   *     The openDate
   */
  @JsonProperty("openDate")
  public void setOpenDate(Date openDate) {
    this.openDate = openDate;
  }

  public User withOpenDate(Date openDate) {
    this.openDate = openDate;
    return this;
  }

  /**
   * 
   * @return
   *     The enrollmentDate
   */
  @JsonProperty("enrollmentDate")
  public Date getEnrollmentDate() {
    return enrollmentDate;
  }

  /**
   * 
   * @param enrollmentDate
   *     The enrollmentDate
   */
  @JsonProperty("enrollmentDate")
  public void setEnrollmentDate(Date enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
  }

  public User withEnrollmentDate(Date enrollmentDate) {
    this.enrollmentDate = enrollmentDate;
    return this;
  }

  /**
   * 
   * @return
   *     The expirationDate
   */
  @JsonProperty("expirationDate")
  public Date getExpirationDate() {
    return expirationDate;
  }

  /**
   * 
   * @param expirationDate
   *     The expirationDate
   */
  @JsonProperty("expirationDate")
  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }

  public User withExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  /**
   * 
   * @return
   *     The lastUpdateDate
   */
  @JsonProperty("lastUpdateDate")
  public Date getLastUpdateDate() {
    return lastUpdateDate;
  }

  /**
   * 
   * @param lastUpdateDate
   *     The lastUpdateDate
   */
  @JsonProperty("lastUpdateDate")
  public void setLastUpdateDate(Date lastUpdateDate) {
    this.lastUpdateDate = lastUpdateDate;
  }

  public User withLastUpdateDate(Date lastUpdateDate) {
    this.lastUpdateDate = lastUpdateDate;
    return this;
  }

}
