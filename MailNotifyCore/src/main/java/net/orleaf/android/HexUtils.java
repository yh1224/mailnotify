package net.orleaf.android;

public class HexUtils {

    /**
     * バイトデータを16進数文字列に変換
     *
     * @param bytes バイトデータ
     * @return 16進数文字列
     */
    public static String bytes2hex(byte[] bytes) {
        StringBuilder strbuf = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int bt = b & 0xff;
            if (bt < 0x10) {
                strbuf.append("0");
            }
            strbuf.append(Integer.toHexString(bt));
        }
        return strbuf.toString();
    }

    /**
     * 16進数文字列をバイトデータに変換
     *
     * @param hex 16進数文字列
     * @return バイナリデータ
     */
    public static byte[] hex2bytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, (i + 1) * 2), 16);
        }
        return bytes;
    }

}
