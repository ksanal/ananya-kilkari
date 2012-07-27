package org.motechproject.ananya.kilkari.obd.gateway;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.motechproject.ananya.kilkari.obd.request.InvalidFailedCallReport;
import org.motechproject.ananya.kilkari.obd.request.InvalidFailedCallReports;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class OnMobileOBDGatewayImplTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private OBDProperties obdProperties;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private OnMobileOBDGateway onMobileOBDGateway;

    @Before
    public void setUp() {
        initMocks(this);
        when(obdProperties.getMessageDeliveryBaseUrl()).thenReturn("mybaseurl");
        when(obdProperties.getMessageDeliveryFileName()).thenReturn("myfile.txt");
        when(obdProperties.getMessageDeliveryFile()).thenReturn("myfile");
        when(obdProperties.getNewMessageSlotStartTime()).thenReturn("130000");
        when(obdProperties.getNewMessageSlotEndTime()).thenReturn("160000");
        when(obdProperties.getRetryMessageSlotStartTime()).thenReturn("180000");
        when(obdProperties.getRetryMessageSlotEndTime()).thenReturn("200000");
        when(obdProperties.getFailureReportUrl()).thenReturn("failureUrl");

        onMobileOBDGateway = new OnMobileOBDGatewayImpl(httpClient, obdProperties, restTemplate);
    }

    @Test
    public void shouldPostTheMessagesFileToOnMobileInNewSlot() throws IOException {
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(statusLine.getReasonPhrase()).thenReturn("created");

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);

        String expectedContent = "expectedContent";

        onMobileOBDGateway.sendNewMessages(expectedContent);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        HttpPost httpPost = (HttpPost) captor.getValue();
        MultipartEntity multipartEntity = (MultipartEntity) httpPost.getEntity();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd");
        String date = dateTimeFormatter.print(DateTime.now());

        assertEquals("mybaseurl", httpPost.getURI().toString());

        String actualContent = readRequest(multipartEntity);

        assertTrue(actualContent.contains(expectedContent));
        assertTrue(actualContent.contains("form-data; name=\"myfile\"; filename=\"myfile.txt\""));
        assertTrue(actualContent.contains(String.format("form-data; name=\"%s\"%s%s", "startDate", date, "130000")));
        assertTrue(actualContent.contains(String.format("form-data; name=\"%s\"%s%s", "endDate", date, "160000")));
    }

    @Test
    public void shouldPostTheMessagesFileToOnMobileInRetrySlot() throws IOException {
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(statusLine.getReasonPhrase()).thenReturn("created");

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);

        String expectedContent = "expectedContent";

        onMobileOBDGateway.sendRetryMessages(expectedContent);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        HttpPost httpPost = (HttpPost) captor.getValue();
        MultipartEntity multipartEntity = (MultipartEntity) httpPost.getEntity();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd");
        String date = dateTimeFormatter.print(DateTime.now());

        assertEquals("mybaseurl", httpPost.getURI().toString());

        String actualContent = readRequest(multipartEntity);

        assertTrue(actualContent.contains(expectedContent));
        assertTrue(actualContent.contains("form-data; name=\"myfile\"; filename=\"myfile.txt\""));
        assertTrue(actualContent.contains(String.format("form-data; name=\"%s\"%s%s", "startDate", date, "180000")));
        assertTrue(actualContent.contains(String.format("form-data; name=\"%s\"%s%s", "endDate", date, "200000")));
    }

    @Test
    public void shouldThrowExceptionForAFailureResponse() throws IOException {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Sending messages to OBD failed with code: 500, reason: failed");

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(500);
        when(statusLine.getReasonPhrase()).thenReturn("failed");

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);

        onMobileOBDGateway.sendNewMessages("content");
    }

    private String readRequest(MultipartEntity multipartEntity) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        multipartEntity.writeTo(out);
        out.close();

        BufferedReader bfr = new BufferedReader(new InputStreamReader(in));
        String line;
        String actualContent = "";
        while((line = bfr.readLine()) != null) {
            actualContent += line;
        }
        return actualContent;
    }

    @Test
    public void shouldSendInvalidFailureRecordsToObd() {
        InvalidFailedCallReports invalidFailedCallReports = new InvalidFailedCallReports();
        ArrayList<InvalidFailedCallReport> recordObjectFaileds = new ArrayList<>();
        recordObjectFaileds.add(new InvalidFailedCallReport("msisdn1", "subscriptionId1", "description1"));
        recordObjectFaileds.add(new InvalidFailedCallReport("msisdn2", "subscriptionId2", "description2"));
        invalidFailedCallReports.setRecordObjectFaileds(recordObjectFaileds);

        onMobileOBDGateway.sendInvalidFailureRecord(invalidFailedCallReports);

        ArgumentCaptor<InvalidFailedCallReports> invalidCallDeliveryFailureRecordArgumentCaptor = ArgumentCaptor.forClass(InvalidFailedCallReports.class);
        ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForLocation(urlArgumentCaptor.capture(), invalidCallDeliveryFailureRecordArgumentCaptor.capture());
        String actualUrl = urlArgumentCaptor.getValue();
        List<InvalidFailedCallReport> actualRecordObjectFaileds = invalidCallDeliveryFailureRecordArgumentCaptor.getValue().getRecordObjectFaileds();

        assertEquals("failureUrl", actualUrl);
        assertEquals(2, actualRecordObjectFaileds.size());
        assertEquals("msisdn1", actualRecordObjectFaileds.get(0).getMsisdn());
        assertEquals("subscriptionId1", actualRecordObjectFaileds.get(0).getSubscriptionId());
        assertEquals("description1", actualRecordObjectFaileds.get(0).getDescription());
        assertEquals("msisdn2", actualRecordObjectFaileds.get(1).getMsisdn());
        assertEquals("subscriptionId2", actualRecordObjectFaileds.get(1).getSubscriptionId());
        assertEquals("description2", actualRecordObjectFaileds.get(1).getDescription());
    }
}
