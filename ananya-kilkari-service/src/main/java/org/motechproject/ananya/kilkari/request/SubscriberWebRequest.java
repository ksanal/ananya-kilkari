package org.motechproject.ananya.kilkari.request;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.obd.service.validator.Errors;
import org.motechproject.ananya.kilkari.request.validator.WebRequestValidator;
import org.motechproject.ananya.kilkari.subscription.service.request.Location;

import java.io.Serializable;

public class SubscriberWebRequest implements Serializable {
    @JsonIgnore
    private String channel;
    @JsonIgnore
    private DateTime createdAt;
    @JsonProperty
    private String beneficiaryName;
    @JsonProperty
    private String beneficiaryAge;
    @JsonProperty
    private LocationRequest location;

    public SubscriberWebRequest() {
        this.createdAt = DateTime.now();
    }

    @JsonIgnore
    public String getChannel() {
        return channel;
    }

    @JsonIgnore
    public DateTime getCreatedAt() {
        return createdAt;
    }

    @JsonIgnore
    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    @JsonIgnore
    public String getBeneficiaryAge() {
        return beneficiaryAge;
    }

    @JsonIgnore
    public Location getLocation() {
        return location == null ? null : new Location(location.getDistrict(), location.getBlock(), location.getPanchayat());
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public void setBeneficiaryAge(String beneficiaryAge) {
        this.beneficiaryAge = beneficiaryAge;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setLocation(LocationRequest location) {
        this.location = location;
    }

    public Errors validate() {
        WebRequestValidator webRequestValidator = new WebRequestValidator();
        webRequestValidator.validateAge(beneficiaryAge);
        webRequestValidator.validateChannel(channel);
        validateLocation(webRequestValidator);
        return webRequestValidator.getErrors();
    }

    private void validateLocation(WebRequestValidator webRequestValidator) {
        webRequestValidator.validateLocation(location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriberWebRequest)) return false;

        SubscriberWebRequest that = (SubscriberWebRequest) o;

        return new EqualsBuilder()
                .append(this.channel, that.channel)
                .append(this.beneficiaryAge, that.beneficiaryAge)
                .append(this.beneficiaryName, that.beneficiaryName)
                .append(this.location, that.location)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.channel)
                .append(this.beneficiaryAge)
                .append(this.beneficiaryName)
                .append(this.location)
                .hashCode();
    }
}
