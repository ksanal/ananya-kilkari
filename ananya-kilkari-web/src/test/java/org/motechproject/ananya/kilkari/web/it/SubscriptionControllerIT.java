package org.motechproject.ananya.kilkari.web.it;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.motechproject.ananya.kilkari.builder.SubscriptionRequestBuilder;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.ananya.kilkari.gateway.OnMobileSubscriptionGateway;
import org.motechproject.ananya.kilkari.gateway.ReportingGateway;
import org.motechproject.ananya.kilkari.messagecampaign.request.KilkariMessageCampaignEnrollmentRecord;
import org.motechproject.ananya.kilkari.messagecampaign.service.KilkariMessageCampaignService;
import org.motechproject.ananya.kilkari.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.service.stub.StubOnMobileSubscriptionGateway;
import org.motechproject.ananya.kilkari.service.stub.StubReportingGateway;
import org.motechproject.ananya.kilkari.web.SpringIntegrationTest;
import org.motechproject.ananya.kilkari.web.TestUtils;
import org.motechproject.ananya.kilkari.web.contract.mapper.SubscriptionDetailsMapper;
import org.motechproject.ananya.kilkari.web.contract.response.BaseResponse;
import org.motechproject.ananya.kilkari.web.contract.response.SubscriberResponse;
import org.motechproject.ananya.kilkari.web.controller.SubscriptionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.server.MvcResult;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.motechproject.ananya.kilkari.web.MVCTestUtils.mockMvc;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;

public class SubscriptionControllerIT extends SpringIntegrationTest {

    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private AllSubscriptions allSubscriptions;

    @Autowired
    private KilkariMessageCampaignService kilkariMessageCampaignService;

    @Autowired
    private StubReportingGateway reportingService;

    @Autowired
    private StubOnMobileSubscriptionGateway onMobileSubscriptionService;

    @Before
    public void setUp() {
        allSubscriptions.removeAll();
    }

    private static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript;charset=UTF-8";
    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
    private static final String IVR_RESPONSE_PREFIX = "var response = ";

    private static final String TWELVE_MONTH_CAMPAIGN_NAME = "kilkari-mother-child-campaign-twelve-months";
    private static final String FIFTEEN_MONTH_CAMPAIGN_NAME = "kilkari-mother-child-campaign-fifteen-months";

