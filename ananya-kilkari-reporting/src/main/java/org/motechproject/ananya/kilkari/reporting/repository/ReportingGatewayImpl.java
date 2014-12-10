package org.motechproject.ananya.kilkari.reporting.repository;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.motechproject.ananya.kilkari.reporting.profile.ProductionProfile;
import org.motechproject.ananya.reports.kilkari.contract.request.*;
import org.motechproject.ananya.reports.kilkari.contract.response.LocationResponse;
import org.motechproject.ananya.reports.kilkari.contract.response.SubscriberResponse;
import org.motechproject.http.client.domain.Method;
import org.motechproject.http.client.service.HttpClientService;
import org.motechproject.web.context.HttpThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.motechproject.ananya.kilkari.reporting.domain.URLPath.*;

@Repository
@ProductionProfile
public class ReportingGatewayImpl implements ReportingGateway {
	private RestTemplate restTemplate;
	private HttpClientService httpClientService;
	private Properties kilkariProperties;

	private final Logger logger = LoggerFactory.getLogger(ReportingGatewayImpl.class);
	@Autowired
	public ReportingGatewayImpl(RestTemplate kilkariRestTemplate, HttpClientService httpClientService, @Qualifier("kilkariProperties") Properties kilkariProperties) {
		this.restTemplate = kilkariRestTemplate;
		this.httpClientService = httpClientService;
		this.kilkariProperties = kilkariProperties;
	}

