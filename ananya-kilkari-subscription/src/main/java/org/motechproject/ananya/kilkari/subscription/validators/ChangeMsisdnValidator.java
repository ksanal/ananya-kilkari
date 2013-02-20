package org.motechproject.ananya.kilkari.subscription.validators;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.subscription.service.request.ChangeMsisdnRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChangeMsisdnValidator {

    private AllSubscriptions allSubscriptions;

    @Autowired
    public ChangeMsisdnValidator(AllSubscriptions allSubscriptions) {
        this.allSubscriptions = allSubscriptions;
    }

    public void validate(ChangeMsisdnRequest changeMsisdnRequest) {
        String oldMsisdn = changeMsisdnRequest.getOldMsisdn();
        String newMsisdn = changeMsisdnRequest.getNewMsisdn();

        List<SubscriptionPack> requestedPacks = getRequestedPacks(changeMsisdnRequest);

        List<SubscriptionPack> updatablePacksForOldMsisdn = getUpadatablePacks(oldMsisdn);
        List<SubscriptionPack> updatablePacksForNewMsisdn = getUpadatablePacks(newMsisdn);

        for(SubscriptionPack requestedPack: requestedPacks) {
            validatePack(requestedPack, updatablePacksForOldMsisdn, updatablePacksForNewMsisdn);
        }
    }

    private void validatePack(SubscriptionPack requestedPack, List<SubscriptionPack> updatablePacksForOldMsisdn, List<SubscriptionPack> updatablePacksForNewMsisdn) {
        if(!updatablePacksForOldMsisdn.contains(requestedPack)) {
            throw new ValidationException(String.format("Old msisdn doesn't actively subscribe to the requested pack. Pack: %s", requestedPack));
        }

        if(updatablePacksForNewMsisdn.contains(requestedPack)) {
            throw new ValidationException(String.format("New msisdn actively subscribes to the requested pack. Pack: %s", requestedPack));
        }
    }

    private List<SubscriptionPack> getRequestedPacks(ChangeMsisdnRequest changeMsisdnRequest) {
        if(!changeMsisdnRequest.getShouldChangeAllPacks()) {
            return changeMsisdnRequest.getPacks();
        }
        return getRequestPacksForAll(changeMsisdnRequest);
    }

    private List<SubscriptionPack> getRequestPacksForAll(ChangeMsisdnRequest changeMsisdnRequest) {
        List<Subscription> subscriptions = allSubscriptions.findByMsisdn(changeMsisdnRequest.getOldMsisdn());
        if (subscriptions.isEmpty()) {
            throw new ValidationException("Old msisdn has no subscriptions.");
        }
        validateIfAllUpdatable(subscriptions);
        return toPacks(subscriptions);
    }

    private void validateIfAllUpdatable(List<Subscription> subscriptions) {
        for(Subscription subscription: subscriptions) {
            if(!subscription.isInUpdatableState()) {
                throw new ValidationException(String.format("All the subscription for old msisdn are not updatable. SubscriptionId: %s; Pack: %s, Status: %s", subscription.getSubscriptionId(), subscription.getPack(), subscription.getStatus()));
            }
        }
    }

    private List<SubscriptionPack> getUpadatablePacks(String msisdn) {
        List<Subscription> updatableSubscriptions = allSubscriptions.findUpdatableSubscriptions(msisdn);
        return toPacks(updatableSubscriptions);
    }

    private List<SubscriptionPack> toPacks(List<Subscription> subscriptions) {
        return new ArrayList<SubscriptionPack>(CollectionUtils.collect(subscriptions, new Transformer() {
            @Override
            public Object transform(Object o) {
                return ((Subscription) o).getPack();
            }
        }));
    }

    private void validateIfAtleastOneUpdatableSubscriptionExists(List<SubscriptionPack> packs) {
        if (packs.isEmpty()) {
            throw new ValidationException("Old msisdn has no subscriptions in the updatable state");
        }
    }

}
