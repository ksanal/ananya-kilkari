package org.motechproject.ananya.kilkari.handlers.callback.subscription;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.request.CallbackRequest;
import org.motechproject.ananya.kilkari.request.CallbackRequestWrapper;
import org.motechproject.ananya.kilkari.service.KilkariCampaignService;
import org.motechproject.ananya.kilkari.subscription.domain.Operator;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ActivateHandlerTest {
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private KilkariCampaignService kilkariCampaignService;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldInvokeSubscriptionServiceToActivateASubscription() {
        ActivateHandler activateHandler = new ActivateHandler(subscriptionService, kilkariCampaignService);
        String subscriptionId = "abcd1234";
        String operator = Operator.AIRTEL.name();
        DateTime now = DateTime.now();

        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setOperator(operator);
        activateHandler.perform(new CallbackRequestWrapper(callbackRequest, subscriptionId, now, true));

        verify(subscriptionService).activate(subscriptionId, now, operator,"ivr");
    }
}
