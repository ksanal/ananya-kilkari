package org.motechproject.ananya.kilkari.domain;

import org.junit.Test;
import org.motechproject.ananya.kilkari.exceptions.ValidationException;

import static org.junit.Assert.assertEquals;

public class SubscriptionRequestTest {

    @Test
    public void shouldCreateSubscriptionRequest() throws ValidationException {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest("1234567890", SubscriptionPack.FIFTEEN_MONTHS.name(), Channel.IVR.name());
        assertEquals(SubscriptionPack.FIFTEEN_MONTHS.name(), subscriptionRequest.getPack());
        assertEquals(Channel.IVR.name(), subscriptionRequest.getChannel());
        assertEquals("1234567890", subscriptionRequest.getMsisdn());
    }

    @Test
    public void shouldNotThrowExceptionWhenGivenSubscriptionDetailsAreAllValid() throws ValidationException {
        new SubscriptionRequest("1234567890", SubscriptionPack.FIFTEEN_MONTHS.name(), Channel.IVR.name()).validate();
    }

    @Test(expected = ValidationException.class)
    public void shouldThrowExceptionWhenInvalidPackIsGivenToCreateNewSubscription() throws ValidationException {
        new SubscriptionRequest("1234567890", "Invalid-Pack", Channel.IVR.name()).validate();
    }

    @Test(expected = ValidationException.class)
    public void shouldThrowExceptionWhenInvalidChannelIsGivenToCreateNewSubscription() throws ValidationException {
        new SubscriptionRequest("1234567890", SubscriptionPack.TWELVE_MONTHS.name(), "Invalid-Channel").validate();
    }

    @Test(expected = ValidationException.class)
    public void shouldThrowExceptionWhenInvalidMsisdnNumberIsGivenToCreateNewSubscription() throws ValidationException {
        new SubscriptionRequest("12345", SubscriptionPack.TWELVE_MONTHS.name(), Channel.IVR.name()).validate();
    }

    @Test(expected = ValidationException.class)
    public void shouldThrowExceptionWhenNonNumericMsisdnNumberIsGivenToCreateNewSubscription() throws ValidationException {
        new SubscriptionRequest("123456789a", SubscriptionPack.TWELVE_MONTHS.name(), Channel.IVR.name()).validate();
    }

    @Test
    public void ShouldCreateSubscription() {
        String msisdn = "1234567890";
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(msisdn, "twelve_months", "ivr");
        Subscription subscription = subscriptionRequest.getSubscription();
        assertEquals(msisdn, subscription.getMsisdn());
        assertEquals(SubscriptionPack.TWELVE_MONTHS, subscription.getPack());
    }
}
