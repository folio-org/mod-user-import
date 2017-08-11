
package org.folio.rest.jaxrs.model;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "value",
    "desc"
})
public class PreferredContact {

    @JsonProperty("value")
    private PreferredContact.Value value;
    @JsonProperty("desc")
    private PreferredContact.Desc desc;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The value
     */
    @JsonProperty("value")
    public PreferredContact.Value getValue() {
        return value;
    }

    /**
     * 
     * @param value
     *     The value
     */
    @JsonProperty("value")
    public void setValue(PreferredContact.Value value) {
        this.value = value;
    }

    public PreferredContact withValue(PreferredContact.Value value) {
        this.value = value;
        return this;
    }

    /**
     * 
     * @return
     *     The desc
     */
    @JsonProperty("desc")
    public PreferredContact.Desc getDesc() {
        return desc;
    }

    /**
     * 
     * @param desc
     *     The desc
     */
    @JsonProperty("desc")
    public void setDesc(PreferredContact.Desc desc) {
        this.desc = desc;
    }

    public PreferredContact withDesc(PreferredContact.Desc desc) {
        this.desc = desc;
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

    public PreferredContact withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Generated("org.jsonschema2pojo")
    public static enum Desc {

        EMAIL("Email"),
        MAIL_PRIMARY_ADDRESS("Mail (Primary Address)"),
        TEXT_MESSAGE("Text message"),
        PHONE("Phone"),
        MOBILE_PHONE("Mobile phone");
        private final String value;
        private final static Map<String, PreferredContact.Desc> CONSTANTS = new HashMap<String, PreferredContact.Desc>();

        static {
            for (PreferredContact.Desc c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Desc(String value) {
            this.value = value;
        }

        @JsonValue
        @Override
        public String toString() {
            return this.value;
        }

        @JsonCreator
        public static PreferredContact.Desc fromValue(String value) {
            PreferredContact.Desc constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("org.jsonschema2pojo")
    public static enum Value {

        EMAIL("EMAIL"),
        MAIL("MAIL"),
        TEXT("TEXT"),
        PHONE("PHONE"),
        MOBILE("MOBILE");
        private final String value;
        private final static Map<String, PreferredContact.Value> CONSTANTS = new HashMap<String, PreferredContact.Value>();

        static {
            for (PreferredContact.Value c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        private Value(String value) {
            this.value = value;
        }

        @JsonValue
        @Override
        public String toString() {
            return this.value;
        }

        @JsonCreator
        public static PreferredContact.Value fromValue(String value) {
            PreferredContact.Value constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
