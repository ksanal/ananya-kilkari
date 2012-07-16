package org.motechproject.ananya.kilkari.subscription.mappers;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriberLocation;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriptionCreationReportRequest;
import org.motechproject.ananya.kilkari.subscription.builder.SubscriptionRequestBuilder;
import org.motechproject.ananya.kilkari.subscription.domain.*;

import static org.junit.Assert.assertEquals;

public class SubscriptionRequestMapperTest {
    @Test
    public void shouldReturnSubscriptionFromSubscriptionRequest() {
        DateTime createdAt = DateTime.now();
        SubscriptionRequestMapper subscriptionRequestMapper = new SubscriptionRequestMapper(createSubscriptionRequest("1234567890", "twelve_months", "ivr", "25", null, null, null, null, null, null, createdAt));
        Subscription subscription = subscriptionRequestMapper.getSubscription();
        assertEquals("1234567890", subscription.getMsisdn());
        assertEquals(SubscriptionPack.TWELVE_MONTHS, subscription.getPack());
        assertEquals(SubscriptionStatus.NEW, subscription.getStatus());

        assertEquals(createdAt, subscription.getCreationDate());
    }

    @Test
    public void shouldReturnSubscriptionActivationRequestFromSubscriptionRequest() {
        SubscriptionRequestMapper subscriptionRequestMapper = new SubscriptionRequestMapper(createSubscriptionRequest("1234567890", "twelve_months", "ivr", "25", null, null, null, null, null, null, DateTime.now()));
        ProcessSubscriptionRequest processSubscriptionRequest = subscriptionRequestMapper.getProcessSubscriptionRequest();
        Subscription subscription = subscriptionRequestMapper.getSubscription();
        assertEquals("1234567890", processSubscriptionRequest.getMsisdn());
        assertEquals(SubscriptionPack.TWELVE_MONTHS, processSubscriptionRequest.getPack());
        assertEquals(Channel.IVR, processSubscriptionRequest.getChannel());
        assertEquals(subscription.getSubscriptionId(), processSubscriptionRequest.getSubscriptionId());
    }

    @Test
    public void shouldReturnSubscriptionReportRequestFromSubscriptionRequest() {
        String dob = "21-01-2012";
        String edd = "23-02-2013";
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        String name = "name";

        SubscriptionRequest subscriptionRequest = createSubscriptionRequest("1234567890", "twelve_months", "ivr", "25", dob, edd, name, "mydistrict", "myblock", "mypanchayat", DateTime.now());
        SubscriptionRequestMapper subscriptionRequestMapper = new SubscriptionRequestMapper(subscriptionRequest);

        SubscriptionCreationReportRequest subscriptionCreationReportRequest = subscriptionRequestMapper.getSubscriptionCreationReportRequest();
        Subscription subscription = subscriptionRequestMapper.getSubscription();

        assertEquals("1234567890", subscriptionCreationReportRequest.getMsisdn());
        assertEquals(SubscriptionPack.TWELVE_MONTHS.name(), subscriptionCreationReportRequest.getPack());
        assertEquals(25, subscriptionCreationReportRequest.getAgeOfBeneficiary());
        assertEquals(dateTimeFormatter.parseDateTime(dob), subscriptionCreationReportRequest.getDob());
        assertEquals(dateTimeFormatter.parseDateTime(edd), subscriptionCreationReportRequest.getEdd());
        SubscriberLocation location = subscriptionCreationReportRequest.getLocation();
        assertEquals("mydistrict", location.getDistrict());
        assertEquals("myblock", location.getBlock());
        assertEquals("mypanchayat", location.getPanchayat());
        assertEquals(name, subscriptionCreationReportRequest.getName());
        assertEquals(SubscriptionStatus.NEW.name(), subscriptionCreationReportRequest.getSubscriptionStatus());
        assertEquals(subscriptionRequestMapper.getSubscription().getCreationDate(), subscriptionCreationReportRequest.getCreatedAt());
        assertEquals(subscription.getSubscriptionId(), subscriptionCreationReportRequest.getSubscriptionId());
    }

    private SubscriptionRequest createSubscriptionRequest(String msisdn, String pack, String channel, String age, String dob, String edd, String name, String district, String block, String panchayat, DateTime createdAt) {
        return new SubscriptionRequestBuilder().withDefaults().withCreatedAt(createdAt).withMsisdn(msisdn).withPack(pack).withChannel(channel).withBeneficiaryAge(age).withDOB(dob).withEDD(edd)
                .withBeneficiaryName(name)
                .withDistrict(district)
                .withBlock(block)
                .withPanchayat(panchayat)
                .build();
    }
}
