package org.motechproject.ananya.kilkari.reporting.gateway;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.reporting.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ReportingGatewayImplTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private Properties kilkariProperties;
    @Captor
    private ArgumentCaptor<Class<String>> responseTypeArgumentCaptor;
    @Captor
    private ArgumentCaptor<HashMap<String,String>> urlVariablesArgumentCaptor;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldInvokeReportingServiceWithSubscriptionDetails() {
        String msisdn = "msisdn";
        String pack = "TWELVE_MONTHS";
        String channel = "IVR";
        String subscriptionId = "abcd1234";
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        DateTime dob = DateTime.now().minusMonths(8);
        DateTime edd = DateTime.now().plusMonths(3);
        String name = "name";
        SubscriptionDetails subscriptionDetails = new SubscriptionDetails(msisdn, pack, DateTime.now(),"ACTIVE", subscriptionId);
        new ReportingGatewayImpl(restTemplate, kilkariProperties).createSubscription(new SubscriptionCreationReportRequest(subscriptionDetails,channel, 42, name, dob, edd, new SubscriberLocation("district", "block", "panchayat")));

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SubscriptionCreationReportRequest> subscriptionReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCreationReportRequest.class);
        verify(restTemplate).postForLocation(urlArgumentCaptor.capture(), subscriptionReportRequestArgumentCaptor.capture(), responseTypeArgumentCaptor.capture(), urlVariablesArgumentCaptor.capture());
        SubscriptionCreationReportRequest subscriptionCreationReportRequest = subscriptionReportRequestArgumentCaptor.getValue();

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        assertEquals("url/subscription", urlArgumentCaptor.getValue());
        assertEquals(msisdn, subscriptionCreationReportRequest.getMsisdn());
        assertEquals(pack, subscriptionCreationReportRequest.getPack());
        assertEquals(channel, subscriptionCreationReportRequest.getChannel());
        assertEquals(subscriptionDetails.getSubscriptionId(), subscriptionCreationReportRequest.getSubscriptionId());
    }

    @Test
    public void shouldInvokeReportingServiceWithGetLocations() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        SubscriberLocation expectedLocation = new SubscriberLocation("mydistrict", "myblock", "mypanchayat");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(expectedLocation, HttpStatus.OK));

        SubscriberLocation actualLocation = new ReportingGatewayImpl(restTemplate, kilkariProperties).getLocation("mydistrict", "myblock", "mypanchayat");

        assertEquals(expectedLocation, actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(SubscriberLocation.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("district=mydistrict"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }

    @Test
    public void shouldReturnNullIfLocationNotPresent() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        SubscriberLocation actualLocation = new ReportingGatewayImpl(restTemplate, kilkariProperties).getLocation("mydistrict", "myblock", "mypanchayat");

        assertNull(actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(SubscriberLocation.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("district=mydistrict"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }
    @Test
    public void shouldRethrowAnyOtherExceptionOnGetLocation() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        expectedException.expect(HttpClientErrorException.class);
        expectedException.expectMessage("400 BAD_REQUEST");

        new ReportingGatewayImpl(restTemplate, kilkariProperties).getLocation("mydistrict", "myblock", "mypanchayat");

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(SubscriberLocation.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("district=mydistrict"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }

    @Test
    public void shouldInvokeReportingServiceWithGetLocationsIfDistrctNotPresent() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        SubscriberLocation expectedLocation = new SubscriberLocation(null, "myblock", "mypanchayat");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(expectedLocation, HttpStatus.OK));

        SubscriberLocation actualLocation = new ReportingGatewayImpl(restTemplate, kilkariProperties).getLocation(null, "myblock", "mypanchayat");

        assertEquals(expectedLocation, actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(SubscriberLocation.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertTrue(url.startsWith("url/location?"));
        assertTrue(url.contains("block=myblock"));
        assertTrue(url.contains("panchayat=mypanchayat"));
    }

    @Test
    public void shouldInvokeReportingServiceToGetLocationIfDistrictBlockAndPanchayatAreNotPresent() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        SubscriberLocation expectedLocation = new SubscriberLocation(null, "myblock", "mypanchayat");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(expectedLocation, HttpStatus.OK));

        SubscriberLocation actualLocation = new ReportingGatewayImpl(restTemplate, kilkariProperties).getLocation(null, null, null);

        assertEquals(expectedLocation, actualLocation);

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Class> subscriberLocationCaptor = ArgumentCaptor.forClass(Class.class);
        verify(restTemplate).getForEntity(urlArgumentCaptor.capture(), subscriberLocationCaptor.capture());

        assertEquals(SubscriberLocation.class, subscriberLocationCaptor.getValue());

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        String url = urlArgumentCaptor.getValue();
        assertEquals("url/location", url);
    }

    @Test
    public void shouldInvokeUpdateOnReportingServiceWithSubscriptionStateChangeDetails() {
        String subscriptionId = "abcd1234";
        String subscriptionStatus = "ACTIVE";
        String operator = "AIRTEL";
        String reason = "my own error reason";
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        new ReportingGatewayImpl(restTemplate, kilkariProperties).updateSubscriptionStateChange(new SubscriptionStateChangeReportRequest(subscriptionId, subscriptionStatus, DateTime.now(), reason, operator));

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SubscriptionStateChangeReportRequest> subscriptionStateChangeReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionStateChangeReportRequest.class);
        verify(restTemplate).put(urlArgumentCaptor.capture(), subscriptionStateChangeReportRequestArgumentCaptor.capture(), urlVariablesArgumentCaptor.capture());
        SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest = subscriptionStateChangeReportRequestArgumentCaptor.getValue();

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        assertEquals("url/updatesubscription/abcd1234", urlArgumentCaptor.getValue());
        assertEquals(subscriptionId, subscriptionStateChangeReportRequest.getSubscriptionId());
        assertEquals(subscriptionStatus, subscriptionStateChangeReportRequest.getSubscriptionStatus());
        assertEquals(reason, subscriptionStateChangeReportRequest.getReason());
        assertEquals(operator, subscriptionStateChangeReportRequest.getOperator());
    }

    @Test
    public void shouldInvokeReportingServiceWithCampaignMessageDeliveryDetails() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        String msisdn = "1234567890";
        String subscriptionId = "abcd1234";
        String campaignId = "WEEK12";
        String retryCount = "3";
        String startTime = "25-12-2012";
        String endTime = "27-12-2012";
        String serviceOption = "HELP";
        String status = "NEW";
        new ReportingGatewayImpl(restTemplate, kilkariProperties).reportCampaignMessageDelivery(new CampaignMessageDeliveryReportRequest(subscriptionId, msisdn, campaignId, serviceOption, retryCount, status, new CallDetailsReportRequest(startTime, endTime)));

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CampaignMessageDeliveryReportRequest> campaignMessageDeliveryReportRequestArgumentCaptor = ArgumentCaptor.forClass(CampaignMessageDeliveryReportRequest.class);
        verify(restTemplate).postForLocation(urlArgumentCaptor.capture(), campaignMessageDeliveryReportRequestArgumentCaptor.capture(), urlVariablesArgumentCaptor.capture());
        CampaignMessageDeliveryReportRequest campaignMessageDeliveryReportRequest = campaignMessageDeliveryReportRequestArgumentCaptor.getValue();

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        assertEquals("url/obd/callDetails", urlArgumentCaptor.getValue());
        assertEquals(msisdn, campaignMessageDeliveryReportRequest.getMsisdn());
        assertEquals(subscriptionId, campaignMessageDeliveryReportRequest.getSubscriptionId());
        assertEquals(campaignId, campaignMessageDeliveryReportRequest.getCampaignId());
        assertEquals(retryCount, campaignMessageDeliveryReportRequest.getRetryCount());
        assertEquals(serviceOption, campaignMessageDeliveryReportRequest.getServiceOption());
        CallDetailsReportRequest callDetailReportRequest = campaignMessageDeliveryReportRequest.getCallDetailRecord();
        assertEquals(startTime, callDetailReportRequest.getStartTime());
        assertEquals(endTime, callDetailReportRequest.getEndTime());
    }
}
