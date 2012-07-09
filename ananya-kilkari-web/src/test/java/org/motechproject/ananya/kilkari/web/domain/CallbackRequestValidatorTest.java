package org.motechproject.ananya.kilkari.web.domain;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.ananya.kilkari.service.SubscriptionService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CallbackRequestValidatorTest {
    @Mock
    private SubscriptionService subscriptionService;

    @Before
    public void setup(){
        initMocks(this);
    }
    
    @Test
    public void shouldReturnValidIfCallbackRequestDetailsAreCorrect() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        callbackRequest.setOperator(Operator.AIRTEL.name());
        new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now());
        assertTrue(new CallbackRequestValidator(subscriptionService).validate(new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now())).isEmpty());
    }

    @Test
    public void shouldReturnInValidIfMsisdnIsAnInvalidNumber() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("12345");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        List<String> errors = new CallbackRequestValidator(subscriptionService).validate(new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now()));
        assertFalse(errors.isEmpty());
        assertEquals("Invalid msisdn 12345", errors.get(0));
    }

    @Test
    public void shouldReturnInValidIfMsisdnIsAlphaNumeric() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("123456789a");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        List<String> errors = new CallbackRequestValidator(subscriptionService).validate(new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now()));
        assertFalse(errors.isEmpty());
        assertEquals("Invalid msisdn 123456789a", errors.get(0));
    }

    @Test
    public void shouldReturnInValidIfCallbackActionIsInvalid() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction("invalid");
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        List<String> errors = new CallbackRequestValidator(subscriptionService).validate(new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now()));
        assertFalse(errors.isEmpty());
        assertEquals("Invalid callbackAction invalid", errors.get(0));
    }

    @Test
    public void shouldReturnInValidIfCallbackStatusIsInvalid() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus("invalid");
        List<String> errors = new CallbackRequestValidator(subscriptionService).validate(new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now()));
        assertFalse(errors.isEmpty());
        assertEquals("Invalid callbackStatus invalid", errors.get(0));
    }

    @Test
    public void shouldReturnInvalidIfOperatorIsInvalid() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        callbackRequest.setOperator("invalid_operator");
        List<String> errors = new CallbackRequestValidator(subscriptionService).validate(new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now()));
        assertFalse(errors.isEmpty());
        assertEquals("Invalid operator invalid_operator", errors.get(0));
    }

    @Test
    public void shouldReturnInvalidIfOperatorIsNotGiven() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        callbackRequest.setOperator(null);
        List<String> errors = new CallbackRequestValidator(subscriptionService).validate(new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now()));
        assertFalse(errors.isEmpty());
        assertEquals("Invalid operator null", errors.get(0));
    }

    @Test
    public void shouldInvokeSubscriptionServiceValidation() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.REN.name());
        callbackRequest.setStatus(CallbackStatus.BAL_LOW.name());
        callbackRequest.setOperator(Operator.AIRTEL.name());
        final CallbackRequestWrapper callbackRequestWrapper = new CallbackRequestWrapper(callbackRequest, "subId", DateTime.now());
        final ArrayList<String> errorFromService = new ArrayList<>();
        final String error = "Invalid status";
        errorFromService.add(error);
        when(subscriptionService.validate(callbackRequestWrapper)).thenReturn(errorFromService);

        final List<String> errorsFromValidator = new CallbackRequestValidator(subscriptionService).validate(callbackRequestWrapper);

        assertEquals(1, errorsFromValidator.size());
        assertEquals(error, errorsFromValidator.get(0));
    }
}
