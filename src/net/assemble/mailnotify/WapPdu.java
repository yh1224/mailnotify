package net.assemble.mailnotify;

import android.util.Log;

import com.android.internal.telephony.WspTypeDecoder;

public class WapPdu {
    private static final String TAG = "EmailNotify";

    private byte[] wapData;
    private int dataIndex = 0;

    private String mimeType;
    private int binaryContentType;
    private String mailBox = "unknown";

    /**
     * Constructor
     *
     * @param ctype Content-Type (binary)
     */
    public WapPdu(int ctype) {
        binaryContentType = ctype;
        mimeType = convertContentType(binaryContentType);
        wapData = null;
    }

    /**
     * Constructor
     *
     * @param ctype Content-Type (binary)
     * @param data WAP Data
     */
    public WapPdu(int ctype, byte[] data) {
        binaryContentType = ctype;
        mimeType = convertContentType(binaryContentType);
        wapData = data;
    }

    /**
     * Constructor
     *
     * @param data WAP PDU
     */
    public WapPdu(byte[] data) {
        wapData = data;
    }

    /**
     * WAP PDU解析 (基本ぱくり)
     *
     * frameworks/base/telephony/java/com/android/internal/telephony/WapPushOverSms.java
     * WapPushOverSms#dispatchWapPdu()
     *
     * mimeTypeがすでに設定されている場合、ボディ以降のみとみなす。
     *
     * @param pdu WAP PDU
     * @return true:メールを受信した
     */
    public boolean decode() {
        if (mimeType == null) {
            WspTypeDecoder pduDecoder = new WspTypeDecoder(wapData);

            int index = 0;
            int transactionId = wapData[index++] & 0xFF;
            int pduType = wapData[index++] & 0xFF;
            int headerLength = 0;

            try {
                if ((pduType != WspTypeDecoder.PDU_TYPE_PUSH) &&
                        (pduType != WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH)) {
                    Log.w(TAG, "Received non-PUSH WAP PDU. Type = " + pduType);                    return false;
                }

                pduDecoder = new WspTypeDecoder(wapData);

                /**
                 * Parse HeaderLen(unsigned integer).
                 * From wap-230-wsp-20010705-a section 8.1.2
                 * The maximum size of a uintvar is 32 bits.
                 * So it will be encoded in no more than 5 octets.
                 */
                if (pduDecoder.decodeUintvarInteger(index) == false) {
                    Log.w(TAG, "Received PDU. Header Length error.");
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
                    Log.w(TAG, "Received PDU. Header Content-Type error.");
                    return false;
                }
                mimeType = pduDecoder.getValueString();
                if (mimeType == null) {
                    binaryContentType = (int)pduDecoder.getValue32();
                    mimeType = convertContentType(binaryContentType);
                } else {
                    binaryContentType = convertContentType(mimeType);
                }
                index += pduDecoder.getDecodedDataLength();
                dataIndex = headerStartIndex + headerLength;

                Log.d(TAG ,"Received WAP PDU. transactionId=" + transactionId + ", pduType=" + pduType +
                        ", contentType=" + mimeType + "(" + binaryContentType + ")" +
                        ", dataIndex=" + dataIndex);
            } catch (IndexOutOfBoundsException e) {
                Log.w(TAG, "PDU decode error.");
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
        try {
            // TODO: 超決めうちデコード
            if (binaryContentType == 0x030a) {  // application/vnd.wap.emn+wbxml
                // mailbox取得
                if (wapData[dataIndex + 5] == 7) {  // mailbox attribute
                    // mailat:
                    int strLen = 0;
                    for (int i = dataIndex + 7; wapData[i] != 0; i++) {
                        strLen++;
                    }
                    byte[] m = new byte[strLen];
                    for (int i = 0; i < strLen; i++) {
                        m[i] = wapData[dataIndex + 7 + i];
                    }
                    mailBox = "mailat:" + new String(m, 0);
                    int tld = wapData[dataIndex + 7 + strLen + 1];
                    if (tld < 0) {
                        tld += 256;
                    }
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
                }
                //Log.d(TAG, "mailbox: " + mailbox);
            } else if (binaryContentType == WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL) {
                // mailbox取得
                if (wapData[dataIndex + 5] == 9) {  // mailbox attribute
                    // imap://
                    int strLen = 0;
                    for (int i = dataIndex + 7; wapData[i] != 0; i++) {
                        strLen++;
                    }
                    byte[] m = new byte[strLen];
                    for (int i = 0; i < strLen; i++) {
                        m[i] = wapData[dataIndex + 7 + i];
                    }
                    mailBox = "imap://" + new String(m, 0);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, "PDU analyze error.");
        }
    }

    /**
     * Content-Type変換 (バイナリ値→文字列)
     *
     * @param ctype バイナリ値
     * @return 文字列
     */
    private String convertContentType(int ctype) {
        switch (binaryContentType) {
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
        default:
            Log.w(TAG, "Unknown Content-Type = " + binaryContentType);
            return null;
        }
    }

    /**
     * Content-Type変換 (文字列→バイナリ値)
     *
     * @param mimeType 文字列
     * @return バイナリ値
     */
    private int convertContentType(String mimeType) {
        if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML)) {
            return WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML;
        } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML)) {
            return WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML;
        } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI)) {
            return WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI;
        } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL)) {
            return WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL;
        } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO)) {
            return WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO;
        } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS)) {
            return WspTypeDecoder.CONTENT_TYPE_B_MMS;
        } else if (mimeType.equals("application/vnd.wap.emn+wbxml")) {
            return 0x030a;
        } else {
            Log.w(TAG, "Unknown Content-Type = " + mimeType);
            return 0;
        }
    }

    /**
     * デコードされたContent-Type(文字列)を取得
     *
     * @return Content-Type文字列
     */
    public String getContentType() {
        return mimeType;
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
     * デコードされたボディのmailbox属性を取得
     *
     * @return mailbox属性
     */
    public String getMailbox() {
        return mailBox;
    }

    /**
     * データの16進数文字列を取得
     *
     * @return 16進数文字列
     */
    public String getHexString() {
        StringBuffer strbuf = new StringBuffer(wapData.length * 2);
        for (int i = 0; i < wapData.length; i++) {
            int bt = wapData[i] & 0xff;
            if (bt < 0x10) {
                strbuf.append("0");
            }
            strbuf.append(Integer.toHexString(bt));
        }
        return strbuf.toString();
    }

}
