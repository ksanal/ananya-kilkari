package org.motechproject.ananya.kilkari.subscription.service.request;


import org.joda.time.DateTime;

public class SubscriberRequest {
    private String subscriptionId;
    private String channel;
    private DateTime createdAt;
    private String beneficiaryName;
    private Integer beneficiaryAge;
    private Location location;

    public SubscriberRequest(String subscriptionId, String channel, DateTime createdAt,
                             String beneficiaryName, Integer beneficiaryAge,
                             Location location) {
        this.subscriptionId = subscriptionId;
        this.channel = channel;
        this.createdAt = createdAt;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryAge = beneficiaryAge;
        this.location = location;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getChannel() {
        return channel;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public Integer getBeneficiaryAge() {
        return beneficiaryAge;
    }

    public String getBlock() {
        return location.getBlock();
    }

    public String getDistrict() {
        return location.getDistrict();
    }

    public String getPanchayat() {
        return location.getPanchayat();
    }

    public Location getLocation() {
        return location == null ? Location.NULL : location;
    }

    public boolean hasLocation() {
        return !Location.NULL.equals(getLocation());
    }
}