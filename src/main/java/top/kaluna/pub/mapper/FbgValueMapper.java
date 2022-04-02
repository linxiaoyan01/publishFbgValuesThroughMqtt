package top.kaluna.pub.mapper;

import org.apache.ibatis.annotations.Param;
import top.kaluna.pub.domain.FbgValue;
import top.kaluna.pub.domain.FbgValueExample;


import java.math.BigDecimal;
import java.util.List;

public interface FbgValueMapper {
    long countByExample(FbgValueExample example);

    int deleteByExample(FbgValueExample example);

    int deleteByPrimaryKey(Long id);

    int insert(FbgValue record);

    int insertSelective(FbgValue record);

    List<FbgValue> selectByExample(FbgValueExample example);

    FbgValue selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") FbgValue record, @Param("example") FbgValueExample example);

    int updateByExample(@Param("record") FbgValue record, @Param("example") FbgValueExample example);

    int updateByPrimaryKeySelective(FbgValue record);

    int updateByPrimaryKey(FbgValue record);

    List<FbgValue> selectForAbnormal(Long startTime, Long endTime);

    int multipleInsert(List<FbgValue> fbgValues);

    BigDecimal temperatureNow();
}