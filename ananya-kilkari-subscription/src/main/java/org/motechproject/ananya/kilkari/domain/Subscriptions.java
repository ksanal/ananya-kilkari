package org.motechproject.ananya.kilkari.domain;

import java.util.ArrayList;
import java.util.List;

public class Subscriptions extends ArrayList<Subscription> {

    public Subscriptions() {
    }

    public Subscriptions(List<Subscription> subscriptionList) {
        super(subscriptionList);
    }

    public Subscription subscriptionInProgress() {
        for (Subscription subscription : this) {
            if (subscription.isInProgress())
                return subscription;
        }
        return null;
    }
}
