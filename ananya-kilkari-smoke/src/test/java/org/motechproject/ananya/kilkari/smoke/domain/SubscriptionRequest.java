package org.motechproject.ananya.kilkari.smoke.domain;

import org.joda.time.DateTime;

public class SubscriptionRequest {
    private String msisdn;
    private String pack;
    private DateTime createdAt;
    private String beneficiaryName;
    private String beneficiaryAge;
    private String expectedDateOfDelivery;
    private String dateOfBirth;
    private Location location;
    private String status;
    private String referredBy;

   

	public SubscriptionRequest() {
    }

    public SubscriptionRequest(String msisdn, String pack, DateTime createdAt, String beneficiaryName, String beneficiaryAge, String expectedDateOfDelivery, String dateOfBirth, Location location, String referredBy) {
        this.msisdn = msisdn;
        this.pack = pack;
        this.createdAt = createdAt;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryAge = beneficiaryAge;
        this.expectedDateOfDelivery = expectedDateOfDelivery;
        this.dateOfBirth = dateOfBirth;
        this.location = location;
        this.setReferredBy(referredBy);
    }

    public String getMsisdn() {
        return msisdn;
    }

    public String getPack() {
        return pack;
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

    public String getExpectedDateOfDelivery() {
        return expectedDateOfDelivery;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public Location getLocation() {
        return location;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
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

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setDistrict(String district) {
        this.location.setDistrict(district);
    }

    public void setBlock(String block) {
        this.location.setBlock(block);
    }

    public void setPanchayat(String panchayat) {
        this.location.setPanchayat(panchayat);
    }

    public void setStatus(String status) {
        this.status = status;
    }

	public String getReferredBy() {
		return referredBy;
	}

	public void setReferredBy(String referredBy) {
		this.referredBy = referredBy;
	}
}
