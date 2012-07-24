package org.motechproject.ananya.kilkari.messagecampaign.contract.mapper;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.motechproject.ananya.kilkari.messagecampaign.contract.MessageCampaignRequest;
import org.motechproject.ananya.kilkari.messagecampaign.domain.SubscriptionPack;
import org.motechproject.model.Time;
import org.motechproject.server.messagecampaign.contract.CampaignRequest;

public class MessageCampaignRequestMapper {

    public static CampaignRequest newRequestFrom(MessageCampaignRequest messageCampaignRequest, int campaignScheduleDeltaDays, int campaignScheduleDeltaMinutes) {
        String campaignName = SubscriptionPack.from(messageCampaignRequest.getSubscriptionPack()).getCampaignName();
        Time reminderTime = new Time(messageCampaignRequest.getSubscriptionCreationDate().plusMinutes(campaignScheduleDeltaMinutes).toLocalTime());
        DateTime referenceDate = messageCampaignRequest.getSubscriptionCreationDate();

        LocalDate referenceDateWithDelta = referenceDate.plusDays(campaignScheduleDeltaDays).toLocalDate();
        CampaignRequest campaignRequest = new CampaignRequest(messageCampaignRequest.getExternalId(), campaignName, reminderTime, referenceDateWithDelta);
        campaignRequest.setDeliverTime(reminderTime);
        return campaignRequest;
    }
}
