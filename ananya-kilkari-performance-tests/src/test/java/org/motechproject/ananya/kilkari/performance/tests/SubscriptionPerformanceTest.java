package org.motechproject.ananya.kilkari.performance.tests;

import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.motechproject.ananya.kilkari.builder.SubscriptionWebRequestBuilder;
import org.motechproject.ananya.kilkari.performance.tests.domain.BaseResponse;
import org.motechproject.ananya.kilkari.performance.tests.service.SubscriptionService;
import org.motechproject.ananya.kilkari.performance.tests.utils.BasePerformanceTest;
import org.motechproject.ananya.kilkari.performance.tests.utils.HttpUtils;
import org.motechproject.ananya.kilkari.performance.tests.utils.runner.LoadRunner;
import org.motechproject.ananya.kilkari.performance.tests.utils.runner.LoadTest;
import org.motechproject.ananya.kilkari.request.SubscriptionWebRequest;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.service.request.Location;

import java.util.*;

@RunWith(LoadRunner.class)
public class SubscriptionPerformanceTest extends BasePerformanceTest {

    private List<Location> locationList;

    public SubscriptionPerformanceTest(String testName) {
        super(testName);
    }

    @Before
    public void setUp() {
        setupLocations();
    }

    private void setupLocations() {
        locationList = new ArrayList<Location>();
        locationList.add(new Location("Begusarai", "Bachhwara", "Kadarabad"));
        locationList.add(new Location("Begusarai", "Bachhwara", "Godhana"));
        locationList.add(new Location("Begusarai", "Bakhri", "Aakha"));
        locationList.add(new Location("Begusarai", "Bakhri", "Bagban"));
        locationList.add(new Location("Begusarai", "Bakhri", "Bahuwara"));
        locationList.add(new Location("Begusarai", "Bakhri", "Bakhari East"));
        locationList.add(new Location("Begusarai", "Begusarai", "Suja"));
        locationList.add(new Location("Begusarai", "Begusarai", "Ulao"));
        locationList.add(new Location("Begusarai", "Bhagwanpur", "Banwaripur"));
        locationList.add(new Location("Begusarai", "Bhagwanpur", "Bhitsari"));
        locationList.add(new Location("Begusarai", "Bhagwanpur", "Chandaur"));
        locationList.add(new Location("Begusarai", "Bhagwanpur", "Damodarpur"));
    }

    @LoadTest(concurrentUsers = 100)
    public void shouldCreateAnIvrSubscription() throws InterruptedException {
        SubscriptionService subscriptionService = new SubscriptionService();
        DateTime beforeTest = DateTime.now();
        String expectedStatus = "PENDING_ACTIVATION";
        Map<String, String> parametersMap = constructParameters();

        BaseResponse baseResponse = HttpUtils.httpGetWithJsonResponse(parametersMap, "subscription");
        assertEquals("SUCCESS", baseResponse.getStatus());

        Subscription subscription = subscriptionService.getSubscriptionData(parametersMap.get("msisdn"), expectedStatus);
        assertNotNull(subscription);
        DateTime afterTest = DateTime.now();
        Period p = new Period(beforeTest, afterTest);
        System.out.println(p.getMillis() + " ms");
    }

    @LoadTest(concurrentUsers = 5)
    public void shouldCreateIvrSubscriptionsForBulkUsers() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            shouldCreateAnIvrSubscription();
        }
    }

    @LoadTest(concurrentUsers = 100)
    public void shouldCreateACallCenterSubscription() throws InterruptedException {
        SubscriptionService subscriptionService = new SubscriptionService();
        DateTime beforeTest = DateTime.now();
        String expectedStatus = "PENDING_ACTIVATION";
        Map<String, String> parametersMap = constructParameters();

        SubscriptionWebRequest subscriptionWebRequest = getSubscriptionWebRequest();

        BaseResponse baseResponse = HttpUtils.httpPostWithJsonResponse(parametersMap, subscriptionWebRequest, "subscription");
        assertEquals("SUCCESS", baseResponse.getStatus());

        Subscription subscription = subscriptionService.getSubscriptionData(subscriptionWebRequest.getMsisdn(), expectedStatus);
        assertNotNull(subscription);

        DateTime afterTest = DateTime.now();
        Period p = new Period(beforeTest, afterTest);
        System.out.println(p.getMillis() + " ms");
    }

    /*
     * randomly returns regular, early or late subscription
     * regular subscription - 7/11, early subscription 2/11, late subscription 2/11
     */
    private SubscriptionWebRequest getSubscriptionWebRequest() {
        int subscriptionType = new Random().nextInt(11);
        if (subscriptionType < 7) return getRegularSubscription();
        if (subscriptionType >= 7 && subscriptionType < 9) return getEarlySubscription();
        return getLateSubscription();
    }

    private SubscriptionWebRequest getRegularSubscription() {
        Location location = locationList.get(new Random().nextInt(locationList.size()));
        return new SubscriptionWebRequestBuilder()
                .withDefaults()
                .withDistrict(location.getDistrict()).withBlock(location.getBlock()).withPanchayat(location.getPanchayat())
                .withMsisdn(getRandomMsisdn())
                .build();
    }

    private SubscriptionWebRequest getEarlySubscription() {
        SubscriptionWebRequest earlySubscription = getRegularSubscription();
        earlySubscription.setDateOfBirth(DateTime.now().plusMonths(3).plusDays(new Random().nextInt(100)).toString("dd/MM/yyyy"));
        return earlySubscription;
    }

    private SubscriptionWebRequest getLateSubscription() {
        SubscriptionWebRequest lateSubscription = getRegularSubscription();
        // Date of Birth range is : -30 - DateTime.now - +30
        lateSubscription.setDateOfBirth(DateTime.now().plusDays(-30 + new Random().nextInt(60)).toString("dd/MM/yyyy"));
        return lateSubscription;
    }

    private String getRandomMsisdn() {
        return "9" + RandomStringUtils.randomNumeric(9);
    }

    private Map<String, String> constructParameters() {
        Map<String, String> parametersMap = new HashMap<>();
        parametersMap.put("msisdn", getRandomMsisdn());
        parametersMap.put("channel", "IVR");
        parametersMap.put("pack", "bari_kilkari");
        return parametersMap;

    }
}
