package org.motechproject.ananya.kilkari.obd.repository;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessageAlert;
import org.motechproject.ananya.kilkari.obd.utils.SpringIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class AllCampaignMessageAlertsIT extends SpringIntegrationTest {

    @Autowired
    private AllCampaignMessageAlerts allCampaignMessageAlerts;

    @Test
    public void shouldReturnTheCampaignMessageAlertIfExists() {
        String subscriptionId = "subscriptionId";
        String messageId = "messageId";
        DateTime messageExpiryDate = DateTime.now().plusWeeks(1);
        CampaignMessageAlert actualCampaignMessage = new CampaignMessageAlert(subscriptionId, messageId, true, messageExpiryDate);
        obdDbConnector.create(actualCampaignMessage);
        markForDeletion(actualCampaignMessage);

        CampaignMessageAlert expectedCampaignMessage = allCampaignMessageAlerts.findBySubscriptionId(subscriptionId);
        assertEquals(subscriptionId, expectedCampaignMessage.getSubscriptionId());
        assertEquals(messageId, expectedCampaignMessage.getMessageId());
        assertEquals(messageExpiryDate.withZone(DateTimeZone.UTC), expectedCampaignMessage.getMessageExpiryDate());
        assertTrue(expectedCampaignMessage.isRenewed());
    }
}
