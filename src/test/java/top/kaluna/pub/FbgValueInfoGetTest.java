package top.kaluna.pub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.kaluna.pub.domain.FbgValueInfo;
import top.kaluna.pub.mapper.FbgValueInfoMapper;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Yuery
 * @date 2022/9/18/0018 - 18:43
 */
@SpringBootTest
public class FbgValueInfoGetTest {

    @Resource
    private FbgValueInfoMapper fbgValueInfoMapper;
    @Test
    void contextLoads() {
        final List<FbgValueInfo> fbgValueInfos = fbgValueInfoMapper.selectAllRecord();
        final List<FbgValueInfo> fbgValueInfos1 = fbgValueInfos.stream().sorted(Comparator.comparing(FbgValueInfo::getArrayNum)).collect(Collectors.toList());
        final List<FbgValueInfo> fbgValueInfos2 = fbgValueInfos1.stream().sorted(Comparator.comparing(FbgValueInfo::getChannel)).collect(Collectors.toList());
        LinkedHashMap<Integer, Integer> linkedHashMap = new LinkedHashMap<>();
        final LinkedHashMap<Integer, List<FbgValueInfo>> collect = fbgValueInfos2.stream().collect(Collectors.groupingBy(FbgValueInfo::getChannel, LinkedHashMap::new, Collectors.toList()));
        for(int key : collect.keySet()){
            linkedHashMap.put(key, collect.get(key).size());
        }
        System.out.println(linkedHashMap.toString());
    }

}
