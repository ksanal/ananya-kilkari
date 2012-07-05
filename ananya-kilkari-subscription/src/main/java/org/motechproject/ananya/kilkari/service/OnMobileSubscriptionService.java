package org.motechproject.ananya.kilkari.service;

import org.motechproject.ananya.kilkari.domain.SubscriptionActivationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Properties;

@Service
@Profile("prod")
public class OnMobileSubscriptionService implements IOnMobileSubscriptionService{

    private RestTemplate restTemplate;
    private Properties kilkariProperties;
    private final static Logger logger = LoggerFactory.getLogger(OnMobileSubscriptionService.class);

    @Autowired
    public OnMobileSubscriptionService(@Qualifier("kilkariRestTemplate") RestTemplate restTemplate, @Qualifier("kilkariProperties") Properties kilkariProperties) {
        this.restTemplate = restTemplate;
        this.kilkariProperties = kilkariProperties;
    }

    public void activateSubscription(SubscriptionActivationRequest subscriptionActivationRequest) {
        String url = String.format("%s%s", baseUrl(), IOnMobileSubscriptionService.ACTIVATE_SUBSCRIPTION_PATH);
        String urlWithParams = String.format("%s?msisdn={msisdn}&srvkey={srvkey}&mode={mode}&refid={refid}&user={user}&pass={pass}", url);

        String username = kilkariProperties.getProperty("omsm.username");
        String password = kilkariProperties.getProperty("omsm.password");

        HashMap<String, String> urlVariables = new HashMap<>();
        urlVariables.put("msisdn", subscriptionActivationRequest.getMsisdn());
        urlVariables.put("srvkey", subscriptionActivationRequest.getPack().name());
        urlVariables.put("mode", subscriptionActivationRequest.getChannel().name());
        urlVariables.put("refid", subscriptionActivationRequest.getSubscriptionId());
        urlVariables.put("user", username);
        urlVariables.put("pass", password);

        try {
            restTemplate.getForEntity(urlWithParams, String.class, urlVariables);
        } catch  (HttpClientErrorException ex) {
            logger.error(String.format("OnMobile subscription request failed with errorCode: %s, error: %s", ex.getStatusCode(), ex.getResponseBodyAsString()));
            throw ex;
        }
    }
    
    private String baseUrl() {
        String baseUrl = kilkariProperties.getProperty("omsm.base.url");
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

}
