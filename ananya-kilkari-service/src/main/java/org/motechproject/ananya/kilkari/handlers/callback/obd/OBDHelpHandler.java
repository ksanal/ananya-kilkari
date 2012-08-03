package org.motechproject.ananya.kilkari.handlers.callback.obd;

import org.motechproject.ananya.kilkari.domain.SubscriberCareRequest;
import org.motechproject.ananya.kilkari.request.OBDSuccessfulCallDetailsRequest;
import org.motechproject.ananya.kilkari.service.SubscriberCareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OBDHelpHandler implements ServiceOptionHandler {

    private SubscriberCareService subscriberCareService;

    @Autowired
    public OBDHelpHandler(SubscriberCareService subscriberCareService) {
        this.subscriberCareService = subscriberCareService;
    }

    @Override
    public void process(OBDSuccessfulCallDetailsRequest obdSuccessfulCallDetailsRequest) {
        createSubscriberCareDoc(obdSuccessfulCallDetailsRequest);
    }

    private void createSubscriberCareDoc(OBDSuccessfulCallDetailsRequest obdSuccessfulCallDetailsRequest) {
        SubscriberCareRequest subscriberCareRequest = new SubscriberCareRequest(obdSuccessfulCallDetailsRequest.getMsisdn(), obdSuccessfulCallDetailsRequest.getServiceOption().name(),
                obdSuccessfulCallDetailsRequest.getChannel().name(), obdSuccessfulCallDetailsRequest.getCreatedAt());
        subscriberCareService.createSubscriberCareRequest(subscriberCareRequest);
    }
}
