package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.motechproject.ananya.kilkari.domain.CampaignMessageAlert;
import org.motechproject.ananya.kilkari.domain.CampaignTriggerType;
import org.motechproject.ananya.kilkari.factory.OBDServiceOptionFactory;
import org.motechproject.ananya.kilkari.handlers.callback.obd.ServiceOptionHandler;
import org.motechproject.ananya.kilkari.messagecampaign.service.MessageCampaignService;
import org.motechproject.ananya.kilkari.obd.domain.CallDetailRecord;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessage;
import org.motechproject.ananya.kilkari.obd.domain.ServiceOption;
import org.motechproject.ananya.kilkari.obd.domain.ValidFailedCallReport;
import org.motechproject.ananya.kilkari.obd.request.*;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.reporting.domain.CampaignMessageDeliveryReportRequest;
import org.motechproject.ananya.kilkari.reporting.service.ReportingService;
import org.motechproject.ananya.kilkari.repository.AllCampaignMessageAlerts;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallRequestWrapper;
import org.motechproject.ananya.kilkari.subscription.builder.SubscriptionBuilder;
import org.motechproject.ananya.kilkari.subscription.domain.*;
import org.motechproject.ananya.kilkari.subscription.service.KilkariInboxService;
import org.motechproject.ananya.kilkari.subscription.service.response.SubscriptionResponse;
import org.motechproject.ananya.kilkari.subscription.validators.Errors;
import org.motechproject.ananya.kilkari.utils.CampaignMessageIdStrategy;
import org.motechproject.ananya.kilkari.validators.CallDeliveryFailureRecordValidator;
import org.motechproject.ananya.kilkari.validators.OBDSuccessfulCallRequestValidator;

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
    private MessageCampaignService messageCampaignService;
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
    private KilkariInboxService kilkariInboxService;
    @Mock
    private OBDServiceOptionFactory obdServiceOptionFactory;
    @Mock
    private ServiceOptionHandler serviceOptionHandler;
    @Mock
    private OBDSuccessfulCallRequestValidator successfulCallRequestValidator;


    @Before
    public void setUp() {
        initMocks(this);
        kilkariCampaignService = new KilkariCampaignService(messageCampaignService, kilkariSubscriptionService, campaignMessageIdStrategy, allCampaignMessageAlerts, campaignMessageService, reportingService, obdRequestPublisher, callDeliveryFailureRecordValidator, kilkariInboxService, obdServiceOptionFactory, successfulCallRequestValidator);
    }

    @Test
    public void shouldGetMessageTimings() {
        String msisdn = "1234567890";
        List<SubscriptionResponse> subscriptionResponses = new ArrayList<>();

        SubscriptionResponse subscriptionResponse1 = new SubscriptionBuilder().withDefaults().withCreationDate(DateTime.now()).withStatus(SubscriptionStatus.PENDING_ACTIVATION).build();
        SubscriptionResponse subscriptionResponse2 = new SubscriptionBuilder().withDefaults().withCreationDate(DateTime.now()).withStatus(SubscriptionStatus.PENDING_ACTIVATION).build();
        subscriptionResponses.add(subscriptionResponse1);
        subscriptionResponses.add(subscriptionResponse2);

        List<DateTime> dateTimes = new ArrayList<>();
        dateTimes.add(DateTime.now());

        when(kilkariSubscriptionService.findByMsisdn(msisdn)).thenReturn(subscriptionResponses);

        when(messageCampaignService.getMessageTimings(
                subscriptionResponse1.getSubscriptionId(),
                subscriptionResponse1.getCreationDate(),
                subscriptionResponse1.endDate())).thenReturn(dateTimes);
        when(messageCampaignService.getMessageTimings(
                subscriptionResponse2.getSubscriptionId(),
                subscriptionResponse2.getCreationDate(),
                subscriptionResponse2.endDate())).thenReturn(dateTimes);

        Map<String, List<DateTime>> messageTimings = kilkariCampaignService.getMessageTimings(msisdn);

        verify(messageCampaignService).getMessageTimings(
                eq(subscriptionResponse1.getSubscriptionId()),
                eq(subscriptionResponse1.getCreationDate()),
                eq(subscriptionResponse1.endDate()));

        verify(messageCampaignService).getMessageTimings(
                eq(subscriptionResponse2.getSubscriptionId()),
                eq(subscriptionResponse2.getCreationDate()),
                eq(subscriptionResponse2.endDate()));

        assertThat(messageTimings.size(), is(2));
        assertThat(messageTimings, hasEntry(subscriptionResponse1.getSubscriptionId(), dateTimes));
        assertThat(messageTimings, hasEntry(subscriptionResponse2.getSubscriptionId(), dateTimes));
    }

    @Test
    public void shouldSaveCampaignMessageAlertIfDoesNotExist() {
        String subscriptionId = "mysubscriptionid";
        String messageId = "mymessageid";
        String campaignName = "campaignName";
        DateTime campaignCreatedDate = DateTime.now();
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(1));

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(null);
        when(messageCampaignService.getCampaignStartDate(subscriptionId, campaignName)).thenReturn(campaignCreatedDate);
        when(campaignMessageIdStrategy.createMessageId(campaignName, campaignCreatedDate, subscription.getPack())).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId, campaignName);


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
        String campaignName = "campaignName";
        DateTime creationDate = DateTime.now();
        SubscriptionPack subscriptionPack = SubscriptionPack.FIFTEEN_MONTHS;

        Subscription mockSubscription = mock(Subscription.class);
        String subscriptionId = "subscriptionId";
        when(mockSubscription.getSubscriptionId()).thenReturn(subscriptionId);
        when(mockSubscription.getMsisdn()).thenReturn(msisdn);
        when(mockSubscription.getOperator()).thenReturn(operator);
        when(mockSubscription.getPack()).thenReturn(subscriptionPack);
        DateTime messageExpiryDate = creationDate.plusWeeks(1);
        when(mockSubscription.currentWeeksMessageExpiryDate()).thenReturn(messageExpiryDate);

        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, "previousMessageId", true, messageExpiryDate);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(mockSubscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);
        when(messageCampaignService.getCampaignStartDate(subscriptionId, campaignName)).thenReturn(creationDate);
        when(campaignMessageIdStrategy.createMessageId(campaignName, creationDate, subscriptionPack)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId, campaignName);

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
        String campaignName = "campaignName";
        DateTime creationDate = DateTime.now();
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, creationDate);
        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, "previousMessageId", false, creationDate.plusWeeks(1));

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);
        when(messageCampaignService.getCampaignStartDate(subscriptionId, campaignName)).thenReturn(creationDate);
        when(campaignMessageIdStrategy.createMessageId(campaignName, creationDate, subscription.getPack())).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId, campaignName);

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

        when(successfulCallRequestValidator.validate(successfulCallRequestWrapper)).thenReturn(new Errors());
        when(obdServiceOptionFactory.getHandler(ServiceOption.HELP)).thenReturn(serviceOptionHandler);

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

        verify(serviceOptionHandler).process(successfulCallRequestWrapper);
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

        when(successfulCallRequestValidator.validate(successfulCallRequestWrapper)).thenReturn(new Errors());

        kilkariCampaignService.processSuccessfulMessageDelivery(successfulCallRequestWrapper);

        verify(campaignMessageService).find(subscriptionId, campaignId);
        verify(campaignMessageService, never()).deleteCampaignMessage(any(CampaignMessage.class));
        verifyZeroInteractions(reportingService);
    }

    @Test
    public void shouldProcessInvalidCallRecords() {
        InvalidOBDRequestEntries invalidOBDRequestEntries = new InvalidOBDRequestEntries();

        kilkariCampaignService.publishInvalidCallRecordsRequest(invalidOBDRequestEntries);

        verify(obdRequestPublisher).publishInvalidCallRecordsRequest(invalidOBDRequestEntries);
    }

    @Test
    public void shouldScheduleUnsubscriptionWhenPackIsCompletedAndWhenStatusIsNotDeactivated() {
        String subscriptionId = "abcd1234";
        String campaignName = "campaignName";
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(1));
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);

        kilkariCampaignService.processCampaignCompletion(subscriptionId);

        verify(kilkariSubscriptionService).processSubscriptionCompletion(subscription);
    }

    @Test
    public void shouldNotScheduleUnsubscriptionWhenPackIsCompletedAndStatusIsDeactivated() {
        String subscriptionId = "abcd1234";
        String campaignName = "campaignName";
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now().minusWeeks(1));
        subscription.setStatus(SubscriptionStatus.PENDING_DEACTIVATION);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);

        kilkariCampaignService.processCampaignCompletion(subscriptionId);

        verify(kilkariSubscriptionService, never()).processSubscriptionCompletion(subscription);
    }

    @Test
    public void shouldPublishCallDeliveryFailureRecords() {
        FailedCallReports failedCallReports = Mockito.mock(FailedCallReports.class);
        kilkariCampaignService.publishCallDeliveryFailureRequest(failedCallReports);
        verify(obdRequestPublisher).publishCallDeliveryFailureRecord(failedCallReports);
    }

    @Test
    public void shouldAssignAExpiryDateToWeeklyScheduledMessageWhichIsNWeeksFromCreationDate_whenRenewalHasHappened() {
        DateTime createdAt = DateTime.now().minusWeeks(2);
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.SEVEN_MONTHS, createdAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        String subscriptionId = subscription.getSubscriptionId();
        String messageId = "week2";
        String campaignName = "campaignName";
        CampaignMessageAlert mockedCampaignMessageAlert = mock(CampaignMessageAlert.class);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(mockedCampaignMessageAlert);
        when(messageCampaignService.getCampaignStartDate(subscriptionId, campaignName)).thenReturn(createdAt);
        when(campaignMessageIdStrategy.createMessageId(campaignName, createdAt, subscription.getPack())).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId, campaignName);

        verify(mockedCampaignMessageAlert).updateWith(messageId, false, createdAt.plusWeeks(3));
    }

    @Test
    public void shouldAssignAExpiryDateToWeeklyScheduledMessageWhichIsNWeeksFromCreationDate_WhenRenewalHasNotHappened() {
        DateTime createdAt = DateTime.now().minusWeeks(2);
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.SEVEN_MONTHS, createdAt);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        String subscriptionId = subscription.getSubscriptionId();
        String campaignName = "campaignName";

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(null);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId, campaignName);

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
        FailedCallReports failedCallReports = new FailedCallReports();

        ArrayList<FailedCallReport> reportArrayList = new ArrayList<>();
        FailedCallReport failedCallReport = mock(FailedCallReport.class);
        reportArrayList.add(failedCallReport);
        failedCallReports.setFailedCallReports(reportArrayList);

        when(callDeliveryFailureRecordValidator.validate(failedCallReport)).thenReturn(new Errors());

        kilkariCampaignService.processCallDeliveryFailureRecord(failedCallReports);

        verify(callDeliveryFailureRecordValidator, times(1)).validate(any(FailedCallReport.class));
    }

    @Test
    public void shouldPublishErroredOutCallDeliveryFailureRecords() {
        String msisdn = "12345";
        String subscriptionId = "abcd";
        FailedCallReports failedCallReports = new FailedCallReports();

        ArrayList<FailedCallReport> callDeliveryFailureRecordObjects = new ArrayList<>();
        FailedCallReport erroredOutFailedCallReport = mock(FailedCallReport.class);
        when(erroredOutFailedCallReport.getMsisdn()).thenReturn(msisdn);
        when(erroredOutFailedCallReport.getSubscriptionId()).thenReturn(subscriptionId);
        FailedCallReport successfulFailedCallReport = mock(FailedCallReport.class);
        callDeliveryFailureRecordObjects.add(erroredOutFailedCallReport);
        callDeliveryFailureRecordObjects.add(successfulFailedCallReport);
        failedCallReports.setFailedCallReports(callDeliveryFailureRecordObjects);

        when(callDeliveryFailureRecordValidator.validate(successfulFailedCallReport)).thenReturn(new Errors());

        Errors errors = new Errors();
        errors.add("Some error description");
        when(callDeliveryFailureRecordValidator.validate(erroredOutFailedCallReport)).thenReturn(errors);

        kilkariCampaignService.processCallDeliveryFailureRecord(failedCallReports);

        verify(callDeliveryFailureRecordValidator, times(2)).validate(any(FailedCallReport.class));

        ArgumentCaptor<InvalidFailedCallReports> invalidCallDeliveryFailureRecordArgumentCaptor = ArgumentCaptor.forClass(InvalidFailedCallReports.class);
        verify(obdRequestPublisher).publishInvalidCallDeliveryFailureRecord(invalidCallDeliveryFailureRecordArgumentCaptor.capture());
        InvalidFailedCallReports invalidFailedCallReports = invalidCallDeliveryFailureRecordArgumentCaptor.getValue();
        List<InvalidFailedCallReport> recordObjectFaileds = invalidFailedCallReports.getRecordObjectFaileds();

        assertEquals(1, recordObjectFaileds.size());
        assertEquals("Some error description", recordObjectFaileds.get(0).getDescription());
        assertEquals(msisdn, recordObjectFaileds.get(0).getMsisdn());
        assertEquals(subscriptionId, recordObjectFaileds.get(0).getSubscriptionId());
    }

    @Test
    public void shouldPublishSuccessfulCallDeliveryFailureRecords() {
        FailedCallReports failedCallReports = new FailedCallReports();

        ArrayList<FailedCallReport> callDeliveryFailureRecordObjects = new ArrayList<>();
        FailedCallReport erroredOutFailedCallReport = mock(FailedCallReport.class);
        FailedCallReport successfulFailedCallReport1 = new FailedCallReport("sub1","1234567890","WEEK13","ACTIVE");
        FailedCallReport successfulFailedCallReport2 = new FailedCallReport("sub2","1234567891","WEEK13","ACTIVE");
        callDeliveryFailureRecordObjects.add(erroredOutFailedCallReport);
        callDeliveryFailureRecordObjects.add(successfulFailedCallReport1);
        callDeliveryFailureRecordObjects.add(successfulFailedCallReport2);
        failedCallReports.setFailedCallReports(callDeliveryFailureRecordObjects);

        Errors errors = new Errors();
        errors.add("Some error description");
        when(callDeliveryFailureRecordValidator.validate(erroredOutFailedCallReport)).thenReturn(errors);
        Errors noError = new Errors();
        when(callDeliveryFailureRecordValidator.validate(successfulFailedCallReport1)).thenReturn(noError);
        when(callDeliveryFailureRecordValidator.validate(successfulFailedCallReport2)).thenReturn(noError);

        kilkariCampaignService.processCallDeliveryFailureRecord(failedCallReports);

        verify(callDeliveryFailureRecordValidator, times(3)).validate(any(FailedCallReport.class));

        ArgumentCaptor<ValidFailedCallReport> captor = ArgumentCaptor.forClass(ValidFailedCallReport.class);
        verify(obdRequestPublisher, times(2)).publishValidCallDeliveryFailureRecord(captor.capture());
        List<ValidFailedCallReport> actualValidFailedCallReports = captor.getAllValues();
        assertEquals("1234567890",actualValidFailedCallReports.get(0).getMsisdn());
        assertEquals("1234567891", actualValidFailedCallReports.get(1).getMsisdn());
    }

    @Test
    public void shouldNotPublishToErrorQueueIfErroredOutCallDeliveryFailureRecordsAreEmpty() {
        FailedCallReports failedCallReports = new FailedCallReports();

        ArrayList<FailedCallReport> callDeliveryFailureRecordObjects = new ArrayList<>();
        FailedCallReport successfulFailedCallReport = mock(FailedCallReport.class);
        callDeliveryFailureRecordObjects.add(successfulFailedCallReport);
        failedCallReports.setFailedCallReports(callDeliveryFailureRecordObjects);

        when(callDeliveryFailureRecordValidator.validate(successfulFailedCallReport)).thenReturn(new Errors());

        kilkariCampaignService.processCallDeliveryFailureRecord(failedCallReports);

        verify(callDeliveryFailureRecordValidator, times(1)).validate(any(FailedCallReport.class));
        verify(obdRequestPublisher, never()).publishInvalidCallDeliveryFailureRecord(any(InvalidFailedCallReports.class));
    }

    @Test
    public void shouldUpdateInboxToHoldLastScheduledMessage() {
        DateTime creationDate = DateTime.now();
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, creationDate);
        String subscriptionId = subscription.getSubscriptionId();
        String messageId = "week10";
        String campaignName = "campaignName";

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(messageCampaignService.getCampaignStartDate(subscriptionId, campaignName)).thenReturn(creationDate);
        when(campaignMessageIdStrategy.createMessageId(campaignName, creationDate, subscription.getPack())).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId, campaignName);

        verify(kilkariInboxService).newMessage(subscriptionId, messageId);
    }

    @Test
    public void shouldNotUpdateInboxWhenSubscriptionIsNotActive() {
        Subscription subscription = new Subscription("9988776655", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        subscription.setStatus(SubscriptionStatus.PENDING_ACTIVATION);
        String subscriptionId = subscription.getSubscriptionId();
        String messageId = "week10";
        String campaignName = "campaignName";

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(campaignMessageIdStrategy.createMessageId(campaignName, subscription.getCreationDate(), subscription.getPack())).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId, campaignName);

        verify(kilkariInboxService, never()).newMessage(subscriptionId, messageId);
    }

    @Test
    public void shouldNotUpdateInboxDuringRenewal() {
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
    public void shouldNotUpdateInboxWhenMessageHasNotAlreadyBeenScheduled() {
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