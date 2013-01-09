package org.motechproject.ananya.kilkari.test.data;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessage;
import org.motechproject.ananya.kilkari.obd.repository.AllCampaignMessages;
import org.motechproject.ananya.kilkari.request.CallDurationWebRequest;
import org.motechproject.ananya.kilkari.request.CallbackRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallDetailsWebRequest;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.subscription.repository.KilkariPropertiesData;
import org.motechproject.ananya.kilkari.test.data.contract.SubscriberSubscriptions;
import org.motechproject.ananya.kilkari.test.data.contract.SubscriptionRequest;
import org.motechproject.ananya.kilkari.test.data.contract.builders.SubscriptionRequestBuilder;
import org.motechproject.ananya.kilkari.test.data.utils.TimedRunner;
import org.motechproject.ananya.kilkari.test.data.utils.TimedRunnerResponse;
import org.motechproject.ananya.kilkari.web.response.SubscriptionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.motechproject.ananya.kilkari.test.data.utils.TestUtils.constructUrl;
import static org.motechproject.ananya.kilkari.test.data.utils.TestUtils.fromJson;

@ContextConfiguration("classpath:applicationKilkariTestDataContext.xml")
@ActiveProfiles("production")
public class BaseDataSetup {
    @Autowired
    private TestDataConfig testDataConfig;

    @Autowired
    protected KilkariPropertiesData kilkariProperties;

    @Autowired
    private AllCampaignMessages allCampaignMessages;

    @Autowired
    private AllSubscriptions allSubscriptions;

    private RestTemplate restTemplate;


    public BaseDataSetup() {
        restTemplate = new RestTemplate();
    }



    protected String createSubscriptionForCallCenter() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequestBuilder().withDefaults().withEDD(null).withDOB(null).build();
        Map<String, String> parametersMap = new HashMap<>();
        String channel = "CONTACT_CENTER";
        parametersMap.put("channel", channel);
        restTemplate.postForEntity(constructUrl(baseUrl(), "subscription", parametersMap), subscriptionRequest, String.class);
        return subscriptionRequest.getMsisdn();
    }

    protected SubscriptionDetails getSubscriptionDetails(final String msisdn) {
        Map<String, String> parametersMap = new HashMap<String, String>(){{
            put("channel", "CONTACT_CENTER");
            put("msisdn",msisdn);
        }};
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(new ArrayList<MediaType>() {{
            add(MediaType.APPLICATION_JSON);
        }});

        ResponseEntity<String> subscriber = restTemplate.exchange(constructUrl(baseUrl(), "subscriber", parametersMap), HttpMethod.GET, new HttpEntity<byte[]>(headers), String.class);
        SubscriberSubscriptions subscriptionDetails = fromJson(subscriber.getBody(), SubscriberSubscriptions.class);
        return subscriptionDetails.getSubscriptionDetails().get(0);
    }

    protected void activateSubscription(String msisdn, String subscriptionId, String status, String operator){
        Map<String, String> parametersMap = new HashMap<String, String>(){{
            put("_method", "PUT");

        }};
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn(msisdn);
        callbackRequest.setAction("ACT");
        callbackRequest.setReason("By script");
        callbackRequest.setOperator(operator);
        callbackRequest.setStatus(status);
        restTemplate.put(constructUrl(baseUrl(), "subscription/" + subscriptionId, parametersMap), callbackRequest);

        waitForSubscription(msisdn, "Activated");
    }

    protected void renewSubscription(String msisdn, String subscriptionId, String status, String operator){
        Map<String, String> parametersMap = new HashMap<String, String>(){{
            put("_method", "PUT");

        }};
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn(msisdn);
        callbackRequest.setAction("REN");
        callbackRequest.setReason("By script");
        callbackRequest.setOperator(operator);
        callbackRequest.setStatus(status);
        restTemplate.put(constructUrl(baseUrl(), "subscription/" + subscriptionId, parametersMap), callbackRequest);
        waitForSubscription(msisdn, "Activated");
        System.out.println("Subscription renewed");
    }

    protected void waitForCampaignAlert(final String subscriptionId, final String weekMessageId) {
        Boolean result = new TimedRunner<Boolean>(120, 1000) {
            public TimedRunnerResponse<Boolean> run() {
                CampaignMessage campaignMessage = findOBDCampaignMessage(subscriptionId, weekMessageId);
                return campaignMessage == null ? null : new TimedRunnerResponse<>(true);
            }
        }.executeWithTimeout();

        assertNotNull(result);
        assertTrue(result);
    }

    protected void makeOBDCallBack(String msisdn, String subscriptionId, String campaignId, String serviceOption, DateTime startTime, DateTime endTime) {
        CallDurationWebRequest callDurationWebRequest = new CallDurationWebRequest(startTime.toString("dd-MM-yyyy HH-mm-ss"), endTime.toString("dd-MM-yyyy HH-mm-ss"));
        OBDSuccessfulCallDetailsWebRequest callDetailsWebRequest = new OBDSuccessfulCallDetailsWebRequest(msisdn, campaignId, callDurationWebRequest, serviceOption);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(constructUrl(baseUrl(), "/obd/calldetails/" + subscriptionId, new HashMap<String, String>()), callDetailsWebRequest, String.class);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        System.out.println(responseEntity.getBody());
    }


    protected void moveToFutureTime(final DateTime dateTime){
        System.out.println("Moving to time "+dateTime);
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>() {{
            put("newDateTime", Arrays.asList(dateTime.toString("dd/MM/yyyy HH:mm")));
        }};
        String response = restTemplate.postForObject(constructUrl(baseUrl(), "utils/fake_time.jsp"), parameters, String.class);

        int beginIndex = response.indexOf("newDateTime\" value=\"");
        System.out.println(response.substring(beginIndex+20, beginIndex + 36));

    }


    private void waitForSubscription(final String msisdn, final String status) {
        Boolean result = new TimedRunner<Boolean>(20, 6000) {
            public TimedRunnerResponse<Boolean> run() {
                SubscriptionDetails subscriptionDetails = getSubscriptionDetails(msisdn);
                return subscriptionDetails != null && subscriptionDetails.getStatus().equals(status) ? new TimedRunnerResponse<>(true) : null;
            }
        }.executeWithTimeout();
        assertNotNull(result);
        assertTrue(result);
    }

    private CampaignMessage findOBDCampaignMessage(String subscriptionId, String weekMessageId) {
        return allCampaignMessages.find(subscriptionId, weekMessageId);
    }
    protected String baseUrl() {
        return testDataConfig.baseUrl();
    }
}
