package org.motechproject.ananya.kilkari.web.contract.mapper;

import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.web.contract.response.SubscriptionDetails;

public class SubscriptionDetailsMapper {
    public static SubscriptionDetails mapFrom(Subscription subscription) {
        return new SubscriptionDetails(subscription.getSubscriptionId(), subscription.getPack().name(), subscription.getStatus().name());
    }
}
