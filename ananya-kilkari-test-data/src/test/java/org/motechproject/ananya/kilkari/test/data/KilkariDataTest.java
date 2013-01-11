package org.motechproject.ananya.kilkari.test.data;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionStatus;
import org.motechproject.ananya.kilkari.web.response.SubscriptionDetails;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class KilkariDataTest extends BaseDataSetup {

    private int scheduleDeltaDays;
    private int deltaMinutes;

    @Before
    public void setUp() {
        scheduleDeltaDays = kilkariProperties.getCampaignScheduleDeltaDays();
        deltaMinutes = kilkariProperties.getCampaignScheduleDeltaMinutes();
    }

    @After
    public void tearDown(){
        moveToTime(DateTime.now());
    }

    @Test
    public void createCase1() {
        DateTime startDate = DateTime.now();
        String operator = getRandomOperator();
        DateTime activationDate = startDate.plusDays(2);
        DateTime week1 = activationDate.plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1);
        DateTime week2 = week1.plusWeeks(1);
        DateTime week3 = week2.plusWeeks(1);
        DateTime week4 = week3.plusWeeks(1);
        DateTime week5 = week4.plusWeeks(1);
        DateTime week6 = week5.plusWeeks(1);

        moveToTime(startDate);
        String msisdn = createSubscriptionForIVR(SubscriptionPack.BARI_KILKARI.name());
        SubscriptionDetails subscriptionDetails = getSubscriptionDetails(msisdn).getSubscriptionDetails().get(0);
        String subscriptionId = subscriptionDetails.getSubscriptionId();
        System.out.println(subscriptionId + " " + msisdn);

        moveToTime(activationDate);
        activateSubscription(msisdn, subscriptionId, operator);

        moveToTime(week1);
        String currentCampaignId = "WEEK1";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week2);
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK2";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week3);
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK3";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week4);
        suspendSubscription(msisdn, subscriptionId, operator);

        moveToTime(week4.plusDays(3));
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK4";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week5);
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK5";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week6);
        suspendSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK6";
        waitForCampaignMessageAlert(subscriptionId, currentCampaignId);

        moveToTime(week6.plusDays(10));
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK7";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(DateTime.now());
    }

    @Test
    public void createCase2() {
        DateTime startDate = DateTime.now();
        DateTime activationDate = startDate.plusDays(2);
        DateTime week1 = activationDate.plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1);
        DateTime week2 = week1.plusWeeks(1);
        DateTime week3 = week2.plusWeeks(1);
        DateTime week4 = week3.plusWeeks(1);
        DateTime week5 = week4.plusWeeks(1);

        moveToTime(startDate);
        String msisdn = createSubscriptionForIVR(SubscriptionPack.BARI_KILKARI.name());
        SubscriptionDetails subscriptionDetails = getSubscriptionDetails(msisdn).getSubscriptionDetails().get(0);
        String subscriptionId = subscriptionDetails.getSubscriptionId();
        System.out.println(subscriptionId + " " + msisdn);

        moveToTime(activationDate);
        String operator = getRandomOperator();

        activateSubscription(msisdn, subscriptionId, operator);

        moveToTime(week1);
        String currentCampaignId = "WEEK1";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week2);
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK2";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week3);
        suspendSubscription(msisdn, subscriptionId, operator);

        moveToTime(week3.plusDays(2));
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK3";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week4);
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK4";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week5);
        suspendSubscription(msisdn, subscriptionId, operator);

        moveToTime(week5.plusDays(20));
        deactivateSubscription(msisdn, subscriptionId, operator);

        moveToTime(DateTime.now());
    }

    @Test
    public void createCase3() {
        DateTime startDate = DateTime.now();

        DateTime dateOfBirth = startDate.minusWeeks(44).plusDays(1);
        System.out.println("dateOfBirth "+dateOfBirth);


        DateTime week61 = startDate.plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1).plusDays(1);
        DateTime week62 = week61.plusWeeks(1);
        DateTime week63 = week62.plusWeeks(1);
        DateTime week64 = week63.plusWeeks(1);

        moveToTime(startDate);

        String msisdn = createSubscriptionForCallCenter("nanhi_kilkari", dateOfBirth.toString("dd-MM-yyyy"), null);
        SubscriptionDetails subscriptionDetails = getSubscriptionDetails(msisdn).getSubscriptionDetails().get(0);
        String subscriptionId = subscriptionDetails.getSubscriptionId();
        System.out.println(subscriptionId + " " + msisdn);

        String operator = getRandomOperator();

        activateSubscription(msisdn, subscriptionId, operator);

        moveToTime(week61);
        String currentCampaignId = "WEEK61";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week62);
        suspendSubscription(msisdn, subscriptionId, operator);

        moveToTime(week62.plusDays(1));
        renewSubscription(msisdn,subscriptionId, operator);
        currentCampaignId = "WEEK62";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week63);
        renewSubscription(msisdn,subscriptionId, operator);
        currentCampaignId = "WEEK63";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week64);
        renewSubscription(msisdn,subscriptionId, operator);
        currentCampaignId = "WEEK64";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));
        waitForSubscription(msisdn, subscriptionId, SubscriptionStatus.PENDING_COMPLETION.getDisplayString());
        completeSubscription(msisdn, subscriptionId, operator);

        moveToTime(DateTime.now());
    }

    @Test
    public void createCase4() {
        DateTime startDate = DateTime.now().minusWeeks(1).plusDays(1);
        String operator = getRandomOperator();

        DateTime edd = startDate.plusWeeks(1);
        System.out.println("edd "+edd);

        DateTime week17 = startDate.plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1).plusWeeks(1).plusDays(1);
        DateTime week18 = week17.plusWeeks(1);
        DateTime week19 = week18.plusWeeks(1);

        moveToTime(startDate);

        String msisdn = createSubscriptionForCallCenter(SubscriptionPack.NAVJAAT_KILKARI.name(), null, edd.toString("dd-MM-yyyy"));
        SubscriptionDetails subscriptionDetails = getSubscriptionDetails(msisdn).getSubscriptionDetails().get(0);
        String subscriptionId = subscriptionDetails.getSubscriptionId();
        System.out.println(subscriptionId + " " + msisdn);

        moveToTime(edd.plusDays(1));
        waitForSubscription(msisdn, subscriptionId, SubscriptionStatus.PENDING_ACTIVATION.getDisplayString());
        activateSubscription(msisdn, subscriptionId, operator);

        moveToTime(week17);
        String currentCampaignId = "WEEK17";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week18);
        renewSubscription(msisdn,subscriptionId,operator);
        currentCampaignId = "WEEK18";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week19);
        suspendSubscription(msisdn,subscriptionId,operator);
        moveToTime(week19.plusDays(2));
        renewSubscription(msisdn,subscriptionId,operator);
        currentCampaignId = "WEEK19";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));
    }

    @Test
    public void createCase5(){
        DateTime startDate = DateTime.now();
        String operator = getRandomOperator();
        String pack = SubscriptionPack.BARI_KILKARI.name();

        DateTime activationDate = startDate.plusDays(2);
        DateTime week1 = activationDate.plusDays(scheduleDeltaDays).plusMinutes(deltaMinutes + 1);
        DateTime week2 = week1.plusWeeks(1);
        DateTime eddToChange = week2.plusWeeks(1);
        DateTime week17 = eddToChange.plusWeeks(1);

        moveToTime(startDate);

        String msisdn = createSubscriptionForIVR(pack);
        SubscriptionDetails subscriptionDetails = getSubscriptionDetails(msisdn).getSubscriptionDetails().get(0);
        String subscriptionId = subscriptionDetails.getSubscriptionId();
        System.out.println(subscriptionId + " " + msisdn);

        moveToTime(activationDate);
        activateSubscription(msisdn, subscriptionId, operator);

        moveToTime(week1);
        String currentCampaignId = "WEEK1";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        moveToTime(week2);
        renewSubscription(msisdn, subscriptionId, operator);
        currentCampaignId = "WEEK2";
        waitForCampaignMessage(subscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, subscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));

        String otherSubscriptionId = changeSchedule(msisdn, subscriptionId, pack, eddToChange, null);
        deactivateSubscription(msisdn,subscriptionId,operator);

        moveToTime(eddToChange);
        waitForSubscription(msisdn,otherSubscriptionId,SubscriptionStatus.PENDING_ACTIVATION.getDisplayString());
        activateSubscription(msisdn,otherSubscriptionId,operator);

        moveToTime(week17);
        currentCampaignId = "WEEK17";
        waitForCampaignMessage(otherSubscriptionId, currentCampaignId);
        makeOBDCallBack(msisdn, otherSubscriptionId, currentCampaignId, "HANGUP", DateTime.now(), DateTime.now().plusMinutes(10));
    }




}


