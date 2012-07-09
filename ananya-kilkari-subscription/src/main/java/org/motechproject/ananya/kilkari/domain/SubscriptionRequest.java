package org.motechproject.ananya.kilkari.domain;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.motechproject.ananya.kilkari.exceptions.DuplicateSubscriptionException;
import org.motechproject.ananya.kilkari.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.validation.ValidationUtils;

import java.io.Serializable;

public class SubscriptionRequest implements Serializable {
    public static final String DATE_TIME_FORMAT = "dd-MM-yyyy";

    private String msisdn;
    private String pack;
    private String channel;
    @JsonIgnore
    private DateTime createdAt;
    private String beneficiaryName;
    private String beneficiaryAge;
    private String district;
    private String block;
    private String panchayat;
    private String expectedDateOfDelivery;
    private String dateOfBirth;

    public SubscriptionRequest() {
        this.createdAt = DateTime.now();
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getPanchayat() {
        return panchayat;
    }

    public void setPanchayat(String panchayat) {
        this.panchayat = panchayat;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public int getBeneficiaryAge() {
        return StringUtils.isNotEmpty(beneficiaryAge) ? Integer.parseInt(beneficiaryAge) : 0;
    }

    public DateTime getExpectedDateOfDelivery() {
        return parseDateTime(expectedDateOfDelivery);
    }

    public DateTime getDateOfBirth() {
        return parseDateTime(dateOfBirth);
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

    public String getMsisdn() {
        return msisdn;
    }

    public String getPack() {
        return pack;
    }

    public String getChannel() {
        return channel;
    }

    public DateTime getCreatedAt() {
        return createdAt;
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

    public void validate(SubscriberLocation reportLocation, Subscription existingActiveSubscription) {
        ValidationUtils.assertMsisdn(msisdn);
        ValidationUtils.assertPack(pack);
        ValidationUtils.assertChannel(channel);
        validateIfAlreadySubscribed(existingActiveSubscription);
        if (!Channel.isIVR(channel)) {
            validateAge();
            validateDOB();
            validateEDD();
            validateLocation(reportLocation);
        }
    }

    private void validateIfAlreadySubscribed(Subscription existingActiveSubscription) {
        if (existingActiveSubscription == null)
            return;
        throw new DuplicateSubscriptionException(String.format("Subscription already exists for msisdn: %s, pack: %s",
                msisdn, pack));
    }

    private void validateLocation(SubscriberLocation reportLocation) {
        if (isLocationEmpty())
            return;
        if (reportLocation == null)
            throw new ValidationException(String.format("Invalid location with district: %s, block: %s, panchayat: %s", district, block, panchayat));
    }

    private boolean isLocationEmpty() {
        return district == null && block == null && panchayat == null;
    }

    private void validateEDD() {
        if (StringUtils.isNotEmpty(expectedDateOfDelivery)) {
            ValidationUtils.assertDateFormat(expectedDateOfDelivery, SubscriptionRequest.DATE_TIME_FORMAT, "Invalid expected date of delivery %s");
        }
    }

    private void validateDOB() {
        if (StringUtils.isNotEmpty(dateOfBirth)) {
            ValidationUtils.assertDateFormat(dateOfBirth, SubscriptionRequest.DATE_TIME_FORMAT, "Invalid date of birth %s");
        }
    }

    private void validateAge() {
        if (StringUtils.isNotEmpty(beneficiaryAge)) {
            ValidationUtils.assertNumeric(beneficiaryAge, "Invalid beneficiary age %s");
        }
    }

    private DateTime parseDateTime(String dateTime) {
        return StringUtils.isNotEmpty(dateTime) ? DateTimeFormat.forPattern(SubscriptionRequest.DATE_TIME_FORMAT).parseDateTime(dateTime) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionRequest)) return false;

        SubscriptionRequest that = (SubscriptionRequest) o;

        return new EqualsBuilder()
                .append(this.msisdn, that.msisdn)
                .append(this.pack, that.pack)
                .append(this.channel, that.channel)
                .append(this.channel, that.channel)
                .append(this.beneficiaryAge, that.beneficiaryAge)
                .append(this.beneficiaryName, that.beneficiaryName)
                .append(this.dateOfBirth, that.dateOfBirth)
                .append(this.expectedDateOfDelivery, that.expectedDateOfDelivery)
                .append(this.district, that.district)
                .append(this.block, that.block)
                .append(this.panchayat, that.panchayat)
                .isEquals();

    }
}
