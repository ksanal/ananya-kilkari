package org.motechproject.ananya.kilkari.request;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.subscription.validators.Errors;
import org.motechproject.ananya.kilkari.request.validator.WebRequestValidator;

import java.io.Serializable;

public class CampaignChangeRequest extends BaseWebRequest implements Serializable {

    @JsonProperty
    private String reason;
    @JsonIgnore
    private DateTime createdAt;

    public CampaignChangeRequest() {
        this.createdAt = DateTime.now();
    }

    @JsonIgnore
    public String getReason() {
        return reason;
    }

    @JsonIgnore
    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Errors validate() {
        WebRequestValidator webRequestValidator = new WebRequestValidator();
        webRequestValidator.validateCampaignChangeReason(reason);
        return webRequestValidator.getErrors();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CampaignChangeRequest)) return false;

        CampaignChangeRequest that = (CampaignChangeRequest) o;

        return new EqualsBuilder()
                .append(this.reason, that.reason)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.reason)
                .hashCode();
    }
}