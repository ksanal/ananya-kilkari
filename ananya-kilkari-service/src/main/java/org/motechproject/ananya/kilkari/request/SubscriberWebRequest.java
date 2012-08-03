package org.motechproject.ananya.kilkari.request;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;
import org.motechproject.ananya.kilkari.subscription.validators.Errors;
import org.motechproject.ananya.kilkari.subscription.validators.WebRequestValidator;

import java.io.Serializable;

public class SubscriberWebRequest implements Serializable {
    @JsonProperty
    private String channel;
    @JsonIgnore
    private DateTime createdAt;
    @JsonProperty
    private String beneficiaryName;
    @JsonProperty
    private String beneficiaryAge;
    @JsonProperty
    private String expectedDateOfDelivery;
    @JsonProperty
    private String dateOfBirth;
    @JsonProperty
    private LocationRequest location;

    public SubscriberWebRequest() {
        this.location = new LocationRequest();
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
    public String getExpectedDateOfDelivery() {
        return expectedDateOfDelivery;
    }

    @JsonIgnore
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    @JsonIgnore
    public String getDistrict() {
        return location == null ? null : location.getDistrict();
    }

    @JsonIgnore
    public String getBlock() {
        return location == null ? null : location.getBlock();
    }

    @JsonIgnore
    public String getPanchayat() {
        return location == null ? null : location.getPanchayat();
    }

    @JsonIgnore
    public LocationRequest getLocation() {
        return location;
    }

    public void setDistrict(String district) {
        location.setDistrict(district);
    }

    public void setBlock(String block) {
        location.setBlock(block);
    }

    public void setPanchayat(String panchayat) {
        location.setPanchayat(panchayat);
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public void setBeneficiaryAge(String beneficiaryAge) {
        this.beneficiaryAge = beneficiaryAge;
    }

    public void setExpectedDateOfDelivery(String expectedDateOfDelivery) {
        this.expectedDateOfDelivery = expectedDateOfDelivery;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Errors validate() {
        WebRequestValidator webRequestValidator = new WebRequestValidator();
        webRequestValidator.validateChannel(channel, Channel.IVR);
        webRequestValidator.validateAge(beneficiaryAge);
        webRequestValidator.validateDOB(dateOfBirth, createdAt);
        webRequestValidator.validateEDD(expectedDateOfDelivery, createdAt);
        webRequestValidator.validateOnlyOneOfEDDOrDOBIsPresent(expectedDateOfDelivery, dateOfBirth);
        return webRequestValidator.getErrors();
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
                .append(this.dateOfBirth, that.dateOfBirth)
                .append(this.expectedDateOfDelivery, that.expectedDateOfDelivery)
                .append(this.location, that.location)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.channel)
                .append(this.beneficiaryAge)
                .append(this.beneficiaryName)
                .append(this.dateOfBirth)
                .append(this.expectedDateOfDelivery)
                .append(this.location)
                .hashCode();
    }
}