    @Test
    public void shouldRetrieveSubscriptionDetailsFromDatabase() throws Exception {
        String msisdn = "9876543210";
        String channelString = Channel.IVR.toString();
        Subscription subscription1 = new Subscription(msisdn, SubscriptionPack.TWELVE_MONTHS, DateTime.now());
        Subscription subscription2 = new Subscription(msisdn, SubscriptionPack.FIFTEEN_MONTHS, DateTime.now());
        allSubscriptions.add(subscription1);
        allSubscriptions.add(subscription2);
        markForDeletion(subscription1);
        markForDeletion(subscription2);

        SubscriberResponse subscriberResponse = new SubscriberResponse();
        subscriberResponse.addSubscriptionDetail(SubscriptionDetailsMapper.mapFrom(subscription1));
        subscriberResponse.addSubscriptionDetail(SubscriptionDetailsMapper.mapFrom(subscription2));

        reportingService.setBehavior(mock(ReportingGateway.class));
        onMobileSubscriptionService.setBehavior(mock(OnMobileSubscriptionGateway.class));

        MvcResult result = mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channelString))
                .andExpect(status().isOk())
                .andExpect(content().type(SubscriptionControllerIT.CONTENT_TYPE_JAVASCRIPT))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        responseString = performIVRChannelValidationAndCleanup(responseString, channelString);

        SubscriberResponse actualResponse = TestUtils.fromJson(responseString, SubscriberResponse.class);
        assertEquals(subscriberResponse, actualResponse);
    }

    @Test
    public void shouldCreateSubscriptionForTheGivenMsisdnForTheIVRChannel() throws Exception {

        final String msisdn = "9876543210";
        String channelString = Channel.IVR.toString();
        final SubscriptionPack pack = SubscriptionPack.TWELVE_MONTHS;
        BaseResponse expectedResponse = new BaseResponse("SUCCESS", "Subscription request submitted successfully");

        reportingService.setBehavior(mock(ReportingGateway.class));
        onMobileSubscriptionService.setBehavior(mock(OnMobileSubscriptionGateway.class));

        MvcResult result = mockMvc(subscriptionController)
                .perform(get("/subscription").param("msisdn", msisdn).param("pack", pack.toString())
                        .param("channel", channelString))
                .andExpect(status().isOk())
                .andExpect(content().type(SubscriptionControllerIT.CONTENT_TYPE_JAVASCRIPT))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        responseString = performIVRChannelValidationAndCleanup(responseString, channelString);

        BaseResponse actualResponse =  TestUtils.fromJson(responseString, BaseResponse.class);
        assertEquals(expectedResponse, actualResponse);

        final Subscription subscription = new TimedRunner<Subscription>(20, 1000) {
            @Override
            Subscription run() {
                List<Subscription> subscriptionList = allSubscriptions.findByMsisdnAndPack(msisdn, pack);
                return subscriptionList.isEmpty() ? null : subscriptionList.get(0);
            }
        }.execute();

        assertNotNull(subscription);
        markForDeletion(subscription);
        assertEquals(msisdn, subscription.getMsisdn());
        assertEquals(pack, subscription.getPack());
        assertFalse(StringUtils.isBlank(subscription.getSubscriptionId()));


        KilkariMessageCampaignEnrollmentRecord campaignEnrollmentRecord = new TimedRunner<KilkariMessageCampaignEnrollmentRecord>(20, 1000) {
            @Override
            KilkariMessageCampaignEnrollmentRecord run() {
                KilkariMessageCampaignEnrollmentRecord enrollmentRecord = kilkariMessageCampaignService.searchEnrollment(
                        subscription.getSubscriptionId(), SubscriptionControllerIT.TWELVE_MONTH_CAMPAIGN_NAME);
                return enrollmentRecord;
            }
        }.execute();


        assertNotNull(campaignEnrollmentRecord);
        assertEquals(subscription.getSubscriptionId(), campaignEnrollmentRecord.getExternalId());
        assertEquals(SubscriptionControllerIT.TWELVE_MONTH_CAMPAIGN_NAME, campaignEnrollmentRecord.getCampaignName());
        List<DateTime> messageTimings = kilkariMessageCampaignService.getMessageTimings(
                subscription.getSubscriptionId(), pack.name(), DateTime.now().minusDays(3), DateTime.now().plusYears(4));
        assertEquals(48, messageTimings.size());
    }

    @Test
    public void shouldCreateSubscriptionForTheGivenMsisdnForTheCallCentreChannel() throws Exception {
        final String msisdn = "9876543210";
        String channelString = Channel.CALL_CENTER.toString();
        final SubscriptionPack pack = SubscriptionPack.FIFTEEN_MONTHS;
        BaseResponse expectedResponse = new BaseResponse("SUCCESS", "Subscription request submitted successfully");

        ReportingGateway mockedReportingGateway = Mockito.mock(ReportingGateway.class);
        when(mockedReportingGateway.getLocation("district", "block", "panchayat")).thenReturn(new SubscriberLocation("district", "block", "panchayat"));
        reportingService.setBehavior(mockedReportingGateway);
        onMobileSubscriptionService.setBehavior(mock(OnMobileSubscriptionGateway.class));

        SubscriptionRequest expectedRequest = new SubscriptionRequestBuilder().withDefaults().build();
        MvcResult result = mockMvc(subscriptionController)
                .perform(post("/subscription").body(TestUtils.toJson(expectedRequest).getBytes()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().type(SubscriptionControllerIT.CONTENT_TYPE_JSON))
                .andReturn();


        String responseString = result.getResponse().getContentAsString();
        responseString = performIVRChannelValidationAndCleanup(responseString, channelString);

        BaseResponse actualResponse =  TestUtils.fromJson(responseString, BaseResponse.class);
        assertEquals(expectedResponse, actualResponse);

        final Subscription subscription = new TimedRunner<Subscription>(20, 1000) {
            @Override
            Subscription run() {
                List<Subscription> subscriptions = allSubscriptions.findByMsisdnAndPack(msisdn, pack);
                return subscriptions.isEmpty() ? null : subscriptions.get(0);

            }
        }.execute();

        assertNotNull(subscription);
        markForDeletion(subscription);
        assertEquals(msisdn, subscription.getMsisdn());
        assertEquals(pack, subscription.getPack());
        assertFalse(StringUtils.isBlank(subscription.getSubscriptionId()));


        KilkariMessageCampaignEnrollmentRecord campaignEnrollmentRecord = new TimedRunner<KilkariMessageCampaignEnrollmentRecord>(20, 1000) {
            @Override
            KilkariMessageCampaignEnrollmentRecord run() {
                return kilkariMessageCampaignService.searchEnrollment(
                        subscription.getSubscriptionId(), SubscriptionControllerIT.FIFTEEN_MONTH_CAMPAIGN_NAME);

            }
        }.execute();


        assertNotNull(campaignEnrollmentRecord);
        assertEquals(subscription.getSubscriptionId(), campaignEnrollmentRecord.getExternalId());
        assertEquals(SubscriptionControllerIT.FIFTEEN_MONTH_CAMPAIGN_NAME, campaignEnrollmentRecord.getCampaignName());
        List<DateTime> messageTimings = kilkariMessageCampaignService.getMessageTimings(
                subscription.getSubscriptionId(), pack.name(), DateTime.now().minusDays(3), DateTime.now().plusYears(4));
        assertEquals(60, messageTimings.size());
    }

    private String performIVRChannelValidationAndCleanup(String jsonContent, String channel) {
        if (Channel.isIVR(channel)) {
            assertTrue(jsonContent.startsWith(SubscriptionControllerIT.IVR_RESPONSE_PREFIX));
            jsonContent = jsonContent.replace(SubscriptionControllerIT.IVR_RESPONSE_PREFIX, "");
        }
        return jsonContent;
    }
}