	@Override
	public LocationResponse getLocation(String state, String district, String block, String panchayat) {
		List<NameValuePair> locationParameters = constructParameterMap(state, district, block, panchayat);
		String url = constructGetLocationUrl(locationParameters);
		try {
			return restTemplate.getForEntity(url, LocationResponse.class).getBody();
		} catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND))
				return null;
			throw ex;
		}
	}

	@Override
	public SubscriberResponse getSubscriber(String subscriptionId) {
		String url = String.format("%s%s/%s", getBaseUrl(), GET_SUBSCRIBER_PATH, subscriptionId);
		return restTemplate.getForEntity(url, SubscriberResponse.class).getBody();
	}

	@Override
	public void reportSubscriptionCreation(SubscriptionReportRequest subscriptionReportRequest) {
		String url = String.format("%s%s", getBaseUrl(), CREATE_SUBSCRIPTION_PATH);
		performHttpRequestBasedOnChannel(url, subscriptionReportRequest, Method.POST);
	}

	@Override
	public void reportSubscriptionStateChange(SubscriptionStateChangeRequest subscriptionStateChangeRequest) {
		String subscriptionId = subscriptionStateChangeRequest.getSubscriptionId();
		String url = String.format("%s%s/%s", getBaseUrl(), SUBSCRIPTION_STATE_CHANGE_PATH, subscriptionId);
		performHttpRequestBasedOnChannel(url, subscriptionStateChangeRequest, Method.PUT);
	}

	@Override
	public void reportCampaignMessageDeliveryStatus(CallDetailsReportRequest callDetailsReportRequest) {
		String url = String.format("%s%s", getBaseUrl(), CALL_DETAILS_PATH);
		performHttpRequestBasedOnChannel(url, callDetailsReportRequest, Method.POST);
	}

	@Override
	public void reportSubscriberDetailsChange(String subscriptionId, SubscriberReportRequest subscriberReportRequest) {
		String url = String.format("%s%s/%s", getBaseUrl(), SUBSCRIBER_UPDATE_PATH, subscriptionId);
		performHttpRequestBasedOnChannel(url, subscriberReportRequest, Method.PUT);
	}

	@Override
	public void reportChangeMsisdnForSubscriber(SubscriberChangeMsisdnReportRequest reportRequest) {
		String url = String.format("%s%s", getBaseUrl(), CHANGE_MSISDN_PATH);
		performHttpRequestBasedOnChannel(url, reportRequest, Method.POST);
	}

	@Override
	public List<SubscriberResponse> getSubscribersByMsisdn(String msisdn) {
		String url = String.format("%s%s?msisdn=%s", getBaseUrl(), GET_SUBSCRIBER_BY_MSISDN_PATH, msisdn);
		return Arrays.asList(restTemplate.getForEntity(url, SubscriberResponse[].class).getBody());
	}

	@Override
	public void reportCampaignScheduleAlertReceived(CampaignScheduleAlertRequest campaignScheduleAlertRequest) {
		String url = String.format("%s%s", getBaseUrl(), CAMPAIGN_SCHEDULE_ALERT_PATH);
		performHttpRequestBasedOnChannel(url, campaignScheduleAlertRequest, Method.POST);
	}

	@Override
	public void reportCampaignChange(CampaignChangeReportRequest campaignChangeReportRequest, String subscriptionId) {
		String url = String.format("%s%s", getBaseUrl(), String.format(CHANGE_CAMPAIGN_PATH, subscriptionId));
		performHttpRequestBasedOnChannel(url, campaignChangeReportRequest, Method.PUT);
	}

	@Override
	public void reportCareRequest(SubscriberCareReportRequest subscriberCareReportRequest) {
		String url = String.format("%s%s", getBaseUrl(), SUBSCRIBER_CARE_PATH);
		performHttpRequestBasedOnChannel(url, subscriberCareReportRequest, Method.POST);
	}

	private boolean isCallCenterCall() {
		String channel = HttpThreadContext.get();
		return "CONTACT_CENTER".equalsIgnoreCase(channel);
	}

	private boolean isSMCall(){
		String notInitiatedByMotech = HttpThreadContext.get();
		logger.debug("Returning isSMCall as "+"NOT_INITIATED_BY_MOTECH".equalsIgnoreCase(notInitiatedByMotech));
		return "NOT_INITIATED_BY_MOTECH".equalsIgnoreCase(notInitiatedByMotech);
	}

	private <T> void performHttpRequestBasedOnChannel(String url, T postObject, Method method) {
		if (isCallCenterCall() || isSMCall()) {
			try {
				logger.debug("executeSync");
				httpClientService.executeSync(url, postObject, method);
			} catch (Exception e) {
				httpClientService.execute(url, postObject, method);
			}
		} else
			httpClientService.execute(url, postObject, method);
	}

	private String constructGetLocationUrl(List<NameValuePair> params) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
		try {
			uriComponentsBuilder.uri(new URI(getBaseUrl())).path(GET_LOCATION_PATH);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Error constructing reporting URI", e);
		}

		for (NameValuePair nameValuePair : params) {
			uriComponentsBuilder.queryParam(nameValuePair.getName(), nameValuePair.getValue());
		}

		return uriComponentsBuilder.build().toString();
	}

	private List<NameValuePair> constructParameterMap(String state, String district, String block, String panchayat) {
		List<NameValuePair> locationParameters = new ArrayList<>();
		if (StringUtils.isNotBlank(state)) locationParameters.add(new BasicNameValuePair("state", state));
		if (StringUtils.isNotBlank(district)) locationParameters.add(new BasicNameValuePair("district", district));
		if (StringUtils.isNotBlank(block)) locationParameters.add(new BasicNameValuePair("block", block));
		if (StringUtils.isNotBlank(panchayat)) locationParameters.add(new BasicNameValuePair("panchayat", panchayat));
		return locationParameters;
	}

	private String getBaseUrl() {
		String baseUrl = kilkariProperties.getProperty("reporting.service.base.url");
		return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
	}

	@Override
	public void reportChangeReferredByFlwMsisdnForSubscriber(SubscriptionChangeReferredFLWMsisdnReportRequest reportRequest) {
		String url = String.format("%s%s", getBaseUrl(), CHANGE_REFERRED_BY_PATH);
		performHttpRequestBasedOnChannel(url, reportRequest, Method.POST);
	}

	@Override
	public void reportSubscriberDetailsForChangeSubscription(String subscriptionId, SubscriberChangeSubscriptionReportRequest request) {
		String url = String.format("%s%s/%s", getBaseUrl(), SUBSCRIBER_UPDATE_FOR_CHANGE_REQ_PATH, subscriptionId);
		performHttpRequestBasedOnChannel(url, request, Method.PUT);
	}

	@Override
	public void reportChangeSubscription(ChangeSubscriptionReportRequest changeSubscriptionReportRequest) {
		String subscriptionId = changeSubscriptionReportRequest.getSubscriptionId();
		String url = String.format("%s%s/%s", getBaseUrl(), SUBSCRIPRION_UPDATE_FOR_CHANGE_REQ_PATH, subscriptionId);
		performHttpRequestBasedOnChannel(url, changeSubscriptionReportRequest, Method.PUT);
	}


}
