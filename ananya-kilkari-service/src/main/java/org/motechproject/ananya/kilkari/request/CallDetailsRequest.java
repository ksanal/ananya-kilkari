package org.motechproject.ananya.kilkari.request;


import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.obd.domain.ServiceOption;
import org.motechproject.ananya.kilkari.reporting.domain.CampaignMessageCallSource;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;

import java.io.Serializable;

public class CallDetailsRequest implements Serializable {
    private String msisdn;
    private String campaignId;
    private CallDurationRequest callDurationRequest;
    private DateTime createdAt;
    private CampaignMessageCallSource callSource;
    private Channel channel;
    protected String subscriptionId;
    protected ServiceOption serviceOption;

    public CallDetailsRequest(String subscriptionId, ServiceOption serviceOption, String msisdn, String campaignId, CallDurationRequest callDurationRequest, DateTime createdAt, CampaignMessageCallSource callSource) {
        this.msisdn = msisdn;
        this.campaignId = campaignId;
        this.callDurationRequest = callDurationRequest;
        this.createdAt = createdAt;
        this.callSource = callSource;

        this.channel = Channel.IVR;
        this.subscriptionId = subscriptionId;
        this.serviceOption = serviceOption;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public CallDurationRequest getCallDurationRequest() {
        return callDurationRequest;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public CampaignMessageCallSource getCallSource() {
        return callSource;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public ServiceOption getServiceOption() {
        return serviceOption;
    }
}
