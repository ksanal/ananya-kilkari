package org.motechproject.ananya.kilkari.request;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;
import org.motechproject.ananya.kilkari.subscription.domain.LocationRequest;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.validators.Errors;
import org.motechproject.ananya.kilkari.subscription.validators.ValidationUtils;
import org.motechproject.common.domain.PhoneNumber;

import java.io.Serializable;

public class SubscriptionWebRequest implements Serializable {
    @JsonProperty
    private String msisdn;
    @JsonProperty
    private String pack;
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

    @JsonProperty
    private String weekNumber;

    public SubscriptionWebRequest() {
        this.location = new LocationRequest();
        this.createdAt = DateTime.now();
    }

    @JsonIgnore
    public String getMsisdn() {
        return msisdn;
    }

    @JsonIgnore
    public String getPack() {
        return pack;
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
    public int getBeneficiaryAge() {
        return StringUtils.isNotEmpty(beneficiaryAge) ? Integer.parseInt(beneficiaryAge) : 0;
    }

    @JsonIgnore
    public DateTime getExpectedDateOfDelivery() {
        return parseDateTime(expectedDateOfDelivery);
    }

    @JsonIgnore
    public DateTime getDateOfBirth() {
        return parseDateTime(dateOfBirth);
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

    @JsonIgnore
    public String getWeekNumber() {
        return weekNumber;
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

    public void setWeekNumber(String weekNumber) {
        this.weekNumber = weekNumber;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public void validate(Errors errors) {
        validateMsisdn(errors);
        validatePack(errors);
        validateChannel(errors);
        if (!Channel.isIVR(channel)) {
            validateAge(errors);
            validateDOB(errors);
            validateEDD(errors);
            validateWeekNumber(errors);
        }
    }

    private void validateWeekNumber(Errors errors) {
        if (StringUtils.isNotEmpty(weekNumber)) {
            if (!ValidationUtils.assertNumeric(weekNumber)) {
                errors.add("Invalid week number %s", weekNumber);
            }
        }
    }

    private void validatePack(Errors errors) {
        if (!ValidationUtils.assertPack(pack)) {
            errors.add("Invalid subscription pack %s", pack);
        }
    }

    private void validateMsisdn(Errors errors) {
        if (PhoneNumber.isNotValid(msisdn)) {
            errors.add("Invalid msisdn %s", msisdn);
        }
    }

    public void validateChannel(Errors errors) {
        if (!ValidationUtils.assertChannel(channel)) {
            errors.add("Invalid channel %s", channel);
        }
    }

    public void validateChannel() {
        if (!ValidationUtils.assertChannel(channel)) {
            throw new ValidationException(String.format("Invalid channel %s", channel));
        }
    }

    private void validateEDD(Errors errors) {
        if (StringUtils.isNotEmpty(expectedDateOfDelivery)) {
            String errorMessage = "Invalid expected date of delivery %s";

            if (!ValidationUtils.assertDateFormat(expectedDateOfDelivery)) {
                errors.add(errorMessage, expectedDateOfDelivery);
                return;
            }

            if (!ValidationUtils.assertDateBefore(createdAt, parseDateTime(expectedDateOfDelivery))) {
                errors.add(errorMessage, expectedDateOfDelivery);
            }
        }
    }

    private void validateDOB(Errors errors) {
        if (StringUtils.isNotEmpty(dateOfBirth)) {
            String errorMessage = "Invalid date of birth %s";

            if (!ValidationUtils.assertDateFormat(dateOfBirth)) {
                errors.add(errorMessage, dateOfBirth);
                return;
            }

            if (!ValidationUtils.assertDateBefore(parseDateTime(dateOfBirth), createdAt)) {
                errors.add(errorMessage, dateOfBirth);
            }
        }
    }

    private void validateAge(Errors errors) {
        if (StringUtils.isNotEmpty(beneficiaryAge)) {
            if (!ValidationUtils.assertNumeric(beneficiaryAge)) {
                errors.add("Invalid beneficiary age %s", beneficiaryAge);
            }
        }
    }

    private DateTime parseDateTime(String dateTime) {
        return StringUtils.isNotEmpty(dateTime) ? DateTimeFormat.forPattern("dd-MM-yyyy").parseDateTime(dateTime) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionWebRequest)) return false;

        SubscriptionWebRequest that = (SubscriptionWebRequest) o;

        return new EqualsBuilder()
                .append(this.msisdn, that.msisdn)
                .append(this.pack, that.pack)
                .append(this.channel, that.channel)
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
                .append(this.msisdn)
                .append(this.pack)
                .append(this.channel)
                .append(this.channel)
                .append(this.beneficiaryAge)
                .append(this.beneficiaryName)
                .append(this.dateOfBirth)
                .append(this.expectedDateOfDelivery)
                .append(this.location)
                .hashCode();
    }
}
