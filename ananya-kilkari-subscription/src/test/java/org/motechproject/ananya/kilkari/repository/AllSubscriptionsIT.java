package org.motechproject.ananya.kilkari.repository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.ananya.kilkari.domain.Subscription;
import org.motechproject.ananya.kilkari.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.domain.SubscriptionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class AllSubscriptionsIT extends SubscriptionBaseIT {
    @Autowired
    private AllSubscriptions allSubscriptions;

    @Test
    public void shouldAddASubscriptionIntoDb() {
        String msisdn = "123456";

        Subscription subscription = new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS);
        allSubscriptions.add(subscription);

        markForDeletion(subscription);
        List<Subscription> allSubscriptionsList = allSubscriptions.getAll();

        assertNotNull(allSubscriptionsList);
        assertEquals(1, allSubscriptionsList.size());
        Subscription subscriptionFromDb = allSubscriptionsList.get(0);
        assertEquals(msisdn, subscriptionFromDb.getMsisdn());
        assertNotNull(subscriptionFromDb.getCreationDate());
        assertNotNull(subscriptionFromDb.getSubscriptionId());
        assertEquals(SubscriptionPack.TWELVE_MONTHS, subscriptionFromDb.getPack());
        assertEquals(SubscriptionStatus.NEW, subscriptionFromDb.getStatus());
    }

    @Test
    public void shouldQueryForSubscriptionsInDbBasedOnGivenMsisdn() {
        String msisdn = "123456";

        Subscription subscription1 = new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS);
        Subscription subscription2 = new Subscription(msisdn, SubscriptionPack.FIFTEEN_MONTHS);
        Subscription subscription3 = new Subscription("some_other_msisdn", SubscriptionPack.FIFTEEN_MONTHS);
        allSubscriptions.add(subscription1);
        allSubscriptions.add(subscription2);
        allSubscriptions.add(subscription3);

        markForDeletion(subscription1);
        markForDeletion(subscription2);
        markForDeletion(subscription3);
        List<Subscription> filteredSubscriptions = allSubscriptions.findByMsisdn(msisdn);

        assertNotNull(filteredSubscriptions);
        assertEquals(2, filteredSubscriptions.size());
        assertEquals(msisdn, filteredSubscriptions.get(0).getMsisdn());
        assertEquals(msisdn, filteredSubscriptions.get(1).getMsisdn());
        assertTrue(Arrays.asList(new SubscriptionPack[]{SubscriptionPack.TWELVE_MONTHS, SubscriptionPack.FIFTEEN_MONTHS}).contains(filteredSubscriptions.get(0).getPack()));
        assertTrue(Arrays.asList(new SubscriptionPack[]{SubscriptionPack.TWELVE_MONTHS, SubscriptionPack.FIFTEEN_MONTHS}).contains(filteredSubscriptions.get(1).getPack()));
    }

    @Test
    public void shouldFindSubscriptionByMsisdnAndPack() {
        String msisdn = "123456";

        Subscription subscription1 = new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS);
        allSubscriptions.add(subscription1);

        Subscription subscription2 = new Subscription(msisdn, SubscriptionPack.FIFTEEN_MONTHS);
        allSubscriptions.add(subscription2);

        Subscription subscription3 = new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS);
        subscription3.setStatus(SubscriptionStatus.COMPLETED);
        allSubscriptions.add(subscription3);

        markForDeletion(subscription1);
        markForDeletion(subscription2);
        markForDeletion(subscription3);

        List<Subscription> allSubscriptionsByMsisdnAndPack = allSubscriptions.findByMsisdnAndPack(msisdn, SubscriptionPack.TWELVE_MONTHS);

        assertEquals(2, allSubscriptionsByMsisdnAndPack.size());
        assertEquals(subscription1, allSubscriptionsByMsisdnAndPack.get(0));
        assertEquals(subscription3, allSubscriptionsByMsisdnAndPack.get(1));
    }

    @Test
    public void shouldFindSubscriptionBySubscriptionId() {
        String msisdn = "123456";

        Subscription subscription1 = new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS);
        Subscription subscription2 = new Subscription(msisdn, SubscriptionPack.FIFTEEN_MONTHS);
        allSubscriptions.add(subscription1);
        allSubscriptions.add(subscription2);

        markForDeletion(subscription1);
        markForDeletion(subscription2);
        Subscription filteredSubscription = allSubscriptions.findBySubscriptionId(subscription1.getSubscriptionId());

        assertNotNull(filteredSubscription);
        assertEquals(subscription1, filteredSubscription);
    }
}
