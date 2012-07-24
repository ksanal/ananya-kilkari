package org.motechproject.ananya.kilkari.obd.domain;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessageStatus;

import java.io.Serializable;

public class ValidFailedCallReport implements Serializable {
    private CampaignMessageStatus statusCode;
    private String subscriptionId;
    private String msisdn;
    private String campaignId;
    private DateTime createdAt;

    public ValidFailedCallReport() {
    }

    public ValidFailedCallReport(String subscriptionId, String msisdn, String campaignId, CampaignMessageStatus statusCode, DateTime createdAt) {
        this.statusCode = statusCode;
        this.subscriptionId = subscriptionId;
        this.msisdn = msisdn;
        this.campaignId = campaignId;
        this.createdAt = createdAt;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public CampaignMessageStatus getStatusCode() {
        return statusCode;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }
}
