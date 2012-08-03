package org.motechproject.ananya.kilkari.obd.handlers;


import org.apache.log4j.Logger;
import org.motechproject.ananya.kilkari.obd.domain.ValidFailedCallReport;
import org.motechproject.ananya.kilkari.obd.repository.OnMobileOBDGateway;
import org.motechproject.ananya.kilkari.obd.request.InvalidFailedCallReports;
import org.motechproject.ananya.kilkari.obd.service.CallDeliveryFailureEventKeys;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.scheduler.domain.MotechEvent;
import org.motechproject.server.event.annotations.MotechListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CallDeliveryFailureRecordHandler {
    Logger logger = Logger.getLogger(CallDeliveryFailureRecordHandler.class);
    private OnMobileOBDGateway onMobileOBDGateway;
    private CampaignMessageService campaignMessageService;

    @Autowired
    public CallDeliveryFailureRecordHandler(OnMobileOBDGateway onMobileOBDGateway, CampaignMessageService campaignMessageService) {
        this.onMobileOBDGateway = onMobileOBDGateway;
        this.campaignMessageService = campaignMessageService;
    }

    @MotechListener(subjects = {CallDeliveryFailureEventKeys.PROCESS_INVALID_CALL_DELIVERY_FAILURE_RECORD})
    public void handleInvalidCallDeliveryFailureRecord(MotechEvent motechEvent) {
        InvalidFailedCallReports invalidFailedCallReports = (InvalidFailedCallReports) motechEvent.getParameters().get("0");
        logger.info("Handling OBD invalid call delivery failure records");
        onMobileOBDGateway.sendInvalidFailureRecord(invalidFailedCallReports);
    }

    @MotechListener(subjects = {CallDeliveryFailureEventKeys.PROCESS_VALID_CALL_DELIVERY_FAILURE_RECORD})
    public void handleValidCallDeliveryFailureRecord(MotechEvent motechEvent) {
        ValidFailedCallReport validFailedCallReport = (ValidFailedCallReport) motechEvent.getParameters().get("0");
        logger.info("Handling OBD invalid call delivery failure records");
        campaignMessageService.processValidCallDeliveryFailureRecords(validFailedCallReport);
    }
}
