package org.motechproject.ananya.kilkari.subscription.service;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.message.service.CampaignMessageAlertService;
import org.motechproject.ananya.kilkari.messagecampaign.request.MessageCampaignRequest;
import org.motechproject.ananya.kilkari.messagecampaign.service.MessageCampaignService;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriberLocation;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriberReportRequest;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriptionStateChangeReportRequest;
import org.motechproject.ananya.kilkari.reporting.service.ReportingService;
import org.motechproject.ananya.kilkari.subscription.domain.*;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.subscription.repository.KilkariPropertiesData;
import org.motechproject.ananya.kilkari.subscription.repository.OnMobileSubscriptionGateway;
import org.motechproject.ananya.kilkari.subscription.request.OMSubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.mapper.SubscriptionMapper;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriberUpdateRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.validators.SubscriptionValidator;
import org.motechproject.common.domain.PhoneNumber;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.MotechEvent;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class SubscriptionService {
    private AllSubscriptions allSubscriptions;
    private OnMobileSubscriptionManagerPublisher onMobileSubscriptionManagerPublisher;
    private SubscriptionValidator subscriptionValidator;
    private ReportingService reportingService;
    private KilkariInboxService kilkariInboxService;
    private MessageCampaignService messageCampaignService;
    private OnMobileSubscriptionGateway onMobileSubscriptionGateway;
    private CampaignMessageService campaignMessageService;
    private CampaignMessageAlertService campaignMessageAlertService;
    private KilkariPropertiesData kilkariPropertiesData;
    private MotechSchedulerService motechSchedulerService;

    private final static Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired
    public SubscriptionService(AllSubscriptions allSubscriptions, OnMobileSubscriptionManagerPublisher onMobileSubscriptionManagerPublisher,
                               SubscriptionValidator subscriptionValidator, ReportingService reportingService,
                               KilkariInboxService kilkariInboxService, MessageCampaignService messageCampaignService, OnMobileSubscriptionGateway onMobileSubscriptionGateway,
                               CampaignMessageService campaignMessageService, CampaignMessageAlertService campaignMessageAlertService, KilkariPropertiesData kilkariPropertiesData, MotechSchedulerService motechSchedulerService) {
        this.allSubscriptions = allSubscriptions;
        this.onMobileSubscriptionManagerPublisher = onMobileSubscriptionManagerPublisher;
        this.subscriptionValidator = subscriptionValidator;
        this.reportingService = reportingService;
        this.kilkariInboxService = kilkariInboxService;
        this.messageCampaignService = messageCampaignService;
        this.onMobileSubscriptionGateway = onMobileSubscriptionGateway;
        this.campaignMessageService = campaignMessageService;
        this.campaignMessageAlertService = campaignMessageAlertService;
        this.kilkariPropertiesData = kilkariPropertiesData;
        this.motechSchedulerService = motechSchedulerService;
    }

    public Subscription createSubscription(SubscriptionRequest subscriptionRequest, Channel channel) {
        subscriptionValidator.validate(subscriptionRequest);

        DateTime startDate = determineSubscriptionStartDate(subscriptionRequest);
        org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription = new org.motechproject.ananya.kilkari.subscription.domain.Subscription(subscriptionRequest.getMsisdn(), subscriptionRequest.getPack(), subscriptionRequest.getCreationDate());
        subscription.setStartDate(startDate);
        allSubscriptions.add(subscription);

        SubscriptionMapper subscriptionMapper = new SubscriptionMapper();

        OMSubscriptionRequest omSubscriptionRequest = subscriptionMapper.createOMSubscriptionRequest(subscription, channel);
        if (startDate.isAfter(subscriptionRequest.getCreationDate())) {
            scheduleEarlySubscription(startDate, omSubscriptionRequest);
        }
        else
            initiateActivationRequest(omSubscriptionRequest);

        reportingService.reportSubscriptionCreation(
                subscriptionMapper.createSubscriptionCreationReportRequest(subscription, channel, subscriptionRequest.getLocation(), subscriptionRequest.getSubscriber()));

        return subscription;
    }

    private void scheduleEarlySubscription(DateTime startDate, OMSubscriptionRequest omSubscriptionRequest) {

        String subjectKey = SubscriptionEventKeys.EARLY_SUBSCRIPTION;

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(MotechSchedulerService.JOB_ID_KEY, omSubscriptionRequest.getSubscriptionId());
        parameters.put("0", omSubscriptionRequest);
        MotechEvent motechEvent = new MotechEvent(subjectKey, parameters);

        RunOnceSchedulableJob runOnceSchedulableJob = new RunOnceSchedulableJob(motechEvent, startDate.toDate());

        motechSchedulerService.safeScheduleRunOnceJob(runOnceSchedulableJob);
    }

    private DateTime determineSubscriptionStartDate(SubscriptionRequest subscriptionRequest) {
        DateTime startDate = subscriptionRequest.getCreationDate();
        SubscriptionPack subscriptionRequestPack = subscriptionRequest.getPack();

        Integer weekNumber = subscriptionRequest.getSubscriber().getWeek();
        if (weekNumber != null) {
            return subscriptionRequestPack.adjustStartDate(startDate, weekNumber);
        }

        DateTime dateOfBirth = subscriptionRequest.getSubscriber().getDateOfBirth();
        if (dateOfBirth != null) {
            return subscriptionRequestPack.adjustStartDate(dateOfBirth);
        }

        DateTime expectedDateOfDelivery = subscriptionRequest.getSubscriber().getExpectedDateOfDelivery();
        if (expectedDateOfDelivery != null) {
            return subscriptionRequestPack.adjustStartDate(expectedDateOfDelivery);
        }

        return startDate;
    }

    public void initiateActivationRequest(OMSubscriptionRequest omSubscriptionRequest) {
        onMobileSubscriptionManagerPublisher.sendActivationRequest(omSubscriptionRequest);
    }

    public List<Subscription> findByMsisdn(String msisdn) {
        validateMsisdn(msisdn);
        return (List<Subscription>) (List<? extends Subscription>) allSubscriptions.findByMsisdn(msisdn);
    }

    public void activate(String subscriptionId, final DateTime activatedOn, final String operator) {
        org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription = allSubscriptions.findBySubscriptionId(subscriptionId);
        scheduleCampaign(subscription, activatedOn);
        updateStatusAndReport(subscriptionId, activatedOn, null, operator, null, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.activate(operator, activatedOn);
            }
        });
    }

    public void activationFailed(String subscriptionId, DateTime updatedOn, String reason, final String operator) {
        updateStatusAndReport(subscriptionId, updatedOn, reason, operator, null, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.activationFailed(operator);
            }
        });
    }

    public void activationRequested(OMSubscriptionRequest omSubscriptionRequest) {
        onMobileSubscriptionGateway.activateSubscription(omSubscriptionRequest);
        updateStatusAndReport(omSubscriptionRequest.getSubscriptionId(), DateTime.now(), null, null, null, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.activationRequestSent();
            }
        });
    }

    public void requestDeactivation(DeactivationRequest deactivationRequest) {
        String subscriptionId = deactivationRequest.getSubscriptionId();
        org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription = allSubscriptions.findBySubscriptionId(subscriptionId);
        if (!subscription.isInProgress()) {
            logger.debug(String.format("Cannot unsubscribe. Subscription in %s status", subscription.getStatus()));
            return;
        }
        updateStatusAndReport(subscriptionId, deactivationRequest.getCreatedAt(), null, null, null, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.deactivationRequestReceived();
            }
        });
        onMobileSubscriptionManagerPublisher.processDeactivation(new SubscriptionMapper().createOMSubscriptionRequest(subscription, deactivationRequest.getChannel()));
    }

    public void deactivationRequested(OMSubscriptionRequest omSubscriptionRequest) {
        onMobileSubscriptionGateway.deactivateSubscription(omSubscriptionRequest);
        updateStatusAndReport(omSubscriptionRequest.getSubscriptionId(), DateTime.now(), null, null, null, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.deactivationRequestSent();
            }
        });
    }

    public void renewSubscription(String subscriptionId, final DateTime renewedDate, Integer graceCount) {
        updateStatusAndReport(subscriptionId, renewedDate, null, null, graceCount, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.activateOnRenewal();
            }
        });
    }

    public void suspendSubscription(String subscriptionId, final DateTime renewalDate, String reason, Integer graceCount) {
        updateStatusAndReport(subscriptionId, renewalDate, reason, null, graceCount, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.suspendOnRenewal();
            }
        });
    }

    public void deactivateSubscription(String subscriptionId, final DateTime deactivationDate, String reason, Integer graceCount) {
        updateStatusAndReport(subscriptionId, deactivationDate, reason, null, graceCount, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.deactivate();
                kilkariInboxService.scheduleInboxDeletion(subscription);
                unScheduleCampaign(subscription);
                campaignMessageAlertService.deleteFor(subscription.getSubscriptionId());
            }
        });
    }

    public void subscriptionComplete(OMSubscriptionRequest omSubscriptionRequest) {
        org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription = allSubscriptions.findBySubscriptionId(omSubscriptionRequest.getSubscriptionId());
        if (subscription.isInDeactivatedState()) {
            logger.info(String.format("Cannot unsubscribe for subscriptionid: %s  msisdn: %s as it is already in the %s state", omSubscriptionRequest.getSubscriptionId(), omSubscriptionRequest.getMsisdn(), subscription.getStatus()));
            return;
        }
        onMobileSubscriptionGateway.deactivateSubscription(omSubscriptionRequest);

        updateStatusAndReport(omSubscriptionRequest.getSubscriptionId(), DateTime.now(), "Subscription completed", null, null, new Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription>() {
            @Override
            public void perform(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
                subscription.complete();
                kilkariInboxService.scheduleInboxDeletion(subscription);
                campaignMessageAlertService.deleteFor(subscription.getSubscriptionId());
            }
        });
    }

    public Subscription findBySubscriptionId(String subscriptionId) {
        return allSubscriptions.findBySubscriptionId(subscriptionId);
    }

    public void rescheduleCampaign(CampaignRescheduleRequest campaignRescheduleRequest) {
        String subscriptionId = campaignRescheduleRequest.getSubscriptionId();
        subscriptionValidator.validateActiveSubscriptionExists(subscriptionId);
        org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription = allSubscriptions.findBySubscriptionId(subscriptionId);

        unScheduleCampaign(subscription);
        removeScheduledMessagesFromOBD(subscriptionId);
        scheduleCampaign(campaignRescheduleRequest);
    }

    public void updateSubscriberDetails(SubscriberUpdateRequest request) {
        subscriptionValidator.validateSubscriberDetails(request);

        SubscriberLocation subscriberLocation = new SubscriberLocation(request.getDistrict(), request.getBlock(), request.getPanchayat());
        reportingService.reportSubscriberDetailsChange(new SubscriberReportRequest(request.getSubscriptionId(), request.getCreatedAt(),
                request.getBeneficiaryName(), request.getBeneficiaryAge(), request.getExpectedDateOfDelivery(), request.getDateOfBirth(), subscriberLocation));
    }

    private void unScheduleCampaign(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription) {
        MessageCampaignRequest unEnrollRequest = new MessageCampaignRequest(subscription.getSubscriptionId(), subscription.getPack().name(), subscription.getStartDate());
        messageCampaignService.stop(unEnrollRequest);
    }

    private void scheduleCampaign(CampaignRescheduleRequest campaignRescheduleRequest) {
        DateTime existingCampaignStartDate = messageCampaignService.getActiveCampaignStartDate(campaignRescheduleRequest.getSubscriptionId());
        DateTime newCampaignStartDate = getNewCampaignStartDate(existingCampaignStartDate, campaignRescheduleRequest.getCreatedAt());

        MessageCampaignRequest enrollRequest = new MessageCampaignRequest(campaignRescheduleRequest.getSubscriptionId(),
                campaignRescheduleRequest.getReason().name(), newCampaignStartDate);
        messageCampaignService.start(enrollRequest, 0, kilkariPropertiesData.getCampaignScheduleDeltaMinutes());
    }

    private DateTime getNewCampaignStartDate(DateTime existingCampaignStartDate, DateTime rescheduleRequestedDate) {
        DateTime sameDayOfCurrentWeekAsExistingCampaignStartDate = rescheduleRequestedDate.withDayOfWeek(existingCampaignStartDate.getDayOfWeek());
        if (rescheduleRequestedDate.isAfter(sameDayOfCurrentWeekAsExistingCampaignStartDate))
            return sameDayOfCurrentWeekAsExistingCampaignStartDate.plusWeeks(1);
        return sameDayOfCurrentWeekAsExistingCampaignStartDate;
    }

    private void scheduleCampaign(org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription, DateTime activatedOn) {
        MessageCampaignRequest campaignRequest = new MessageCampaignRequest(
                subscription.getSubscriptionId(), subscription.getPack().name(), activatedOn);
        messageCampaignService.start(campaignRequest, kilkariPropertiesData.getCampaignScheduleDeltaDays(), kilkariPropertiesData.getCampaignScheduleDeltaMinutes());
    }

    private void removeScheduledMessagesFromOBD(String subscriptionId) {
        campaignMessageService.deleteCampaignMessagesFor(subscriptionId);
        campaignMessageAlertService.clearMessageId(subscriptionId);
    }

    private void updateStatusAndReport(String subscriptionId, DateTime updatedOn, String reason, String operator, Integer graceCount, Action<org.motechproject.ananya.kilkari.subscription.domain.Subscription> action) {
        org.motechproject.ananya.kilkari.subscription.domain.Subscription subscription = allSubscriptions.findBySubscriptionId(subscriptionId);
        action.perform(subscription);
        allSubscriptions.update(subscription);
        reportingService.reportSubscriptionStateChange(new SubscriptionStateChangeReportRequest(subscription.getSubscriptionId(), subscription.getStatus().name(), updatedOn, reason, operator, graceCount));
    }

    private void validateMsisdn(String msisdn) {
        if (PhoneNumber.isNotValid(msisdn))
            throw new ValidationException(String.format("Invalid msisdn %s", msisdn));
    }
}