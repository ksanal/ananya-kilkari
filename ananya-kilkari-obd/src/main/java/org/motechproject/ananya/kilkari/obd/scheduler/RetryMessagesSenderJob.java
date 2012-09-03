package org.motechproject.ananya.kilkari.obd.scheduler;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.obd.service.OBDProperties;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.annotations.MotechListener;
import org.motechproject.retry.EventKeys;
import org.motechproject.retry.domain.RetryRequest;
import org.motechproject.retry.service.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class RetryMessagesSenderJob extends MessagesSenderJob {

    private static final String SLOT_EVENT_SUBJECT = "obd.send.retry.messages";
    private static final String RETRY_EVENT_SUBJECT = "obd.send.retry.messages.with.retry";

    private static final String RETRY_GROUP_NAME = "obd-send-retry-messages-group";
    private static final String RETRY_NAME = "obd-send-retry-messages";

    private static final Logger logger = LoggerFactory.getLogger(RetryMessagesSenderJob.class);
    private CampaignMessageService campaignMessageService;
    private RetryService retryService;
    private OBDProperties obdProperties;

    @Autowired
    public RetryMessagesSenderJob(CampaignMessageService campaignMessageService, RetryService retryService, OBDProperties obdProperties) {
        super(RetryMessagesSenderJob.SLOT_EVENT_SUBJECT, obdProperties.getRetryMessageJobCronExpression());
        this.campaignMessageService = campaignMessageService;
        this.retryService = retryService;
        this.obdProperties = obdProperties;
    }

    @MotechListener(subjects = {RetryMessagesSenderJob.SLOT_EVENT_SUBJECT})
    public void sendMessages(MotechEvent motechEvent) {
        logger.info("Handling send retry messages event");
        RetryRequest retryRequest = new RetryRequest(RetryMessagesSenderJob.RETRY_NAME, UUID.randomUUID().toString(), DateTime.now());
        retryService.schedule(retryRequest);
    }

    @MotechListener(subjects = {RetryMessagesSenderJob.RETRY_EVENT_SUBJECT})
    public void sendMessagesWithRetry(MotechEvent motechEvent) {
        logger.info("Handling send retry messages with retry event");
        Map<String, Object> parameters = motechEvent.getParameters();

        if (!canSendMessages(obdProperties.getRetryMessageStartTimeLimitHours(), obdProperties.getRetryMessageStartTimeLimitMinute())){
            retryService.fulfill((String) parameters.get(EventKeys.EXTERNAL_ID), RetryMessagesSenderJob.RETRY_GROUP_NAME);
            return;
        }

        try {
            campaignMessageService.sendRetryMessages();
            retryService.fulfill((String) parameters.get(EventKeys.EXTERNAL_ID), RetryMessagesSenderJob.RETRY_GROUP_NAME);
        } catch (Exception ex) {
            logger.error("Error occurred while sending retry messages to obd.", ex);
        }
    }
}