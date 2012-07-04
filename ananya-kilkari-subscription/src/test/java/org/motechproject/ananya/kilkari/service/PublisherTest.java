package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.scheduler.context.EventContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class PublisherTest {
    @Mock
    private EventContext eventContext;

    private Publisher publisher;

    @Before
    public void setUp() {
        initMocks(this);
        publisher = new Publisher(eventContext);
    }

    @Test
    public void shouldPublishProcessSubscriptionEventIntoQueue() {
        String subscriptionId = "ABCD1234";
        publisher.processSubscription(new SubscriptionActivationRequest("1234567890", SubscriptionPack.TWELVE_MONTHS, Channel.IVR, subscriptionId));

        ArgumentCaptor<SubscriptionActivationRequest> subscriptionActivationRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionActivationRequest.class);
        ArgumentCaptor<String> eventArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventContext).send(eventArgumentCaptor.capture(), subscriptionActivationRequestArgumentCaptor.capture());
        SubscriptionActivationRequest subscriptionActivationRequest = subscriptionActivationRequestArgumentCaptor.getValue();
        String eventName = eventArgumentCaptor.getValue();

        assertEquals(SubscriptionEventKeys.PROCESS_SUBSCRIPTION, eventName);
        assertEquals("1234567890", subscriptionActivationRequest.getMsisdn());
        assertEquals(SubscriptionPack.TWELVE_MONTHS, subscriptionActivationRequest.getPack());
        assertEquals(Channel.IVR, subscriptionActivationRequest.getChannel());
        assertEquals(subscriptionId, subscriptionActivationRequest.getSubscriptionId());
    }

    @Test
    public void shouldPublishReportSubscriptionCreationEventIntoQueue() {
        Subscription subscription = new Subscription("1234567890", SubscriptionPack.TWELVE_MONTHS);
        publisher.reportSubscriptionCreation(new SubscriptionCreationReportRequest(subscription, Channel.IVR, 0, null, null, null, null));

        ArgumentCaptor<SubscriptionCreationReportRequest> subscriptionReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCreationReportRequest.class);
        ArgumentCaptor<String> eventArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventContext).send(eventArgumentCaptor.capture(), subscriptionReportRequestArgumentCaptor.capture());
        SubscriptionCreationReportRequest subscriptionCreationReportRequest = subscriptionReportRequestArgumentCaptor.getValue();
        String eventName = eventArgumentCaptor.getValue();

        assertEquals(SubscriptionEventKeys.REPORT_SUBSCRIPTION_CREATION, eventName);
        assertEquals("1234567890", subscriptionCreationReportRequest.getMsisdn());
        assertEquals(SubscriptionPack.TWELVE_MONTHS, subscriptionCreationReportRequest.getPack());
        assertEquals(Channel.IVR, subscriptionCreationReportRequest.getChannel());
        assertEquals(subscription.getSubscriptionId(), subscriptionCreationReportRequest.getSubscriptionId());
    }

    @Test
    public void shouldPublishSubscriptionStateChangeEventIntoQueue() {
        String subscriptionId = "ABCD1234";
        SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
        String reason = "my own reason";
        String operator = Operator.AIRTEL.name();
        SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest = new SubscriptionStateChangeReportRequest(subscriptionId, subscriptionStatus, DateTime.now(), reason, operator);

        publisher.reportSubscriptionStateChange(subscriptionStateChangeReportRequest);

        ArgumentCaptor<SubscriptionStateChangeReportRequest> subscriptionStateChangeReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionStateChangeReportRequest.class);
        ArgumentCaptor<String> eventArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventContext).send(eventArgumentCaptor.capture(), subscriptionStateChangeReportRequestArgumentCaptor.capture());

        SubscriptionStateChangeReportRequest actualSubscriptionStateChangeReportRequest = subscriptionStateChangeReportRequestArgumentCaptor.getValue();
        String eventName = eventArgumentCaptor.getValue();

        assertEquals(SubscriptionEventKeys.REPORT_SUBSCRIPTION_STATE_CHANGE, eventName);
        assertEquals(subscriptionId, actualSubscriptionStateChangeReportRequest.getSubscriptionId());
        assertEquals(subscriptionStatus, actualSubscriptionStateChangeReportRequest.getSubscriptionStatus());
        assertEquals(reason, actualSubscriptionStateChangeReportRequest.getReason());
        assertEquals(operator, actualSubscriptionStateChangeReportRequest.getOperator());
    }
}

