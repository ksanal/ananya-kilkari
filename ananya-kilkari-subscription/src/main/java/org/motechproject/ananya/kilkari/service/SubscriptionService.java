package org.motechproject.ananya.kilkari.service;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.ananya.kilkari.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.mappers.SubscriptionMapper;
import org.motechproject.ananya.kilkari.repository.AllSubscriptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {
    private AllSubscriptions allSubscriptions;

    private Publisher publisher;

    @Autowired
    public SubscriptionService(AllSubscriptions allSubscriptions, Publisher publisher) {
        this.allSubscriptions = allSubscriptions;
        this.publisher = publisher;
    }

    public String createSubscription(SubscriptionRequest subscriptionRequest) {
        subscriptionRequest.validate();
        SubscriptionMapper subscriptionMapper = new SubscriptionMapper(subscriptionRequest);
        Subscription subscription = subscriptionMapper.getSubscription();
        allSubscriptions.add(subscription);

        sendProcessSubscriptionEvent(subscriptionMapper.getSubscriptionActivationRequest());
        sendReportSubscriptionCreationEvent(subscriptionMapper.getSubscriptionCreationReportRequest());

        return subscription.getSubscriptionId();
    }

    public List<Subscription> findByMsisdn(String msisdn) {
        validateMsisdn(msisdn);
        return allSubscriptions.findByMsisdn(msisdn);
    }

    public Subscription findByMsisdnAndPack(String msisdn, String pack) {
        return allSubscriptions.findByMsisdnAndPack(msisdn, SubscriptionPack.from(pack));
    }

    public void activate(String subscriptionId, DateTime activatedOn, String operator) {
        updateStatusAndReport(subscriptionId, activatedOn, null, operator, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.activate();
            }
        });
    }

    public void activationFailed(String subscriptionId, DateTime updatedOn, String reason, String operator) {
        updateStatusAndReport(subscriptionId, updatedOn, reason, operator, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.activationFailed();
            }
        });
    }

    public void activationRequested(String subscriptionId) {
        updateStatusAndReport(subscriptionId, DateTime.now(), null, null, new Action<Subscription>() {
            @Override
            public void perform(Subscription subscription) {
                subscription.activationRequested();
            }
        });
    }

    private void updateStatusAndReport(String subscriptionId, DateTime updatedOn, String reason, String operator, Action<Subscription> action) {
        Subscription subscription = allSubscriptions.findBySubscriptionId(subscriptionId);
        action.perform(subscription);
        updateWithReporting(subscription, updatedOn, reason, operator);
    }

    private void sendProcessSubscriptionEvent(SubscriptionActivationRequest subscriptionActivationRequest) {
        publisher.processSubscription(subscriptionActivationRequest);
    }

    private void sendReportSubscriptionCreationEvent(SubscriptionCreationReportRequest subscriptionCreationReportRequest) {
        publisher.reportSubscriptionCreation(subscriptionCreationReportRequest);
    }

    private void validateMsisdn(String msisdn) {
        if (!isValidMsisdn(msisdn))
            throw new ValidationException(String.format("Invalid msisdn %s", msisdn));
    }

    private boolean isValidMsisdn(String msisdn) {
        return (StringUtils.length(msisdn) >= 10 && StringUtils.isNumeric(msisdn));
    }

    private void sendSubscriptionStateChangeEvent(String subscriptionId, SubscriptionStatus status, DateTime updatedOn, String reason, String operator) {
        publisher.reportSubscriptionStateChange(new SubscriptionStateChangeReportRequest(subscriptionId, status.name(), updatedOn, reason, operator));
    }

    private void updateWithReporting(Subscription subscription, DateTime updatedOn, String reason, String operator) {
        allSubscriptions.update(subscription);
        sendSubscriptionStateChangeEvent(subscription.getSubscriptionId(), subscription.getStatus(), updatedOn, reason, operator);
    }
}