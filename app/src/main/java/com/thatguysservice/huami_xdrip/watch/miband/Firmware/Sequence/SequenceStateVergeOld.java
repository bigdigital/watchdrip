package com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence;

public class SequenceStateVergeOld extends SequenceStateMiBand5 {
    {
        sequence.add(sequence.indexOf(FW_UPLOADING_FINISHED), FORCE_DISABLE_VIBRATION);
    }
}
