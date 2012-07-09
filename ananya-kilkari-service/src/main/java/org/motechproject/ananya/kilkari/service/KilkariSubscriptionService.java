package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.domain.CallbackRequestWrapper;
import org.motechproject.ananya.kilkari.domain.SubscriberCareRequest;
import org.motechproject.ananya.kilkari.domain.Subscription;
import org.motechproject.ananya.kilkari.domain.SubscriptionRequest;
import org.motechproject.ananya.kilkari.exceptions.DuplicateSubscriptionException;
import org.motechproject.ananya.kilkari.messagecampaign.request.KilkariMessageCampaignRequest;
import org.motechproject.ananya.kilkari.messagecampaign.service.KilkariMessageCampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KilkariSubscriptionService {

    private SubscriptionPublisher subscriptionPublisher;
    private SubscriptionService subscriptionService;
    private KilkariMessageCampaignService kilkariMessageCampaignService;

    private final Logger LOGGER = LoggerFactory.getLogger(KilkariSubscriptionService.class);

    private int campaignScheduleDeltaDays;

    private int campaignScheduleDeltaMinutes;

    @Value("#{kilkariProperties['kilkari.campaign.schedule.delta.days']}")
    public void setCampaignScheduleDeltaDays(int campaignScheduleDeltaDays) {
        this.campaignScheduleDeltaDays = campaignScheduleDeltaDays;
    }

    @Value("#{kilkariProperties['kilkari.campaign.schedule.delta.minutes']}")
    public void setCampaignScheduleDeltaMinutes(int campaignScheduleDeltaMinutes) {
        this.campaignScheduleDeltaMinutes = campaignScheduleDeltaMinutes;
    }

    @Autowired
    public KilkariSubscriptionService(SubscriptionPublisher subscriptionPublisher,
                                      SubscriptionService subscriptionService,
                                      KilkariMessageCampaignService kilkariMessageCampaignService) {
        this.subscriptionPublisher = subscriptionPublisher;
        this.subscriptionService = subscriptionService;
        this.kilkariMessageCampaignService = kilkariMessageCampaignService;
    }

    public void createSubscription(SubscriptionRequest subscriptionRequest) {
        subscriptionPublisher.createSubscription(subscriptionRequest);
    }

    public void processCallbackRequest(CallbackRequestWrapper callbackRequestWrapper) {
        subscriptionPublisher.processCallbackRequest(callbackRequestWrapper);
    }

    public List<Subscription> getSubscriptionsFor(String msisdn) {
        return subscriptionService.findByMsisdn(msisdn);
    }

    public void processSubscriptionRequest(SubscriptionRequest subscriptionRequest) {
        try {
            String subscriptionId = subscriptionService.createSubscription(subscriptionRequest);

            KilkariMessageCampaignRequest campaignRequest = new KilkariMessageCampaignRequest(
                    subscriptionId, subscriptionRequest.getPack(), subscriptionRequest.getCreatedAt());

            kilkariMessageCampaignService.start(campaignRequest);
        } catch (DuplicateSubscriptionException e) {
            LOGGER.warn(String.format("Subscription for msisdn[%s] and pack[%s] already exists.",
                    subscriptionRequest.getMsisdn(), subscriptionRequest.getPack()));
        }
    }
}
