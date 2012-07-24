package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.motechproject.ananya.kilkari.domain.CampaignMessageAlert;
import org.motechproject.ananya.kilkari.mapper.ValidCallDeliveryFailureRecordObjectMapper;
import org.motechproject.ananya.kilkari.domain.CampaignTriggerType;
import org.motechproject.ananya.kilkari.messagecampaign.service.KilkariMessageCampaignService;
import org.motechproject.ananya.kilkari.obd.contract.*;
import org.motechproject.ananya.kilkari.obd.domain.CallDetailRecord;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessage;
import org.motechproject.ananya.kilkari.obd.domain.ServiceOption;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.reporting.domain.CampaignMessageDeliveryReportRequest;
import org.motechproject.ananya.kilkari.reporting.service.ReportingService;
import org.motechproject.ananya.kilkari.repository.AllCampaignMessageAlerts;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallRequestWrapper;
import org.motechproject.ananya.kilkari.subscription.domain.*;
import org.motechproject.ananya.kilkari.subscription.service.KilkariInboxService;
import org.motechproject.ananya.kilkari.utils.CampaignMessageIdStrategy;
import org.motechproject.ananya.kilkari.validators.CallDeliveryFailureRecordValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class KilkariCampaignServiceTest {

    private KilkariCampaignService kilkariCampaignService;

    @Mock
    private KilkariMessageCampaignService kilkariMessageCampaignService;
    @Mock
    private KilkariSubscriptionService kilkariSubscriptionService;
    @Mock
    private AllCampaignMessageAlerts allCampaignMessageAlerts;
    @Mock
    private CampaignMessageIdStrategy campaignMessageIdStrategy;
    @Mock
    private CampaignMessageService campaignMessageService;
    @Mock
    private ReportingService reportingService;
    @Mock
    private OBDRequestPublisher obdRequestPublisher;
    @Mock
    private CallDeliveryFailureRecordValidator callDeliveryFailureRecordValidator;
    @Mock
    private ValidCallDeliveryFailureRecordObjectMapper validCallDeliveryFailureRecordObjectMapper;
    @Mock
    private KilkariInboxService kilkariInboxService;


    @Before
    public void setUp() {
        initMocks(this);
        kilkariCampaignService = new KilkariCampaignService(kilkariMessageCampaignService, kilkariSubscriptionService, campaignMessageIdStrategy, allCampaignMessageAlerts, campaignMessageService, reportingService, obdRequestPublisher, callDeliveryFailureRecordValidator, kilkariInboxService, validCallDeliveryFailureRecordObjectMapper);
    }

    @Test
    public void shouldGetMessageTimings() {
        String msisdn = "1234567890";
        List<Subscription> subscriptions = new ArrayList<>();
        Subscription subscription1 = new Subscription(msisdn, SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        Subscription subscription2 = new Subscription(msisdn, SubscriptionPack.SEVEN_MONTHS, DateTime.now());
        subscriptions.add(subscription1);
        subscriptions.add(subscription2);

        List<DateTime> dateTimes = new ArrayList<>();
        dateTimes.add(DateTime.now());

        when(kilkariSubscriptionService.findByMsisdn(msisdn)).thenReturn(subscriptions);

        when(kilkariMessageCampaignService.getMessageTimings(
                subscription1.getSubscriptionId(),
                subscription1.getPack().name(),
                subscription1.getCreationDate(), subscription1.endDate())).thenReturn(dateTimes);
        when(kilkariMessageCampaignService.getMessageTimings(
                subscription2.getSubscriptionId(),
                subscription2.getPack().name(),
                subscription2.getCreationDate(), subscription2.endDate())).thenReturn(dateTimes);


        Map<String, List<DateTime>> messageTimings = kilkariCampaignService.getMessageTimings(msisdn);

        verify(kilkariMessageCampaignService).getMessageTimings(
                eq(subscription1.getSubscriptionId()),
                eq(subscription1.getPack().name()),
                eq(subscription1.getCreationDate()),
                eq(subscription1.endDate()));

        verify(kilkariMessageCampaignService).getMessageTimings(
                eq(subscription2.getSubscriptionId()),
                eq(subscription2.getPack().name()),
                eq(subscription2.getCreationDate()),
                eq(subscription2.endDate()));

        assertThat(messageTimings.size(), is(2));
        assertThat(messageTimings, hasEntry(subscription1.getSubscriptionId(), dateTimes));
        assertThat(messageTimings, hasEntry(subscription2.getSubscriptionId(), dateTimes));
    }

    @Test
    public void shouldSaveCampaignMessageAlertIfDoesNotExist() {

        String subscriptionId = "mysubscriptionid";
        String messageId = "mymessageid";
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(1));

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(null);
        when(campaignMessageIdStrategy.createMessageId(subscription)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);


        verify(allCampaignMessageAlerts).findBySubscriptionId(subscriptionId);
        ArgumentCaptor<CampaignMessageAlert> campaignMessageAlertArgumentCaptor = ArgumentCaptor.forClass(CampaignMessageAlert.class);
        verify(allCampaignMessageAlerts).add(campaignMessageAlertArgumentCaptor.capture());
        CampaignMessageAlert campaignMessageAlert = campaignMessageAlertArgumentCaptor.getValue();
        assertEquals(subscriptionId, campaignMessageAlert.getSubscriptionId());
        assertEquals(messageId, campaignMessageAlert.getMessageId());
        assertFalse(campaignMessageAlert.isRenewed());

        verifyZeroInteractions(campaignMessageService);
        verify(allCampaignMessageAlerts, never()).remove(any(CampaignMessageAlert.class));
    }

    @Test
    public void shouldAddCampaignMessageAlertOnRenewIfItDoesNotExist() {
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(3));
        String subscriptionId = subscription.getSubscriptionId();

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(null);

        kilkariCampaignService.activateOrRenewSchedule(subscriptionId, CampaignTriggerType.RENEWAL);

        ArgumentCaptor<CampaignMessageAlert> campaignMessageAlertArgumentCaptor = ArgumentCaptor.forClass(CampaignMessageAlert.class);
        verify(allCampaignMessageAlerts).add(campaignMessageAlertArgumentCaptor.capture());
        CampaignMessageAlert value = campaignMessageAlertArgumentCaptor.getValue();

        assertEquals(subscriptionId, value.getSubscriptionId());
        assertNull(value.getMessageId());
        assertTrue(value.isRenewed());
    }

    @Test
    public void shouldRenewCampaignMessageAlertAndScheduleCampaignMessageIfMessageIdExists() {
        String messageId = "mymessageid";
        String msisdn = "1234567890";
        Operator operator = Operator.AIRTEL;
        Subscription mockSubscription = mock(Subscription.class);
        String subscriptionId = "subscriptionId";
        when(mockSubscription.getSubscriptionId()).thenReturn(subscriptionId);
        when(mockSubscription.getMsisdn()).thenReturn(msisdn);
        when(mockSubscription.getOperator()).thenReturn(operator);
        DateTime messageExpiryDate = DateTime.now().plusWeeks(1);
        when(mockSubscription.currentWeeksMessageExpiryDate()).thenReturn(messageExpiryDate);

        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, messageId, true, messageExpiryDate);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(mockSubscription);

        kilkariCampaignService.activateOrRenewSchedule(subscriptionId, CampaignTriggerType.RENEWAL);

        verify(allCampaignMessageAlerts).findBySubscriptionId(subscriptionId);
        verify(campaignMessageService).scheduleCampaignMessage(subscriptionId, messageId, msisdn, operator.name(), messageExpiryDate);
        ArgumentCaptor<CampaignMessageAlert> captor = ArgumentCaptor.forClass(CampaignMessageAlert.class);
        verify(allCampaignMessageAlerts).remove(captor.capture());
        CampaignMessageAlert actualCampaignMessageAlert = captor.getValue();
        assertEquals(subscriptionId, actualCampaignMessageAlert.getSubscriptionId());
    }

    @Test
    public void shouldRemoveCampaignMessageAlertIfAlreadyExistsAndScheduleCampaignMessageIfRenewed() {
        String messageId = "mymessageid";
        String msisdn = "1234567890";
        Operator operator = Operator.AIRTEL;

        Subscription mockSubscription = mock(Subscription.class);
        String subscriptionId = "subscriptionId";
        when(mockSubscription.getSubscriptionId()).thenReturn(subscriptionId);
        when(mockSubscription.getMsisdn()).thenReturn(msisdn);
        when(mockSubscription.getOperator()).thenReturn(operator);
        DateTime messageExpiryDate = DateTime.now().plusWeeks(1);
        when(mockSubscription.currentWeeksMessageExpiryDate()).thenReturn(messageExpiryDate);

        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, "previousMessageId", true, messageExpiryDate);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(mockSubscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);
        when(campaignMessageIdStrategy.createMessageId(mockSubscription)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(allCampaignMessageAlerts).findBySubscriptionId(subscriptionId);
        verify(campaignMessageService).scheduleCampaignMessage(subscriptionId, messageId, msisdn, operator.name(), messageExpiryDate);
        ArgumentCaptor<CampaignMessageAlert> captor = ArgumentCaptor.forClass(CampaignMessageAlert.class);
        verify(allCampaignMessageAlerts).remove(captor.capture());
        CampaignMessageAlert actualCampaignMessageAlert = captor.getValue();
        assertEquals(subscriptionId, actualCampaignMessageAlert.getSubscriptionId());
    }

    @Test
    public void shouldUpdateCampaignMessageAlertIfAlreadyExistsButShouldNotScheduleCampaignMessageIfNotRenewed() {

        String subscriptionId = "mysubscriptionid";
        String messageId = "mymessageid";
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, "previousMessageId", false, DateTime.now().plusWeeks(1));

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);
        when(campaignMessageIdStrategy.createMessageId(subscription)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(allCampaignMessageAlerts).findBySubscriptionId(subscriptionId);

        ArgumentCaptor<CampaignMessageAlert> campaignMessageAlertArgumentCaptor = ArgumentCaptor.forClass(CampaignMessageAlert.class);
        verify(allCampaignMessageAlerts).update(campaignMessageAlertArgumentCaptor.capture());
        CampaignMessageAlert actualCampaignMessageAlert = campaignMessageAlertArgumentCaptor.getValue();
        assertEquals(subscriptionId, actualCampaignMessageAlert.getSubscriptionId());
        assertEquals(messageId, actualCampaignMessageAlert.getMessageId());
        assertFalse(actualCampaignMessageAlert.isRenewed());

        verifyZeroInteractions(campaignMessageService);
        verify(allCampaignMessageAlerts, never()).remove(any(CampaignMessageAlert.class));
    }

    @Test
    public void shouldPublishObdCallbackRequest() {
        OBDSuccessfulCallRequestWrapper successfulCallRequestWrapper = new OBDSuccessfulCallRequestWrapper(new OBDSuccessfulCallRequest(), "subscriptionId", DateTime.now(), Channel.IVR);

        kilkariCampaignService.publishSuccessfulCallRequest(successfulCallRequestWrapper);

        verify(obdRequestPublisher).publishSuccessfulCallRequest(successfulCallRequestWrapper);
    }

    @Test
    public void shouldProcessSuccessfulCampaignMessageDelivery() {
        OBDSuccessfulCallRequest successfulCallRequest = new OBDSuccessfulCallRequest();
        String subscriptionId = "subscriptionId";
        String campaignId = "WEEK1";
        String msisdn = "1234567890";
        Integer retryCount = 3;
        String serviceOption = ServiceOption.HELP.name();
        String startTime = "25-12-2012";
        String endTime = "27-12-2012";
        CallDetailRecord callDetailRecord = new CallDetailRecord();
        callDetailRecord.setStartTime(startTime);
        callDetailRecord.setEndTime(endTime);

        successfulCallRequest.setMsisdn(msisdn);
        successfulCallRequest.setCampaignId(campaignId);
        successfulCallRequest.setServiceOption(serviceOption);
        successfulCallRequest.setCallDetailRecord(callDetailRecord);
        OBDSuccessfulCallRequestWrapper successfulCallRequestWrapper = new OBDSuccessfulCallRequestWrapper(successfulCallRequest, subscriptionId, DateTime.now(), Channel.IVR);
        CampaignMessage campaignMessage = mock(CampaignMessage.class);
        when(campaignMessage.getDnpRetryCount()).thenReturn(retryCount);
        when(campaignMessageService.find(subscriptionId, campaignId)).thenReturn(campaignMessage);

        kilkariCampaignService.processSuccessfulMessageDelivery(successfulCallRequestWrapper);

        InOrder inOrder = Mockito.inOrder(campaignMessageService, reportingService);
        inOrder.verify(campaignMessageService).find(subscriptionId, campaignId);

        ArgumentCaptor<CampaignMessageDeliveryReportRequest> campaignMessageDeliveryReportRequestArgumentCaptor = ArgumentCaptor.forClass(CampaignMessageDeliveryReportRequest.class);
        inOrder.verify(reportingService).reportCampaignMessageDeliveryStatus(campaignMessageDeliveryReportRequestArgumentCaptor.capture());
        CampaignMessageDeliveryReportRequest campaignMessageDeliveryReportRequest = campaignMessageDeliveryReportRequestArgumentCaptor.getValue();

        inOrder.verify(campaignMessageService).deleteCampaignMessage(campaignMessage);

        assertEquals(subscriptionId, campaignMessageDeliveryReportRequest.getSubscriptionId());
        assertEquals(msisdn, campaignMessageDeliveryReportRequest.getMsisdn());
        assertEquals(campaignId, campaignMessageDeliveryReportRequest.getCampaignId());
        assertEquals(retryCount.toString(), campaignMessageDeliveryReportRequest.getRetryCount());
        assertEquals(serviceOption, campaignMessageDeliveryReportRequest.getServiceOption());
        assertEquals(startTime, campaignMessageDeliveryReportRequest.getCallDetailRecord().getStartTime());
        assertEquals(endTime, campaignMessageDeliveryReportRequest.getCallDetailRecord().getEndTime());
    }

    @Test
    public void shouldNotProcessSuccessfulCampaignMessageDeliveryIfThereIsNoSubscriptionAvailable() {
        OBDSuccessfulCallRequest successfulCallRequest = new OBDSuccessfulCallRequest();
        String subscriptionId = "subscriptionId";
        String campaignId = "WEEK1";
        String msisdn = "1234567890";
        String serviceOption = ServiceOption.HELP.name();
        String startTime = "25-12-2012";
        String endTime = "27-12-2012";
        CallDetailRecord callDetailRecord = new CallDetailRecord();
        callDetailRecord.setStartTime(startTime);
        callDetailRecord.setEndTime(endTime);

        successfulCallRequest.setMsisdn(msisdn);
        successfulCallRequest.setCampaignId(campaignId);
        successfulCallRequest.setServiceOption(serviceOption);
        successfulCallRequest.setCallDetailRecord(callDetailRecord);
        OBDSuccessfulCallRequestWrapper successfulCallRequestWrapper = new OBDSuccessfulCallRequestWrapper(successfulCallRequest, subscriptionId, DateTime.now(), Channel.IVR);
        when(campaignMessageService.find(subscriptionId, campaignId)).thenReturn(null);

        kilkariCampaignService.processSuccessfulMessageDelivery(successfulCallRequestWrapper);

        verify(campaignMessageService).find(subscriptionId, campaignId);
        verify(campaignMessageService, never()).deleteCampaignMessage(any(CampaignMessage.class));
        verifyZeroInteractions(reportingService);
    }

    @Test
    public void shouldProcessInvalidCallRecords() {
        InvalidCallRecordsRequest invalidCallRecordsRequest = new InvalidCallRecordsRequest();

        kilkariCampaignService.publishInvalidCallRecordsRequest(invalidCallRecordsRequest);

        verify(obdRequestPublisher).publishInvalidCallRecordsRequest(invalidCallRecordsRequest);
    }

    @Test
    public void shouldScheduleUnsubscriptionWhenPackIsCompletedWhenCampaignAlertDoesNotExist() {
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(59));
        String subscriptionId = subscription.getSubscriptionId();

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(null);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(kilkariSubscriptionService).processSubscriptionCompletion(subscription);
        verify(allCampaignMessageAlerts).add(any(CampaignMessageAlert.class));
    }

    @Test
    public void shouldScheduleUnsubscriptionWhenPackIsCompletedWhenCampaignAlertDoesExist() {
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(59));
        String subscriptionId = subscription.getSubscriptionId();
        CampaignMessageAlert mockedCampaignMessageAlert = mock(CampaignMessageAlert.class);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(mockedCampaignMessageAlert);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(kilkariSubscriptionService).processSubscriptionCompletion(subscription);
    }

    @Test
    public void shouldPublishCallDeliveryFailureRecords() {
        CallDeliveryFailureRecord callDeliveryFailureRecord = Mockito.mock(CallDeliveryFailureRecord.class);
        kilkariCampaignService.publishCallDeliveryFailureRequest(callDeliveryFailureRecord);
        verify(obdRequestPublisher).publishCallDeliveryFailureRecord(callDeliveryFailureRecord);
    }

    @Test
    public void shouldAssignAExpiryDateToWeeklyScheduledMessageWhichIsNWeeksFromCreationDate_whenRenewalHasHappened() {
        DateTime createdAt = DateTime.now().minusWeeks(2);
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.SEVEN_MONTHS, createdAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        String subscriptionId = subscription.getSubscriptionId();
        String messageId = "week2";
        CampaignMessageAlert mockedCampaignMessageAlert = mock(CampaignMessageAlert.class);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(mockedCampaignMessageAlert);
        when(campaignMessageIdStrategy.createMessageId(subscription)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(mockedCampaignMessageAlert).updateWith(messageId, false, createdAt.plusWeeks(3));
    }

    @Test
    public void shouldAssignAExpiryDateToWeeklyScheduledMessageWhichIsNWeeksFromCreationDate_WhenRenewalHasNotHappened() {
        DateTime createdAt = DateTime.now().minusWeeks(2);
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.SEVEN_MONTHS, createdAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        String subscriptionId = subscription.getSubscriptionId();

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(null);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        ArgumentCaptor<CampaignMessageAlert> campaignMessageAlertArgumentCaptor = ArgumentCaptor.forClass(CampaignMessageAlert.class);
        verify(allCampaignMessageAlerts).add(campaignMessageAlertArgumentCaptor.capture());

        CampaignMessageAlert campaignMessageAlertArgumentCaptorValue = campaignMessageAlertArgumentCaptor.getValue();
        assertNotNull(campaignMessageAlertArgumentCaptorValue.getMessageExpiryDate());
        assertEquals(createdAt.plusWeeks(3), campaignMessageAlertArgumentCaptorValue.getMessageExpiryDate());
    }

    @Test
    public void shouldNotUpdateExpiryDateWhenTryingToScheduleViaRenewal_whenCampaignMessageAlertAlreadyExist() {
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(3));
        subscription.setOperator(Operator.AIRTEL);
        String subscriptionId = subscription.getSubscriptionId();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        String messageId = "week2";
        CampaignMessageAlert mockedCampaignMessageAlert = mock(CampaignMessageAlert.class);
        DateTime actualExpiryDate = DateTime.now().plusWeeks(1);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(mockedCampaignMessageAlert);
        when(mockedCampaignMessageAlert.getMessageId()).thenReturn(messageId);
        when(mockedCampaignMessageAlert.getMessageExpiryDate()).thenReturn(actualExpiryDate);

        kilkariCampaignService.activateOrRenewSchedule(subscriptionId, CampaignTriggerType.RENEWAL);

        verify(mockedCampaignMessageAlert).updateWith(messageId, true, actualExpiryDate);
    }

    @Test
    public void shouldNotSetExpiryDateWhenTryingToScheduleViaRenewal_whenCampaignMessageAlertDoesNotAlreadyExist() {
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(3));
        subscription.setOperator(Operator.AIRTEL);
        String subscriptionId = subscription.getSubscriptionId();
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(null);

        kilkariCampaignService.activateOrRenewSchedule(subscriptionId, CampaignTriggerType.RENEWAL);

        ArgumentCaptor<CampaignMessageAlert> campaignMessageAlertArgumentCaptor = ArgumentCaptor.forClass(CampaignMessageAlert.class);
        verify(allCampaignMessageAlerts).add(campaignMessageAlertArgumentCaptor.capture());

        CampaignMessageAlert campaignMessageAlertArgumentCaptorValue = campaignMessageAlertArgumentCaptor.getValue();
        assertNull(campaignMessageAlertArgumentCaptorValue.getMessageExpiryDate());
    }

    @Test
    public void shouldHandleCallDeliveryFailureRecord() {
        CallDeliveryFailureRecord callDeliveryFailureRecord = new CallDeliveryFailureRecord();

        ArrayList<CallDeliveryFailureRecordObject> callDeliveryFailureRecordObjects = new ArrayList<>();
        callDeliveryFailureRecordObjects.add(mock(CallDeliveryFailureRecordObject.class));
        callDeliveryFailureRecord.setCallDeliveryFailureRecordObjects(callDeliveryFailureRecordObjects);


        kilkariCampaignService.processCallDeliveryFailureRecord(callDeliveryFailureRecord);

        verify(callDeliveryFailureRecordValidator, times(1)).validate(any(CallDeliveryFailureRecordObject.class));
    }

    @Test
    public void shouldPublishErroredOutCallDeliveryFailureRecords() {
        String msisdn = "12345";
        String subscriptionId = "abcd";
        CallDeliveryFailureRecord callDeliveryFailureRecord = new CallDeliveryFailureRecord();

        ArrayList<CallDeliveryFailureRecordObject> callDeliveryFailureRecordObjects = new ArrayList<>();
        CallDeliveryFailureRecordObject erroredOutCallDeliveryFailureRecordObject = mock(CallDeliveryFailureRecordObject.class);
        when(erroredOutCallDeliveryFailureRecordObject.getMsisdn()).thenReturn(msisdn);
        when(erroredOutCallDeliveryFailureRecordObject.getSubscriptionId()).thenReturn(subscriptionId);
        CallDeliveryFailureRecordObject successfulCallDeliveryFailureRecordObject = mock(CallDeliveryFailureRecordObject.class);
        callDeliveryFailureRecordObjects.add(erroredOutCallDeliveryFailureRecordObject);
        callDeliveryFailureRecordObjects.add(successfulCallDeliveryFailureRecordObject);
        callDeliveryFailureRecord.setCallDeliveryFailureRecordObjects(callDeliveryFailureRecordObjects);

        when(callDeliveryFailureRecordValidator.validate(successfulCallDeliveryFailureRecordObject)).thenReturn(new ArrayList<String>());

        ArrayList<String> errors = new ArrayList<>();
        errors.add("Some error description");
        when(callDeliveryFailureRecordValidator.validate(erroredOutCallDeliveryFailureRecordObject)).thenReturn(errors);

        kilkariCampaignService.processCallDeliveryFailureRecord(callDeliveryFailureRecord);

        verify(callDeliveryFailureRecordValidator, times(2)).validate(any(CallDeliveryFailureRecordObject.class));

        ArgumentCaptor<InvalidCallDeliveryFailureRecord> invalidCallDeliveryFailureRecordArgumentCaptor = ArgumentCaptor.forClass(InvalidCallDeliveryFailureRecord.class);
        verify(obdRequestPublisher).publishInvalidCallDeliveryFailureRecord(invalidCallDeliveryFailureRecordArgumentCaptor.capture());
        InvalidCallDeliveryFailureRecord invalidCallDeliveryFailureRecord = invalidCallDeliveryFailureRecordArgumentCaptor.getValue();
        List<InvalidCallDeliveryFailureRecordObject> recordObjects = invalidCallDeliveryFailureRecord.getRecordObjects();

        assertEquals(1, recordObjects.size());
        assertEquals("Some error description", recordObjects.get(0).getDescription());
        assertEquals(msisdn, recordObjects.get(0).getMsisdn());
        assertEquals(subscriptionId, recordObjects.get(0).getSubscriptionId());
    }

    @Test
    public void shouldPublishSuccessfulCallDeliveryFailureRecords() {
        CallDeliveryFailureRecord callDeliveryFailureRecord = new CallDeliveryFailureRecord();

        ArrayList<CallDeliveryFailureRecordObject> callDeliveryFailureRecordObjects = new ArrayList<>();
        CallDeliveryFailureRecordObject erroredOutCallDeliveryFailureRecordObject = mock(CallDeliveryFailureRecordObject.class);
        CallDeliveryFailureRecordObject successfulCallDeliveryFailureRecordObject1 = mock(CallDeliveryFailureRecordObject.class);
        CallDeliveryFailureRecordObject successfulCallDeliveryFailureRecordObject2 = mock(CallDeliveryFailureRecordObject.class);
        callDeliveryFailureRecordObjects.add(erroredOutCallDeliveryFailureRecordObject);
        callDeliveryFailureRecordObjects.add(successfulCallDeliveryFailureRecordObject1);
        callDeliveryFailureRecordObjects.add(successfulCallDeliveryFailureRecordObject2);
        callDeliveryFailureRecord.setCallDeliveryFailureRecordObjects(callDeliveryFailureRecordObjects);

        when(callDeliveryFailureRecordValidator.validate(successfulCallDeliveryFailureRecordObject1)).thenReturn(new ArrayList<String>());
        when(callDeliveryFailureRecordValidator.validate(successfulCallDeliveryFailureRecordObject2)).thenReturn(new ArrayList<String>());

        ValidCallDeliveryFailureRecordObject validCallDeliveryFailureRecordObject1 = mock(ValidCallDeliveryFailureRecordObject.class);
        ValidCallDeliveryFailureRecordObject validCallDeliveryFailureRecordObject2 = mock(ValidCallDeliveryFailureRecordObject.class);
        when(validCallDeliveryFailureRecordObjectMapper.mapFrom(successfulCallDeliveryFailureRecordObject1, callDeliveryFailureRecord)).thenReturn(validCallDeliveryFailureRecordObject1);
        when(validCallDeliveryFailureRecordObjectMapper.mapFrom(successfulCallDeliveryFailureRecordObject2, callDeliveryFailureRecord)).thenReturn(validCallDeliveryFailureRecordObject2);

        ArrayList<String> errors = new ArrayList<>();
        errors.add("Some error description");
        when(callDeliveryFailureRecordValidator.validate(erroredOutCallDeliveryFailureRecordObject)).thenReturn(errors);

        kilkariCampaignService.processCallDeliveryFailureRecord(callDeliveryFailureRecord);

        verify(callDeliveryFailureRecordValidator, times(3)).validate(any(CallDeliveryFailureRecordObject.class));

        verify(obdRequestPublisher).publishValidCallDeliveryFailureRecord(validCallDeliveryFailureRecordObject1);
        verify(obdRequestPublisher).publishValidCallDeliveryFailureRecord(validCallDeliveryFailureRecordObject2);
    }

    @Test
    public void shouldNotPublishToErrorQueueIfErroredOutCallDeliveryFailureRecordsAreEmpty() {
        CallDeliveryFailureRecord callDeliveryFailureRecord = new CallDeliveryFailureRecord();

        ArrayList<CallDeliveryFailureRecordObject> callDeliveryFailureRecordObjects = new ArrayList<>();
        CallDeliveryFailureRecordObject successfulCallDeliveryFailureRecordObject = mock(CallDeliveryFailureRecordObject.class);
        callDeliveryFailureRecordObjects.add(successfulCallDeliveryFailureRecordObject);
        callDeliveryFailureRecord.setCallDeliveryFailureRecordObjects(callDeliveryFailureRecordObjects);

        when(callDeliveryFailureRecordValidator.validate(successfulCallDeliveryFailureRecordObject)).thenReturn(new ArrayList<String>());

        kilkariCampaignService.processCallDeliveryFailureRecord(callDeliveryFailureRecord);

        verify(callDeliveryFailureRecordValidator, times(1)).validate(any(CallDeliveryFailureRecordObject.class));
        verify(obdRequestPublisher, never()).publishInvalidCallDeliveryFailureRecord(any(InvalidCallDeliveryFailureRecord.class));
    }

    @Test
    public void shouldUpdateInboxToHoldLastScheduledMessage(){
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        String subscriptionId = subscription.getSubscriptionId();
        String messageId = "week10";

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(campaignMessageIdStrategy.createMessageId(subscription)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(kilkariInboxService).newMessage(subscriptionId, messageId);
    }

    @Test
    public void shouldNotUpdateInboxWhenSubscriptionIsNotActive(){
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        subscription.setStatus(SubscriptionStatus.PENDING_ACTIVATION);
        String subscriptionId = subscription.getSubscriptionId();
        String messageId = "week10";

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(campaignMessageIdStrategy.createMessageId(subscription)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(kilkariInboxService, never()).newMessage(subscriptionId, messageId);
    }

    @Test
    public void shouldNotUpdateInboxDuringRenewal(){
        Subscription subscription = new Subscription("1234567890", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        String subscriptionId = subscription.getSubscriptionId();
        subscription.setOperator(Operator.AIRTEL);
        String messageId = "WEEK13";
        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, messageId, false, DateTime.now().plusWeeks(1));

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);

        kilkariCampaignService.activateOrRenewSchedule(subscriptionId, CampaignTriggerType.RENEWAL);

        verify(kilkariInboxService, never()).newMessage(subscriptionId, messageId);
    }

    @Test
    public void shouldNotUpdateInboxWhenMessageHasNotAlreadyBeenScheduled(){
        Subscription subscription = new Subscription("1234567890", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        String subscriptionId = subscription.getSubscriptionId();
        subscription.setOperator(Operator.AIRTEL);
        CampaignMessageAlert campaignMessageAlert = null;

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);

        kilkariCampaignService.activateOrRenewSchedule(subscriptionId, CampaignTriggerType.ACTIVATION);

        verify(kilkariInboxService, never()).newMessage(anyString(), anyString());
    }

}