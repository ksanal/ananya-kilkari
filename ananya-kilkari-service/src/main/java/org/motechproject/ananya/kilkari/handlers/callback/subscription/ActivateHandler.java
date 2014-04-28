package org.motechproject.ananya.kilkari.handlers.callback.subscription;

import org.motechproject.ananya.kilkari.request.CallbackRequestWrapper;
import org.motechproject.ananya.kilkari.service.KilkariCampaignService;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionStatus;
import org.motechproject.ananya.kilkari.subscription.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivateHandler implements SubscriptionStateHandler {
    private SubscriptionService subscriptionService;
    private KilkariCampaignService kilkariCampaignService;

    @Autowired
    public ActivateHandler(SubscriptionService subscriptionService, KilkariCampaignService kilkariCampaignService) {
        this.subscriptionService = subscriptionService;
        this.kilkariCampaignService = kilkariCampaignService;
    }

    public void perform(CallbackRequestWrapper callbackRequestWrapper) {
        String subscriptionId = callbackRequestWrapper.getSubscriptionId();
        subscriptionService.activate(subscriptionId, callbackRequestWrapper.getCreatedAt(), callbackRequestWrapper.getOperator(),callbackRequestWrapper.getMode());
    }

	public void performForSMReq(CallbackRequestWrapper callbackRequestWrapper) {
		String msisdn = callbackRequestWrapper.getMsisdn();
		SubscriptionStatus status = SubscriptionStatus.REFERRED_MSISDN_RECEIVED;
		SubscriptionPack pack = callbackRequestWrapper.getPack();
		subscriptionService.activateForReqFromSM(msisdn, pack, status,  callbackRequestWrapper.getCreatedAt(), callbackRequestWrapper.getOperator(),callbackRequestWrapper.getMode());
	}
}
