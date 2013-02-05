package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.mapper.ChangeMsisdnRequestMapper;
import org.motechproject.ananya.kilkari.mapper.SubscriptionRequestMapper;
import org.motechproject.ananya.kilkari.message.domain.CampaignMessageAlert;
import org.motechproject.ananya.kilkari.message.service.CampaignMessageAlertService;
import org.motechproject.ananya.kilkari.obd.domain.Channel;
import org.motechproject.ananya.kilkari.obd.domain.PhoneNumber;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.obd.service.validator.Errors;
import org.motechproject.ananya.kilkari.request.*;
import org.motechproject.ananya.kilkari.subscription.domain.CampaignChangeReason;
import org.motechproject.ananya.kilkari.subscription.domain.CampaignRescheduleRequest;
import org.motechproject.ananya.kilkari.subscription.domain.DeactivationRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.exceptions.DuplicateSubscriptionException;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.repository.KilkariPropertiesData;
import org.motechproject.ananya.kilkari.subscription.service.ChangeSubscriptionService;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangeMsisdnRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangeSubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriberRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.response.SubscriptionDetailsResponse;
import org.motechproject.ananya.kilkari.utils.CampaignMessageIdStrategy;
import org.motechproject.scheduler.MotechSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KilkariSubscriptionService {
    private SubscriptionPublisher subscriptionPublisher;
    private SubscriptionService subscriptionService;
    private MotechSchedulerService motechSchedulerService;
    private KilkariPropertiesData kilkariProperties;
    private ChangeSubscriptionService changeSubscriptionService;
    private CampaignMessageAlertService campaignMessageAlertService;
    private CampaignMessageService campaignMessageService;

    private final Logger logger = LoggerFactory.getLogger(KilkariSubscriptionService.class);

    @Autowired
    public KilkariSubscriptionService(SubscriptionPublisher subscriptionPublisher,
                                      SubscriptionService subscriptionService,
                                      MotechSchedulerService motechSchedulerService,
                                      ChangeSubscriptionService changeSubscriptionService,
                                      KilkariPropertiesData kilkariProperties,
                                      CampaignMessageAlertService campaignMessageAlertService, CampaignMessageService campaignMessageService) {
        this.subscriptionPublisher = subscriptionPublisher;
        this.subscriptionService = subscriptionService;
        this.motechSchedulerService = motechSchedulerService;
        this.changeSubscriptionService = changeSubscriptionService;
        this.kilkariProperties = kilkariProperties;
        this.campaignMessageAlertService = campaignMessageAlertService;
        this.campaignMessageService = campaignMessageService;
    }

    public void createSubscriptionAsync(SubscriptionWebRequest subscriptionWebRequest) {
        subscriptionPublisher.createSubscription(subscriptionWebRequest);
    }

    public void createSubscription(SubscriptionWebRequest subscriptionWebRequest) {
        validateSubscriptionRequest(subscriptionWebRequest);
        SubscriptionRequest subscriptionRequest = SubscriptionRequestMapper.mapToSubscriptionRequest(subscriptionWebRequest);
        try {
            subscriptionService.createSubscription(subscriptionRequest, Channel.from(subscriptionWebRequest.getChannel()));
        } catch (DuplicateSubscriptionException e) {
            logger.warn(String.format("Subscription for msisdn[%s] and pack[%s] already exists.",
                    subscriptionWebRequest.getMsisdn(), subscriptionWebRequest.getPack()));
        }
    }

    public void processCallbackRequest(CallbackRequestWrapper callbackRequestWrapper) {
        subscriptionPublisher.processCallbackRequest(callbackRequestWrapper);
    }

    public List<SubscriptionDetailsResponse> getSubscriptionDetails(String msisdn, Channel channel) {
        validateMsisdn(msisdn);
        return subscriptionService.getSubscriptionDetails(msisdn, channel);
    }

    public List<Subscription> findByMsisdn(String msisdn) {
        return subscriptionService.findByMsisdn(msisdn);
    }

    public Subscription findBySubscriptionId(String subscriptionId) {
        return subscriptionService.findBySubscriptionId(subscriptionId);
    }

    public void processSubscriptionCompletion(Subscription subscription, String campaignName) {
        String subscriptionId = subscription.getSubscriptionId();
        CampaignMessageAlert campaignMessageAlert = campaignMessageAlertService.findBy(subscriptionId);
        boolean isRenewed = campaignMessageAlert != null && campaignMessageAlert.isRenewed();
        String messageId = new CampaignMessageIdStrategy().createMessageId(campaignName, subscription.getScheduleStartDate(), subscription.getPack());
        if (isRenewed || campaignMessageService.find(subscriptionId, messageId) != null) {
            subscriptionService.scheduleCompletion(subscription, DateTime.now());
            logger.info(String.format("Scheduling completion now for %s, since already renewed for last week", subscriptionId));
            return;
        }
        subscriptionService.scheduleCompletion(subscription, subscription.getCurrentWeeksMessageExpiryDate());
    }

    public void requestUnsubscription(String subscriptionId, UnSubscriptionWebRequest unSubscriptionWebRequest) {
        Errors validationErrors = unSubscriptionWebRequest.validate();
        raiseExceptionIfThereAreErrors(validationErrors);
        subscriptionService.requestUnsubscription(new DeactivationRequest(subscriptionId, Channel.from(unSubscriptionWebRequest.getChannel()),
                unSubscriptionWebRequest.getCreatedAt(), unSubscriptionWebRequest.getReason()));
    }

    public void processCampaignChange(CampaignChangeRequest campaignChangeRequest, String subscriptionId) {
        Errors validationErrors = campaignChangeRequest.validate();
        raiseExceptionIfThereAreErrors(validationErrors);

        subscriptionService.rescheduleCampaign(new CampaignRescheduleRequest(subscriptionId,
                CampaignChangeReason.from(campaignChangeRequest.getReason()), campaignChangeRequest.getCreatedAt()));
    }

    public void updateSubscriberDetails(SubscriberWebRequest request, String subscriptionId) {
        Errors errors = request.validate();
        raiseExceptionIfThereAreErrors(errors);
        SubscriberRequest subscriberRequest = SubscriptionRequestMapper.mapToSubscriberRequest(request, subscriptionId);
        subscriptionService.updateSubscriberDetails(subscriberRequest);
    }

    public void changeSubscription(ChangeSubscriptionWebRequest changeSubscriptionWebRequest, String subscriptionId) {
        Errors errors = changeSubscriptionWebRequest.validate();
        raiseExceptionIfThereAreErrors(errors);
        ChangeSubscriptionRequest changeSubscriptionRequest = SubscriptionRequestMapper.mapToChangeSubscriptionRequest(changeSubscriptionWebRequest, subscriptionId);
        changeSubscriptionService.process(changeSubscriptionRequest);
    }

    public void changeMsisdn(ChangeMsisdnWebRequest changeMsisdnWebRequest) {
        Errors validationErrors = changeMsisdnWebRequest.validate();
        raiseExceptionIfThereAreErrors(validationErrors);

        ChangeMsisdnRequest changeMsisdnRequest = ChangeMsisdnRequestMapper.mapFrom(changeMsisdnWebRequest);
        subscriptionService.changeMsisdn(changeMsisdnRequest);
    }

    private void validateMsisdn(String msisdn) {
        if (PhoneNumber.isNotValid(msisdn))
            throw new ValidationException(String.format("Invalid msisdn %s", msisdn));
    }

    private void raiseExceptionIfThereAreErrors(Errors validationErrors) {
        if (validationErrors.hasErrors()) {
            throw new ValidationException(validationErrors.allMessages());
        }
    }

    private void validateSubscriptionRequest(SubscriptionWebRequest subscriptionWebRequest) {
        Errors errors = subscriptionWebRequest.validate();
        if (errors.hasErrors()) {
            throw new ValidationException(errors.allMessages());
        }
    }
}
