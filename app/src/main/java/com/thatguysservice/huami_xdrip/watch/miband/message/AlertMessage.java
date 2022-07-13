package com.thatguysservice.huami_xdrip.watch.miband.message;


import com.thatguysservice.huami_xdrip.watch.miband.Const;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

import lombok.Getter;

import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew.fromUint8;

public class AlertMessage extends BaseMessage {

    public static int utf8ByteLength(String string, int length) {
        if (string == null) {
            return 0;
        }
        ByteBuffer outBuf = ByteBuffer.allocate(length);
        byte[] temp = string.getBytes(Charset.forName("UTF-8"));
        outBuf.put(temp);
        return outBuf.position();
    }

    public byte[] getAlertMessage(final AlertCategory category, final CustomIcon icon, final String msg) {
        return getAlertMessage(category, icon, "", msg);
    }

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHAR_NEW_ALERT;
    }

    public void queueChunkedMessage(MiBandService service, final CustomIcon icon, final String title, final String subject, final String body) {
        queueChunkedMessage(service, AlertCategory.CustomHuami, icon, title, subject, body, true, 230);
    }

    /*
        This works on all Huami devices except Mi Band 2
    */
    public void queueChunkedMessage(MiBandService service, final AlertCategory alertCategory, final CustomIcon icon, final String title, String subject, final String body, final boolean hasExtraHeader, final int maxLength) {
        String msg = StringUtils.truncate(title, 32) + "\0";
        if (subject != null) {
            msg += StringUtils.truncate(subject, 128) + "\n\n";
        }
        if (body != null) {
            msg += StringUtils.truncate(body, 512);
        }
        if (body == null && subject == null) {
            msg += " ";
        }

        int prefixlength = 2;

        // We also need a (fake) source name for Mi Band 3 for SMS/EMAIL, else the message is not displayed
        byte[] appSuffix = "\0 \0".getBytes();
        int suffixlength = appSuffix.length;

        if (alertCategory == AlertCategory.CustomHuami) {
            String appName = "xDrip";
            prefixlength = 3;
            appName = "\0" + appName + "\0";
            appSuffix = appName.getBytes();
            suffixlength = appSuffix.length;
        }
        if (hasExtraHeader) {
            prefixlength += 4;
        }

        byte[] rawmessage = msg.getBytes();
        int length = Math.min(rawmessage.length, maxLength - prefixlength);
        if (length < rawmessage.length) {
            length = utf8ByteLength(msg, length);
        }

        init(length + prefixlength + suffixlength);
        int pos = 0;
        putData(alertCategory.getValue());
        if (hasExtraHeader) {
            putData((byte) 0);
            putData((byte) 0);
            putData((byte) 0);
            putData((byte) 0);
        }
        putData((byte) 1);
        if (alertCategory == AlertCategory.CustomHuami) {
            putData(icon.getValue());
        }
        putData(rawmessage);
        putData(appSuffix);
        service.writeChunked(0, getBytes());
    }

    /*
    This works on all Huami devices except Mi Band 2 and Amazfit GTR, GTS
    */
    public byte[] getAlertMessage(final AlertCategory category, final CustomIcon icon, final String title, String msg) {
        byte[] messageBytes = new byte[1];
        byte[] titleBytes = new byte[1];
        if (msg.isEmpty())
            msg = title;
        String message = "\0" + StringUtils.truncate(msg, 128) + "\0";
        String titleString = StringUtils.truncate(title, 18) + "\0";
        try {
            messageBytes = message.getBytes("UTF-8");
            titleBytes = titleString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //
        }
        int len = 0;
        if (msg.length() > 0)
            len += messageBytes.length;
        if (title.length() > 0)
            len += titleBytes.length;

        if (category == AlertCategory.CustomHuami) {
            init(3 + len);
        } else {
            init(2 + len);
        }
        putData(category.getValue()); //alertCategory
        putData((byte) 0x01); //number of alerts
        if (category == AlertCategory.CustomHuami) {
            putData(fromUint8(icon.getValue()));
        }
        if (msg.length() > 0)
            putData(messageBytes);

        if (title.length() > 0)
            putData(titleBytes);
        return getBytes();
    }

    public byte[] getAlertMessageOld(String message, final AlertCategory category) {
        return getAlertMessageOld(message, category, null);
    }

    // suitable for miband 2 and call with title
    public byte[] getAlertMessageOld(String message, final AlertCategory category, final CustomIcon icon) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(100);
        stream.write(fromUint8(category.getValue()));
        stream.write(fromUint8(0x01));
        if (category == AlertCategory.CustomHuami) {
            stream.write(fromUint8(icon.getValue()));
        }
        if (message.length() > 0) {
            try {
                stream.write(message.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // some write a null byte instead of leaving out this optional value
            // stream.write(new byte[] {0});
        }
        return stream.toByteArray();
    }

    public enum AlertCategory {
        Simple(0),
        Email(1),
        Call(3),
        MissedCall(4),
        SMS_MMS(5),
        SMS(5),
        VoiceMail(6),
        Schedule(7),
        HighPriorityAlert(8),
        InstantMessage(9),
        // 10-250 reserved for future use
        // 251-255 defined by service specification
        Any(255),
        Custom(-1),
        CustomHuami(-6);

        @Getter
        private final byte value;

        AlertCategory(final int value) {
            this.value = (byte) value;
        }
    }


    public enum CustomIcon {
        // icons which are unsure which app they are for are suffixed with _NN
        WECHAT(0),
        PENGUIN_1(1),
        MI_CHAT_2(2),
        FACEBOOK(3),
        TWITTER(4),
        MI_APP_5(5),
        SNAPCHAT(6),
        WHATSAPP(7),
        RED_WHITE_FIRE_8(8),
        CHINESE_9(9),
        ALARM_CLOCK(10),
        APP_11(11),
        INSTAGRAM(12),
        CHAT_BLUE_13(13),
        COW_14(14),
        CHINESE_15(15),
        CHINESE_16(16),
        STAR_17(17),
        APP_18(18),
        CHINESE_19(19),
        CHINESE_20(20),
        CALENDAR(21),
        FACEBOOK_MESSENGER(22),
        VIBER(23),
        LINE(24),
        TELEGRAM(25),
        KAKAOTALK(26),
        SKYPE(27),
        VKONTAKTE(28),
        POKEMONGO(29),
        HANGOUTS(30),
        MI_31(31),
        CHINESE_32(32),
        CHINESE_33(33),
        EMAIL(34),
        WEATHER(35),
        HR_WARNING_36(36);

        @Getter
        private final byte value;

        CustomIcon(final int value) {
            this.value = (byte) value;
        }
    }
}
