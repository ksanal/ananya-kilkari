package org.motechproject.ananya.kilkari.obd.repository;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.motechproject.ananya.kilkari.obd.profile.ProductionProfile;
import org.motechproject.ananya.kilkari.obd.scheduler.SubSlot;
import org.motechproject.ananya.kilkari.obd.service.OBDProperties;
import org.motechproject.ananya.kilkari.obd.service.request.InvalidFailedCallReports;
import org.motechproject.ananya.kilkari.obd.service.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

@Component
@ProductionProfile
public class OnMobileOBDGatewayImpl implements OnMobileOBDGateway {
    public static final String START_DATE_PARAM_NAME = "startDate";
    public static final String END_DATE_PARAM_NAME = "endDate";
    private HttpClient obdHttpClient;
    private OBDProperties obdProperties;
    private RestTemplate restTemplate;

    private final Logger logger = LoggerFactory.getLogger(OnMobileOBDGatewayImpl.class);
    private final Logger requestResponseLogger = LoggerFactory.getLogger("RequestResponseLogger");


    @Autowired
    public OnMobileOBDGatewayImpl(HttpClient obdHttpClient, OBDProperties obdProperties, RestTemplate kilkariRestTemplate) {
        this.obdHttpClient = obdHttpClient;
        this.obdProperties = obdProperties;
        this.restTemplate = kilkariRestTemplate;
    }

    @Override
    public void sendNewMessages(String content, SubSlot subSlot) {
        String url = obdProperties.getMessageDeliveryBaseUrl();
        String date = getCurrentDate();
        send(content, url, String.format("%s%s", date, obdProperties.getMainSlotStartTimeFor(subSlot.name())), String.format("%s%s", date, obdProperties.getMainSlotEndTimeFor(subSlot.name())));
    }

    @Override
    public void sendRetryMessages(String content, SubSlot subSlot) {
        String url = obdProperties.getMessageDeliveryBaseUrl();
        String date = getCurrentDate();
        send(content, url, String.format("%s%s", date, obdProperties.getRetrySlotStartTimeFor(subSlot.name())), String.format("%s%s", date, obdProperties.getRetrySlotEndTimeFor(subSlot.name())));
    }

    private String getCurrentDate() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd");
        return dateTimeFormatter.print(DateTime.now());
    }

    @Override
    public void sendInvalidFailureRecord(final InvalidFailedCallReports invalidFailedCallReports) {
        String url = String.format("%s?%s=%s", obdProperties.getFailureReportUrl(), "msisdn", "{msisdn}");
        HashMap<String, String> urlVariables = new HashMap<String, String>() {{
            put("msisdn", formatInvalidFailureRecordRequestParam(invalidFailedCallReports));
        }};
        restTemplate.postForEntity(url, "", String.class, urlVariables);
    }

    private String formatInvalidFailureRecordRequestParam(InvalidFailedCallReports invalidFailedCallReports) {
        return JsonUtils.toJson(invalidFailedCallReports.getRecordObjectFaileds());
    }

    private void send(String content, String url, String slotStartDate, String slotEndDate) {
        logger.info(String.format("Uploading the campaign messages to url: %s", url));
        logger.debug(String.format("Uploading campaign messages content : %s", content));

        String fileName = obdProperties.getMessageDeliveryFileName();
        String file = obdProperties.getMessageDeliveryFile();

        HttpPost httpPost = new HttpPost(url);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart(file, new ByteArrayBody(content.getBytes(), "text/plain", fileName));

        try {
            reqEntity.addPart(START_DATE_PARAM_NAME, new StringBody(slotStartDate));
            reqEntity.addPart(END_DATE_PARAM_NAME, new StringBody(slotEndDate));
            logRequest(url, slotStartDate, slotEndDate);

            httpPost.setEntity(reqEntity);

            HttpResponse response = obdHttpClient.execute(httpPost);
            String responseContent = readResponse(response); //Read the response from stream first thing. As it should be read completely before making any other request.
            validateResponse(response, responseContent);
            logger.info(String.format("Uploaded campaign messages successfully.\nResponse:\n%s", responseContent));
        } catch (IOException ex) {
            logger.error("Sending messages to OBD failed", ex);
            throw new RuntimeException(ex);
        }
    }

    private void logRequest(String url, String slotStartDate, String slotEndDate) {
        requestResponseLogger.debug("Before request [" + url + "?" + START_DATE_PARAM_NAME + "=" + slotStartDate + "&" + END_DATE_PARAM_NAME + "=" + slotEndDate + "]");
    }

    private void validateResponse(HttpResponse response, String responseContent) {
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        String reasonPhrase = statusLine.getReasonPhrase();
        if (!isStatusOk(statusCode)) {
            String errorMessage = String.format("Sending messages to OBD failed with code: %s, reason: %s", statusCode, reasonPhrase);
            logger.error(errorMessage);
            logger.error(String.format("response:\n%s", responseContent));
            throw new RuntimeException(errorMessage);
        }
    }

    private String readResponse(HttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            String content = "";
            String line;
            while ((line = reader.readLine()) != null) {
                content += line;
            }
            return content;
        } catch (IOException ex) {
            logger.warn("Could not read response");
            return null;
        }
    }

    private boolean isStatusOk(int status) {
        return status >= 200 && status <= 299;
    }
}