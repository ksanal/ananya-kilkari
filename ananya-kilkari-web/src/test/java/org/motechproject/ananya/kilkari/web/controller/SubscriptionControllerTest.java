package org.motechproject.ananya.kilkari.web.controller;

import com.google.gson.Gson;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.ananya.kilkari.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.service.SubscriptionService;
import org.motechproject.ananya.kilkari.web.KilkariExceptionResolver;
import org.motechproject.ananya.kilkari.web.domain.CallbackAction;
import org.motechproject.ananya.kilkari.web.domain.CallbackStatus;
import org.motechproject.ananya.kilkari.web.domain.KilkariConstants;
import org.motechproject.ananya.kilkari.web.interceptors.KilkariChannelInterceptor;
import org.motechproject.ananya.kilkari.web.response.BaseResponse;
import org.motechproject.ananya.kilkari.web.response.SubscriberResponse;
import org.motechproject.ananya.kilkari.web.response.SubscriptionDetails;
import org.motechproject.ananya.kilkari.web.services.SubscriptionPublisher;
import org.motechproject.ananya.kilkari.web.views.ExceptionView;
import org.motechproject.ananya.kilkari.web.views.ValidationExceptionView;
import org.springframework.http.MediaType;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.setup.MockMvcBuilders;
import org.springframework.test.web.server.setup.StandaloneMockMvcBuilder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;

public class SubscriptionControllerTest {
    private SubscriptionController subscriptionController;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Subscription mockedSubscription;

    @Mock
    private SubscriptionPublisher subscriptionPublisher;

    @Before
    public void setUp() {
        initMocks(this);
        subscriptionController = new SubscriptionController(subscriptionService, subscriptionPublisher);
    }

    @Test
    public void shouldGetSubscriptionsForGivenMsisdnForChannelIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "ivr";

        mockSubscription(msisdn);
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(mockedSubscription);
        when(subscriptionService.findByMsisdn(msisdn)).thenReturn(subscriptions);

