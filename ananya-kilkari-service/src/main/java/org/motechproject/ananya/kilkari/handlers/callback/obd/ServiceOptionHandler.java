package org.motechproject.ananya.kilkari.handlers.callback.obd;

import org.motechproject.ananya.kilkari.obd.contract.OBDRequestWrapper;

public interface ServiceOptionHandler {
    public void process(OBDRequestWrapper obdRequestWrapper);
}
