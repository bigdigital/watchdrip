package com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence;

public class SequenceStateVerge2 extends SequenceStateVergeNew {
    {
        sequence.clear();

        sequence.add(INIT);
        sequence.add(NOTIFICATION_ENABLE);
        sequence.add(TRANSFER_WF_ID);
        sequence.add(REQUEST_PARAMETERS);
        sequence.add(WAITING_REQUEST_PARAMETERS_RESPONSE);

        sequence.add(TRANSFER_SEND_WF_INFO);
        sequence.add(WAITING_TRANSFER_SEND_WF_INFO_RESPONSE);

        sequence.add(TRANSFER_FW_START);
        sequence.add(WAITING_TRANSFER_FW_START_RESPONSE);

        sequence.add(TRANSFER_FW_DATA);
        sequence.add(WAITING_FINALIZE_FW_DATA);

        sequence.add(FW_UPLOADING_FINISHED);
    }
}
