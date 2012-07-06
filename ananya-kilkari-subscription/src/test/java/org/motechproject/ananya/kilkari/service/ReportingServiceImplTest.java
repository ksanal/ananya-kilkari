package org.motechproject.ananya.kilkari.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ReportingServiceImplTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private Properties kilkariProperties;
    @Captor
    private ArgumentCaptor<Class<String>> responseTypeArgumentCaptor;
    @Captor
    private ArgumentCaptor<HashMap<String,String>> urlVariablesArgumentCaptor;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldInvokeReportingServiceWithSubscriptionDetails() {
        String msisdn = "msisdn";
        SubscriptionPack pack = SubscriptionPack.TWELVE_MONTHS;
        Channel channel = Channel.IVR;
        String subscriptionId = "abcd1234";
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        DateTime dob = DateTime.now().minusMonths(8);
        DateTime edd = DateTime.now().plusMonths(3);
        String name = "name";
        Subscription subscription = new Subscription(msisdn, pack);
        new ReportingServiceImpl(restTemplate, kilkariProperties).createSubscription(new SubscriptionCreationReportRequest(subscription,channel, 42, name, dob, edd, new SubscriberLocation("district", "block", "panchayat")));

        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SubscriptionCreationReportRequest> subscriptionReportRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionCreationReportRequest.class);
        verify(restTemplate).postForLocation(urlArgumentCaptor.capture(), subscriptionReportRequestArgumentCaptor.capture(), responseTypeArgumentCaptor.capture(), urlVariablesArgumentCaptor.capture());
        SubscriptionCreationReportRequest subscriptionCreationReportRequest = subscriptionReportRequestArgumentCaptor.getValue();

        verify(kilkariProperties).getProperty("reporting.service.base.url");
        assertEquals("url/subscription", urlArgumentCaptor.getValue());
        assertEquals(msisdn, subscriptionCreationReportRequest.getMsisdn());
        assertEquals(pack, subscriptionCreationReportRequest.getPack());
        assertEquals(channel, subscriptionCreationReportRequest.getChannel());
        assertEquals(subscription.getSubscriptionId(), subscriptionCreationReportRequest.getSubscriptionId());
    }

    @Test
    public void shouldInvokeReportingServiceWithGetLocations() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(new SubscriberLocation("mydistrict","myblock","mypanchayat"), HttpStatus.OK));

        new ReportingServiceImpl(restTemplate, kilkariProperties).getLocation("mydistrict", "myblock", "mypanchayat");

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
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(new SubscriberLocation("mydistrict","myblock","mypanchayat"), HttpStatus.OK));

        new ReportingServiceImpl(restTemplate, kilkariProperties).getLocation(null, "myblock", "mypanchayat");

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
    public void shouldInvokeReportingServiceWithGetLocationsIfDistrctBlockAndPanchayatAreNotPresent() {
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");
        when(restTemplate.getForEntity(any(String.class), any(Class.class))).thenReturn(new ResponseEntity(new SubscriberLocation("mydistrict","myblock","mypanchayat"), HttpStatus.OK));

        new ReportingServiceImpl(restTemplate, kilkariProperties).getLocation(null, null, null);

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
        SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
        String operator = Operator.AIRTEL.name();
        String reason = "my own error reason";
        when(kilkariProperties.getProperty("reporting.service.base.url")).thenReturn("url");

        new ReportingServiceImpl(restTemplate, kilkariProperties).updateSubscriptionStateChange(new SubscriptionStateChangeReportRequest(subscriptionId, subscriptionStatus, DateTime.now(), reason, operator));

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
}
