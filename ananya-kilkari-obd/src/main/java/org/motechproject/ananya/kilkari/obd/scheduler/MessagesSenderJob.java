package org.motechproject.ananya.kilkari.obd.scheduler;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.obd.domain.OBDSubSlot;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.obd.service.OBDProperties;
import org.motechproject.event.MotechEvent;
import org.motechproject.retry.EventKeys;
import org.motechproject.retry.domain.RetryRequest;
import org.motechproject.retry.service.RetryService;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.CronSchedulableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public abstract class MessagesSenderJob {

    private final String subject;
    private final String retryName;
    private final String retryGroupName;
    private List<OBDSubSlot> subSlots;

    @Autowired
    protected RetryService retryService;
    @Autowired
    protected OBDProperties obdProperties;
    @Autowired
    protected CampaignMessageService campaignMessageService;

    private static final Logger logger = LoggerFactory.getLogger(MessagesSenderJob.class);
    protected static final String SUB_SLOT_KEY = "sub_slot";

    public MessagesSenderJob(String subject, List<OBDSubSlot> subSlots, String retryName, String retryGroupName) {
        this.subject = subject;
        this.subSlots = subSlots;
        this.retryName = retryName;
        this.retryGroupName = retryGroupName;
    }

    protected abstract void sendMessages(OBDSubSlot subSlot);

    public ArrayList<CronSchedulableJob> getCronJobs() {
        ArrayList<CronSchedulableJob> cronJobs = new ArrayList<>();
        for (OBDSubSlot subSlot : subSlots) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(MotechSchedulerService.JOB_ID_KEY, subSlot.getSlotName());
            parameters.put(SUB_SLOT_KEY, subSlot);
            MotechEvent motechEvent = new MotechEvent(subject, parameters);
            cronJobs.add(new CronSchedulableJob(motechEvent, obdProperties.getCronJobExpressionFor(subSlot)));
        }
        return cronJobs;
    }

    protected void scheduleMessagesWithRetry(MotechEvent motechEvent) {
        OBDSubSlot subSlot = (OBDSubSlot) motechEvent.getParameters().get(SUB_SLOT_KEY);
        logger.info(String.format("Handling send %s sub slot messages event", subSlot.getSlotName()));
        RetryRequest retryRequest = new RetryRequest(retryName, UUID.randomUUID().toString(), DateTime.now());
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(SUB_SLOT_KEY, subSlot);
        retryService.schedule(retryRequest, parameters);
    }

    protected void sendMessagesWithRetry(MotechEvent motechEvent) {
        Map<String, Object> parameters = motechEvent.getParameters();
        OBDSubSlot subSlot = (OBDSubSlot) parameters.get(SUB_SLOT_KEY);
        logger.info(String.format("Handling send %s sub slot messages with retry event", subSlot.getSlotName()));

        if (!canSendMessages(obdProperties.getSlotStartTimeLimitFor(subSlot), obdProperties.getSlotEndTimeLimitFor(subSlot))) {
            retryService.fulfill((String) parameters.get(EventKeys.EXTERNAL_ID), retryGroupName);
            return;
        }

        try {
            sendMessages(subSlot);
            retryService.fulfill((String) parameters.get(EventKeys.EXTERNAL_ID), retryGroupName);
        } catch (Exception ex) {
            logger.error(String.format("Error occurred while sending %s sub slot messages to obd.", subSlot.getSlotName()), ex);
        }
    }

    private boolean canSendMessages(DateTime slotStartTimeLimit, DateTime slotEndTimeLimit) {
        DateTime now = DateTime.now();
        DateTime slotStartTime = now.withTime(slotStartTimeLimit.getHourOfDay(), slotStartTimeLimit.getMinuteOfHour(), 0, 0);
        DateTime slotEndTime = now.withTime(slotEndTimeLimit.getHourOfDay(), slotEndTimeLimit.getMinuteOfHour(), 0, 0);
        /**Allowing buffer time of one minute between slots*/
        Integer bufferMinutes = obdProperties.getBufferTime();
        boolean canSendMessages = !now.isBefore(slotStartTime.minusMinutes(bufferMinutes)) && !now.isAfter(slotEndTime.plusMinutes(bufferMinutes));
        if (!canSendMessages) {
            logger.info(String.format("Current Time : %s is not within the slot time limits - %s to %s.", now, slotStartTime, slotEndTime));
        }
        return canSendMessages;
    }
}
