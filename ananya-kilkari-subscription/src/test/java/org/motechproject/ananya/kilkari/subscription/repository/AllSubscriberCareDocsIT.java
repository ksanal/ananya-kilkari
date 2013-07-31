package org.motechproject.ananya.kilkari.subscription.repository;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.motechproject.ananya.kilkari.obd.domain.Channel;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriberCareDoc;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriberCareReasons;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class AllSubscriberCareDocsIT extends SpringIntegrationTest {
    @Autowired
    private AllSubscriberCareDocs allSubscriberCareDocs;

    @Before
    @After
    public void setUp() {
        allSubscriberCareDocs.removeAll();
    }

    @Test
    public void shouldFindSubscriberCareDoc() {
        String msisdn = "9876543211";
        SubscriberCareReasons reason = SubscriberCareReasons.HELP;
        DateTime createdAt = DateTime.now();
        SubscriberCareDoc subscriberCareDoc = new SubscriberCareDoc(msisdn, reason, createdAt, Channel.IVR);
        allSubscriberCareDocs.add(subscriberCareDoc);
        markForDeletion(subscriberCareDoc);

        SubscriberCareDoc subscriberCareDocs = allSubscriberCareDocs.find(msisdn, reason.name());

        assertNotNull(subscriberCareDocs);
        assertEquals(msisdn, subscriberCareDocs.getMsisdn());
        assertEquals(reason, subscriberCareDocs.getReason());
        assertEquals(createdAt.withZone(DateTimeZone.UTC), subscriberCareDocs.getCreatedAt());
    }

    @Test
    public void shouldNotFetchCareDocsIfNotCreatedInTheRange() {
        SubscriberCareDoc subscriberCareDoc = new SubscriberCareDoc("9876543211", SubscriberCareReasons.HELP, DateTime.now(), Channel.IVR);
        allSubscriberCareDocs.add(subscriberCareDoc);

        List<SubscriberCareDoc> allCareDocs = allSubscriberCareDocs.findByCreatedAt(DateTime.now().plusDays(1), DateTime.now().plusDays(2));

        assertEquals(0, allCareDocs.size());
    }

    @Test
    public void shouldCorrectlyFetchTheCareDocs() {
        String msisdn = "9876543211";
        SubscriberCareReasons reason = SubscriberCareReasons.HELP;
        DateTime now = DateTime.now();
        Channel ivrChannel = Channel.IVR;
        SubscriberCareDoc subscriberCareDoc = new SubscriberCareDoc(msisdn, reason, now, ivrChannel);
        allSubscriberCareDocs.add(subscriberCareDoc);

        List<SubscriberCareDoc> allCareDocs = allSubscriberCareDocs.findByCreatedAt(DateTime.now().minusDays(1), DateTime.now());

        assertEquals(1, allCareDocs.size());
        SubscriberCareDoc fetchedSubscriberCareDoc = allCareDocs.get(0);
        assertEquals(msisdn, fetchedSubscriberCareDoc.getMsisdn());
        assertEquals(reason, fetchedSubscriberCareDoc.getReason());
        assertEquals(now.getMillis(), fetchedSubscriberCareDoc.getCreatedAt().getMillis());
        assertEquals(ivrChannel, fetchedSubscriberCareDoc.getChannel());
    }

    @Test
    public void shouldDeleteAllDocumentsForAnMsisdn() {
        String msisdn = "12345";
        SubscriberCareReasons reason = SubscriberCareReasons.HELP;
        allSubscriberCareDocs.add(new SubscriberCareDoc(msisdn, reason, DateTime.now(), Channel.IVR));

        allSubscriberCareDocs.deleteFor(msisdn);

        assertTrue(allSubscriberCareDocs.findByMsisdn(msisdn).isEmpty());
    }

    @Test
    public void shouldFindDocumentsForAnMsisdn() {
        String msisdn = "12345";
        SubscriberCareReasons reason = SubscriberCareReasons.HELP;
        SubscriberCareDoc subscriberCareDoc = new SubscriberCareDoc(msisdn, reason, DateTime.now(), Channel.IVR);
        allSubscriberCareDocs.add(subscriberCareDoc);

        List<SubscriberCareDoc> byMsisdn = allSubscriberCareDocs.findByMsisdn(msisdn);

        Assert.assertEquals(subscriberCareDoc.getChannel(), byMsisdn.get(0).getChannel());
        Assert.assertEquals(subscriberCareDoc.getReason(), byMsisdn.get(0).getReason());
        Assert.assertEquals(subscriberCareDoc.getMsisdn(), byMsisdn.get(0).getMsisdn());
    }
}
