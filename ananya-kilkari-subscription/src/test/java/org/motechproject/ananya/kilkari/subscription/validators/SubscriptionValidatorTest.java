package org.motechproject.ananya.kilkari.subscription.validators;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.motechproject.ananya.kilkari.reporting.domain.SubscriberLocation;
import org.motechproject.ananya.kilkari.reporting.service.ReportingService;
import org.motechproject.ananya.kilkari.subscription.builder.SubscriptionRequestBuilder;
import org.motechproject.ananya.kilkari.subscription.domain.Channel;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.exceptions.ValidationException;
import org.motechproject.ananya.kilkari.subscription.repository.AllSubscriptions;
import org.motechproject.ananya.kilkari.subscription.service.request.Location;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriberUpdateRequest;
import org.motechproject.ananya.kilkari.subscription.service.request.SubscriptionRequest;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubscriptionValidatorTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ReportingService reportingService;
    @Mock
    private AllSubscriptions allSubscriptions;

    private SubscriptionValidator subscriptionValidator;

    @Before
    public void setUp() {
        initMocks(this);
        subscriptionValidator = new SubscriptionValidator(allSubscriptions, reportingService);
    }

    @Test
    public void shouldValidateIfLocationDoesNotExist() {
        SubscriptionRequest subscription = new SubscriptionRequestBuilder().withDefaults().build();
        Location location = subscription.getLocation();
        when(reportingService.getLocation(location.getDistrict(), location.getBlock(), location.getPanchayat())).thenReturn(null);

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Location does not exist for District[district] Block[block] and Panchayat[panchayat]");

        subscriptionValidator.validate(subscription);
    }

    @Test
    public void shouldValidateIfSubscriptionAlreadyExists() {
        SubscriptionRequest subscription = new SubscriptionRequestBuilder().withDefaults().build();

        SubscriberLocation existingLocation = new SubscriberLocation();
        Location location = subscription.getLocation();
        when(reportingService.getLocation(location.getDistrict(), location.getBlock(), location.getPanchayat())).thenReturn(existingLocation);

        Subscription existingActiveSubscription = new Subscription();
        when(allSubscriptions.findSubscriptionInProgress(subscription.getMsisdn(), subscription.getPack())).thenReturn(existingActiveSubscription);

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Active subscription already exists for msisdn[9876543210] and pack[FIFTEEN_MONTHS]");

        subscriptionValidator.validate(subscription);
    }

    @Test
    public void shouldNotValidateLocationIfLocationIsCompletelyEmptyForCC() {
        SubscriptionRequest subscription = new SubscriptionRequestBuilder().withDefaults().withDistrict(null).withBlock(null).withPanchayat(null).build();

        try {
            subscriptionValidator.validate(subscription);
        } catch (ValidationException e) {
            Assert.fail("Unexpected ValidationException");
        }

        verify(reportingService, never()).getLocation(anyString(), anyString(), anyString());
    }

    @Test
    public void shouldFailValidationIfWeekNumberIsOutsidePacksRange() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequestBuilder().withDefaults().withPack(SubscriptionPack.SEVEN_MONTHS).withWeek(2).build();

        Location location = subscriptionRequest.getLocation();
        SubscriberLocation existingLocation = new SubscriberLocation();
        when(reportingService.getLocation(location.getDistrict(), location.getBlock(), location.getPanchayat())).thenReturn(existingLocation);

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Given week[2] is not within the pack[SEVEN_MONTHS] range");

        subscriptionValidator.validate(subscriptionRequest);
    }

    @Test
    public void shouldFailValidationAndAppendErrorMessagesIfThereAreMultipleFailures() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequestBuilder().withDefaults().withPack(SubscriptionPack.SEVEN_MONTHS).withWeek(2).build();

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Location does not exist for District[district] Block[block] and Panchayat[panchayat],Given week[2] is not within the pack[SEVEN_MONTHS] range");

        subscriptionValidator.validate(subscriptionRequest);
    }

    @Test
    public void blankWeekNumberIsValid() {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequestBuilder().withDefaults().withPack(SubscriptionPack.SEVEN_MONTHS).withWeek(null).build();

        Location location = subscriptionRequest.getLocation();
        SubscriberLocation existingLocation = new SubscriberLocation();
        when(reportingService.getLocation(location.getDistrict(), location.getBlock(), location.getPanchayat())).thenReturn(existingLocation);
        try {
            subscriptionValidator.validate(subscriptionRequest);
        } catch (ValidationException e) {
            Assert.fail("Unexpected ValidationException");
        }
    }

    @Test
    public void shouldValidateInvalidLocationInSubscriberDetails() {
        Location location = new Location("district", "block", "panchayat");
        when(reportingService.getLocation(location.getDistrict(), location.getBlock(), location.getPanchayat())).thenReturn(null);
        String subscriptionId = "subscriptionId";
        when(allSubscriptions.findBySubscriptionId(subscriptionId)).thenReturn(new Subscription());

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Location does not exist for District[district] Block[block] and Panchayat[panchayat]");

        subscriptionValidator.validateSubscriberDetails(new SubscriberUpdateRequest(subscriptionId, Channel.CALL_CENTER.name(), DateTime.now(), "name", "23",
                "20-10-2038", "20-10-1985", location));
    }

    @Test
    public void shouldValidateInvalidSubscriptionIdInSubscriberDetails() {
        Location location = new Location("district", "block", "panchayat");
        when(reportingService.getLocation(location.getDistrict(), location.getBlock(), location.getPanchayat())).thenReturn(new SubscriberLocation());
        String subscriptionId = "subscriptionId";
        when(allSubscriptions.findBySubscriptionId(subscriptionId)).thenReturn(null);

        expectedException.expect(ValidationException.class);
        expectedException.expectMessage("Subscription does not exist for subscriptionId subscriptionId");

        subscriptionValidator.validateSubscriberDetails(new SubscriberUpdateRequest(subscriptionId, Channel.CALL_CENTER.name(), DateTime.now(), "name", "23",
                "20-10-2038", "20-10-1985", location));
    }
}
