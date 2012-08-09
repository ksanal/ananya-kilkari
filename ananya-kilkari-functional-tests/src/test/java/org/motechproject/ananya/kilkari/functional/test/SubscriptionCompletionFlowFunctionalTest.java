package org.motechproject.ananya.kilkari.functional.test;

import org.joda.time.DateTime;
import org.junit.Test;
import org.motechproject.ananya.kilkari.functional.test.builder.SubscriptionDataBuilder;
import org.motechproject.ananya.kilkari.functional.test.domain.OBD;
import org.motechproject.ananya.kilkari.functional.test.domain.SubscriptionData;
import org.motechproject.ananya.kilkari.functional.test.utils.FunctionalTestUtils;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionStatus;
import org.motechproject.ananya.kilkari.subscription.repository.KilkariPropertiesData;
import org.springframework.beans.factory.annotation.Autowired;

import static org.motechproject.ananya.kilkari.functional.test.Actions.*;

public class SubscriptionCompletionFlowFunctionalTest extends FunctionalTestUtils {

    @Autowired
    private KilkariPropertiesData kilkariProperties;



    @Test
    public void shouldSubscribeAndProgressAndCompleteSubscriptionSuccessfully() throws Exception {
        int scheduleDeltaDays = kilkariProperties.getCampaignScheduleDeltaDays();
        int deltaMinutes = kilkariProperties.getCampaignScheduleDeltaMinutes();
        DateTime futureDateForFirstCampaignAlertToBeRaised = DateTime.now().plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1);
        DateTime futureDateOfSecondCampaignAlert = futureDateForFirstCampaignAlertToBeRaised.plusWeeks(1);
        DateTime futureDateOfPackCompletion = futureDateForFirstCampaignAlertToBeRaised.plusWeeks(59);

        SubscriptionData subscriptionData = new SubscriptionDataBuilder().withDefaults().build();

        when(callCenter).subscribes(subscriptionData);
        and(subscriptionManager).activates(subscriptionData);
        and(time).isMovedToFuture(futureDateForFirstCampaignAlertToBeRaised);
        then(user).messageIsReady(subscriptionData, "WEEK1");

        when(subscriptionManager).renews(subscriptionData);
        and(time).isMovedToFuture(futureDateOfSecondCampaignAlert);
        then(user).messageIsReady(subscriptionData, "WEEK2");

        when(time).isMovedToFuture(futureDateOfPackCompletion.plusHours(1));
        then(subscriptionVerifier).verifySubscriptionState(subscriptionData, SubscriptionStatus.PENDING_COMPLETION);
    }

    @Test
    public void shouldSubscribeAndProgressAndCompleteSubscriptionWithIntermediateSuspensionsSuccessfully() throws Exception {
        int scheduleDeltaDays = kilkariProperties.getCampaignScheduleDeltaDays();
        int deltaMinutes = kilkariProperties.getCampaignScheduleDeltaMinutes();
        DateTime futureDateForFirstCampaignAlertToBeRaised = DateTime.now().plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1);
        DateTime futureDateOfSecondCampaignAlert = futureDateForFirstCampaignAlertToBeRaised.plusWeeks(1);
        DateTime futureDateForThirdCampaignAlert = futureDateOfSecondCampaignAlert.plusWeeks(1);
        DateTime futureDateOfPackCompletion = futureDateForFirstCampaignAlertToBeRaised.plusWeeks(59);
        DateTime week2MessageExpiryDate = futureDateOfSecondCampaignAlert.plusWeeks(1);

        SubscriptionData subscriptionData = new SubscriptionDataBuilder().withDefaults().build();

        when(callCenter).subscribes(subscriptionData);
        and(subscriptionManager).activates(subscriptionData);
        and(time).isMovedToFuture(futureDateForFirstCampaignAlertToBeRaised);
        then(user).messageIsReady(subscriptionData, "WEEK1");

        when(subscriptionManager).failsRenew(subscriptionData);
        and(time).isMovedToFuture(futureDateOfSecondCampaignAlert);
        then(user).messageIsNotReady(subscriptionData, "WEEK2");

        when(time).isMovedToFuture(week2MessageExpiryDate);
        and(subscriptionManager).renews(subscriptionData);
        then(user).messageIsNotReady(subscriptionData, "WEEK2");

        when(time).isMovedToFuture(futureDateForThirdCampaignAlert);
        and(user).messageIsReady(subscriptionData, "WEEK3");

        when(time).isMovedToFuture(futureDateOfPackCompletion.plusHours(1));
        then(subscriptionVerifier).verifySubscriptionState(subscriptionData, SubscriptionStatus.PENDING_COMPLETION);
    }

    @Test
    public void shouldRetryDeliveryOfMessagesWhenTheSubscriberDoesNotReceiveMessagesOrCallIsNotMade() throws Exception {
        int scheduleDeltaDays = kilkariProperties.getCampaignScheduleDeltaDays();
        int deltaMinutes = kilkariProperties.getCampaignScheduleDeltaMinutes();
        DateTime futureDateForFirstCampaignAlert = DateTime.now().plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1);
        DateTime futureDateForFirstDayFirstSlot = futureDateForFirstCampaignAlert.withHourOfDay(13).withMinuteOfHour(30);
        DateTime futureDateForFirstDaySecondSlot = futureDateForFirstCampaignAlert.withHourOfDay(18).withMinuteOfHour(30);
        DateTime futureDateForSecondDayFirstSlot = futureDateForFirstDaySecondSlot.plusDays(1).withHourOfDay(13).withMinuteOfHour(30);
        DateTime futureDateForSecondDaySecondSlot = futureDateForSecondDayFirstSlot.withHourOfDay(18).withMinuteOfHour(30);
        String week1 = "WEEK1";

        SubscriptionData subscriptionData = new SubscriptionDataBuilder().withDefaults().build();

        when(callCenter).subscribes(subscriptionData);
        and(subscriptionManager).activates(subscriptionData);
        and(time).isMovedToFuture(futureDateForFirstCampaignAlert);
        then(user).messageIsReady(subscriptionData, week1);

        when(time).isMovedToFuture(futureDateForFirstDayFirstSlot);
        then(user).verifyThatNewMessageWasDelivered(subscriptionData, week1);
        and(user).resetOnMobileOBDVerifier();

        when(obd).userDoesNotPickUpTheCall(subscriptionData, week1);
        and(time).isMovedToFuture(futureDateForFirstDaySecondSlot);
        then(user).verifyThatRetryMessageWasDelivered(subscriptionData, week1);
        and(user).resetOnMobileOBDVerifier();

        when(obd).callIsNotDelivered(subscriptionData, week1);
        and(time).isMovedToFuture(futureDateForSecondDaySecondSlot);
        then(user).verifyThatNewMessageWasDelivered(subscriptionData, week1);
    }
}

