
package org.folio.rest.jaxrs.model;

import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "id",
    "countryId",
    "addressLine1",
    "addressLine2",
    "city",
    "region",
    "postalCode",
    "addressTypeId",
    "primaryAddress"
})
public class Address {

    @JsonProperty("id")
    private String id;
    @JsonProperty("countryId")
    private String countryId;
    @JsonProperty("addressLine1")
    private String addressLine1;
    @JsonProperty("addressLine2")
    private String addressLine2;
    @JsonProperty("city")
    private String city;
    @JsonProperty("region")
    private String region;
    @JsonProperty("postalCode")
    private String postalCode;
    @JsonProperty("addressTypeId")
    private String addressTypeId;
    @JsonProperty("primaryAddress")
    private Boolean primaryAddress;

    /**
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
     * @param id
     *     The id
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Address withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * 
     * @return
     *     The countryId
     */
    @JsonProperty("countryId")
    public String getCountryId() {
        return countryId;
    }

    /**
     * 
     * @param countryId
     *     The countryId
     */
    @JsonProperty("countryId")
    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public Address withCountryId(String countryId) {
        this.countryId = countryId;
        return this;
    }

    /**
     * 
     * @return
     *     The addressLine1
     */
    @JsonProperty("addressLine1")
    public String getAddressLine1() {
        return addressLine1;
    }

    /**
     * 
     * @param addressLine1
     *     The addressLine1
     */
    @JsonProperty("addressLine1")
    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public Address withAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
        return this;
    }

    /**
     * 
     * @return
     *     The addressLine2
     */
    @JsonProperty("addressLine2")
    public String getAddressLine2() {
        return addressLine2;
    }

    /**
     * 
     * @param addressLine2
     *     The addressLine2
     */
    @JsonProperty("addressLine2")
    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public Address withAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
        return this;
    }

    /**
     * 
     * @return
     *     The city
     */
    @JsonProperty("city")
    public String getCity() {
        return city;
    }

    /**
     * 
     * @param city
     *     The city
     */
    @JsonProperty("city")
    public void setCity(String city) {
        this.city = city;
    }

    public Address withCity(String city) {
        this.city = city;
        return this;
    }

    /**
     * 
     * @return
     *     The region
     */
    @JsonProperty("region")
    public String getRegion() {
        return region;
    }

    /**
     * 
     * @param region
     *     The region
     */
    @JsonProperty("region")
    public void setRegion(String region) {
        this.region = region;
    }

    public Address withRegion(String region) {
        this.region = region;
        return this;
    }

    /**
     * 
     * @return
     *     The postalCode
     */
    @JsonProperty("postalCode")
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * 
     * @param postalCode
     *     The postalCode
     */
    @JsonProperty("postalCode")
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Address withPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    /**
     * 
     * @return
     *     The addressTypeId
     */
    @JsonProperty("addressTypeId")
    public String getAddressTypeId() {
        return addressTypeId;
    }

    /**
     * 
     * @param addressTypeId
     *     The addressTypeId
     */
    @JsonProperty("addressTypeId")
    public void setAddressTypeId(String addressTypeId) {
        this.addressTypeId = addressTypeId;
    }

    public Address withAddressTypeId(String addressTypeId) {
        this.addressTypeId = addressTypeId;
        return this;
    }

    /**
     * 
     * @return
     *     The primaryAddress
     */
    @JsonProperty("primaryAddress")
    public Boolean getPrimaryAddress() {
        return primaryAddress;
    }

    /**
     * 
     * @param primaryAddress
     *     The primaryAddress
     */
    @JsonProperty("primaryAddress")
    public void setPrimaryAddress(Boolean primaryAddress) {
        this.primaryAddress = primaryAddress;
    }

    public Address withPrimaryAddress(Boolean primaryAddress) {
        this.primaryAddress = primaryAddress;
        return this;
    }

}
