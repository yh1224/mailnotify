package net.assemble.emailnotify.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.orleaf.android.HexUtils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.android.internal.telephony.WspTypeDecoder;

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
     */
    public WapPdu() {
        wapData = null;
    }

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
        binaryContentType = convertContentType(contentType);
        binaryApplicationId = appid;
        applicationId = convertApplicationId(binaryApplicationId);
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
            int transactionId = wapData[index++] & 0xFF;
            int pduType = wapData[index++] & 0xFF;
            int headerLength = 0;

            try {
                if ((pduType != WspTypeDecoder.PDU_TYPE_PUSH) &&
                        (pduType != WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH)) {
                    Log.w(EmailNotify.TAG, "WapPdu: non-PUSH WAP PDU. Type = " + pduType);
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
                    Log.w(EmailNotify.TAG, "WapPdu: Header Length error.");
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
                    Log.w(EmailNotify.TAG, "WapPdu: Header Content-Type error.");
                    return false;
                }
                contentType = pduDecoder.getValueString();
                if (contentType == null) {
                    binaryContentType = (int)pduDecoder.getValue32();
                    contentType = convertContentType(binaryContentType);
                } else {
                    binaryContentType = convertContentType(contentType);
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
                        Log.w(EmailNotify.TAG, "WapPdu: Header X-Wap-Application-Id error.");
                        return false;
                    }
                    applicationId = pduDecoder.getValueString();
                    if (applicationId == null) {
                        binaryApplicationId = (int)pduDecoder.getValue32();
                        applicationId = convertApplicationId(binaryApplicationId);
                    } else {
                        binaryApplicationId = convertApplicationId(applicationId);
                    }
                    index += pduDecoder.getDecodedDataLength() + 1;
                } else {
                    Log.w(EmailNotify.TAG, "WapPdu: Header X-Wap-Application-Id not present." + wapData[index]);
                    return false;
                }

                Log.d(EmailNotify.TAG ,"WapPdu: WAP PDU. transactionId=" + transactionId + ", pduType=" + pduType +
                        ", Content-Type=" + contentType + "(" + binaryContentType + ")" +
                        ", X-Wap-Application-Id=" + applicationId + "(" + binaryApplicationId + ")" +
                        ", dataIndex=" + dataIndex);
            } catch (IndexOutOfBoundsException e) {
                Log.w(EmailNotify.TAG, "WapPdu: PDU decode error.");
                return false;
            }
        }
        decodeBody();
        return true;
    }

    /**
     * ボディ部のデコード処理
     *
     * mailbox属性を取得する。
     */
    public void decodeBody() {
        int index = dataIndex;
        try {
            // TODO: 超決めうちデコード
            if (binaryContentType == 0x030a) {  // application/vnd.wap.emn+wbxml
                index += 5;
                // mailbox取得
                if (wapData[index] == 0x07) {  // mailbox attribute
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
                    mailBox = "mailat:" + new String(m, 0);
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
                    //Log.d(TAG, "mailbox=" + mailBox);
                }
                // timestamp取得
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
                    //Log.d(TAG, "timestampe=" + byte2hex(timestamp));
                }
            } else if (binaryContentType == WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL) {
                index += 5;
                // mailbox取得
                if (wapData[index] == 0x09) {  // mailbox attribute
                    index += 2;
                    // imap://
                    int strLen = 0;
                    for (int i = index; wapData[i] != 0; i++) {
                        strLen++;
                    }
                    byte[] m = new byte[strLen];
                    for (int i = 0; i < strLen; i++) {
                        m[i] = wapData[index + i];
                    }
                    mailBox = "imap://" + new String(m, 0);
                    index += strLen;
                }
            } else {
                mailBox = "unknown (" + getContentType() + ")";
            }
        } catch (IndexOutOfBoundsException e) {
            Log.w(EmailNotify.TAG, "WapPdu: PDU analyze error.");
        }
    }

    /**
     * Content-Type変換 (バイナリ値→文字列)
     *
     * @param type バイナリ値
     * @return 文字列
     */
    private String convertContentType(int type) {
        switch (type) {
        case WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML:
            return WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML;
        case WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML:
            return WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML;
        case WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI:
            return WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI;
        case WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL:
            return WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL;
        case WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO:
            return WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO;
        case WspTypeDecoder.CONTENT_TYPE_B_MMS:
            return WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS;
        case 0x030a:
            return "application/vnd.wap.emn+wbxml";
        case 0x0310:
            return "application/vnd.docomo.pf";
        case 0x0311:
            return "application/vnd.docomo.ub";
        default:
            Log.w(EmailNotify.TAG, "WapPdu: Unknown Content-Type = " + type);
            return "unknown";
        }
    }

    /**
     * Content-Type変換 (文字列→バイナリ値)
     *
     * @param type 文字列
     * @return バイナリ値
     */
    private int convertContentType(String type) {
        if (type.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML)) {
            return WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML;
        } else if (type.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML)) {
            return WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML;
        } else if (type.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI)) {
            return WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI;
        } else if (type.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL)) {
            return WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL;
        } else if (type.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO)) {
            return WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO;
        } else if (type.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS)) {
            return WspTypeDecoder.CONTENT_TYPE_B_MMS;
        } else if (type.equals("application/vnd.wap.emn+wbxml")) {
            return 0x030a;
        } else if (type.equals("application/vnd.docomo.pf")) {
            return 0x0310;
        } else if (type.equals("application/vnd.docomo.ub")) {
            return 0x0311;
        } else {
            Log.w(EmailNotify.TAG, "WapPdu: Unknown Content-Type = " + type);
            return 0;
        }
    }

    /**
     * X-Wap-Application-id変換 (バイナリ値→文字列)
     *
     * @param id バイナリ値
     * @return 文字列
     */
    private String convertApplicationId(int id) {
        switch (id) {
        case 0x09:      // mopera Uメール
            return "x-wap-application:emn.ua";
        case 0x8002:    // iモードメール
            return "x-wap-docomo:imode.mail.ua";
        case 0x905c:    // spモードメール
            return "x-oma-docomo:xmd.mail.ua";
        default:
            Log.w(EmailNotify.TAG, "WapPdu: Unknown X-Wap-Application-id = " + id);
            return "unknown";
        }
    }

    /**
     * X-Wap-Application-id変換 (文字列→バイナリ値)
     *
     * @param id 文字列
     * @return バイナリ値
     */
    private int convertApplicationId(String id) {
        if (id.equals("x-wap-application:emn.ua")) {
            return 0x09;
        } else if (id.equals("x-wap-docomo:imode.mail.ua")) {
            return 0x8002;
        } else if (id.equals("x-oma-docomo:xmd.mail.ua")) {
            return 0x905c;
        } else {
            Log.w(EmailNotify.TAG, "WapPdu: Unknown X-Wap-Application-id = " + id);
            return 0;
        }
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
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss z");
                date = sdf.parse(HexUtils.bytes2hex(timestamp) + " GMT");
            } catch (ParseException e) {
                Log.w(EmailNotify.TAG, "WapPdu: Unexpected timestamp: " + HexUtils.bytes2hex(timestamp));
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
    public static String getServiceName(String cype, String mailbox) {
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
            service = EmailNotifyPreferences.SERVICE_OTHER;
        }
        return service;
    }

}
