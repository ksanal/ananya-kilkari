package org.motechproject.ananya.kilkari.service.validator;

import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.motechproject.ananya.kilkari.subscription.validators.Errors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UnsubscriptionRequestValidator {

    private final SubscriptionService subscriptionService;

    @Autowired
    public UnsubscriptionRequestValidator(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public Errors validate(String subscriptionId) {
        Errors errors = new Errors();
        validateSubscription(subscriptionId, errors);

        return errors;
    }

    private void validateSubscription(String subscriptionId, Errors errors) {
        Subscription subscription = subscriptionService.findBySubscriptionId(subscriptionId);

        if (subscription == null) {
            errors.add("Invalid subscriptionId %s", subscriptionId);
            return;
        }

        if (!subscription.isInProgress())
            errors.add(String.format("Cannot unsubscribe. Subscription in %s status", subscription.getStatus()));
    }
}
