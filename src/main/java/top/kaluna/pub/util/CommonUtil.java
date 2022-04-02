package top.kaluna.pub.util;

/**
 * @author Yuery
 * @date 2022/3/4/0004 - 21:55
 */
public class CommonUtil {
    /**
     *
     * @param src byte数组
     * @param offset 从数组的offset位开始
     * @return int 数值
     */
    public static int bytesToInt2(byte[] src, int offset){
        int value;
        value = (int) ((src[offset] & 0xFF) << 24)
                | ((src[offset+1] & 0xFF) << 16)
                | ((src[offset+2] & 0xFF) << 8)
                | ((src[offset+3] & 0xFF));
        return value;
    }
}

