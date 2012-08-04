package org.motechproject.ananya.kilkari.reporting.repository;

import org.motechproject.ananya.kilkari.reporting.domain.*;

public interface ReportingGateway {
    SubscriberLocation getLocation(String district, String block, String panchayat);

    void reportSubscriptionCreation(SubscriptionCreationReportRequest subscriptionCreationReportRequest);

    void reportSubscriptionStateChange(SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest);

    void reportCampaignMessageDeliveryStatus(CampaignMessageDeliveryReportRequest campaignMessageDeliveryReportRequest);

    void reportSubscriberDetailsChange(String subscriptionId, SubscriberReportRequest request);
}
