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
        assertEquals(0x030a, wapPdu.getBinaryContentType());
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
                "030d6a0085" +
                    "0703" + "796831323234406d6f7065726100" + "87" +
                    "05c307" + "20110123012345" +
                "01");

        WapPdu wapPdu = new WapPdu(ctype, appId, body);
        assertTrue(wapPdu.decode());
        assertEquals(0x030a, wapPdu.getBinaryContentType());
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
        assertEquals(0x030a, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.emn+wbxml", wapPdu.getContentType());
        assertEquals(0x905c, wapPdu.getBinaryApplicationId());
        assertEquals("x-oma-docomo:xmd.mail.ua", wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_SPMODE, wapPdu.getServiceName());
        assertEquals("mailat:hirose-y1224-sp@docomo.ne.jp", wapPdu.getMailbox());
        assertEquals("20110123012345", wapPdu.getTimestampString());
    }

    /**
     * spモードメール
     */
    public void testSpmodeNull() {
        WapPdu wapPdu = new WapPdu(EmailNotifyPreferences.SERVICE_SPMODE, "docomo.ne.jp");
        assertEquals(0, wapPdu.getBinaryContentType());
        assertNull(wapPdu.getContentType());
        assertEquals(0, wapPdu.getBinaryApplicationId());
        assertNull(wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_SPMODE, wapPdu.getServiceName());
        assertEquals("docomo.ne.jp", wapPdu.getMailbox());
        assertNull(wapPdu.getTimestampString());
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
        assertEquals(0x30, wapPdu.getBinaryContentType());
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
                "02066a0085" +
                    "0903" + "646f636f6d6f2e6e652e6a703f50493d303600" +
                "01");

        WapPdu wapPdu = new WapPdu(ctype, appId, body);
        assertTrue(wapPdu.decode());
        assertEquals(0x30, wapPdu.getBinaryContentType());
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
        assertEquals(0x030a, wapPdu.getBinaryContentType());
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


    /**
     * spモード?
     */
    public void testXXX1() {
        byte[] pdu = HexUtils.hex2bytes(
                // header
                "000605" +
                    "b0af029062" +
                // body
                "02066a0085" +
                    "0b03" + "73706d6f64652e6e652e6a703f44493d303700" +
                "01");
        
        WapPdu wapPdu = new WapPdu(pdu);
        assertTrue(wapPdu.decode());
        assertEquals(0x30, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.slc", wapPdu.getContentType());
        assertEquals(0x9062, wapPdu.getBinaryApplicationId());
        assertEquals("x-oma-docomo:xmd.agent.ua", wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_UNKNOWN, wapPdu.getServiceName());
        assertEquals("http://www.spmode.ne.jp?DI=07", wapPdu.getMailbox());
        assertNull(wapPdu.getTimestampString());
    }

    /**
     * spモード?
     */
    public void testXXX2() {
        byte[] header = HexUtils.hex2bytes("b0af02905f");
        byte[] body = HexUtils.hex2bytes(
                "02066a0085" + 
                    "0b03" + "6261636b75702e73706d6f64652e6e652e6a703f53493d3831323000" +
                "01");

        WapPdu wapPdu = new WapPdu(header, body);
        assertTrue(wapPdu.decode());
        assertEquals(0x30, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.slc", wapPdu.getContentType());
        assertEquals(0x905f, wapPdu.getBinaryApplicationId());
        assertEquals("x-oma-docomo:xmd.storage.ua", wapPdu.getApplicationId());
        assertEquals(EmailNotifyPreferences.SERVICE_UNKNOWN, wapPdu.getServiceName());
        assertEquals("http://www.backup.spmode.ne.jp?SI=8120", wapPdu.getMailbox());
        assertNull(wapPdu.getTimestampString());
    }


    /**
     * Content-Type
     */
    public void testContentType1() {
        WapPdu wapPdu;

        // application/vnd.wap.emn+wbxml (0x030a)
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006060302030aaf8901"));
        assertTrue(wapPdu.decode());
        assertEquals(0x030a, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.emn+wbxml", wapPdu.getContentType());

        // application/vnd.docomo.pf (0x0310)
        wapPdu = new WapPdu(HexUtils.hex2bytes("00060603020310af8901"));
        assertTrue(wapPdu.decode());
        assertEquals(0x0310, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.docomo.pf", wapPdu.getContentType());

        // application/vnd.docomo.ub (0x0311)
        wapPdu = new WapPdu(HexUtils.hex2bytes("00060603020311af8901"));
        assertTrue(wapPdu.decode());
        assertEquals(0x0311, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.docomo.ub", wapPdu.getContentType());

        // application/vnd.wap.slc (0x30)
        wapPdu = new WapPdu(HexUtils.hex2bytes("000605b0af02800201"));
        assertTrue(wapPdu.decode());
        assertEquals(0x030, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.slc", wapPdu.getContentType());

        // unknown
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006060302030baf8901"));
        assertTrue(wapPdu.decode());
        assertEquals(0x030b, wapPdu.getBinaryContentType());
        assertEquals("unknown(0x30b)", wapPdu.getContentType());
    }

    /**
     * Content-Type
     */
    public void testContentType2() {
        WapPdu wapPdu;

        // application/vnd.wap.emn+wbxml (0x030a)
        wapPdu = new WapPdu("application/vnd.wap.emn+wbxml", 9, HexUtils.hex2bytes("01"));
        assertTrue(wapPdu.decode());
        assertEquals(0x030a, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.emn+wbxml", wapPdu.getContentType());

        // application/vnd.wap.slc (0x30)
        wapPdu = new WapPdu("application/vnd.wap.slc", 9, HexUtils.hex2bytes("01"));
        assertTrue(wapPdu.decode());
        assertEquals(0x30, wapPdu.getBinaryContentType());
        assertEquals("application/vnd.wap.slc", wapPdu.getContentType());

        // unknown
        wapPdu = new WapPdu("hoge", 9, HexUtils.hex2bytes("01"));
        assertTrue(wapPdu.decode());
        assertEquals(0, wapPdu.getBinaryContentType());
        assertEquals("hoge", wapPdu.getContentType());
    }


    /**
     * X-Wap-Application-Id
     */
    public void testApplicationId() {
        WapPdu wapPdu;

        // x-wap-application:emn.ua (0x09)
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006060302030aaf8901"));
        assertTrue(wapPdu.decode());
        assertEquals(9, wapPdu.getBinaryApplicationId());
        assertEquals("x-wap-application:emn.ua", wapPdu.getApplicationId());

        // x-oma-docomo:xmd.mail.ua (0x905c)
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006080302030aaf02905c01"));
        assertTrue(wapPdu.decode());
        assertEquals(0x905c, wapPdu.getBinaryApplicationId());
        assertEquals("x-oma-docomo:xmd.mail.ua", wapPdu.getApplicationId());

        // unknown
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006060302030aaf8a01"));
        assertTrue(wapPdu.decode());
        assertEquals(0x0a, wapPdu.getBinaryApplicationId());
        assertEquals("unknown(0xa)", wapPdu.getApplicationId());

        wapPdu = new WapPdu(HexUtils.hex2bytes("0006080302030aaf02905d01"));
        assertTrue(wapPdu.decode());
        assertEquals(0x905d, wapPdu.getBinaryApplicationId());
        assertEquals("unknown(0x905d)", wapPdu.getApplicationId());
    }

    /**
     * X-Wap-Application-Id
     */
    public void testApplicationId2() {
        WapPdu wapPdu;

        // x-wap-application:emn.ua (0x09)
        wapPdu = new WapPdu("application/vnd.wap.emn+wbxml", 9, HexUtils.hex2bytes("01"));
        assertTrue(wapPdu.decode());
        assertEquals(9, wapPdu.getBinaryApplicationId());
        assertEquals("x-wap-application:emn.ua", wapPdu.getApplicationId());

        // unknown
        wapPdu = new WapPdu("application/vnd.wap.emn+wbxml", 8, HexUtils.hex2bytes("01"));
        assertTrue(wapPdu.decode());
        assertEquals(8, wapPdu.getBinaryApplicationId());
        assertEquals("unknown(0x8)", wapPdu.getApplicationId());
    }


    /**
     * Invalid length
     */
    public void testLength() {
        WapPdu wapPdu;

        // no length
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006"));
        assertFalse(wapPdu.decode());

        // length unmatch
        wapPdu = new WapPdu(HexUtils.hex2bytes("00067f"));
        assertFalse(wapPdu.decode());

        // no X-Wap-Application-Id
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006060302030aae89"));
        assertFalse(wapPdu.decode());

        // no body (OK)
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006060302030aaf89"));
        assertTrue(wapPdu.decode());
    }

    /**
     * PDU TYPE
     */
    public void testPduType() {
        WapPdu wapPdu;

        // PDU_TYPE_PUSH (0x06)
        wapPdu = new WapPdu(HexUtils.hex2bytes("0006060302030aaf8901"));
        assertTrue(wapPdu.decode());

        // PDU_TYPE_CONFIRMED_PUSH (0x07)
        wapPdu = new WapPdu(HexUtils.hex2bytes("0007060302030aaf8901"));
        assertTrue(wapPdu.decode());

        // Invalid
        wapPdu = new WapPdu(HexUtils.hex2bytes("0005060302030aaf8901"));
        assertFalse(wapPdu.decode());
    }

}
