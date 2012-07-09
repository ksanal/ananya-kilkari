package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.domain.CampaignMessageAlert;
import org.motechproject.ananya.kilkari.domain.Subscription;
import org.motechproject.ananya.kilkari.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.messagecampaign.service.KilkariMessageCampaignService;
import org.motechproject.ananya.kilkari.repository.AllCampaignMessageAlerts;
import org.motechproject.ananya.kilkari.utils.CampaignMessageIdStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
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
    private OBDService obdService;

    @Before
    public void setUp() {
        initMocks(this);
        kilkariCampaignService = new KilkariCampaignService(kilkariMessageCampaignService, kilkariSubscriptionService, campaignMessageIdStrategy, allCampaignMessageAlerts, obdService);
    }

    @Test
    public void shouldGetMessageTimings() {
        String msisdn = "99880";

        List<Subscription> subscriptions = new ArrayList<>();
        Subscription subscription1 = new Subscription(msisdn, SubscriptionPack.FIFTEEN_MONTHS);
        Subscription subscription2 = new Subscription(msisdn, SubscriptionPack.SEVEN_MONTHS);
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
        Subscription subscription = new Subscription();

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

        verifyZeroInteractions(obdService);
        verify(allCampaignMessageAlerts, never()).remove(any(CampaignMessageAlert.class));
    }

    @Test
    public void shouldUpdateCampaignMessageAlertIfAlreadyExistsAndScheduleCampaignMessageIfRenewed() {

        String subscriptionId = "mysubscriptionid";
        String messageId = "mymessageid";
        Subscription subscription = new Subscription();
        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, "previousMessageId", true);

        when(kilkariSubscriptionService.findBySubscriptionId(subscriptionId)).thenReturn(subscription);
        when(allCampaignMessageAlerts.findBySubscriptionId(subscriptionId)).thenReturn(campaignMessageAlert);
        when(campaignMessageIdStrategy.createMessageId(subscription)).thenReturn(messageId);

        kilkariCampaignService.scheduleWeeklyMessage(subscriptionId);

        verify(allCampaignMessageAlerts).findBySubscriptionId(subscriptionId);

        verify(obdService).scheduleCampaignMessage(subscriptionId, messageId);
        verify(allCampaignMessageAlerts).remove(campaignMessageAlert);
    }

    @Test
    public void shouldUpdateCampaignMessageAlertIfAlreadyExistsButShouldNotScheduleCampaignMessageIfNotRenewed() {

        String subscriptionId = "mysubscriptionid";
        String messageId = "mymessageid";
        Subscription subscription = new Subscription();
        CampaignMessageAlert campaignMessageAlert = new CampaignMessageAlert(subscriptionId, "previousMessageId");

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

        verifyZeroInteractions(obdService);
        verify(allCampaignMessageAlerts, never()).remove(any(CampaignMessageAlert.class));
    }
}
