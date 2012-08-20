package org.motechproject.ananya.kilkari.mapper;

import org.joda.time.DateTime;
import org.junit.Test;
import org.motechproject.ananya.kilkari.builder.ChangeSubscriptionWebRequestBuilder;
import org.motechproject.ananya.kilkari.builder.SubscriptionWebRequestBuilder;
import org.motechproject.ananya.kilkari.request.ChangeSubscriptionWebRequest;
import org.motechproject.ananya.kilkari.request.SubscriberWebRequest;
import org.motechproject.ananya.kilkari.request.SubscriptionWebRequest;
import org.motechproject.ananya.kilkari.subscription.domain.ChangeSubscriptionType;
import org.motechproject.ananya.kilkari.subscription.service.request.*;
import org.motechproject.ananya.kilkari.subscription.validators.DateUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SubscriptionRequestMapperTest {
    @Test
    public void shouldMapFromWebRequestToSubscriptionRequest() {
        SubscriptionWebRequest subscriptionWebRequest = new SubscriptionWebRequestBuilder().withDefaults().withWeek("2").build();

        SubscriptionRequest subscriptionDomainRequest = SubscriptionRequestMapper.mapToSubscriptionRequest(subscriptionWebRequest);

        assertEquals(subscriptionWebRequest.getMsisdn(), subscriptionDomainRequest.getMsisdn());
        assertEquals(subscriptionWebRequest.getPack(), subscriptionDomainRequest.getPack().name());
        assertEquals(subscriptionWebRequest.getCreatedAt(), subscriptionDomainRequest.getCreationDate());
        Location location = subscriptionDomainRequest.getLocation();
        assertEquals(subscriptionWebRequest.getLocation().getBlock(), location.getBlock());
        assertEquals(subscriptionWebRequest.getLocation().getDistrict(), location.getDistrict());
        assertEquals(subscriptionWebRequest.getLocation().getPanchayat(), location.getPanchayat());
        Subscriber subscriber = subscriptionDomainRequest.getSubscriber();
        assertEquals(subscriptionWebRequest.getBeneficiaryAge(), subscriber.getBeneficiaryAge());
        assertEquals(subscriptionWebRequest.getBeneficiaryName(), subscriber.getBeneficiaryName());
        assertEquals(subscriptionWebRequest.getDateOfBirth(), subscriber.getDateOfBirth());
        assertEquals(subscriptionWebRequest.getExpectedDateOfDelivery(), subscriber.getExpectedDateOfDelivery());
        assertEquals(subscriptionWebRequest.getWeek(), subscriber.getWeek().toString());
    }

    @Test
    public void shouldMapToASubscriptionUpdateRequest() {
        SubscriberWebRequest subscriberWebRequest = new SubscriberWebRequest();
        String subscriptionId = "subscriptionId";
        String name = "Name";
        String beneficiaryAge = "23";
        String district = "district";
        String block = "block";
        String panchayat = "panchayat";
        String call_center = "CALL_CENTER";
        String dob = "01-11-2011";
        String edd = "01-01-2111";
        DateTime createdAtTime = DateTime.now();
        subscriberWebRequest.setBeneficiaryName(name);
        subscriberWebRequest.setBeneficiaryAge(beneficiaryAge);
        subscriberWebRequest.setDistrict(district);
        subscriberWebRequest.setBlock(block);
        subscriberWebRequest.setPanchayat(panchayat);
        subscriberWebRequest.setChannel(call_center);
        subscriberWebRequest.setCreatedAt(createdAtTime);
        subscriberWebRequest.setDateOfBirth(dob);
        subscriberWebRequest.setExpectedDateOfDelivery(edd);

        SubscriberRequest subscriberRequest = SubscriptionRequestMapper.mapToSubscriberRequest(subscriberWebRequest, subscriptionId);

        assertEquals(name, subscriberRequest.getBeneficiaryName());
        assertEquals(Integer.valueOf(beneficiaryAge), subscriberRequest.getBeneficiaryAge());
        assertEquals(DateUtils.parseDate(edd), subscriberRequest.getExpectedDateOfDelivery());
        assertEquals(DateUtils.parseDate(dob), subscriberRequest.getDateOfBirth());
        assertEquals(district, subscriberRequest.getDistrict());
        assertEquals(block, subscriberRequest.getBlock());
        assertEquals(panchayat, subscriberRequest.getPanchayat());
        assertEquals(subscriptionId, subscriberRequest.getSubscriptionId());
    }

    @Test
    public void allFieldsAreOptionalExceptSubscriptionId() {
        SubscriberWebRequest subscriberWebRequest = new SubscriberWebRequest();
        String subscriptionId = "subscriptionId";

        SubscriberRequest subscriberRequest = SubscriptionRequestMapper.mapToSubscriberRequest(subscriberWebRequest, subscriptionId);

        assertNull(subscriberRequest.getBeneficiaryName());
        assertNull(subscriberRequest.getBeneficiaryAge());
        assertNull(subscriberRequest.getExpectedDateOfDelivery());
        assertNull(subscriberRequest.getDateOfBirth());
        assertNull(subscriberRequest.getDistrict());
        assertNull(subscriberRequest.getBlock());
        assertNull(subscriberRequest.getPanchayat());
        assertEquals(subscriptionId, subscriberRequest.getSubscriptionId());
    }

    @Test
    public void shouldMapToChangeSubscriptionRequest() {
        ChangeSubscriptionWebRequest webRequest = new ChangeSubscriptionWebRequestBuilder().withDefaults()
                .withChangeType("change schedule")
                .withEDD("25-11-2013")
                .build();

        String subscriptionId = "subscriptionId";
        ChangeSubscriptionRequest changeSubscriptionRequest = SubscriptionRequestMapper.mapToChangeSubscriptionRequest(webRequest, subscriptionId);

        assertEquals(webRequest.getMsisdn(), changeSubscriptionRequest.getMsisdn());
        assertEquals(subscriptionId, changeSubscriptionRequest.getSubscriptionId());
        assertEquals(webRequest.getPack(), changeSubscriptionRequest.getPack().name());
        assertEquals(webRequest.getChannel(), changeSubscriptionRequest.getChannel().name());
        assertEquals(new DateTime(2013, 11, 25, 0, 0, 0), changeSubscriptionRequest.getExpectedDateOfDelivery());
        assertNull(changeSubscriptionRequest.getDateOfBirth());
        assertEquals(webRequest.getCreatedAt(), changeSubscriptionRequest.getCreatedAt());
        assertEquals(ChangeSubscriptionType.CHANGE_SCHEDULE, changeSubscriptionRequest.getChangeType());
        assertEquals("reason for change pack", changeSubscriptionRequest.getReason());
    }
}
