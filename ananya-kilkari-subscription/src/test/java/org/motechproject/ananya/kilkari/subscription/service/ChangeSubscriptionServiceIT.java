package org.motechproject.ananya.kilkari.subscription.service;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.motechproject.ananya.kilkari.obd.domain.Channel;
import org.motechproject.ananya.kilkari.subscription.builder.SubscriptionBuilder;
import org.motechproject.ananya.kilkari.subscription.domain.ChangeSubscriptionType;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionStatus;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.subscription.repository.SpringIntegrationTest;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangeSubscriptionRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ChangeSubscriptionServiceIT extends SpringIntegrationTest {

    @Autowired
    private ChangeSubscriptionService changeSubscriptionService;

    @Autowired
    private AllSubscriptions allSubscriptions;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Autowired
    private SubscriptionService subscriptionService;
    private String msisdn = "1111111111";

    @Before
    @After
    public void tearDown() {
        allSubscriptions.removeAll();
    }

    @Test
    public void shouldChangePackForAnExistingSubscription() {
        Subscription existingSubscription = new SubscriptionBuilder().withDefaults().withMsisdn(msisdn).withPack(SubscriptionPack.BARI_KILKARI).build();
        allSubscriptions.add(existingSubscription);
        ChangeSubscriptionRequest changeSubscriptionRequest = new ChangeSubscriptionRequest(ChangeSubscriptionType.CHANGE_PACK, msisdn, existingSubscription.getSubscriptionId(), SubscriptionPack.NAVJAAT_KILKARI, Channel.CONTACT_CENTER, DateTime.now(), DateTime.now().plusMonths(1), null, "reason");

        changeSubscriptionService.process(changeSubscriptionRequest);

        List<Subscription> subscriptions = allSubscriptions.findByMsisdn(msisdn);
        Subscription deactivatedSubscription = subscriptions.get(0);
        assertTrue(deactivatedSubscription.getStatus() == SubscriptionStatus.DEACTIVATION_REQUEST_RECEIVED ||
                deactivatedSubscription.getStatus() == SubscriptionStatus.PENDING_DEACTIVATION);
        assertEquals(deactivatedSubscription.getPack(), deactivatedSubscription.getPack());
        Subscription newSubscription = subscriptions.get(1);
        assertEquals(SubscriptionStatus.NEW_EARLY, newSubscription.getStatus());
        assertEquals(changeSubscriptionRequest.getPack(), newSubscription.getPack());
    }
}