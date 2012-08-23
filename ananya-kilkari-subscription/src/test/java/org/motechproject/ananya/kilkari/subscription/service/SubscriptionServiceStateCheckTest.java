package org.motechproject.ananya.kilkari.subscription.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.message.repository.AllInboxMessages;
import org.motechproject.ananya.kilkari.message.service.CampaignMessageAlertService;
import org.motechproject.ananya.kilkari.message.service.InboxService;
import org.motechproject.ananya.kilkari.messagecampaign.service.MessageCampaignService;
import org.motechproject.ananya.kilkari.obd.domain.Channel;
import org.motechproject.ananya.kilkari.obd.service.CampaignMessageService;
import org.motechproject.ananya.kilkari.reporting.service.ReportingServiceImpl;
import org.motechproject.ananya.kilkari.subscription.domain.DeactivationRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.subscription.repository.KilkariPropertiesData;
import org.motechproject.ananya.kilkari.subscription.repository.OnMobileSubscriptionGateway;
import org.motechproject.ananya.kilkari.subscription.request.OMSubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;
import org.motechproject.ananya.kilkari.subscription.validators.ChangeMsisdnValidator;
import org.motechproject.ananya.kilkari.subscription.validators.SubscriptionValidator;
import org.motechproject.ananya.reports.kilkari.contract.request.SubscriptionReportRequest;
import org.motechproject.ananya.reports.kilkari.contract.request.SubscriptionStateChangeRequest;
import org.motechproject.scheduler.MotechSchedulerService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubscriptionServiceStateCheckTest {

    private SubscriptionService subscriptionService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private AllSubscriptions allSubscriptions;
    @Mock
    private OnMobileSubscriptionManagerPublisher onMobileSubscriptionManagerPublisher;
    @Mock
    private SubscriptionValidator subscriptionValidator;
    @Mock
    private ReportingServiceImpl reportingServiceImpl;
    @Mock
    private AllInboxMessages allInboxMessages;
    @Mock
    private InboxService inboxService;
    @Mock
    private MessageCampaignService messageCampaignService;
    @Mock
    private OnMobileSubscriptionGateway onMobileSubscriptionGateway;
    @Mock
    private CampaignMessageService campaignMessageService;
    @Mock
    private CampaignMessageAlertService campaignMessageAlertService;
    @Mock
    private KilkariPropertiesData kilkariPropertiesData;
    @Mock
    private MotechSchedulerService motechSchedulerService;
    @Mock
    private ChangeMsisdnValidator changeMsisdnValidator;
    private String subscriptionId;
    private Subscription mockSubscription;

    @Before
    public void setUp() {
        initMocks(this);
        subscriptionService = new SubscriptionService(allSubscriptions, onMobileSubscriptionManagerPublisher, subscriptionValidator, reportingServiceImpl,
                inboxService, messageCampaignService, onMobileSubscriptionGateway, campaignMessageService, campaignMessageAlertService, kilkariPropertiesData, motechSchedulerService, changeMsisdnValidator);
        subscriptionId = "subscriptionId";
        mockSubscription = mock(Subscription.class);
        when(allSubscriptions.findBySubscriptionId(subscriptionId)).thenReturn(mockSubscription);
    }

    @Test
    public void shouldNotActivateIfNotInTheRightState() {
        when(mockSubscription.canActivate()).thenReturn(false);

        subscriptionService.activate(subscriptionId, DateTime.now(), "AIRTEL");

        verify(mockSubscription).canActivate();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotFailActivationIfNotInTheRightState() {
        when(mockSubscription.canFailActivation()).thenReturn(false);

        subscriptionService.activationFailed(subscriptionId, DateTime.now(), "reason", "AIRTEL");

        verify(mockSubscription).canFailActivation();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotGoToPendingActivationStateIfNotInTheRightState() {
        when(mockSubscription.canSendActivationRequest()).thenReturn(false);

        subscriptionService.activationRequested(new OMSubscriptionRequest("123", SubscriptionPack.BARI_KILKARI, Channel.IVR, subscriptionId));

        verify(mockSubscription).canSendActivationRequest();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotGoToDeactivationRequestReceivedStateIfNotInTheRightState() {
        when(mockSubscription.canReceiveDeactivationRequest()).thenReturn(false);

        subscriptionService.requestDeactivation(new DeactivationRequest(subscriptionId, Channel.IVR, DateTime.now(),null));

        verify(mockSubscription).canReceiveDeactivationRequest();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotGoToPendingDeactivationStateIfNotInTheRightState() {
        when(mockSubscription.canMoveToPendingDeactivation()).thenReturn(false);

        subscriptionService.deactivationRequested(new OMSubscriptionRequest(null, null, null, subscriptionId));

        verify(mockSubscription).canMoveToPendingDeactivation();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotRenewIfNotInTheRightState() {
        when(mockSubscription.canActivate()).thenReturn(false);

        subscriptionService.renewSubscription(subscriptionId, null, null);

        verify(mockSubscription).canActivate();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotSuspendIfNotInTheRightState() {
        when(mockSubscription.canSuspend()).thenReturn(false);

        subscriptionService.suspendSubscription(subscriptionId, null, null, null);

        verify(mockSubscription).canSuspend();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotDeactivateIfNotInTheRightState() {
        when(mockSubscription.canDeactivate()).thenReturn(false);

        subscriptionService.deactivateSubscription(subscriptionId, null, null, null);

        verify(mockSubscription).canDeactivate();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void deactivationShouldCheckBothDeactivationAndCompletionState() {
        when(mockSubscription.canDeactivate()).thenReturn(false);
        when(mockSubscription.canComplete()).thenReturn(false);

        subscriptionService.deactivateSubscription(subscriptionId, null, null, null);

        verify(mockSubscription).canDeactivate();
        verify(mockSubscription).canComplete();
        verifySubscriptionStatusUpdation();
    }

    @Test
    public void shouldNotCreateASubscriptionIfNotInTheRightState() {
        String msisdn = "msisdn";
        SubscriptionPack subscriptionPack = SubscriptionPack.NANHI_KILKARI;
        when(mockSubscription.canCreateANewEarlySubscription()).thenReturn(false);
        when(mockSubscription.canCreateNewSubscription()).thenReturn(false);
        when(allSubscriptions.findSubscriptionInProgress(msisdn, subscriptionPack)).thenReturn(mockSubscription);

        subscriptionService.createSubscription(new SubscriptionRequest(msisdn, null, subscriptionPack, null, null, null), Channel.IVR);

        verify(mockSubscription).canCreateANewEarlySubscription();
        verify(mockSubscription).canCreateNewSubscription();
        verify(allSubscriptions, never()).add(any(Subscription.class));
        verify(reportingServiceImpl, never()).reportSubscriptionCreation(any(SubscriptionReportRequest.class));
    }

    private void verifySubscriptionStatusUpdation() {
        verify(allSubscriptions, never()).update(mockSubscription);
        verify(reportingServiceImpl, never()).reportSubscriptionStateChange(any(SubscriptionStateChangeRequest.class));
    }

}
