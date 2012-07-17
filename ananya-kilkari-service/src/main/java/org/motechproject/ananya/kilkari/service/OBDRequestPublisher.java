package org.motechproject.ananya.kilkari.service;

import org.motechproject.ananya.kilkari.obd.contract.InvalidCallRecordsRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallRequestWrapper;
import org.motechproject.ananya.kilkari.obd.domain.OBDEventKeys;
import org.motechproject.scheduler.context.EventContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OBDRequestPublisher {
    private EventContext eventContext;

    @Autowired
    public OBDRequestPublisher(@Qualifier("eventContext") EventContext eventContext) {
        this.eventContext = eventContext;
    }

    public void publishSuccessfulCallRequest(OBDSuccessfulCallRequestWrapper successfulCallRequestWrapper) {
        eventContext.send(OBDEventKeys.PROCESS_SUCCESSFUL_CALL_REQUEST_SUBJECT, successfulCallRequestWrapper);
    }

    public void publishInvalidCallRecordsRequest(InvalidCallRecordsRequest invalidCallRecordsRequest) {
        eventContext.send(OBDEventKeys.PROCESS_INVALID_CALL_RECORDS_REQUEST_SUBJECT, invalidCallRecordsRequest);
    }
}
