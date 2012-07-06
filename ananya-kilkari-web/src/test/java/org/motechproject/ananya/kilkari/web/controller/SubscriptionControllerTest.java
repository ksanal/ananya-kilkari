package org.motechproject.ananya.kilkari.web.controller;

import com.google.gson.Gson;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.motechproject.ananya.kilkari.builder.SubscriptionRequestBuilder;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.ananya.kilkari.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.service.KilkariSubscriptionService;
import org.motechproject.ananya.kilkari.service.ReportingService;
import org.motechproject.ananya.kilkari.service.SubscriptionService;
import org.motechproject.ananya.kilkari.web.HttpConstants;
import org.motechproject.ananya.kilkari.web.contract.response.BaseResponse;
import org.motechproject.ananya.kilkari.web.contract.response.SubscriberResponse;
import org.motechproject.ananya.kilkari.web.contract.response.SubscriptionDetails;
import org.motechproject.ananya.kilkari.web.domain.CallbackAction;
import org.motechproject.ananya.kilkari.web.domain.CallbackStatus;
import org.springframework.http.MediaType;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.ananya.kilkari.web.MVCTestUtils.mockMvc;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;

public class SubscriptionControllerTest {
    private SubscriptionController subscriptionController;

    @Mock
    private Subscription mockedSubscription;

    @Mock
    private KilkariSubscriptionService kilkariSubscriptionService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ReportingService reportingService;
    private static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript;charset=UTF-8";
    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
    private static final String IVR_RESPONSE_PREFIX = "var response = ";

    @Before
    public void setUp() {
        initMocks(this);
        subscriptionController = new SubscriptionController(kilkariSubscriptionService, reportingService, subscriptionService);
    }

    @Test
    public void shouldGetSubscriptionsForGivenMsisdnForChannelIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "ivr";

