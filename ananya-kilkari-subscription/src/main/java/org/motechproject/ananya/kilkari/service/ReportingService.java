package org.motechproject.ananya.kilkari.service;

import org.motechproject.ananya.kilkari.domain.SubscriptionCreationReportRequest;
import org.motechproject.ananya.kilkari.domain.SubscriptionStateChangeReportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Properties;

@Service
public class ReportingService {

    public static final String CREATE_SUBSCRIPTION_PATH = "subscription";
    public static final String SUBSCRIPTION_STATE_CHANGE_PATH = "updatesubscription";
    private RestTemplate restTemplate;
    private Properties kilkariProperties;
    private final static Logger logger = LoggerFactory.getLogger(ReportingService.class);

    @Autowired
    public ReportingService(@Qualifier("kilkariRestTemplate") RestTemplate restTemplate, @Qualifier("kilkariProperties") Properties kilkariProperties) {
        this.restTemplate = restTemplate;
        this.kilkariProperties = kilkariProperties;
    }

    public void createSubscription(SubscriptionCreationReportRequest subscriptionCreationReportRequest) {
        String baseUrl = kilkariProperties.getProperty("reporting.service.base.url");
        String url = (baseUrl.endsWith("/")) ? String.format("%s%s", baseUrl, CREATE_SUBSCRIPTION_PATH) : String.format("%s/%s", baseUrl, CREATE_SUBSCRIPTION_PATH);
        try {
            restTemplate.postForLocation(url, subscriptionCreationReportRequest, String.class, new HashMap<String, String>());
        } catch  (HttpClientErrorException ex) {
            logger.error(String.format("Reporting subscription creation failed with errorCode: %s, error: %s", ex.getStatusCode(), ex.getResponseBodyAsString()));
            throw ex;
        }
    }

    public void updateSubscriptionStateChange(SubscriptionStateChangeReportRequest subscriptionStateChangeReportRequest) {
        String baseUrl = kilkariProperties.getProperty("reporting.service.base.url");
        String subscriptionId = subscriptionStateChangeReportRequest.getSubscriptionId();
        String url = (baseUrl.endsWith("/")) ? String.format("%s%s/%s", baseUrl, SUBSCRIPTION_STATE_CHANGE_PATH, subscriptionId) : String.format("%s/%s/%s", baseUrl, SUBSCRIPTION_STATE_CHANGE_PATH, subscriptionId);
        try {
            restTemplate.put(url, subscriptionStateChangeReportRequest, new HashMap<String, String>());
        } catch  (HttpClientErrorException ex) {
            logger.error(String.format("Reporting subscription state change failed with errorCode: %s, error: %s", ex.getStatusCode(), ex.getResponseBodyAsString()));
            throw ex;
        }

    }
}
