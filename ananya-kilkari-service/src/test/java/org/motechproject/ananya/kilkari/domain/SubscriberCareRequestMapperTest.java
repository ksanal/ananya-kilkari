package org.motechproject.ananya.kilkari.domain;

import org.joda.time.DateTime;
import org.junit.Test;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;

import static org.junit.Assert.assertEquals;

public class SubscriberCareRequestMapperTest {
    @Test
    public void shouldMapSubscriberCareRequestToSubscriberCareDoc() {
        String msisdn = "1234567890";
        SubscriberCareReasons reason = SubscriberCareReasons.HELP;
        String channel = "ivr";
        DateTime dateTime = DateTime.now().minusSeconds(42);
        SubscriberCareRequest subscriberCareRequest = new SubscriberCareRequest(msisdn, reason.name().toLowerCase(), channel, dateTime);

        SubscriberCareDoc subscriberCareDoc = SubscriberCareRequestMapper.map(subscriberCareRequest);

        assertEquals(msisdn, subscriberCareDoc.getMsisdn());
        assertEquals(reason, subscriberCareDoc.getReason());
        assertEquals(Channel.IVR, subscriberCareDoc.getChannel());
        assertEquals(dateTime, subscriberCareDoc.getCreatedAt());
    }
}
