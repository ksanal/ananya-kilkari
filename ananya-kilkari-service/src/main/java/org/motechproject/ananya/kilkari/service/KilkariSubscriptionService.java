package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.domain.PhoneNumber;
import org.motechproject.ananya.kilkari.mapper.ChangeMsisdnRequestMapper;
import org.motechproject.ananya.kilkari.mapper.SubscriptionRequestMapper;
import org.motechproject.ananya.kilkari.request.*;
import org.motechproject.ananya.kilkari.subscription.domain.*;
import org.motechproject.ananya.kilkari.subscription.exceptions.DuplicateSubscriptionException;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.repository.KilkariPropertiesData;
import org.motechproject.ananya.kilkari.subscription.service.ChangePackProcessor;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.motechproject.ananya.kilkari.subscription.service.mapper.SubscriptionMapper;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangeMsisdnRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriberRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.validators.Errors;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.MotechEvent;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;


@Service
public class KilkariSubscriptionService {
    private SubscriptionPublisher subscriptionPublisher;
    private SubscriptionService subscriptionService;
    private MotechSchedulerService motechSchedulerService;
    private KilkariPropertiesData kilkariProperties;
    private ChangePackProcessor changePackProcessor;

    private final Logger logger = LoggerFactory.getLogger(KilkariSubscriptionService.class);

    @Autowired
    public KilkariSubscriptionService(SubscriptionPublisher subscriptionPublisher,
                                      SubscriptionService subscriptionService,
                                      MotechSchedulerService motechSchedulerService,
                                      KilkariPropertiesData kilkariProperties, ChangePackProcessor changePackProcessor) {
        this.subscriptionPublisher = subscriptionPublisher;
        this.subscriptionService = subscriptionService;
        this.motechSchedulerService = motechSchedulerService;
        this.kilkariProperties = kilkariProperties;
        this.changePackProcessor = changePackProcessor;
    }

    public void createSubscriptionAsync(SubscriptionWebRequest subscriptionWebRequest) {
        subscriptionPublisher.createSubscription(subscriptionWebRequest);
    }

    public void createSubscription(SubscriptionWebRequest subscriptionWebRequest) {
        validateSubscriptionRequest(subscriptionWebRequest);
        SubscriptionRequest subscriptionRequest = SubscriptionRequestMapper.mapToSubscriptionRequest(subscriptionWebRequest);
        try {
            subscriptionService.createSubscriptionWithReporting(subscriptionRequest, Channel.from(subscriptionWebRequest.getChannel()));
        } catch (DuplicateSubscriptionException e) {
            logger.warn(String.format("Subscription for msisdn[%s] and pack[%s] already exists.",
                    subscriptionWebRequest.getMsisdn(), subscriptionWebRequest.getPack()));
        }
    }

    private void validateSubscriptionRequest(SubscriptionWebRequest subscriptionWebRequest) {
        Errors errors = subscriptionWebRequest.validate();
        if (errors.hasErrors()) {
            throw new ValidationException(errors.allMessages());
        }
    }

    public void processCallbackRequest(CallbackRequestWrapper callbackRequestWrapper) {
        subscriptionPublisher.processCallbackRequest(callbackRequestWrapper);
    }

    public List<Subscription> findByMsisdn(String msisdn) {
        validateMsisdn(msisdn);
        return subscriptionService.findByMsisdn(msisdn);
    }

    public Subscription findBySubscriptionId(String subscriptionId) {
        return subscriptionService.findBySubscriptionId(subscriptionId);
    }

    public void processSubscriptionCompletion(Subscription subscription) {
        String subjectKey = SubscriptionEventKeys.SUBSCRIPTION_COMPLETE;
        Date startDate = DateTime.now().plusDays(kilkariProperties.getBufferDaysToAllowRenewalForPackCompletion()).toDate();
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(MotechSchedulerService.JOB_ID_KEY, subscription.getSubscriptionId());
        parameters.put("0", new SubscriptionMapper().createOMSubscriptionRequest(subscription, Channel.MOTECH));

        MotechEvent motechEvent = new MotechEvent(subjectKey, parameters);
        RunOnceSchedulableJob runOnceSchedulableJob = new RunOnceSchedulableJob(motechEvent, startDate);

        motechSchedulerService.safeScheduleRunOnceJob(runOnceSchedulableJob);
    }

    public void requestDeactivation(String subscriptionId, UnsubscriptionRequest unsubscriptionRequest) {
        subscriptionService.requestDeactivation(new DeactivationRequest(subscriptionId, Channel.from(unsubscriptionRequest.getChannel()), unsubscriptionRequest.getCreatedAt()));
    }

    public void processCampaignChange(CampaignChangeRequest campaignChangeRequest) {
        subscriptionService.rescheduleCampaign(new CampaignRescheduleRequest(campaignChangeRequest.getSubscriptionId(),
                CampaignChangeReason.from(campaignChangeRequest.getReason()), campaignChangeRequest.getCreatedAt()));
    }

    public void updateSubscriberDetails(SubscriberWebRequest request, String subscriptionId) {
        Errors errors = request.validate();
        raiseExceptionIfThereAreErrors(errors);
        SubscriberRequest subscriberRequest = SubscriptionRequestMapper.mapToSubscriberRequest(request, subscriptionId);
        subscriptionService.updateSubscriberDetails(subscriberRequest);
    }

    public void changePack(ChangePackWebRequest changePackWebRequest) {
        Errors errors = changePackWebRequest.validate();
        raiseExceptionIfThereAreErrors(errors);
        changePackProcessor.process(SubscriptionRequestMapper.mapToChangePackRequest(changePackWebRequest));
    }

    private void raiseExceptionIfThereAreErrors(Errors validationErrors) {
        if (validationErrors.hasErrors()) {
            throw new ValidationException(validationErrors.allMessages());
        }
    }

    public Subscription findSubscriptionInProgress(String msisdn, SubscriptionPack pack) {
        return subscriptionService.findSubscriptionInProgress(msisdn, pack);
    }

    public void changeMsisdn(ChangeMsisdnWebRequest changeMsisdnWebRequest) {
        ChangeMsisdnRequest changeMsisdnRequest = ChangeMsisdnRequestMapper.mapFrom(changeMsisdnWebRequest);
        subscriptionService.changeMsisdn(changeMsisdnRequest);
    }

    private void validateMsisdn(String msisdn) {
        if (PhoneNumber.isNotValid(msisdn))
            throw new ValidationException(String.format("Invalid msisdn %s", msisdn));
    }
}
