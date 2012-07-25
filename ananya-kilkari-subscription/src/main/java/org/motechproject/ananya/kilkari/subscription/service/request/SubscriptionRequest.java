package org.motechproject.ananya.kilkari.subscription.service.request;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.subscription.domain.*;

public class SubscriptionRequest {
    private String msisdn;
    private DateTime creationDate;
    private SubscriptionPack pack;
    private Location location;
    private Subscriber subscriber;

    public SubscriptionRequest(String msisdn, DateTime creationDate,
                               SubscriptionPack pack, Location location, Subscriber subscriber) {
        this.msisdn = msisdn;
        this.creationDate = creationDate;
        this.pack = pack;
        this.location = location;
        this.subscriber = subscriber;
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
        return location == null ? Location.NULL : location;
    }

    public Subscriber getSubscriber() {
        return subscriber == null ? Subscriber.NULL : subscriber;
    }

    public boolean hasLocation() {
        return !Location.NULL.equals(getLocation());
    }
}
