package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.builder.SubscriptionRequestBuilder;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.ananya.kilkari.exceptions.DuplicateSubscriptionException;
import org.motechproject.ananya.kilkari.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.repository.AllSubscriptions;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubscriptionServiceTest {

    private SubscriptionService subscriptionService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private AllSubscriptions allSubscriptions;
    @Mock
    private Publisher publisher;
    @Mock
    private SubscriptionRequest mockedSubscriptionRequest;

    @Mock
    private ReportingService reportingService;


    @Before
    public void setUp() {
        initMocks(this);
        subscriptionService = new SubscriptionService(allSubscriptions, publisher, reportingService);
    }

    @Test
    public void shouldCreateNewSubscription() {
        String msisdn = "1234567890";
        Channel channel = Channel.IVR;
        SubscriptionPack subscriptionPack = SubscriptionPack.TWELVE_MONTHS;

        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);
        ArgumentCaptor<SubscriptionActivationRequest> subscriptionActivationRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionActivationRequest.class);
        ArgumentCaptor<SubscriptionCreationReportRequest> subscriptionReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCreationReportRequest.class);

        SubscriptionRequest subscriptionRequest = createSubscriptionRequest(msisdn, subscriptionPack.name(), channel.name());

        when(reportingService.getLocation("district", "block", "panchayat")).thenReturn(new SubscriberLocation("district", "block", "panchayat"));

        String subscriptionId = subscriptionService.createSubscription(subscriptionRequest);

        assertNotNull(subscriptionId);
        verify(allSubscriptions).add(subscriptionArgumentCaptor.capture());
        verify(publisher).processSubscription(subscriptionActivationRequestArgumentCaptor.capture());
        verify(publisher).reportSubscriptionCreation(subscriptionReportRequestArgumentCaptor.capture());

        Subscription subscription = subscriptionArgumentCaptor.getValue();
        assertEquals(msisdn, subscription.getMsisdn());
        assertEquals(subscriptionPack, subscription.getPack());

        SubscriptionActivationRequest actualSubscriptionActivationRequest = subscriptionActivationRequestArgumentCaptor.getValue();
        assertEquals(msisdn, actualSubscriptionActivationRequest.getMsisdn());
        assertEquals(subscriptionPack, actualSubscriptionActivationRequest.getPack());
        assertEquals(channel, actualSubscriptionActivationRequest.getChannel());

        SubscriptionCreationReportRequest actualSubscriptionCreationReportRequest = subscriptionReportRequestArgumentCaptor.getValue();
        assertEquals(msisdn, actualSubscriptionCreationReportRequest.getMsisdn());
        assertEquals(subscriptionPack, actualSubscriptionCreationReportRequest.getPack());
        assertEquals(channel, actualSubscriptionCreationReportRequest.getChannel());
        assertNull(actualSubscriptionCreationReportRequest.getOperator());
    }

    @Test
    public void shouldThrowExceptionWhenInvalidSubscriptionRequestIsGiven() {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid Request");

        doThrow(new ValidationException("Invalid Request")).when(mockedSubscriptionRequest).validate(reportingService, subscriptionService);
        subscriptionService.createSubscription(mockedSubscriptionRequest);
    }

    @Test
    public void shouldGetSubscriptionsForAGivenMsisdn() {
        String msisdn = "1234567890";
        ArrayList<Subscription> subscriptionsToBeReturned = new ArrayList<>();
        subscriptionsToBeReturned.add(new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS));
        subscriptionsToBeReturned.add(new Subscription(msisdn, SubscriptionPack.FIFTEEN_MONTHS));
        when(allSubscriptions.findByMsisdn(msisdn)).thenReturn(subscriptionsToBeReturned);

        List<Subscription> subscriptions = subscriptionService.findByMsisdn(msisdn);

        assertEquals(2, subscriptions.size());
        assertEquals(msisdn, subscriptions.get(0).getMsisdn());
        assertEquals(SubscriptionPack.TWELVE_MONTHS, subscriptions.get(0).getPack());
        assertEquals(msisdn, subscriptions.get(1).getMsisdn());
        assertEquals(SubscriptionPack.FIFTEEN_MONTHS, subscriptions.get(1).getPack());
    }

    @Test
    public void shouldThrowAnExceptionForInvalidMsisdnNumbers() {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid msisdn 12345");

        subscriptionService.findByMsisdn("12345");
    }

    @Test
    public void shouldThrowAnExceptionForNonNumericMsisdn() {
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Invalid msisdn 123456789a");

        subscriptionService.findByMsisdn("123456789a");
    }

    @Test
    public void shouldFindTheGivenSubscriptionByMsisdnAndPack() {
        String pack = "twelve_months";
        String msisdn = "123456890";
        Subscription subscription = new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS);
        when(allSubscriptions.findByMsisdnAndPack(msisdn, SubscriptionPack.TWELVE_MONTHS)).thenReturn(subscription);

        Subscription actualSubscription = subscriptionService.findByMsisdnAndPack(msisdn, pack);

        verify(allSubscriptions).findByMsisdnAndPack(msisdn, SubscriptionPack.TWELVE_MONTHS);
        assertEquals(subscription, actualSubscription);
    }

    @Test
    public void shouldUpdateTheSubscriptionStatusToPendingActivation_WhenActivationIsRequested() {
        String subscriptionId = "abcd1234";
        SubscriptionStatus status = SubscriptionStatus.PENDING_ACTIVATION;
        Subscription mockedSubscription = mock(Subscription.class);

        when(mockedSubscription.getStatus()).thenReturn(status);
        when(mockedSubscription.getSubscriptionId()).thenReturn(subscriptionId);
        when(allSubscriptions.findBySubscriptionId(subscriptionId)).thenReturn(mockedSubscription);

        subscriptionService.activationRequested(subscriptionId);

        InOrder order = inOrder(allSubscriptions, mockedSubscription, publisher);
        order.verify(allSubscriptions).findBySubscriptionId(subscriptionId);
        order.verify(mockedSubscription).activationRequested();
        order.verify(allSubscriptions).update(mockedSubscription);
        ArgumentCaptor<SubscriptionStateChangeReportRequest> subscriptionStateChangeReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionStateChangeReportRequest.class);
        order.verify(publisher).reportSubscriptionStateChange(subscriptionStateChangeReportRequestArgumentCaptor.capture());
        SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest = subscriptionStateChangeReportRequestArgumentCaptor.getValue();

        assertEquals(subscriptionId, subscriptionStateChangeReportRequest.getSubscriptionId());
        assertEquals(status, subscriptionStateChangeReportRequest.getSubscriptionStatus());
        assertNull(subscriptionStateChangeReportRequest.getOperator());
    }

    @Test
    public void shouldUpdateTheSubscriptionStatusToActivationFailed_GivenSubscriptionId() {
        SubscriptionStatus status = SubscriptionStatus.ACTIVATION_FAILED;
        Subscription mockedSubscription = mock(Subscription.class);
        String subscriptionId = "abcd1234";
        String operator = Operator.AIRTEL.name();
        String reason = "Activation Failed For some error";

        when(mockedSubscription.getStatus()).thenReturn(status);
        when(mockedSubscription.getSubscriptionId()).thenReturn(subscriptionId);
        when(allSubscriptions.findBySubscriptionId(subscriptionId)).thenReturn(mockedSubscription);

        subscriptionService.activationFailed(subscriptionId, DateTime.now(), reason, operator);

        InOrder order = inOrder(allSubscriptions, mockedSubscription, publisher);
        order.verify(allSubscriptions).findBySubscriptionId(subscriptionId);
        order.verify(mockedSubscription).activationFailed(operator);
        order.verify(allSubscriptions).update(mockedSubscription);
        ArgumentCaptor<SubscriptionStateChangeReportRequest> subscriptionStateChangeReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionStateChangeReportRequest.class);
        order.verify(publisher).reportSubscriptionStateChange(subscriptionStateChangeReportRequestArgumentCaptor.capture());
        SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest = subscriptionStateChangeReportRequestArgumentCaptor.getValue();

        assertEquals(subscriptionId, subscriptionStateChangeReportRequest.getSubscriptionId());
        assertEquals(status, subscriptionStateChangeReportRequest.getSubscriptionStatus());
        assertEquals(reason, subscriptionStateChangeReportRequest.getReason());
        assertEquals(operator, subscriptionStateChangeReportRequest.getOperator());
    }

    @Test
    public void shouldActivateTheSubscriptionGivenTheSubscriptionId() {
        Subscription mockedSubscription = mock(Subscription.class);
        String subscriptionId = "abcd1234";
        SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
        String operator = Operator.AIRTEL.name();

        when(mockedSubscription.getStatus()).thenReturn(subscriptionStatus);
        when(mockedSubscription.getSubscriptionId()).thenReturn(subscriptionId);
        when(allSubscriptions.findBySubscriptionId(subscriptionId)).thenReturn(mockedSubscription);

        subscriptionService.activate(subscriptionId, DateTime.now(), operator);

        InOrder order = inOrder(allSubscriptions, mockedSubscription, publisher);
        order.verify(allSubscriptions).findBySubscriptionId(subscriptionId);
        order.verify(mockedSubscription).activate(operator);
        order.verify(allSubscriptions).update(mockedSubscription);
        ArgumentCaptor<SubscriptionStateChangeReportRequest> subscriptionStateChangeReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionStateChangeReportRequest.class);
        order.verify(publisher).reportSubscriptionStateChange(subscriptionStateChangeReportRequestArgumentCaptor.capture());
        SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest = subscriptionStateChangeReportRequestArgumentCaptor.getValue();

        assertEquals(subscriptionId, subscriptionStateChangeReportRequest.getSubscriptionId());
        assertEquals(subscriptionStatus, subscriptionStateChangeReportRequest.getSubscriptionStatus());
        assertEquals(operator, subscriptionStateChangeReportRequest.getOperator());
    }

    @Test
    public void shouldNotAddDuplicateSubscriptions() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequestBuilder().withDefaults()
                .withMsisdn("1234567890").withPack(SubscriptionPack.FIFTEEN_MONTHS.toString()).build();

        Subscription existingSubscription = new Subscription();
        when(reportingService.getLocation(subscriptionRequest.getDistrict(), subscriptionRequest.getBlock(), subscriptionRequest.getPanchayat()))
                .thenReturn(new SubscriberLocation("district", "block", "panchayat"));
        when(allSubscriptions.findByMsisdnAndPack(subscriptionRequest.getMsisdn(),
                SubscriptionPack.from(subscriptionRequest.getPack()))).thenReturn(existingSubscription);

        expectedException.expect(DuplicateSubscriptionException.class);

        subscriptionService.createSubscription(subscriptionRequest);

    }

    private SubscriptionRequest createSubscriptionRequest(String msisdn, String pack, String channel) {
        return new SubscriptionRequestBuilder().withDefaults().withMsisdn(msisdn).withPack(pack).withChannel(channel).build();
    }
}
