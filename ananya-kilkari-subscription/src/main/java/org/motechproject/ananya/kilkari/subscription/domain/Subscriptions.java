package org.motechproject.ananya.kilkari.subscription.domain;

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
    public Subscription subscriptionInActiveOrSuspended() {
        for (Subscription subscription : this) {
            if (subscription.isActiveOrSuspended())
                return subscription;
        }
        return null;
    }
    public Subscription subscriptionInActiveSuspendedOrGrace() {
        for (Subscription subscription : this) {
            if (subscription.isActiveSuspendedOrGrace())
                return subscription;
        }
        return null;
    }
    public List<Subscription> subscriptionsInProgress(){
        List<Subscription> subscriptionsInProgress = new ArrayList<>();
        for(Subscription subscription : this)
            if(subscription.isInProgress())
                subscriptionsInProgress.add(subscription);
        return subscriptionsInProgress;
    }

    public List<Subscription> updatableSubscriptions(){
        List<Subscription> updatableSubscriptions = new ArrayList<>();
        for(Subscription subscription : this)
            if(subscription.isInUpdatableState())
                updatableSubscriptions.add(subscription);
        return updatableSubscriptions;
    }
}
