package org.motechproject.ananya.kilkari.subscription.service;

import org.apache.commons.lang.math.NumberUtils;
import org.motechproject.ananya.kilkari.contract.request.SubscriptionChangePackRequest;
import org.motechproject.ananya.kilkari.reporting.service.ReportingService;
import org.motechproject.ananya.kilkari.subscription.domain.DeactivationRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangePackRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.Subscriber;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.validators.SubscriptionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChangePackProcessor {
    private SubscriptionService subscriptionService;
    private SubscriptionValidator subscriptionValidator;
    private ReportingService reportingService;

    @Autowired
    public ChangePackProcessor(SubscriptionService subscriptionService, SubscriptionValidator subscriptionValidator, ReportingService reportingService) {
        this.subscriptionService = subscriptionService;
        this.subscriptionValidator = subscriptionValidator;
        this.reportingService = reportingService;
    }

    public void process(ChangePackRequest changePackRequest) {
        String subscriptionId = changePackRequest.getSubscriptionId();
        subscriptionValidator.validateSubscriptionExists(subscriptionId);
        Subscription existingSubscription = subscriptionService.findBySubscriptionId(subscriptionId);
        ChangePackValidator.validate(existingSubscription, changePackRequest);

        subscriptionService.requestDeactivation(new DeactivationRequest(subscriptionId, changePackRequest.getChannel(), changePackRequest.getCreatedAt()));
        Subscription newSubscription = createSubscriptionWithNewPack(changePackRequest);

        reportingService.reportChangePack(new SubscriptionChangePackRequest(NumberUtils.createLong(newSubscription.getMsisdn()), newSubscription.getSubscriptionId(), subscriptionId, newSubscription.getPack().name(),
                changePackRequest.getChannel().name(), newSubscription.getStatus().name(), changePackRequest.getCreatedAt(), changePackRequest.getExpectedDateOfDelivery(), changePackRequest.getDateOfBirth(), newSubscription.getStartDate()));
    }

    private Subscription createSubscriptionWithNewPack(ChangePackRequest changePackRequest) {
        Subscriber subscriber = new Subscriber(null, null, changePackRequest.getDateOfBirth(), changePackRequest.getExpectedDateOfDelivery(), null);
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(changePackRequest.getMsisdn(), changePackRequest.getCreatedAt(), changePackRequest.getPack(), null, subscriber);
        return subscriptionService.createSubscription(subscriptionRequest, changePackRequest.getChannel());
    }


}
