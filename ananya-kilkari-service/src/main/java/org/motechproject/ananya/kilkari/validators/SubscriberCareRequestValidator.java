package org.motechproject.ananya.kilkari.validators;

import org.motechproject.ananya.kilkari.domain.Channel;
import org.motechproject.ananya.kilkari.domain.SubscriberCareReasons;
import org.motechproject.ananya.kilkari.domain.SubscriberCareRequest;
import org.motechproject.ananya.kilkari.exceptions.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class SubscriberCareRequestValidator {
    public void validate(SubscriberCareRequest subscriberCareRequest) {
        validateMsisdn(subscriberCareRequest);
        assertSubscriberCareReason(subscriberCareRequest);
        assertChannel(subscriberCareRequest);
    }

    private void validateMsisdn(SubscriberCareRequest subscriberCareRequest) {
        String msisdn = subscriberCareRequest.getMsisdn();
        if(!ValidationUtils.assertMsisdn(msisdn)) {
            throw new ValidationException(String.format("Invalid msisdn %s", msisdn));
        }
    }

    private void assertChannel(SubscriberCareRequest subscriberCareRequest) {
        String channel = subscriberCareRequest.getChannel();
        if (!Channel.isValid(channel)) {
            throw new ValidationException(String.format("Invalid channel %s", channel));
        }
    }

    private void assertSubscriberCareReason(SubscriberCareRequest subscriberCareRequest) {
        String reason = subscriberCareRequest.getReason();
        if (!SubscriberCareReasons.isValid(reason))
            throw new ValidationException(String.format("Invalid subscriber care reason %s", reason));
    }
}
