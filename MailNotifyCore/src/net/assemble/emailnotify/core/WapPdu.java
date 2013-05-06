package net.assemble.emailnotify.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.telephony.WspTypeDecoder;

import net.assemble.emailnotify.core.preferences.EmailNotifyPreferences;
import net.orleaf.android.HexUtils;

/**
 * WAP PDU 解析
 */
public class WapPdu implements Parcelable {
    private byte[] wapData;
    private int dataIndex = -1;

    private String contentType;
    private int binaryContentType;
    private String applicationId;
    private int binaryApplicationId;
    private String mailBox = "unknown";
    private byte[] timestamp = null;
    private String serviceName = null;
    private String errorMessage = null;

    // Content-Type
    private static final HashMap<Integer, String> CONTENTTYPES;
    static {
        CONTENTTYPES = new HashMap<Integer, String>();
        CONTENTTYPES.put(WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML,
                WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML);
        CONTENTTYPES.put(WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML,
                WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML);
        CONTENTTYPES.put(WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI,
                WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI);
        CONTENTTYPES.put(WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL,
                WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL);
        CONTENTTYPES.put(WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO,
                WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO);
        CONTENTTYPES.put(WspTypeDecoder.CONTENT_TYPE_B_MMS,
                WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS);
        CONTENTTYPES.put(0x030a, "application/vnd.wap.emn+wbxml");
        CONTENTTYPES.put(0x0310, "application/vnd.docomo.pf");
        CONTENTTYPES.put(0x0311, "application/vnd.docomo.ub");
    }

    // X-Wap-Application-Id
    private static final HashMap<Integer, String> APPIDS;
    static {
        APPIDS = new HashMap<Integer, String>();
        APPIDS.put(0x09, "x-wap-application:emn.ua");
        APPIDS.put(0x8002, "x-wap-docomo:imode.mail.ua");
        APPIDS.put(0x8003, "x-wap-docomo:imode.mr.ua");
        APPIDS.put(0x8004, "x-wap-docomo:imode.mf.ua");
        APPIDS.put(0x9000, "x-wap-docomo:imode.mail2.ua");
        APPIDS.put(0x9056, "x-oma-docomo:sp.mail.ua");
        APPIDS.put(0x905c, "x-oma-docomo:xmd.mail.ua");
        APPIDS.put(0x905e, "x-oma-docomo:imode.relation.ua");
        APPIDS.put(0x905f, "x-oma-docomo:xmd.storage.ua");
        APPIDS.put(0x9060, "x-oma-docomo:xmd.lcsapp.ua");
        APPIDS.put(0x9061, "x-oma-docomo:xmd.info.ua");
        APPIDS.put(0x9062, "x-oma-docomo:xmd.agent.ua");
        APPIDS.put(0x9063, "x-oma-docomo:xmd.sab.ua");
        APPIDS.put(0x9064, "x-oma-docomo:xmd.am.ua");
        APPIDS.put(0x906b, "x-oma-docomo:xmd.emdm.ua");
    }

