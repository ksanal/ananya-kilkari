package org.motechproject.ananya.kilkari.reporting.domain;

import org.joda.time.DateTime;

import java.io.Serializable;

public class SubscriberReportRequest implements Serializable {
    private String subscriptionId;
    private DateTime createdAt;
    private String beneficiaryName;
    private String beneficiaryAge;
    private DateTime expectedDateOfDelivery;
    private DateTime dateOfBirth;
    private SubscriberLocation location;

    public SubscriberReportRequest(String subscriptionId, DateTime createdAt,
                                   String beneficiaryName, String beneficiaryAge, DateTime expectedDateOfDelivery,
                                   DateTime dateOfBirth, SubscriberLocation location) {
        this.subscriptionId = subscriptionId;
        this.createdAt = createdAt;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryAge = beneficiaryAge;
        this.expectedDateOfDelivery = expectedDateOfDelivery;
        this.dateOfBirth = dateOfBirth;
        this.location = location;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public String getBeneficiaryAge() {
        return beneficiaryAge;
    }

    public DateTime getExpectedDateOfDelivery() {
        return expectedDateOfDelivery;
    }

    public DateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public SubscriberLocation getLocation() {
        return location;
    }
}
