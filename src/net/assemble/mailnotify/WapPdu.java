package net.assemble.mailnotify;

import android.util.Log;

import com.android.internal.telephony.WspTypeDecoder;

public class WapPdu {
    private static final String TAG = "EmailNotify";

    private byte[] wapData;
    private String mimeType;
    private int binaryContentType;
    private String mailBox = "Unknown";

    public WapPdu(byte[] data) {
        wapData = data;
    }

    /**
     * WAP PDU解析 (基本ぱくり)
     *
     * frameworks/base/telephony/java/com/android/internal/telephony/WapPushOverSms.java
     * WapPushOverSms#dispatchWapPdu()
     *
     * @param pdu WAP PDU
     * @return true:メールを受信した
     */
    public boolean decode() {
        WspTypeDecoder pduDecoder = new WspTypeDecoder(wapData);
        
        int index = 0;
        int transactionId = wapData[index++] & 0xFF;
        int pduType = wapData[index++] & 0xFF;
        int headerLength = 0;
        int dataIndex = 0;

        try {
            if ((pduType != WspTypeDecoder.PDU_TYPE_PUSH) &&
                    (pduType != WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH)) {
                Log.w(TAG, "Received non-PUSH WAP PDU. Type = " + pduType);
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
                switch (binaryContentType) {
                    case WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML:
                        mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML;
                        break;
                    case WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML:
                        mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML;
                        break;
                    case WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI:
                        mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI;
                        break;
                    case WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL:
                        mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL;
                        break;
                    case WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO:
                        mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO;
                        break;
                    case WspTypeDecoder.CONTENT_TYPE_B_MMS:
                        mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS;
                        break;
                    case 0x030a:
                        mimeType = "application/vnd.wap.emn+wbxml";
                        break;
                    default:
                        Log.w(TAG, "Received PDU. Unsupported Content-Type = " + binaryContentType);
                        return false;
                }
            } else {
                if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML)) {
                    binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML;
                } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML)) {
                    binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML;
                } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI)) {
                    binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI;
                } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL)) {
                    binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL;
                } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO)) {
                    binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO;
                } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS)) {
                    binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_MMS;
                } else if (mimeType.equals("application/vnd.wap.emn+wbxml")) {
                    binaryContentType = 0x030a;
                } else {
                    Log.w(TAG, "Received PDU. Unknown Content-Type = " + mimeType);
                    return false;
                }
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
        return true;
    }

    public String getContentType() {
        return mimeType;
    }

    public int getBinaryContentType() {
        return binaryContentType;
    }

    public String getMailbox() {
        return mailBox;
    }

}
