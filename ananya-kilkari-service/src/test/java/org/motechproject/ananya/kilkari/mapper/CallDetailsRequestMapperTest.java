package org.motechproject.ananya.kilkari.mapper;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.motechproject.ananya.kilkari.obd.domain.ServiceOption;
import org.motechproject.ananya.kilkari.reporting.domain.CampaignMessageCallSource;
import org.motechproject.ananya.kilkari.request.CallDurationWebRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallDetailsRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallDetailsWebRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;

import static org.junit.Assert.assertEquals;

public class CallDetailsRequestMapperTest {

    private CallDetailsRequestMapper callDetailsRequestMapper;

    @Before
    public void setUp() {
        callDetailsRequestMapper = new CallDetailsRequestMapper();
    }

    @Test
    public void shouldMapFromWebRequest() {
        String subscriptionId = "subscriptionId";
        String campaignId = "WEEK34";
        String msisdn = "1234567890";
        CallDurationWebRequest callDurationWebRequest = new CallDurationWebRequest("22-11-2011 11-55-35", "23-12-2012 12-59-34");
        OBDSuccessfulCallDetailsWebRequest webRequest = new OBDSuccessfulCallDetailsWebRequest(msisdn, campaignId, callDurationWebRequest, ServiceOption.HELP.name());
        webRequest.setSubscriptionId(subscriptionId);

        OBDSuccessfulCallDetailsRequest request = callDetailsRequestMapper.mapOBDRequest(webRequest);

        assertEquals(ServiceOption.HELP, request.getServiceOption());
        assertEquals(webRequest.getCreatedAt(), request.getCreatedAt());
        assertEquals(subscriptionId, request.getSubscriptionId());
        assertEquals(new DateTime(2012, 12, 23, 12, 59, 34), request.getCallDurationRequest().getEndTime());
        assertEquals(new DateTime(2011, 11, 22, 11, 55, 35), request.getCallDurationRequest().getStartTime());
        assertEquals(msisdn, request.getMsisdn());
        assertEquals(campaignId, request.getCampaignId());
        assertEquals(CampaignMessageCallSource.OBD, request.getCallSource());
        assertEquals(Channel.IVR, request.getChannel());
    }
}
