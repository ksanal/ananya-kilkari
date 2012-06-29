package org.motechproject.ananya.kilkari.handlers;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.domain.SubscriptionActivationRequest;
import org.motechproject.ananya.kilkari.domain.SubscriptionEventKeys;
import org.motechproject.ananya.kilkari.domain.SubscriptionStatus;
import org.motechproject.ananya.kilkari.service.OnMobileSubscriptionService;
import org.motechproject.ananya.kilkari.service.SubscriptionService;
import org.motechproject.scheduler.domain.MotechEvent;
import org.motechproject.server.event.annotations.MotechListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionActivationHandler {

    private OnMobileSubscriptionService onMobileSubscriptionService;

    Logger logger = Logger.getLogger(SubscriptionActivationHandler.class);
    private SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionActivationHandler(OnMobileSubscriptionService onMobileSubscriptionService, SubscriptionService subscriptionService) {
        this.onMobileSubscriptionService = onMobileSubscriptionService;
        this.subscriptionService = subscriptionService;
    }

    @MotechListener(subjects = {SubscriptionEventKeys.PROCESS_SUBSCRIPTION})
    public void handleProcessSubscription(MotechEvent event) {
        SubscriptionActivationRequest subscriptionActivationRequest = (SubscriptionActivationRequest) event.getParameters().get("0");
        logger.info(String.format("Handling process subscription event for msisdn: %s, pack: %s, channel: %s", subscriptionActivationRequest.getMsisdn(), subscriptionActivationRequest.getPack(), subscriptionActivationRequest.getChannel()));
        try {
            onMobileSubscriptionService.activateSubscription(subscriptionActivationRequest);
            subscriptionService.updateSubscriptionStatus(subscriptionActivationRequest.getMsisdn(), subscriptionActivationRequest.getPack().name(), SubscriptionStatus.PENDING_ACTIVATION, DateTime.now());
        }
        catch (RuntimeException e) {
            logger.error("Exception Occurred while sending subscription activation request", e);
            throw e;
        }
    }
}