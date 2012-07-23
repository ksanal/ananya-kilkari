package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.factory.SubscriptionStateHandlerFactory;
import org.motechproject.ananya.kilkari.messagecampaign.request.KilkariMessageCampaignRequest;
import org.motechproject.ananya.kilkari.messagecampaign.service.KilkariMessageCampaignService;
import org.motechproject.ananya.kilkari.request.UnsubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.builder.SubscriptionRequestBuilder;
import org.motechproject.ananya.kilkari.subscription.domain.*;
import org.motechproject.ananya.kilkari.subscription.exceptions.DuplicateSubscriptionException;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.RunOnceSchedulableJob;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class KilkariSubscriptionServiceTest {

    private KilkariSubscriptionService kilkariSubscriptionService;
    @Mock
    private SubscriptionPublisher subscriptionPublisher;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private KilkariMessageCampaignService kilkariMessageCampaignService;
    @Mock
    private SubscriptionStateHandlerFactory subscriptionStateHandlerFactory;
    @Mock
    private MotechSchedulerService motechSchedulerService;

    @Before
    public void setup() {
        initMocks(this);
        kilkariSubscriptionService = new KilkariSubscriptionService(subscriptionPublisher, subscriptionService, kilkariMessageCampaignService, motechSchedulerService);
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().getMillis());
    }

    @After
    public void clear() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldCreateSubscription() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        kilkariSubscriptionService.createSubscription(subscriptionRequest);
        verify(subscriptionPublisher).createSubscription(subscriptionRequest);
    }

    @Test
    public void shouldGetSubscriptionsFor() {
        String msisdn = "1234567890";
        kilkariSubscriptionService.findByMsisdn(msisdn);
        verify(subscriptionService).findByMsisdn(msisdn);
    }

    @Test
    public void shouldProcessSubscriptionRequest() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
        SubscriptionPack pack = SubscriptionPack.FIFTEEN_MONTHS;
        subscriptionRequest.setCreatedAt(DateTime.now());
        subscriptionRequest.setPack(pack.name());
        Subscription subscription = new Subscription("1234567890", SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());

        when(subscriptionService.createSubscription(subscriptionRequest)).thenReturn(subscription);

        kilkariSubscriptionService.processSubscriptionRequest(subscriptionRequest);

        ArgumentCaptor<KilkariMessageCampaignRequest> captor = ArgumentCaptor.forClass(KilkariMessageCampaignRequest.class);
        verify(kilkariMessageCampaignService).start(captor.capture());
        KilkariMessageCampaignRequest kilkariMessageCampaignRequest = captor.getValue();
        assertNotNull(kilkariMessageCampaignRequest.getExternalId());
        assertEquals(pack.name(), kilkariMessageCampaignRequest.getSubscriptionPack());

        assertEquals(subscriptionRequest.getCreatedAt().toLocalDate(), kilkariMessageCampaignRequest.getSubscriptionCreationDate().toLocalDate());
    }

    @Test
    public void shouldNotScheduleMessageCampaignIfDuplicateSubscriptionIsRequested() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequestBuilder().withDefaults()
                .withMsisdn("1234567890").withPack(SubscriptionPack.FIFTEEN_MONTHS.toString()).build();

        doThrow(new DuplicateSubscriptionException("")).when(subscriptionService).createSubscription(subscriptionRequest);

        kilkariSubscriptionService.processSubscriptionRequest(subscriptionRequest);

        verify(kilkariMessageCampaignService, never()).start(any(KilkariMessageCampaignRequest.class));
    }

    @Test
    public void shouldReturnSubscriptionGivenASubscriptionId() {
        Subscription exptectedSubscription = new Subscription();
        String susbscriptionid = "susbscriptionid";
        when(subscriptionService.findBySubscriptionId(susbscriptionid)).thenReturn(exptectedSubscription);

        Subscription subscription = kilkariSubscriptionService.findBySubscriptionId(susbscriptionid);

        assertEquals(exptectedSubscription, subscription);
    }

    @Test
    public void shouldScheduleASubscriptionCompletionEvent() {
        String subscriptionId = "subscriptionId";
        DateTime now = DateTime.now();
        Subscription mockedSubscription = mock(Subscription.class);
        when(mockedSubscription.getSubscriptionId()).thenReturn(subscriptionId);
        kilkariSubscriptionService.bufferDaysToAllowRenewalForPackCompletion = 3;

        kilkariSubscriptionService.scheduleSubscriptionPackCompletionEvent(mockedSubscription);

        ArgumentCaptor<RunOnceSchedulableJob> runOnceSchedulableJobArgumentCaptor = ArgumentCaptor.forClass(RunOnceSchedulableJob.class);
        verify(motechSchedulerService).safeScheduleRunOnceJob(runOnceSchedulableJobArgumentCaptor.capture());
        RunOnceSchedulableJob runOnceSchedulableJob = runOnceSchedulableJobArgumentCaptor.getValue();
        assertEquals(SubscriptionEventKeys.SUBSCRIPTION_COMPLETE, runOnceSchedulableJob.getMotechEvent().getSubject());
        assertEquals(subscriptionId, runOnceSchedulableJob.getMotechEvent().getParameters().get(MotechSchedulerService.JOB_ID_KEY));
        ProcessSubscriptionRequest processSubscriptionRequest = (ProcessSubscriptionRequest) runOnceSchedulableJob.getMotechEvent().getParameters().get("0");
        assertEquals(ProcessSubscriptionRequest.class, processSubscriptionRequest.getClass());
        assertEquals(now.plusDays(3).toDate(), runOnceSchedulableJob.getStartDate());
        assertEquals(Channel.MOTECH,processSubscriptionRequest.getChannel());
    }

    @Test
    public void shouldDeactivateSubscription() {
        UnsubscriptionRequest unsubscriptionRequest = new UnsubscriptionRequest();
        String subscriptionId = "abcd1234";
        unsubscriptionRequest.setSubscriptionId(subscriptionId);

        kilkariSubscriptionService.requestDeactivation(unsubscriptionRequest, Channel.CALL_CENTER);

        ArgumentCaptor<DeactivationRequest> deactivationRequestArgumentCaptor = ArgumentCaptor.forClass(DeactivationRequest.class);
        verify(subscriptionService).requestDeactivation(deactivationRequestArgumentCaptor.capture());
        DeactivationRequest deactivationRequest = deactivationRequestArgumentCaptor.getValue();

        assertEquals(subscriptionId, deactivationRequest.getSubscriptionId());
        assertEquals(Channel.CALL_CENTER, deactivationRequest.getChannel());
    }
}
