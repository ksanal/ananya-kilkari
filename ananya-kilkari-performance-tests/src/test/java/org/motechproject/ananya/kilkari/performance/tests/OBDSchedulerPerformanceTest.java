package org.motechproject.ananya.kilkari.performance.tests;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessage;
import org.motechproject.ananya.kilkari.obd.domain.CampaignMessageStatus;
import org.motechproject.ananya.kilkari.performance.tests.service.api.OBDApiService;
import org.motechproject.ananya.kilkari.performance.tests.service.db.OBDDbService;
import org.motechproject.ananya.kilkari.performance.tests.utils.BasePerformanceTest;
import org.motechproject.ananya.kilkari.subscription.domain.Operator;
import org.motechproject.performance.tests.LoadPerf;
import org.motechproject.performance.tests.LoadRunner;

import java.util.UUID;

import static org.motechproject.ananya.kilkari.performance.tests.utils.TestUtils.*;

@RunWith(LoadRunner.class)
public class OBDSchedulerPerformanceTest extends BasePerformanceTest {
    private CampaignMessageStatus possibleStatus[] = new CampaignMessageStatus[]{CampaignMessageStatus.DNC, CampaignMessageStatus.DNP, CampaignMessageStatus.NEW};
    private Operator[] possibleOperators = Operator.values();
    private int numberOfUnsentMessages = 25000;

    public OBDSchedulerPerformanceTest(String testName) {
        super(testName);
    }

    private OBDApiService obdApiService = new OBDApiService();
    private OBDDbService obdDbService = new OBDDbService();

    @Before
    public void setUp() {
        DateTime now = DateTime.now();
        for (int i = 0; i < numberOfUnsentMessages; i++) {
            String subscriptionId = UUID.randomUUID().toString();
            String msisdn = getRandomMsisdn();
            String week = getRandomCampaignId();
            Operator operator = getRandomElementFromList(possibleOperators);
            CampaignMessageStatus status = getRandomElementFromList(possibleStatus);

            CampaignMessage campaignMessage = new CampaignMessage(subscriptionId, week, msisdn, operator.name(), now.plusWeeks(1));
            campaignMessage.setStatusCode(status);

            obdDbService.add(campaignMessage);
        }
    }

    @LoadPerf(concurrentUsers = 1)
    public void shouldPerformanceTestOBDScheduling() throws InterruptedException {
        DateTime beforeTest = DateTime.now();
        obdApiService.sendMessagesToOBD();
        DateTime afterTest = DateTime.now();

        System.out.println("Time to process: " + new Period(beforeTest, afterTest).toString());
    }
}
