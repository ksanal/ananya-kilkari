package org.motechproject.ananya.kilkari.request;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;

import java.io.Serializable;

public class CallbackRequestWrapper implements Serializable {

    private static final long serialVersionUID = -9004591146178155518L;
    private CallbackRequest callbackRequest;
    private String subscriptionId;
    private DateTime createdAt;
    private boolean isRequestedByMotech;

    public CallbackRequestWrapper(CallbackRequest callbackRequest,
			String subscriptionId, DateTime createdAt,
			boolean isRequestedByMotech) {
		super();
		this.callbackRequest = callbackRequest;
		this.subscriptionId = subscriptionId;
		this.createdAt = createdAt;
		this.isRequestedByMotech = isRequestedByMotech;
    }

    public String getMsisdn() {
        return callbackRequest.getMsisdn();
    }

    public boolean isRequestedByMotech() {
		return isRequestedByMotech;
	}

	public void setRequestedByMotech(boolean isRequestedByMotech) {
		this.isRequestedByMotech = isRequestedByMotech;
	}

    public String getAction() {
        return callbackRequest.getAction();
    }

    public String getStatus() {
        return callbackRequest.getStatus();
    }

    public SubscriptionPack getPack() {
        return callbackRequest.getPack();
    }

    public String getReason() {
        return callbackRequest.getReason();
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public String getOperator() {
        return callbackRequest.getOperator();
    }
    
    public String getMode() {
		return callbackRequest.getMode();
	}

    public Integer getGraceCount() {
        return (StringUtils.isNotBlank(callbackRequest.getGraceCount()) && StringUtils.isNumeric(callbackRequest.getGraceCount()))
                ? Integer.valueOf(callbackRequest.getGraceCount())
                : null;
    }
}
