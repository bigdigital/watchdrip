package com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence;

public class SequenceStateVergeNew extends SequenceState {
    public static final String REQUEST_PARAMETERS = "REQUEST_PARAMETERS";
    public static final String WAITING_REQUEST_PARAMETERS_RESPONSE = "WAITING_REQUEST_PARAMETERS_RESPONSE";
    public static final String WAITING_TRANSFER_FW_START_RESPONSE = "WAITING_TRANSFER_FW_START_RESPONSE";
    public static final String WAITING_FINALIZE_FW_DATA = "WAITING_FINALIZE_FW_DATA";

    {
        sequence.add(INIT);
        sequence.add(NOTIFICATION_ENABLE);
        sequence.add(REQUEST_PARAMETERS);
        sequence.add(WAITING_REQUEST_PARAMETERS_RESPONSE);

        sequence.add(TRANSFER_SEND_WF_INFO);
        sequence.add(WAITING_TRANSFER_SEND_WF_INFO_RESPONSE);

        sequence.add(TRANSFER_FW_START);
        sequence.add(WAITING_TRANSFER_FW_START_RESPONSE);

        sequence.add(TRANSFER_FW_DATA);
        sequence.add(WAITING_FINALIZE_FW_DATA);
        sequence.add(FORCE_DISABLE_VIBRATION);

        sequence.add(FW_UPLOADING_FINISHED);
    }
}
