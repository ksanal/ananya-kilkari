package org.motechproject.ananya.kilkari.web.validators;

import org.apache.commons.lang.StringUtils;
import org.motechproject.ananya.kilkari.domain.CampaignCode;
import org.motechproject.ananya.kilkari.domain.OBDRequest;
import org.motechproject.ananya.kilkari.domain.ServiceOption;
import org.motechproject.ananya.kilkari.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OBDRequestValidator {

    private SubscriptionService subscriptionService;

    @Autowired
    public OBDRequestValidator(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public List<String> validate(OBDRequest obdRequest, String subscriptionId) {
        List<String> errors = new ArrayList<>();
        validateMsisdn(obdRequest.getMsisdn(), errors);
        validateServiceOption(obdRequest.getServiceOption(), errors);
        validateCampaignId(obdRequest.getCampaignId(), errors);
        validateSubscription(subscriptionId, errors);

        return errors;
    }

    private void validateCampaignId(String campaignId, List errors) {
        if(StringUtils.isEmpty(campaignId))
            errors.add(String.format("Invalid campaign id %s", campaignId));
        else {
            String campaignIdRegExPattern = "^([A-Z]*)([0-9]{1,2})$";
            Pattern pattern = Pattern.compile(campaignIdRegExPattern);
            Matcher matcher = pattern.matcher(campaignId);
            if(!matcher.find() || !CampaignCode.isValid(matcher.group(1)))
                errors.add(String.format("Invalid campaign id %s", campaignId));
        }
    }

    private void validateSubscription(String subscriptionId, List<String> errors) {
        if (subscriptionService.findBySubscriptionId(subscriptionId) == null)
            errors.add(String.format("Invalid subscription id %s", subscriptionId));
    }

    private void validateServiceOption(String serviceOption, List<String> errors) {
        if (!ServiceOption.isValid(serviceOption))
            errors.add(String.format("Invalid service option %s", serviceOption));
    }

    private void validateMsisdn(String msisdn, List<String> errors) {
        if (!isValidMsisdn(msisdn))
            errors.add(String.format("Invalid msisdn %s", msisdn));
    }

    private boolean isValidMsisdn(String msisdn) {
        return (StringUtils.length(msisdn) >= 10 && StringUtils.isNumeric(msisdn));
    }
}
