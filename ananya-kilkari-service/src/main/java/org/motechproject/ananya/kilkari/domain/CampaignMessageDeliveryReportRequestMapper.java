package org.motechproject.ananya.kilkari.domain;

import org.motechproject.ananya.kilkari.obd.domain.CallDetailRecord;
import org.motechproject.ananya.kilkari.reporting.domain.CallDetailsReportRequest;
import org.motechproject.ananya.kilkari.reporting.domain.CampaignMessageDeliveryReportRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallRequestWrapper;

public class CampaignMessageDeliveryReportRequestMapper {
    public CampaignMessageDeliveryReportRequest mapFrom(OBDSuccessfulCallRequestWrapper successfulCallRequestWrapper, Integer retryCount) {
        OBDSuccessfulCallRequest successfulCallRequest = successfulCallRequestWrapper.getSuccessfulCallRequest();
        CallDetailRecord callDetailRecord = successfulCallRequest.getCallDetailRecord();
        CallDetailsReportRequest callDetailsReportRequest = new CallDetailsReportRequest(callDetailRecord.getStartTime(), callDetailRecord.getEndTime());
        return new CampaignMessageDeliveryReportRequest(
                successfulCallRequestWrapper.getSubscriptionId(),
                successfulCallRequest.getMsisdn(),
                successfulCallRequest.getCampaignId(),
                successfulCallRequest.getServiceOption(),
                retryCount.toString(),
                successfulCallRequestWrapper.getStatus().name(),
                callDetailsReportRequest
        );
    }
}
