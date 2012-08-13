package org.motechproject.ananya.kilkari.subscription.domain;

import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.TypeDiscriminator;
import org.joda.time.DateTime;
import org.motechproject.model.MotechBaseDataObject;

@TypeDiscriminator("doc.type === 'SubscriberCareDoc'")
public class SubscriberCareDoc extends MotechBaseDataObject {
    @JsonProperty
    private String msisdn;

    @JsonProperty
    private SubscriberCareReasons reason;

    @JsonProperty
    private Channel channel;

    @JsonProperty
    private DateTime createdAt;

    public SubscriberCareDoc() {
    }

    public SubscriberCareDoc(String msisdn, SubscriberCareReasons reason, DateTime createdAt, Channel channel) {
        this.msisdn = msisdn;
        this.reason = reason;
        this.channel = channel;
        this.createdAt = createdAt;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public SubscriberCareReasons getReason() {
        return reason;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }
}
