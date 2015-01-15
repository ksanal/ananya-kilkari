package org.motechproject.ananya.kilkari.subscription.handlers;

import org.motechproject.ananya.kilkari.subscription.domain.ScheduleDeactivationRequest;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionEventKeys;
import org.motechproject.ananya.kilkari.subscription.request.OMSubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.annotations.MotechListener;
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

    @MotechListener(subjects = {SubscriptionEventKeys.DEACTIVATION_REQUESTED_SUBSCRIPTION})
    public void handleSubscriptionDeactivationRequest
            (MotechEvent event) {
        OMSubscriptionRequest omSubscriptionRequest = (OMSubscriptionRequest) event.getParameters().get("0");
        logger.info(String.format("Handling subscription deactivation event for subscriptionid: %s, msisdn: %s, pack: %s, channel: %s", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), omSubscriptionRequest.getPack(), omSubscriptionRequest.getChannel()));
        subscriptionService.deactivationRequested(omSubscriptionRequest);
    }

    @MotechListener(subjects = {SubscriptionEventKeys.SUBSCRIPTION_COMPLETE})
    public void handleSubscriptionComplete(MotechEvent event) {
        OMSubscriptionRequest omSubscriptionRequest = (OMSubscriptionRequest) event.getParameters().get("0");
        int retryCount = (Integer) event.getMessageRedeliveryCount();
        logger.info("message redelivery count="+retryCount);
        logger.info(String.format("Handling subscription completion event for subscriptionid: %s, msisdn: %s, pack: %s", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), omSubscriptionRequest.getPack()));
        subscriptionService.subscriptionComplete(omSubscriptionRequest, 0, true);
    }
    
    @MotechListener(subjects = {SubscriptionEventKeys.RETRY_SUBSCRIPTION_COMPLETE})
    public void handleRetryOMCompletionReq(MotechEvent event) {
        OMSubscriptionRequest omSubscriptionRequest = (OMSubscriptionRequest) event.getParameters().get("0");
        int retryCount = (Integer) event.getParameters().get("retryCount");
        boolean isFirstTry  = (Boolean) event.getParameters().get("isFirstTry");
        if(retryCount>13){
        	 logger.error(String.format("Failed to move subscriptionId: %s, msisdn: %s, pack: %s to completion. Max retrycount reached.", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), omSubscriptionRequest.getPack()));
        	 return;
        }
        logger.info("message redelivery count for retry completion="+retryCount);
        logger.info(String.format("Handling retry subscription completion event for subscriptionid: %s, msisdn: %s, pack: %s", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), omSubscriptionRequest.getPack()));
        subscriptionService.subscriptionComplete(omSubscriptionRequest, retryCount, isFirstTry);
    }
    
    @MotechListener(subjects = {SubscriptionEventKeys.EARLY_SUBSCRIPTION})
    public void handleEarlySubscription(MotechEvent event) {
        OMSubscriptionRequest omSubscriptionRequest = (OMSubscriptionRequest) event.getParameters().get("0");
        logger.info(String.format("Handling early subscription for subscriptionid: %s, msisdn: %s, pack: %s", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), omSubscriptionRequest.getPack()));
        subscriptionService.initiateActivationRequestForEarlySubscription(omSubscriptionRequest);
    }

    @MotechListener(subjects = {SubscriptionEventKeys.DEACTIVATE_SUBSCRIPTION})
    public void handleDeactivateSubscription(MotechEvent event) {
        ScheduleDeactivationRequest scheduleDeactivationRequest = (ScheduleDeactivationRequest) event.getParameters().get("0");
        logger.info(String.format("Handling deactivation for subscriptionid: %s", scheduleDeactivationRequest.getSubscriptionId()));
        subscriptionService.deactivateSubscription(scheduleDeactivationRequest.getSubscriptionId(), scheduleDeactivationRequest.getDeactivationDate(), scheduleDeactivationRequest.getReason(), scheduleDeactivationRequest.getGraceCount(), scheduleDeactivationRequest.getMode());
    }
}