package org.motechproject.ananya.kilkari.reporting.repository;

import org.motechproject.ananya.kilkari.reporting.domain.SubscriberLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Properties;

import static org.motechproject.ananya.kilkari.reporting.domain.URLPath.GET_LOCATION_PATH;

@Service
public class ReportingGateway {

    private RestTemplate restTemplate;
    private Properties kilkariProperties;

    private final static Logger logger = LoggerFactory.getLogger(ReportingGateway.class);

    @Autowired
    public ReportingGateway(RestTemplate kilkariRestTemplate, @Qualifier("kilkariProperties") Properties kilkariProperties) {
        this.restTemplate = kilkariRestTemplate;
        this.kilkariProperties = kilkariProperties;
    }

    public SubscriberLocation getLocation(String district, String block, String panchayat) {
        HashMap<String, String> locationParameters = constructParameterMap(district, block, panchayat);
        String url = constructGetLocationUrl(locationParameters);
        try {
            return restTemplate.getForEntity(url, SubscriberLocation.class).getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND))
                return null;
            logger.error(String.format("Reporting subscription state change failed with errorCode: %s, error: %s", ex.getStatusCode(), ex.getResponseBodyAsString()));
            throw ex;
        }
    }

    private HashMap<String, String> constructParameterMap(String district, String block, String panchayat) {
        HashMap<String, String> locationParameters = new HashMap();
        locationParameters.put("district", district);
        locationParameters.put("block", block);
        locationParameters.put("panchayat", panchayat);
        return locationParameters;
    }

    private String constructGetLocationUrl(HashMap<String, String> params) {
        String url = String.format("%s%s", getBaseUrl(), GET_LOCATION_PATH);
        boolean paramAdded = false;
        for (String paramName : params.keySet()) {
            String paramValue = params.get(paramName);
            if (paramValue == null) {
                continue;
            }
            url = String.format("%s%s%s=%s", url, (paramAdded ? "&" : "?"), paramName, paramValue);
            paramAdded = true;
        }
        return url;
    }

    private String getBaseUrl() {
        String baseUrl = kilkariProperties.getProperty("reporting.service.base.url");
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
