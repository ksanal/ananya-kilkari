package org.motechproject.ananya.kilkari.reporting.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.reporting.domain.*;
import org.motechproject.ananya.kilkari.reporting.repository.ReportingGateway;
import org.motechproject.http.client.service.HttpClientService;

import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ReportingServiceImplTest {

    @Mock
    private ReportingGateway reportGateway;
    @Mock
    private HttpClientService httpClientService;
    @Mock
    private Properties kilkariProperties;

    private ReportingServiceImpl reportingServiceImpl;

    @Before
    public void setUp() {
        initMocks(this);
        reportingServiceImpl = new ReportingServiceImpl(reportGateway);
    }

    @Test
    public void shouldGetLocation() {
        String district = "district";
        String block = "block";
        String panchayat = "panchayat";
        when(reportGateway.getLocation(district, block, panchayat)).thenReturn(new SubscriberLocation(district, block, panchayat));

        SubscriberLocation location = reportingServiceImpl.getLocation(district, block, panchayat);

        assertEquals(district, location.getDistrict());
        assertEquals(block, location.getBlock());
        assertEquals(panchayat, location.getPanchayat());
    }

    @Test
    public void shouldReportASubscriptionCreation() {
        SubscriptionCreationReportRequest subscriptionCreationReportRequest = mock(SubscriptionCreationReportRequest.class);

        reportingServiceImpl.reportSubscriptionCreation(subscriptionCreationReportRequest);

        verify(reportGateway).reportSubscriptionCreation(subscriptionCreationReportRequest);
    }

    @Test
    public void shouldReportASubscriptionStateChange() {
        SubscriptionStateChangeReportRequest subscriptionCreationReportRequest = mock(SubscriptionStateChangeReportRequest.class);

        reportingServiceImpl.reportSubscriptionStateChange(subscriptionCreationReportRequest);

        verify(reportGateway).reportSubscriptionStateChange(subscriptionCreationReportRequest);
    }

    @Test
    public void shouldReportASuccessfulCampaignMessageDelivery() {
        CampaignMessageDeliveryReportRequest campaignMessageDeliveryReportRequest = mock(CampaignMessageDeliveryReportRequest.class);

        reportingServiceImpl.reportCampaignMessageDeliveryStatus(campaignMessageDeliveryReportRequest);

        verify(reportGateway).reportCampaignMessageDeliveryStatus(campaignMessageDeliveryReportRequest);
    }

    @Test
    public void shouldReportASubscriberUpdate() {
        String subscriptionId = "subscriptionId";
        SubscriberReportRequest subscriberReportRequest = new SubscriberReportRequest(null, "Name", null, null, null, null);

        reportingServiceImpl.reportSubscriberDetailsChange(subscriptionId, subscriberReportRequest);

        verify(reportGateway).reportSubscriberDetailsChange(subscriptionId, subscriberReportRequest);
    }
}