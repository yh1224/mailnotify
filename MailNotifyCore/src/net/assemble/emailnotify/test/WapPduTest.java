package net.assemble.emailnotify.test;

import net.assemble.emailnotify.core.EmailNotifyPreferences;
import net.assemble.emailnotify.core.WapPdu;
import net.orleaf.android.HexUtils;
import junit.framework.TestCase;

public class WapPduTest extends TestCase {

    /**
     * mopera Uメール (WAP PDU)
     */
    public void testMoperaPdu() {
        byte[] pdu = HexUtils.hex2bytes(
                // header
                "000606" +
                    "0302030aaf89" +
                // body
                "030d6a0085" +
                    "0703" + "796831323234406d6f7065726100" + "87" +
                    "05c307" + "20110123012345" +
                "01");

        WapPdu wapPdu = new WapPdu(pdu);
        assertTrue(wapPdu.decode());
        assertEquals(778, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.emn+wbxml", wapPdu.getContentType());
        assertEquals(9, wapPdu.getBinaryApplicationId());
        assertEquals("x-wap-application:emn.ua", wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_MOPERA, wapPdu.getServiceName());
        assertEquals("mailat:yh1224@mopera.net", wapPdu.getMailbox());
        assertEquals("20110123012345", wapPdu.getTimestampString());
    }

    /**
     * mopera Uメール (WAP BODY)
     */
    public void testMoperaBody() {
        String ctype = "application/vnd.wap.emn+wbxml";
        int appId = 9;
        byte[] body = HexUtils.hex2bytes(
                // body
                "030d6a0085" +
                    "0703" + "796831323234406d6f7065726100" + "87" +
                    "05c307" + "20110123012345" +
                "01");

        WapPdu wapPdu = new WapPdu(ctype, appId, body);
        assertTrue(wapPdu.decode());
        assertEquals(778, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.emn+wbxml", wapPdu.getContentType());
        assertEquals(9, wapPdu.getBinaryApplicationId());
        assertEquals("x-wap-application:emn.ua", wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_MOPERA, wapPdu.getServiceName());
        assertEquals("mailat:yh1224@mopera.net", wapPdu.getMailbox());
        assertEquals("20110123012345", wapPdu.getTimestampString());
    }

    /**
     * spモードメール (WAP PDU)
     */
    public void testSpmodePdu() {
        byte[] pdu = HexUtils.hex2bytes(
                // header
                "000608" +
                    "0302030aaf02905c" +
                // body
                "030d6a0085" +
                    "0703" + "6869726f73652d79313232342d737040646f636f6d6f2e6e652e6a700" + "00" +
                    "5c307" + "20110123012345" +
                "01");

        WapPdu wapPdu = new WapPdu(pdu);
        assertTrue(wapPdu.decode());
        assertEquals(778, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.emn+wbxml", wapPdu.getContentType());
        assertEquals(36956, wapPdu.getBinaryApplicationId());
        assertEquals("x-oma-docomo:xmd.mail.ua", wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_SPMODE, wapPdu.getServiceName());
        assertEquals("mailat:hirose-y1224-sp@docomo.ne.jp", wapPdu.getMailbox());
        assertEquals("20110123012345", wapPdu.getTimestampString());
    }

    /**
     * iモードメール (WAP PDU)
     */
    public void testImodePdu() {
        byte[] pdu = HexUtils.hex2bytes(
                // header
                "000605" +
                    "b0af028002" +
                // body
                "02066a0085" +
                    "0903" + "646f636f6d6f2e6e652e6a703f50493d303600" +
                "01");

        WapPdu wapPdu = new WapPdu(pdu);
        assertTrue(wapPdu.decode());
        assertEquals(48, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.slc", wapPdu.getContentType());
        assertEquals(32770, wapPdu.getBinaryApplicationId());
        assertEquals("x-wap-docomo:imode.mail.ua", wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_IMODE, wapPdu.getServiceName());
        assertEquals("imap://docomo.ne.jp?PI=06", wapPdu.getMailbox());
        assertNull(wapPdu.getTimestampString());
    }

    /**
     * iモードメール (WAP BODY)
     */
    public void testImodeBody() {
        String ctype = "application/vnd.wap.slc";
        int appId = 0;
        byte[] body = HexUtils.hex2bytes(
                // body
                "02066a0085" +
                    "0903" + "646f636f6d6f2e6e652e6a703f50493d303600" +
                "01");

        WapPdu wapPdu = new WapPdu(ctype, appId, body);
        assertTrue(wapPdu.decode());
        assertEquals(48, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.slc", wapPdu.getContentType());
        assertEquals(EmailNotifyPreferences.SERVICE_IMODE, wapPdu.getServiceName());
        assertEquals("imap://docomo.ne.jp?PI=06", wapPdu.getMailbox());
        assertNull(wapPdu.getTimestampString());
    }

    /**
     * 不明 (WAP BODY)
     */
    public void testUnknown2() {
        String ctype = "application/vnd.wap.emn+wbxml";
        int appId = 0;
        byte[] body = HexUtils.hex2bytes("01");

        WapPdu wapPdu = new WapPdu(ctype, appId, body);
        assertTrue(wapPdu.decode());
        assertEquals(778, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.emn+wbxml", wapPdu.getContentType());
        assertEquals(EmailNotifyPreferences.SERVICE_UNKNOWN, wapPdu.getServiceName());
        assertEquals("unknown", wapPdu.getMailbox());
        assertNull(wapPdu.getTimestampString());
    }

    /**
     * 不特定
     */
    public void testUnspec() {
        WapPdu wapPdu = new WapPdu(EmailNotifyPreferences.SERVICE_UNSPEC, "");
        assertEquals(0, wapPdu.getBinaryContentType());
        assertNull(wapPdu.getContentType());
        assertEquals(0, wapPdu.getBinaryApplicationId());
        assertNull(wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_UNSPEC, wapPdu.getServiceName());
        assertEquals("", wapPdu.getMailbox());
        assertNull(wapPdu.getTimestampString());
    }

}
