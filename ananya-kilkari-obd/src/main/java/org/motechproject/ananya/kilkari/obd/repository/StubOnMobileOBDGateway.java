package org.motechproject.ananya.kilkari.obd.repository;

import org.motechproject.ananya.kilkari.obd.profile.TestProfile;
import org.motechproject.ananya.kilkari.obd.scheduler.SubSlot;
import org.motechproject.ananya.kilkari.obd.service.request.InvalidFailedCallReports;
import org.springframework.stereotype.Component;

@Component
@TestProfile
public class StubOnMobileOBDGateway implements OnMobileOBDGateway {

    private OnMobileOBDGateway behavior;
    private boolean invalidFailureRecordCalled;

    @Override
    public void sendMainSlotMessages(String content, SubSlot subSlot) {
        if(verify()) {
            behavior.sendMainSlotMessages(content, subSlot);
        }
    }

    @Override
    public void sendRetrySlotMessages(String content, SubSlot subSlot) {
        if(verify()) {
            behavior.sendRetrySlotMessages(content, subSlot);
        }
    }

    @Override
    public void sendInvalidFailureRecord(InvalidFailedCallReports invalidFailedCallReports) {
        if(verify()) {
            behavior.sendInvalidFailureRecord(invalidFailedCallReports);
            invalidFailureRecordCalled = true;
        }
    }

    private boolean verify() {
        if (behavior == null) {
            System.err.println(String.format("WARNING: %s: You need to set behavior before calling this method. Use setBehavior method.", StubOnMobileOBDGateway.class.getCanonicalName()));
            return false;
        }
        return true;
    }

    public void setBehavior(OnMobileOBDGateway behavior) {
        this.behavior = behavior;
    }

    public void setInvalidFailureRecordCalled(boolean invalidFailureRecordCalled) {
        this.invalidFailureRecordCalled = invalidFailureRecordCalled;
    }

    public boolean isInvalidFailureRecordCalled() {
        return invalidFailureRecordCalled;
    }
}
