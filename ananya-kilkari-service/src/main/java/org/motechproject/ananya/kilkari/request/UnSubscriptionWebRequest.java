package org.motechproject.ananya.kilkari.request;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.request.validator.WebRequestValidator;
import org.motechproject.ananya.kilkari.subscription.validators.Errors;

import java.io.Serializable;

public class UnSubscriptionWebRequest implements Serializable {
    @JsonIgnore
    private String channel;
    @JsonProperty
    private String reason;
    @JsonIgnore
    private DateTime createdAt;

    public UnSubscriptionWebRequest() {
        this.createdAt = DateTime.now();
    }

    @JsonIgnore
    public String getChannel() {
        return channel;
    }

    @JsonIgnore
    public String getReason() {
        return reason;
    }

    @JsonIgnore
    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return String.format("reason: %s; channel: %s", reason, channel);
    }

    public Errors validate() {
        WebRequestValidator webRequestValidator = new WebRequestValidator();
        webRequestValidator.validateChannel(channel);
        return webRequestValidator.getErrors();
    }
}