    public WapPdu(Parcel in) {
        wapData = in.createByteArray();
        dataIndex = in.readInt();
        contentType = in.readString();
        binaryContentType = in.readInt();
        applicationId = in.readString();
        binaryApplicationId = in.readInt();
        mailBox = in.readString();
        timestamp = in.createByteArray();
        serviceName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(wapData);
        dest.writeInt(dataIndex);
        dest.writeString(contentType);
        dest.writeInt(binaryContentType);
        dest.writeString(applicationId);
        dest.writeInt(binaryApplicationId);
        dest.writeString(mailBox);
        dest.writeByteArray(timestamp);
        dest.writeString(serviceName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<WapPdu> CREATOR = new Parcelable.Creator<WapPdu>() {
        public WapPdu createFromParcel(Parcel in) {
            return new WapPdu(in);
        }

        public WapPdu[] newArray(int size) {
            return new WapPdu[size];
        }
    };

    /**
     * Constructor
     *
     * WAP ボディのみ
     *
     * @param ctype Content-Type (string)
     * @param appid X-Wap-Application-Id (binary)
     * @param body WAP body
     */
    public WapPdu(String ctype, int appid, byte[] body) {
        contentType = ctype;
        binaryContentType = convertMap(CONTENTTYPES, contentType);
        binaryApplicationId = appid;
        applicationId = convertMap(APPIDS, binaryApplicationId);
        // wapDataはボディ以降を示すため、dataIndexには0を設定
        wapData = body;
        dataIndex = 0;
    }

    /**
     * Constructor
     *
     * @param pdu WAP PDU
     */
    public WapPdu(byte[] pdu) {
        wapData = pdu;
    }

    /**
     * Constructor
     *
     * @param header WAP header
     * @param body WAP body
     */
    public WapPdu(byte[] header, byte[] body) {
        wapData = new byte[3 + header.length + body.length];
        wapData[0] = 0x00;
        wapData[1] = 0x06;
        wapData[2] = (byte) header.length;
        for (int i = 0; i < header.length; i++) {
            wapData[3 + i] = header[i];
        }
        for (int i = 0; i < body.length; i++) {
            wapData[3 + header.length + i] = body[i];
        }
    }

    /**
     * Constructor
     *
     * WAPの生データがないがサービスだけ特定できた場合
     */
    public WapPdu(String service, String mailbox) {
        wapData = null;
        serviceName = service;
        mailBox = mailbox;
    }

    /**
     * WAP PDU解析 (基本ぱくり)
     *
     * frameworks/base/telephony/java/com/android/internal/telephony/WapPushOverSms.java
     * WapPushOverSms#dispatchWapPdu()
     *
     * @return true:解析成功
     */
    public boolean decode() {
        if (dataIndex < 0) {
            WspTypeDecoder pduDecoder = new WspTypeDecoder(wapData);

            int index = 0;
            @SuppressWarnings("unused")
            int transactionId = wapData[index++] & 0xFF;
            int pduType = wapData[index++] & 0xFF;
            int headerLength = 0;

            try {
                if ((pduType != WspTypeDecoder.PDU_TYPE_PUSH) &&
                        (pduType != WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH)) {
                    errorMessage = "WapPdu: non-PUSH WAP PDU. Type = " + pduType;
                    return false;
                }

                pduDecoder = new WspTypeDecoder(wapData);

                /**
                 * Parse HeaderLen(unsigned integer).
                 * From wap-230-wsp-20010705-a section 8.1.2
                 * The maximum size of a uintvar is 32 bits.
                 * So it will be encoded in no more than 5 octets.
                 */
                if (pduDecoder.decodeUintvarInteger(index) == false) {
                    errorMessage = "WapPdu: Header Length error.";
                    return false;
                }
                headerLength = (int)pduDecoder.getValue32();
                index += pduDecoder.getDecodedDataLength();

                int headerStartIndex = index;

                /**
                 * Parse Content-Type.
                 * From wap-230-wsp-20010705-a section 8.4.2.24
                 *
                 * Content-type-value = Constrained-media | Content-general-form
                 * Content-general-form = Value-length Media-type
                 * Media-type = (Well-known-media | Extension-Media) *(Parameter)
                 * Value-length = Short-length | (Length-quote Length)
                 * Short-length = <Any octet 0-30>   (octet <= WAP_PDU_SHORT_LENGTH_MAX)
                 * Length-quote = <Octet 31>         (WAP_PDU_LENGTH_QUOTE)
                 * Length = Uintvar-integer
                 */
                if (pduDecoder.decodeContentType(index) == false) {
                    errorMessage = "WapPdu: Header Content-Type error.";
                    return false;
                }
                contentType = pduDecoder.getValueString();
                if (contentType == null) {
                    binaryContentType = (int)pduDecoder.getValue32();
                    contentType = convertMap(CONTENTTYPES, binaryContentType);
                } else {
                    binaryContentType = convertMap(CONTENTTYPES, contentType);
                }
                index += pduDecoder.getDecodedDataLength();
                dataIndex = headerStartIndex + headerLength;

                /**
                 * Parse X-Wap-Application-Id.
                 * From wap-230-wsp-20010705-a section 8.4.2.54
                 *
                 * Application-id-value = Uri-value | App-assigned-code
                 * App-assigned-code = Integer-value
                 */
                if (wapData[index] == 0xaf - 0x100) {
                    if (pduDecoder.decodeXWapApplicationId(index + 1) == false) {
                        errorMessage = "WapPdu: Header X-Wap-Application-Id error.";
                        return false;
                    }
                    applicationId = pduDecoder.getValueString();
                    if (applicationId == null) {
                        binaryApplicationId = (int)pduDecoder.getValue32();
                        applicationId = convertMap(APPIDS, binaryApplicationId);
                    } else {
                        binaryApplicationId = convertMap(APPIDS, applicationId);
                    }
                    index += pduDecoder.getDecodedDataLength() + 1;
                } else {
                    errorMessage = "WapPdu: Header X-Wap-Application-Id not present." + wapData[index];
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                errorMessage = "WapPdu: PDU decode error.";
                return false;
            }
        }
        decodeBody();
        return true;
    }

    /**
     * ボディ部のデコード処理
     *
     * mailbox/timestamp属性を取得する。
     */
    public void decodeBody() {
        int index = dataIndex;
        try {
            if (binaryContentType == 0x030a || // application/vnd.wap.emn+wbxml
                    binaryContentType == WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL) {
                index += 5;
                while (true) {
                    if (wapData[index] == 0x05) {  // timestamp attribute
                        if (wapData[index + 1] + 0x100 == 0xc3) {
                            index += 2;
                            int tsLen = wapData[index];
                            timestamp = new byte[tsLen];
                            index++;
                            for (int i = 0; i < tsLen; i++) {
                                timestamp[i] = wapData[index + i];
                            }
                            index += tsLen;
                        }
                    } else if (0x06 <= wapData[index] && wapData[index] <= 0x0d) {  // mailbox attribute
                        String prefix = "";
                        switch (wapData[index]) {
                        case 0x07:
                            prefix = "mailat:";
                            break;
                        case 0x08:
                            prefix = "pop://";
                            break;
                        case 0x09:
                            prefix = "imap://";
                            break;
                        case 0x0a:
                            prefix = "http://";
                            break;
                        case 0x0b:
                            prefix = "http://www.";
                            break;
                        case 0x0c:
                            prefix = "https://";
                            break;
                        case 0x0d:
                            prefix = "https://www.";
                            break;
                        }
                        index += 2;
                        // mailat:
                        int strLen = 0;
                        for (int i = index; wapData[i] != 0; i++) {
                            strLen++;
                        }
                        byte[] m = new byte[strLen];
                        for (int i = 0; i < strLen; i++) {
                            m[i] = wapData[index + i];
                        }
                        mailBox = prefix + new String(m, 0);
                        index += strLen + 1;
                        int tld = wapData[index];
                        if (tld < 0) {
                            tld += 0x100;
                            switch (tld) {
                            case 0x85:
                                mailBox += ".com";
                                break;
                            case 0x86:
                                mailBox += ".edu";
                                break;
                            case 0x87:
                                mailBox += ".net";
                                break;
                            case 0x88:
                                mailBox += ".org";
                                break;
                            }
                            index++;
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (IndexOutOfBoundsException e) {
            errorMessage = "WapPdu: PDU analyze error.";
        }
    }

    /**
     * key -> val  (バイナリ値→文字列)
     */
    private String convertMap(HashMap<Integer, String> map, int key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        // unknown
        return "unknown(" + String.format("0x%x", key) + ")";
    }

    /**
     * val -> key (文字列→バイナリ値)
     */
    private int convertMap(HashMap<Integer, String> map, String val) {
        if (CONTENTTYPES.containsValue(val)) {
            for (Iterator<Entry<Integer, String>> it = CONTENTTYPES.entrySet().iterator(); it.hasNext();) {
                Entry<Integer, String> entry = (Entry<Integer, String>)it.next();
                Integer key = entry.getKey();
                String value = entry.getValue();
                if (value.equals(val)) {
                    return key;
                }
            }
        }
        // unknown
        return 0;
    }

    /**
     * デコードされたContent-Type(文字列)を取得
     *
     * @return Content-Type文字列
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * デコードされたContent-Type(バイナリ値)を取得
     *
     * @return Content-Typeバイナリ値
     */
    public int getBinaryContentType() {
        return binaryContentType;
    }

    /**
     * デコードされたX-Wap-Application-Id(文字列)を取得
     *
     * @return X-Wap-Application-Id文字列
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * デコードされたX-Wap-Application-Id(バイナリ値)を取得
     *
     * @return X-Wap-Application-Idバイナリ値
     */
    public int getBinaryApplicationId() {
        return binaryApplicationId;
    }

    /**
     * デコードされたボディのmailbox属性を取得
     *
     * @return mailbox属性
     */
    public String getMailbox() {
        return mailBox;
    }

    /**
     * デコードされたボディのtimestamp属性を取得
     *
     * @return timestamp属性
     */
    public String getTimestampString() {
        if (timestamp != null) {
            return HexUtils.bytes2hex(timestamp);
        } else {
            return null;
        }
    }

    /**
     * デコードされたボディのtimestamp属性をDate型で取得
     *
     * @return timestamp属性
     */
    public Date getTimestampDate() {
        Date date = null;
        if (timestamp != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss z", Locale.US);
                date = sdf.parse(HexUtils.bytes2hex(timestamp) + " GMT");
            } catch (ParseException e) {
                //Log.w(EmailNotify.TAG, "WapPdu: Unexpected timestamp: " + HexUtils.bytes2hex(timestamp));
            }
        }
        return date;
    }

    /**
     * データの16進数文字列を取得
     *
     * @return 16進数文字列
     */
    public String getHexString() {
        if (wapData != null) {
            return HexUtils.bytes2hex(wapData);
        }
        return null;
    }

    /**
     * サービス名を取得
     *
     * @return サービス名
     */
    public String getServiceName() {
        if (serviceName == null) {
            serviceName = getServiceName(contentType, mailBox);
        }
        return serviceName;
    }


    /**
     * サービス名を取得
     *
     * @return サービス名
     */
    private static String getServiceName(String cype, String mailbox) {
        String service;

        // メールサービス別通知
        if (cype != null && cype.equals("application/vnd.wap.emn+wbxml") &&
                mailbox != null && mailbox.endsWith("docomo.ne.jp")) {
            // spモードメール
            service = EmailNotifyPreferences.SERVICE_SPMODE;
        } else if (cype != null && cype.equals("application/vnd.wap.emn+wbxml") &&
                mailbox != null  && mailbox.endsWith("mopera.net")) {
            // mopera Uメール
            service = EmailNotifyPreferences.SERVICE_MOPERA;
        } else if (cype != null && cype.equals("application/vnd.wap.slc") &&
                mailbox != null && mailbox.contains("docomo.ne.jp")) {
            // iモードメール
            service = EmailNotifyPreferences.SERVICE_IMODE;
        } else {
            // その他
            service = EmailNotifyPreferences.SERVICE_UNKNOWN;
        }
        return service;
    }

    /**
     * エラーメッセージを取得
     */
    public String getErrorMessage() {
        return errorMessage;
    }

}
