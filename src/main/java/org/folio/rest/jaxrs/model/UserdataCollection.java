
package org.folio.rest.jaxrs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "users",
    "total_records",
    "deactivate_missing_users",
    "update_only_present_fields"
})
public class UserdataCollection {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("users")
    @Valid
    @NotNull
    private List<User> users = new ArrayList<User>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("total_records")
    @NotNull
    private Integer totalRecords;
    @JsonProperty("deactivate_missing_users")
    private Boolean deactivateMissingUsers;
    @JsonProperty("update_only_present_fields")
    private Boolean updateOnlyPresentFields;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     * @return
     *     The users
     */
    @JsonProperty("users")
    public List<User> getUsers() {
        return users;
    }

    /**
     * 
     * (Required)
     * 
     * @param users
     *     The users
     */
    @JsonProperty("users")
    public void setUsers(List<User> users) {
        this.users = users;
    }

    public UserdataCollection withUsers(List<User> users) {
        this.users = users;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     * @return
     *     The totalRecords
     */
    @JsonProperty("total_records")
    public Integer getTotalRecords() {
        return totalRecords;
    }

    /**
     * 
     * (Required)
     * 
     * @param totalRecords
     *     The total_records
     */
    @JsonProperty("total_records")
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public UserdataCollection withTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
        return this;
    }

    /**
     * 
     * @return
     *     The deactivateMissingUsers
     */
    @JsonProperty("deactivate_missing_users")
    public Boolean getDeactivateMissingUsers() {
        return deactivateMissingUsers;
    }

    /**
     * 
     * @param deactivateMissingUsers
     *     The deactivate_missing_users
     */
    @JsonProperty("deactivate_missing_users")
    public void setDeactivateMissingUsers(Boolean deactivateMissingUsers) {
        this.deactivateMissingUsers = deactivateMissingUsers;
    }

    public UserdataCollection withDeactivateMissingUsers(Boolean deactivateMissingUsers) {
        this.deactivateMissingUsers = deactivateMissingUsers;
        return this;
    }

    /**
     * 
     * @return
     *     The updateOnlyPresentFields
     */
    @JsonProperty("update_only_present_fields")
    public Boolean getUpdateOnlyPresentFields() {
        return updateOnlyPresentFields;
    }

    /**
     * 
     * @param updateOnlyPresentFields
     *     The update_only_present_fields
     */
    @JsonProperty("update_only_present_fields")
    public void setUpdateOnlyPresentFields(Boolean updateOnlyPresentFields) {
        this.updateOnlyPresentFields = updateOnlyPresentFields;
    }

    public UserdataCollection withUpdateOnlyPresentFields(Boolean updateOnlyPresentFields) {
        this.updateOnlyPresentFields = updateOnlyPresentFields;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public UserdataCollection withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
