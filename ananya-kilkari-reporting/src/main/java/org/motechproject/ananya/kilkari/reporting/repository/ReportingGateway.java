package org.motechproject.ananya.kilkari.reporting.repository;

import org.motechproject.ananya.kilkari.reporting.domain.*;

public interface ReportingGateway {
    String CREATE_SUBSCRIPTION_PATH = "subscription";
    String SUBSCRIPTION_STATE_CHANGE_PATH = "updatesubscription";
    String GET_LOCATION_PATH = "location";
    String OBD_CALL_DETAILS_PATH = "obd/callDetails";
    String SUBSCRIBER_UPDATE_PATH = "subscriber";

    void createSubscription(SubscriptionCreationReportRequest subscriptionCreationReportRequest);

    void updateSubscriptionStateChange(SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest);

    SubscriberLocation getLocation(String district, String block, String panchayat);

    void reportCampaignMessageDelivery(CampaignMessageDeliveryReportRequest campaignMessageDeliveryReportRequest);

    void updateSubscriberDetails(SubscriberReportRequest subscriberReportRequest);
}
