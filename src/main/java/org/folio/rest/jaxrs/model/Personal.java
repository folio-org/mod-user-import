
package org.folio.rest.jaxrs.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "lastName",
    "firstName",
    "middleName",
    "email",
    "phone",
    "mobilePhone",
    "dateOfBirth",
    "addresses",
    "preferredContactTypeId"
})
public class Personal {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("lastName")
    @NotNull
    private String lastName;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("middleName")
    private String middleName;
    @JsonProperty("email")
    private String email;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("mobilePhone")
    private String mobilePhone;
    @JsonProperty("dateOfBirth")
    private Date dateOfBirth;
    @JsonProperty("addresses")
    @Size(min = 0)
    @Valid
    private List<Address> addresses = new ArrayList<Address>();
    @JsonProperty("preferredContactTypeId")
    private String preferredContactTypeId;

    /**
     * 
     * (Required)
     * 
     * @return
     *     The lastName
     */
    @JsonProperty("lastName")
    public String getLastName() {
        return lastName;
    }

    /**
     * 
     * (Required)
     * 
     * @param lastName
     *     The lastName
     */
    @JsonProperty("lastName")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Personal withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    /**
     * 
     * @return
     *     The firstName
     */
    @JsonProperty("firstName")
    public String getFirstName() {
        return firstName;
    }

    /**
     * 
     * @param firstName
     *     The firstName
     */
    @JsonProperty("firstName")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Personal withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    /**
     * 
     * @return
     *     The middleName
     */
    @JsonProperty("middleName")
    public String getMiddleName() {
        return middleName;
    }

    /**
     * 
     * @param middleName
     *     The middleName
     */
    @JsonProperty("middleName")
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public Personal withMiddleName(String middleName) {
        this.middleName = middleName;
        return this;
    }

    /**
     * 
     * @return
     *     The email
     */
    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    /**
     * 
     * @param email
     *     The email
     */
    @JsonProperty("email")
    public void setEmail(String email) {
        this.email = email;
    }

    public Personal withEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * 
     * @return
     *     The phone
     */
    @JsonProperty("phone")
    public String getPhone() {
        return phone;
    }

    /**
     * 
     * @param phone
     *     The phone
     */
    @JsonProperty("phone")
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Personal withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    /**
     * 
     * @return
     *     The mobilePhone
     */
    @JsonProperty("mobilePhone")
    public String getMobilePhone() {
        return mobilePhone;
    }

    /**
     * 
     * @param mobilePhone
     *     The mobilePhone
     */
    @JsonProperty("mobilePhone")
    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public Personal withMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
        return this;
    }

    /**
     * 
     * @return
     *     The dateOfBirth
     */
    @JsonProperty("dateOfBirth")
    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * 
     * @param dateOfBirth
     *     The dateOfBirth
     */
    @JsonProperty("dateOfBirth")
    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Personal withDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    /**
     * 
     * @return
     *     The addresses
     */
    @JsonProperty("addresses")
    public List<Address> getAddresses() {
        return addresses;
    }

    /**
     * 
     * @param addresses
     *     The addresses
     */
    @JsonProperty("addresses")
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public Personal withAddresses(List<Address> addresses) {
        this.addresses = addresses;
        return this;
    }

    /**
     * 
     * @return
     *     The preferredContactTypeId
     */
    @JsonProperty("preferredContactTypeId")
    public String getPreferredContactTypeId() {
        return preferredContactTypeId;
    }

    /**
     * 
     * @param preferredContactTypeId
     *     The preferredContactTypeId
     */
    @JsonProperty("preferredContactTypeId")
    public void setPreferredContactTypeId(String preferredContactTypeId) {
        this.preferredContactTypeId = preferredContactTypeId;
    }

    public Personal withPreferredContactTypeId(String preferredContactTypeId) {
        this.preferredContactTypeId = preferredContactTypeId;
        return this;
    }

}
