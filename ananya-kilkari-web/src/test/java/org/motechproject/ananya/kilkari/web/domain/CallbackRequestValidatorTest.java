package org.motechproject.ananya.kilkari.web.domain;

import org.junit.Test;
import org.motechproject.ananya.kilkari.domain.CallbackRequest;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class CallbackRequestValidatorTest {
    @Test
    public void shouldReturnValidIfCallbackRequestDetailsAreCorrect() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        assertTrue(new CallbackRequestValidator().validate(callbackRequest).isEmpty());
    }

    @Test
    public void shouldReturnInValidIfMsisdnIsAnInvalidNumber() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("12345");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        List<String> errors = new CallbackRequestValidator().validate(callbackRequest);
        assertFalse(errors.isEmpty());
        assertEquals("Invalid msisdn 12345", errors.get(0));
    }

    @Test
    public void shouldReturnInValidIfMsisdnIsAlphaNumeric() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("123456789a");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        List<String> errors = new CallbackRequestValidator().validate(callbackRequest);
        assertFalse(errors.isEmpty());
        assertEquals("Invalid msisdn 123456789a", errors.get(0));
    }

    @Test
    public void shouldReturnInValidIfCallbackActionIsInvalid() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction("invalid");
        callbackRequest.setStatus(CallbackStatus.SUCCESS.name());
        List<String> errors = new CallbackRequestValidator().validate(callbackRequest);
        assertFalse(errors.isEmpty());
        assertEquals("Invalid callbackAction invalid", errors.get(0));
    }

    @Test
    public void shouldReturnInValidIfCallbackStatusIsInvalid() {
        CallbackRequest callbackRequest = new CallbackRequest();
        callbackRequest.setMsisdn("1234567890");
        callbackRequest.setAction(CallbackAction.ACT.name());
        callbackRequest.setStatus("invalid");
        List<String> errors = new CallbackRequestValidator().validate(callbackRequest);
        assertFalse(errors.isEmpty());
        assertEquals("Invalid callbackStatus invalid", errors.get(0));
    }
}
