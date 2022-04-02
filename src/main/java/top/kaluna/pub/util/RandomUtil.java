package top.kaluna.pub.util;

import java.math.BigDecimal;

/**
 * @author Yuery
 * @date 2022/3/25/0025 - 15:32
 */
public class RandomUtil {
    //生成100-1000的随机数
    public static BigDecimal From100TO1000(){
        // 产生一个2~100的数
        return new BigDecimal(Double.toString(500 + Math.random() * (1000 - 500)));
    }
}
