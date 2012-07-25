package org.motechproject.ananya.kilkari.messagecampaign.service;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.messagecampaign.contract.MessageCampaignEnrollment;
import org.motechproject.ananya.kilkari.messagecampaign.contract.MessageCampaignRequest;
import org.motechproject.ananya.kilkari.messagecampaign.contract.mapper.MessageCampaignRequestMapper;
import org.motechproject.ananya.kilkari.messagecampaign.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.messagecampaign.utils.KilkariPropertiesData;
import org.motechproject.server.messagecampaign.service.CampaignEnrollmentRecord;
import org.motechproject.server.messagecampaign.service.CampaignEnrollmentsQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("kilkariMessageCampaignService")
public class MessageCampaignService {

    public static final String FIFTEEN_MONTHS_CAMPAIGN_KEY = "kilkari-mother-child-campaign-fifteen-months";
    public static final String TWELVE_MONTHS_CAMPAIGN_KEY = "kilkari-mother-child-campaign-twelve-months";
    public static final String SEVEN_MONTHS_CAMPAIGN_KEY = "kilkari-mother-child-campaign-seven-months";
    public static final String CAMPAIGN_MESSAGE_NAME = "Mother Child Health Care";

    private KilkariPropertiesData kilkariProperties;
    private org.motechproject.server.messagecampaign.service.MessageCampaignService campaignService;

    @Autowired
    public MessageCampaignService(org.motechproject.server.messagecampaign.service.MessageCampaignService campaignService, KilkariPropertiesData kilkariProperties) {
        this.campaignService = campaignService;
        this.kilkariProperties = kilkariProperties;
    }

    public void start(MessageCampaignRequest campaignRequest) {
        campaignService.startFor(MessageCampaignRequestMapper.newRequestFrom(campaignRequest, kilkariProperties));
    }

    public boolean stop(MessageCampaignRequest enrollRequest) {
        campaignService.stopAll(MessageCampaignRequestMapper.newRequestFrom(enrollRequest,kilkariProperties));
        return true;
    }

    public MessageCampaignEnrollment searchEnrollment(String externalId, String campaignName) {
        List<CampaignEnrollmentRecord> enrollmentRecords = campaignService.search(
                new CampaignEnrollmentsQuery().withExternalId(externalId).withCampaignName(campaignName));

        if (enrollmentRecords.isEmpty())
            return null;

        CampaignEnrollmentRecord campaignEnrollmentRecord = enrollmentRecords.get(0);
        return new MessageCampaignEnrollment(campaignEnrollmentRecord.getExternalId(),
                campaignEnrollmentRecord.getCampaignName(), campaignEnrollmentRecord.getStartDate(),
                campaignEnrollmentRecord.getStatus());
    }

    public List<DateTime> getMessageTimings(String subscriptionId, String packName, DateTime startDate, DateTime endDate) {
        String campaignName = SubscriptionPack.from(packName).getCampaignName();
        Map<String, List<Date>> campaignTimings = campaignService.getCampaignTimings(subscriptionId, campaignName,
                startDate.toDate(), endDate.toDate());
        List<Date> campaignMessageTimings = campaignTimings.get(CAMPAIGN_MESSAGE_NAME);

        List<DateTime> alertTimings = new ArrayList<>();
        if (campaignMessageTimings == null || campaignMessageTimings.isEmpty())
            return alertTimings;

        for (Date date : campaignMessageTimings) {
            alertTimings.add(new DateTime(date));
        }
        return alertTimings;
    }
}