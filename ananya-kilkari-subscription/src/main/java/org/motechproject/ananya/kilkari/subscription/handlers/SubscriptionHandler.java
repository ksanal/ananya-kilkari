package org.motechproject.ananya.kilkari.subscription.handlers;

import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionEventKeys;
import org.motechproject.ananya.kilkari.subscription.request.OMSubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.motechproject.scheduler.domain.MotechEvent;
import org.motechproject.server.event.annotations.MotechListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionHandler {
    private final static Logger logger = LoggerFactory.getLogger(SubscriptionHandler.class);
    private SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionHandler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @MotechListener(subjects = {SubscriptionEventKeys.ACTIVATE_SUBSCRIPTION})
    public void handleSubscriptionActivation(MotechEvent event) {
        OMSubscriptionRequest omSubscriptionRequest = (OMSubscriptionRequest) event.getParameters().get("0");
        logger.info(String.format("Handling subscription activation event for subscriptionid: %s, msisdn: %s, pack: %s, channel: %s", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), omSubscriptionRequest.getPack(), omSubscriptionRequest.getChannel()));
        subscriptionService.activationRequested(omSubscriptionRequest);
    }

    @MotechListener(subjects = {SubscriptionEventKeys.DEACTIVATE_SUBSCRIPTION})
    public void handleSubscriptionDeactivation(MotechEvent event) {
        OMSubscriptionRequest omSubscriptionRequest = (OMSubscriptionRequest) event.getParameters().get("0");
        logger.info(String.format("Handling subscription deactivation event for subscriptionid: %s, msisdn: %s, pack: %s, channel: %s", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), omSubscriptionRequest.getPack(), omSubscriptionRequest.getChannel()));
        subscriptionService.deactivationRequested(omSubscriptionRequest);
    }

    @MotechListener(subjects = {SubscriptionEventKeys.SUBSCRIPTION_COMPLETE})
    public void handleSubscriptionComplete(MotechEvent event) {
        OMSubscriptionRequest OMSubscriptionRequest = (OMSubscriptionRequest) event.getParameters().get("0");
        logger.info(String.format("Handling subscription completion event for subscriptionid: %s, msisdn: %s, pack: %s", OMSubscriptionRequest.getSubscriptionId(), OMSubscriptionRequest.getMsisdn(), OMSubscriptionRequest.getPack()));
        subscriptionService.subscriptionComplete(OMSubscriptionRequest);
    }
}