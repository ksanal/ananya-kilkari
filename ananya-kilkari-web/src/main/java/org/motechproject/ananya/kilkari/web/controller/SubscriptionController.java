package org.motechproject.ananya.kilkari.web.controller;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.domain.*;
import org.motechproject.ananya.kilkari.service.KilkariSubscriptionService;
import org.motechproject.ananya.kilkari.service.ReportingService;
import org.motechproject.ananya.kilkari.service.SubscriptionService;
import org.motechproject.ananya.kilkari.web.contract.mapper.SubscriptionDetailsMapper;
import org.motechproject.ananya.kilkari.web.contract.response.BaseResponse;
import org.motechproject.ananya.kilkari.web.contract.response.SubscriberResponse;
import org.motechproject.ananya.kilkari.web.domain.CallbackRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SubscriptionController {

    private KilkariSubscriptionService kilkariSubscriptionService;
    private ReportingService reportingService;
    private SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionController(KilkariSubscriptionService kilkariSubscriptionService, ReportingService reportingService, SubscriptionService subscriptionService) {
        this.kilkariSubscriptionService = kilkariSubscriptionService;
        this.reportingService = reportingService;
        this.subscriptionService = subscriptionService;
    }


    @RequestMapping(value = "/subscription", method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse createSubscriptionFromIVR(SubscriptionRequest subscriptionRequest) {
        return createSubscription(subscriptionRequest);
    }

    @RequestMapping(value = "/subscription", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse createSubscription(@RequestBody SubscriptionRequest subscriptionRequest) {
        if (!Channel.isIVR(subscriptionRequest.getChannel())) {
            SubscriberLocation reportLocation = reportingService.getLocation(subscriptionRequest.getDistrict(), subscriptionRequest.getBlock(), subscriptionRequest.getPanchayat());
            Subscription existingSubscription = subscriptionService.findActiveSubscription(subscriptionRequest.getMsisdn(), subscriptionRequest.getPack());

            subscriptionRequest.validate(reportLocation, existingSubscription);
        }

        kilkariSubscriptionService.createSubscription(subscriptionRequest);
        return BaseResponse.success("Subscription request submitted successfully");
    }

    @RequestMapping(value = "/subscription/{subscriptionId}", method = RequestMethod.PUT)
    @ResponseBody
    public BaseResponse activateSubscriptionCallback(@RequestBody CallbackRequest callbackRequest, @PathVariable String subscriptionId) {
        List<String> validationErrors = new CallbackRequestValidator().validate(callbackRequest);
        if (!(validationErrors.isEmpty()))
            return BaseResponse.failure(String.format("Callback Request Invalid: %s", StringUtils.join(validationErrors.toArray(), ",")));

        kilkariSubscriptionService.processCallbackRequest(new CallbackRequestWrapper(callbackRequest, subscriptionId, DateTime.now()));

        return BaseResponse.success("Callback request processed successfully");
    }

    @RequestMapping(value = "/subscriber", method = RequestMethod.GET)
    @ResponseBody
    public SubscriberResponse getSubscriptions(@RequestParam String msisdn, @RequestParam String channel) {
        SubscriberResponse subscriberResponse = new SubscriberResponse();

        List<Subscription> subscriptions = kilkariSubscriptionService.getSubscriptionsFor(msisdn);

        if (subscriptions != null) {
            for (Subscription subscription : subscriptions)
                subscriberResponse.addSubscriptionDetail(SubscriptionDetailsMapper.mapFrom(subscription));
        }

        return subscriberResponse;
    }

    @RequestMapping(value = "/help", method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse createSubscriberCareRequest(@RequestParam String msisdn, @RequestParam String reason, @RequestParam String channel) {
        kilkariSubscriptionService.processSubscriberCareRequest(new SubscriberCareRequest(msisdn, reason));
        return BaseResponse.success("Subscriber care request processed successfully");
    }

}