        MockMvcBuilders.standaloneSetup(subscriptionController).addInterceptors(new KilkariChannelInterceptor()).build()
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type("application/javascript;charset=UTF-8"))
                .andExpect(content().string(subscriberResponseMatcherWithSubscriptions(channel)));
    }

    @Test
    public void shouldGetSubscriptionsForGivenMsisdnForChannelOtherThanIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "call_center";

        mockSubscription(msisdn);
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(mockedSubscription);
        when(subscriptionService.findByMsisdn(msisdn)).thenReturn(subscriptions);

        MockMvcBuilders.standaloneSetup(subscriptionController).addInterceptors(new KilkariChannelInterceptor()).build()
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type("application/json;charset=UTF-8"))
                .andExpect(content().string(subscriberResponseMatcherWithSubscriptions(channel)));
    }

    @Test
    public void shouldGetEmptySubscriptionResponseIfThereAreNoSubscriptionsForAGivenMsisdnForIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "ivr";

        when(subscriptionService.findByMsisdn(msisdn)).thenReturn(null);

        MockMvcBuilders.standaloneSetup(subscriptionController).addInterceptors(new KilkariChannelInterceptor()).build()
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type("application/javascript;charset=UTF-8"))
                .andExpect(content().string(subscriberResponseMatcherWithNoSubscriptions(channel)));
    }

    @Test
    public void shouldGetEmptySubscriptionResponseIfThereAreNoSubscriptionsForAGivenMsisdnOtherThanIvr() throws Exception {
        String msisdn = "1234567890";
        String channel = "call_center";

        when(subscriptionService.findByMsisdn(msisdn)).thenReturn(null);

        MockMvcBuilders.standaloneSetup(subscriptionController).addInterceptors(new KilkariChannelInterceptor()).build()
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type("application/json;charset=UTF-8"))
                .andExpect(content().string(subscriberResponseMatcherWithNoSubscriptions(channel)));
    }

    private class KilkariTestViewResolver implements ViewResolver {

        private Map<String, View> viewMap;

        public KilkariTestViewResolver() {
            this.viewMap = new HashMap<>();
            this.viewMap.put("exceptionView", new ExceptionView());
            this.viewMap.put("validationExceptionView", new ValidationExceptionView());
        }

        @Override
        public View resolveViewName(String viewName, Locale locale) throws Exception {
            return this.viewMap.get(viewName);
        }
    }

    private MockMvc mockMvc() {
        StandaloneMockMvcBuilder mockMvcBuilder = MockMvcBuilders.standaloneSetup(subscriptionController)
                .addInterceptors(new KilkariChannelInterceptor())
                .setViewResolvers(new KilkariTestViewResolver());

        Properties props = new Properties();
        props.put(".Exception", "exceptionView");
        props.put("org.motechproject.ananya.kilkari.exceptions.ValidationException", "validationExceptionView");

        KilkariExceptionResolver exceptionResolver = new KilkariExceptionResolver();
        exceptionResolver.setExceptionMappings(props);

        mockMvcBuilder.setHandlerExceptionResolvers(Arrays.asList(new HandlerExceptionResolver[]{exceptionResolver}));

        return mockMvcBuilder.build();
    }

    @Test
    public void shouldReturnErrorResponseForInvalidMsisdnNumberForIvr() throws Exception {
        String msisdn = "12345";
        String channel = "ivr";

        when(subscriptionService.findByMsisdn(msisdn)).thenThrow(new ValidationException("Invalid Msisdn"));

        mockMvc()
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().isOk())
                .andExpect(content().type("application/javascript;charset=UTF-8"))
                .andExpect(content().string(errorResponseMatcherForInvalidMsisdn(channel)));

    }

    @Test
    public void shouldReturnErrorResponseForInvalidMsisdnNumberOtherThanIvr() throws Exception {
        String msisdn = "12345";
        String channel = "call_center";

        when(subscriptionService.findByMsisdn(msisdn)).thenThrow(new ValidationException("Invalid Msisdn"));

        mockMvc()
                .perform(get("/subscriber").param("msisdn", msisdn).param("channel", channel))
                .andExpect(status().is(KilkariConstants.ERROR_CODE))
                .andExpect(content().type("application/json;charset=UTF-8"))
                .andExpect(content().string(errorResponseMatcherForInvalidMsisdn(channel)));
    }

    @Test
    public void shouldCreateNewSubscriptionEvent() throws Exception {
        String msisdn = "1234567890";
        String channel = "ivr";
        String pack = "twelve-months";
        DateTime beforeCreate = DateTime.now();
        MockMvcBuilders.standaloneSetup(subscriptionController).addInterceptors(new KilkariChannelInterceptor()).build()
                .perform(get("/subscription").param("msisdn", msisdn).param("channel", channel).param("pack", pack))
                .andExpect(status().isOk())
                .andExpect(content().type("application/javascript;charset=UTF-8"))
                .andExpect(content().string(baseResponseMatcher("SUCCESS", "Subscription request submitted successfully")));

        ArgumentCaptor<SubscriptionRequest> subscriptionRequestArgumentCaptor = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(subscriptionPublisher).createSubscription(subscriptionRequestArgumentCaptor.capture());
        SubscriptionRequest subscriptionRequest = subscriptionRequestArgumentCaptor.getValue();

        assertEquals(msisdn, subscriptionRequest.getMsisdn());
        assertEquals(pack, subscriptionRequest.getPack());
        assertEquals(channel, subscriptionRequest.getChannel());

        DateTime createdAt = subscriptionRequest.getCreatedAt();
        assertTrue(createdAt.isEqual(beforeCreate) || createdAt.isAfter(beforeCreate));
        assertTrue(createdAt.isEqualNow() || createdAt.isBeforeNow());
    }

    @Test
    public void shouldGiveAnErrorMessageWhenCallBackRequestIsInvalid() throws Exception {
        String subscriptionId = "abcd1234";
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("invalidMsisdn");
        callbackRequest.setAction("invalidAction");
        callbackRequest.setStatus("invalidStatus");

        byte[] requestBody = toJson(callbackRequest).getBytes();

        MockMvcBuilders.standaloneSetup(subscriptionController).build()
                .perform(put("/subscription/" + subscriptionId)
                        .body(requestBody).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().type("application/json;charset=UTF-8"))
                .andExpect(content().string(baseResponseMatcher("ERROR", "Callback Request Invalid: Invalid msisdn invalidMsisdn,Invalid callbackAction invalidAction,Invalid callbackStatus invalidStatus")));

        verifyZeroInteractions(subscriptionService);
    }

    @Test
    public void shouldActivateTheSubscriptionWhenCallBackUrlIsInvokedWithSuccessStatusForActivationRequest() throws Exception {
        String subscriptionId = "abcd1234";
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        callbackRequest.setReason("reason");
        callbackRequest.setOperator("operator");
        callbackRequest.setGraceCount("2");
        byte[] requestBody = toJson(callbackRequest).getBytes();

        MockMvcBuilders.standaloneSetup(subscriptionController).build()
                .perform(put("/subscription/" + subscriptionId)
                        .body(requestBody).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().type("application/json;charset=UTF-8"))
                .andExpect(content().string(baseResponseMatcher("SUCCESS", "Callback request processed successfully")));

        verify(subscriptionService).activate(subscriptionId);
    }

    @Test
    public void shouldMakeActivationFailWhenCallBackUrlIsInvokedWithNonSuccessStatusForActivationRequest() throws Exception {
        String subscriptionId = "abcd1234";
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.FAILURE.name());
        callbackRequest.setReason("reason");
        callbackRequest.setOperator("operator");
        callbackRequest.setGraceCount("2");
        byte[] requestBody = toJson(callbackRequest).getBytes();

        MockMvcBuilders.standaloneSetup(subscriptionController).build()
                .perform(put("/subscription/" + subscriptionId)
                        .body(requestBody).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().type("application/json;charset=UTF-8"))
                .andExpect(content().string(baseResponseMatcher("SUCCESS", "Callback request processed successfully")));

        verify(subscriptionService).updateSubscriptionStatus(subscriptionId, SubscriptionStatus.ACTIVATION_FAILED);
    }

    private void mockSubscription(String msisdn) {
        when(mockedSubscription.getMsisdn()).thenReturn(msisdn);
        when(mockedSubscription.getPack()).thenReturn(SubscriptionPack.FIFTEEN_MONTHS);
        when(mockedSubscription.getStatus()).thenReturn(SubscriptionStatus.NEW);
        when(mockedSubscription.getSubscriptionId()).thenReturn("subscription-id");
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
            assertTrue(jsonContent.startsWith(KilkariConstants.IVR_RESPONSE_FORMAT));
            jsonContent = jsonContent.replace(KilkariConstants.IVR_RESPONSE_FORMAT, "");
        }
        return jsonContent;
    }

    private boolean assertErrorResponseForInvalidMsisdn(String jsonContent, String channel) {
        jsonContent = performIVRChannelValidationAndCleanup(jsonContent, channel);

        BaseResponse baseResponse = fromJson(jsonContent, BaseResponse.class);

        return baseResponse.getStatus().equals("ERROR_VALIDATION") &&
                baseResponse.getDescription().equals("Invalid Msisdn");
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
