package org.motechproject.ananya.kilkari.subscription.service.request;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;

public class SubscriptionRequest {
    private String msisdn;
    private DateTime creationDate;
    private SubscriptionPack pack;
    private Location location;
    private Subscriber subscriber;
    private String oldSubscriptionId;
    private String reason;
    private String referredBy;
    private boolean referredByFLW;
    private String mode;

    public SubscriptionRequest(String msisdn, DateTime creationDate,
			SubscriptionPack pack, Location location, Subscriber subscriber,
		     String reason, String referredBy, boolean referredByFLW, String mode) {
        this.msisdn = msisdn;
        this.creationDate = creationDate;
        this.pack = pack;
        this.location = location == null ? Location.NULL : location;
        this.subscriber = subscriber;
        this.reason = reason;
		this.referredBy = referredBy;
		this.referredByFLW = referredByFLW;
		this.mode=mode;
    }

    public boolean isReferredByFLW() {
		return referredByFLW;
	}

	public String getMode() {
		return mode;
	}

	public void setReferredByFLW(boolean referredByFLW) {
		this.referredByFLW = referredByFLW;
	}

	public String getMsisdn() {
        return msisdn;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public SubscriptionPack getPack() {
        return pack;
    }

    public Location getLocation() {
        return location;
    }

    public Subscriber getSubscriber() {
        return subscriber == null ? Subscriber.NULL : subscriber;
    }

    public String getReason() {
        return reason;
    }

    public boolean hasLocation() {
        return location != Location.NULL;
    }

    public String getOldSubscriptionId() {
        return oldSubscriptionId;
    }

    public void setOldSubscriptionId(String oldSubscriptionId) {
        this.oldSubscriptionId = oldSubscriptionId;
    }

    public String getReferredBy() {
		return referredBy;
	}

	public void setReferredByFlwMsisdn(String referredByFlwMsisdn) {
		this.referredBy = referredByFlwMsisdn;
	}

    public DateTime getSubscriptionStartDate() {
        Integer weekNumber = subscriber.getWeek();
        if (weekNumber != null && weekNumber >= 1) {
            return pack.getStartDateForWeek(creationDate, weekNumber);
        }

        DateTime dateOfBirth = subscriber.getDateOfBirth();
        if (dateOfBirth != null) {
            return pack.getStartDate(dateOfBirth);
        }

        DateTime expectedDateOfDelivery = subscriber.getExpectedDateOfDelivery();
        if (expectedDateOfDelivery != null) {
            return pack.getStartDate(expectedDateOfDelivery);
        }

        return creationDate;
    }
}