        mockSubscription(msisdn);
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(mockedSubscription);
        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenReturn(subscriptions);

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JAVASCRIPT))
                .andExpect(content().string(subscriberResponseMatcherWithSubscriptions(channel)));
    }

    @Test
    public void shouldGetSubscriptionsForGivenMsisdnForChannelOtherThanIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "call_center";

        mockSubscription(msisdn);
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(mockedSubscription);
        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenReturn(subscriptions);

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JSON))
                .andExpect(content().string(subscriberResponseMatcherWithSubscriptions(channel)));
    }

    @Test
    public void shouldGetEmptySubscriptionResponseIfThereAreNoSubscriptionsForAGivenMsisdnForIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "ivr";

        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenReturn(null);

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type(HttpConstants.IVR.getResponseContentType()))
                .andExpect(content().string(subscriberResponseMatcherWithNoSubscriptions(channel)));
    }

    @Test
    public void shouldGetEmptySubscriptionResponseIfThereAreNoSubscriptionsForAGivenMsisdnOtherThanIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "call_center";

        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenReturn(null);

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JSON))
                .andExpect(content().string(subscriberResponseMatcherWithNoSubscriptions(channel)));
    }

    @Test
    public void shouldReturnErrorResponseForInvalidMsisdnNumberForIvr() throws Exception {
        String msisdn = "12345";
        String channel = "ivr";

        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenThrow(new ValidationException("Invalid Msisdn"));

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JAVASCRIPT))
                .andExpect(content().string(errorResponseMatcherForInvalidMsisdn(channel)));

    }

    @Test
    public void shouldReturnErrorResponseForInvalidMsisdnNumberOtherThanIvr() throws Exception {
        String msisdn = "12345";
        String channel = "call_center";

        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenThrow(new ValidationException("Invalid Msisdn"));

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().is(400))
                .andExpect(content().type(CONTENT_TYPE_JSON))
                .andExpect(content().string(errorResponseMatcherForInvalidMsisdn(channel)));
    }

    @Test
    public void shouldReturnCorrectErrorResponseForRuntimeExceptionForIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "ivr";

        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenThrow(new RuntimeException("runtime exception"));

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JAVASCRIPT))
                .andExpect(content().string(errorResponseMatcherForRuntimeException(channel)));
    }

    @Test
    public void shouldReturnCorrectErrorResponseForRuntimeExceptionForOtherThanIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "call_center";

        when(kilkariSubscriptionService.getSubscriptionsFor(msisdn)).thenThrow(new RuntimeException("runtime exception"));

        mockMvc(subscriptionController)
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().is(500))
                .andExpect(content().type(CONTENT_TYPE_JSON))
                .andExpect(content().string(errorResponseMatcherForRuntimeException(channel)));
    }

    @Test
    public void shouldCreateNewSubscriptionEvent() throws Exception {
        String msisdn = "1234567890";
        String channel = "ivr";
        String pack = "twelve-months";
        DateTime beforeCreate = DateTime.now();

        mockMvc(subscriptionController)
                .perform(get("/subscription").param("msisdn", msisdn).param("channel", channel).param("pack", pack))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JAVASCRIPT))
                .andExpect(content().string(baseResponseMatcher("SUCCESS", "Subscription request submitted successfully")));

        ArgumentCaptor<SubscriptionRequest> subscriptionRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(kilkariSubscriptionService).createSubscription(subscriptionRequestArgumentCaptor.capture());
        SubscriptionRequest subscriptionRequest = subscriptionRequestArgumentCaptor.getValue();

        assertEquals(msisdn, subscriptionRequest.getMsisdn());
        assertEquals(pack, subscriptionRequest.getPack());
        assertEquals(channel, subscriptionRequest.getChannel());

        assertCreatedAt(beforeCreate, subscriptionRequest);
    }

    @Test
    public void shouldCreateNewSubscriptionEventForCC() throws Exception {
        DateTime beforeCreate = DateTime.now();

        SubscriptionRequest expectedRequest= new SubscriptionRequestBuilder().withDefaults().build();

        when(reportingService.getLocation("district", "block", "panchayat")).thenReturn(new SubscriberLocation("district", "block", "panchayat"));
        mockMvc(subscriptionController)
                .perform(post("/subscription").body(toJson(expectedRequest).getBytes()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JSON))
                .andExpect(content().string(baseResponseMatcher("SUCCESS", "Subscription request submitted successfully")));

        ArgumentCaptor<SubscriptionRequest> subscriptionRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(kilkariSubscriptionService).createSubscription(subscriptionRequestArgumentCaptor.capture());
        SubscriptionRequest subscriptionRequest = subscriptionRequestArgumentCaptor.getValue();

        assertTrue(expectedRequest.equals(subscriptionRequest));
        assertCreatedAt(beforeCreate, subscriptionRequest);
    }


    @Test
    public void shouldGiveAnErrorMessageWhenCallBackRequestIsInvalid() throws Exception {
        String subscriptionId = "abcd1234";
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("invalidMsisdn");
        callbackRequest.setAction("invalidAction");
        callbackRequest.setStatus("invalidStatus");
        callbackRequest.setOperator("invalidOperator");

        byte[] requestBody = toJson(callbackRequest).getBytes();

        mockMvc(subscriptionController)
                .perform(put("/subscription/" + subscriptionId)
                        .body(requestBody).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JSON))
                .andExpect(content().string(baseResponseMatcher("ERROR", "Callback Request Invalid: Invalid msisdn invalidMsisdn,Invalid callbackAction invalidAction,Invalid callbackStatus invalidStatus,Invalid operator invalidOperator")));

        verifyZeroInteractions(kilkariSubscriptionService);
    }

    @Test
    public void shouldPublishTheCallbackRequestIfValidationSucceeds() throws Exception {
        String subscriptionId = "abcd1234";
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        callbackRequest.setReason("reason");
        callbackRequest.setOperator(Operator.AIRTEL.name());
        callbackRequest.setGraceCount("2");
        byte[] requestBody = toJson(callbackRequest).getBytes();

        mockMvc(subscriptionController)
                .perform(put("/subscription/" + subscriptionId)
                        .body(requestBody).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JSON))
                .andExpect(content().string(baseResponseMatcher("SUCCESS", "Callback request processed successfully")));

        ArgumentCaptor<CallbackRequestWrapper> callbackRequestWrapperArgumentCaptor = ArgumentCaptor.forClass(CallbackRequestWrapper.class);
        verify(kilkariSubscriptionService).processCallbackRequest(callbackRequestWrapperArgumentCaptor.capture());
        CallbackRequestWrapper callbackRequestWrapper = callbackRequestWrapperArgumentCaptor.getValue();

        assertEquals(subscriptionId, callbackRequestWrapper.getSubscriptionId());
        assertEquals(CallbackAction.ACT.name(), callbackRequestWrapper.getAction());
        assertEquals(CallbackStatus.SUCCESS.name(), callbackRequestWrapper.getStatus());
        assertNotNull(callbackRequestWrapper.getCreatedAt());
    }

    @Test
    public void shouldValidateSubscriptionRequestIfCreateRequestIsCalledFromCallCenter() {
        SubscriptionRequest subscriptionRequest = Mockito.mock(SubscriptionRequest.class);
        when(subscriptionRequest.getChannel()).thenReturn(Channel.CALL_CENTER.name());
        doThrow(new ValidationException("validation error")).when(subscriptionRequest).validate(reportingService, subscriptionService);
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage(is("validation error"));

        subscriptionController.createSubscription(subscriptionRequest);
        verify(subscriptionRequest).validate(reportingService, subscriptionService);
    }

    @Test
    public void shouldValidateSubscriptionRequestIfCreateRequestIsCalledFromInvalidChannel() {
        SubscriptionRequest subscriptionRequest = Mockito.mock(SubscriptionRequest.class);
        when(subscriptionRequest.getChannel()).thenReturn("invalid_channel");
        doThrow(new ValidationException("validation error")).when(subscriptionRequest).validate(reportingService, subscriptionService);
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage(is("validation error"));

        subscriptionController.createSubscription(subscriptionRequest);
        verify(subscriptionRequest).validate(reportingService, subscriptionService);
    }

    @Test
    public void shouldValidateSubscriptionRequestIfCreateRequestIsCalledWithoutChannel() {
        SubscriptionRequest subscriptionRequest = Mockito.mock(SubscriptionRequest.class);
        when(subscriptionRequest.getChannel()).thenReturn(null);
        doThrow(new ValidationException("validation error")).when(subscriptionRequest).validate(reportingService, subscriptionService);
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage(is("validation error"));

        subscriptionController.createSubscription(subscriptionRequest);
        verify(subscriptionRequest).validate(reportingService, subscriptionService);
    }

    @Test
    public void shouldNotValidateSubscriptionRequestIfCreateRequestIsCalledFromIVR() {
        SubscriptionRequest subscriptionRequest = Mockito.mock(SubscriptionRequest.class);
        when(subscriptionRequest.getChannel()).thenReturn(Channel.IVR.name());
        subscriptionController.createSubscription(subscriptionRequest);
        verify(subscriptionRequest,never()).validate(reportingService, subscriptionService);
    }

    @Test
    public void shouldNotValidateSubscriptionRequestIfCreateRequestIsCalledFromIVRCaseInsensitive() {
        SubscriptionRequest subscriptionRequest = Mockito.mock(SubscriptionRequest.class);
        when(subscriptionRequest.getChannel()).thenReturn("ivR");
        subscriptionController.createSubscription(subscriptionRequest);
        verify(subscriptionRequest, never()).validate(reportingService, subscriptionService);
    }

    @Test
    public void shouldPublishSubscriberCareEventIntoTheQueue() throws Exception {
        String msisdn = "1234567890";
        String reason = SubscriberCareReasons.CHANGE_PACK.name();
        String channel = "ivr";

        mockMvc(subscriptionController)
                .perform(get("/help")
                        .param("msisdn", msisdn).param("reason", reason).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type(CONTENT_TYPE_JAVASCRIPT))
                .andExpect(content().string(baseResponseMatcher("SUCCESS", "Subscriber care request processed successfully")));

        ArgumentCaptor<SubscriberCareRequest> subscriberCareRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriberCareRequest.class);
        verify(kilkariSubscriptionService).processSubscriberCareRequest(subscriberCareRequestArgumentCaptor.capture());
        SubscriberCareRequest careRequest = subscriberCareRequestArgumentCaptor.getValue();

        assertEquals(msisdn, careRequest.getMsisdn());
        assertEquals(reason, careRequest.getReason());
    }

    private void mockSubscription(String msisdn) {
        when(mockedSubscription.getMsisdn()).thenReturn(msisdn);
        when(mockedSubscription.getPack()).thenReturn(SubscriptionPack.FIFTEEN_MONTHS);
        when(mockedSubscription.getStatus()).thenReturn(SubscriptionStatus.NEW);
        when(mockedSubscription.getSubscriptionId()).thenReturn("subscription-id");
    }

    private void assertCreatedAt(DateTime beforeCreate, SubscriptionRequest subscriptionRequest) {
        DateTime createdAt = subscriptionRequest.getCreatedAt();
        DateTime afterCreate =  DateTime.now();
        assertTrue(createdAt.isEqual(beforeCreate) || createdAt.isAfter(beforeCreate));
        assertTrue(createdAt.isEqual(afterCreate) || createdAt.isBefore(afterCreate));
    }

    private BaseMatcher<String> baseResponseMatcher(final String status, final String description) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return assertBaseResponse((String) o, status, description);
            }

            @Override
            public void describeTo(Description matcherDescription) {
            }
        };
    }

    private Matcher<String> errorResponseMatcherForInvalidMsisdn(final String channel) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return assertErrorResponseForInvalidMsisdn((String) o, channel);
            }

            @Override
            public void describeTo(Description matcherDescription) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    private Matcher<String> errorResponseMatcherForRuntimeException(final String channel) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return assertErrorResponseForRuntimeException((String) o, channel);
            }

            @Override
            public void describeTo(Description matcherDescription) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    private BaseMatcher<String> subscriberResponseMatcherWithNoSubscriptions(final String channel) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return assertSubscriberResponseWithNoSubscriptions((String) o, channel);
            }

            @Override
            public void describeTo(Description matcherDescription) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    private BaseMatcher<String> subscriberResponseMatcherWithSubscriptions(final String channel) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return assertSubscriberResponse((String) o, channel);
            }

            @Override
            public void describeTo(Description description) {
            }
        };
    }

    private boolean assertBaseResponse(String jsonContent, String status, String description) {
        BaseResponse baseResponse = fromJson(jsonContent.replace("var response = ", ""), BaseResponse.class);

        return baseResponse.getStatus().equals(status)
                && baseResponse.getDescription().equals(description);
    }

    private String performIVRChannelValidationAndCleanup(String jsonContent, String channel) {
        if (Channel.isIVR(channel)) {
            assertTrue(jsonContent.startsWith(IVR_RESPONSE_PREFIX));
            jsonContent = jsonContent.replace(IVR_RESPONSE_PREFIX, "");
        }
        return jsonContent;
    }

    private boolean assertErrorResponseForInvalidMsisdn(String jsonContent, String channel) {
        jsonContent = performIVRChannelValidationAndCleanup(jsonContent, channel);

        BaseResponse baseResponse = fromJson(jsonContent, BaseResponse.class);

        return baseResponse.isError() &&
                baseResponse.getDescription().equals("Invalid Msisdn");
    }

    private boolean assertErrorResponseForRuntimeException(String jsonContent, String channel) {
        jsonContent = performIVRChannelValidationAndCleanup(jsonContent, channel);

        BaseResponse baseResponse = fromJson(jsonContent, BaseResponse.class);

        return baseResponse.isError() &&
                baseResponse.getDescription().equals("runtime exception");
    }

    private boolean assertSubscriberResponse(String jsonContent, String channel) {
        jsonContent = performIVRChannelValidationAndCleanup(jsonContent, channel);

        SubscriberResponse subscriberResponse = fromJson(jsonContent, SubscriberResponse.class);
        SubscriptionDetails subscriptionDetails = subscriberResponse.getSubscriptionDetails().get(0);

        return subscriptionDetails.getPack().equals(mockedSubscription.getPack().name())
                && subscriptionDetails.getStatus().equals(mockedSubscription.getStatus().name())
                && subscriptionDetails.getSubscriptionId().equals(mockedSubscription.getSubscriptionId());
    }

    private boolean assertSubscriberResponseWithNoSubscriptions(String jsonContent, String channel) {
        jsonContent = performIVRChannelValidationAndCleanup(jsonContent, channel);

        SubscriberResponse subscriberResponse = fromJson(jsonContent, SubscriberResponse.class);

        return subscriberResponse.getSubscriptionDetails().size() == 0;
    }

    private String toJson(Object objectToSerialize) {
        Gson gson = new Gson();
        return gson.toJson(objectToSerialize);
    }

    private <T> T fromJson(String jsonString, Class<T> subscriberResponseClass) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, subscriberResponseClass);
    }
}
