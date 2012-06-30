package org.motechproject.ananya.kilkari.domain;

import org.joda.time.DateTime;

import java.io.Serializable;

public class CallbackRequestWrapper implements Serializable {

    private CallbackRequest callbackRequest;
    private String subscriptionId;
    private DateTime createdAt;

    public CallbackRequestWrapper(CallbackRequest callbackRequest, String subscriptionId, DateTime createdAt) {
        this.callbackRequest = callbackRequest;
        this.subscriptionId = subscriptionId;
        this.createdAt = createdAt;
    }

    public String getAction() {
        return callbackRequest.getAction();
    }

    public String getStatus() {
        return callbackRequest.getStatus();
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }
}
