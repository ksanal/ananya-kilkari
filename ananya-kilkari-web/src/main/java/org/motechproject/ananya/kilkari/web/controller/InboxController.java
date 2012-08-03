package org.motechproject.ananya.kilkari.web.controller;


import org.motechproject.ananya.kilkari.request.InboxCallDetailsWebRequest;
import org.motechproject.ananya.kilkari.request.InboxCallDetailsWebRequestsList;
import org.motechproject.ananya.kilkari.service.KilkariCampaignService;
import org.motechproject.ananya.kilkari.web.response.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class InboxController {

    private KilkariCampaignService kilkariCampaignService;

    @Autowired
    public InboxController(KilkariCampaignService kilkariCampaignService) {
        this.kilkariCampaignService = kilkariCampaignService;
    }

    @RequestMapping(value = "/inbox/calldetails", method = RequestMethod.POST)
    @ResponseBody
    public BaseResponse recordInboxCallDetails(@RequestBody InboxCallDetailsWebRequestsList inboxCallDetailsWebRequestsList) {
        for(InboxCallDetailsWebRequest callDetailsWebRequest : inboxCallDetailsWebRequestsList.getCallRecords()){
            kilkariCampaignService.publishInboxCallDetailsRequest(callDetailsWebRequest);
        }
        return BaseResponse.success("Inbox calldetails request submitted successfully");
    }
}
