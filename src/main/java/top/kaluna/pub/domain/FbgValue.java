package top.kaluna.pub.domain;

import java.math.BigDecimal;

/**
 * @author 86158
 */
public class FbgValue {
    private Long id;

    private Long physicalValueInfoId;

    private BigDecimal value;

    private int  arrayNum;

    private Long createTime;

    private int channel;
    public FbgValue(){

    }

    public FbgValue(Long id, Long physicalValueInfoId, BigDecimal value, int arrayNum, Long createTime) {
        this.id = id;
        this.physicalValueInfoId = physicalValueInfoId;
        this.value = value;
        this.arrayNum = arrayNum;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPhysicalValueInfoId() {
        return physicalValueInfoId;
    }

    public void setPhysicalValueInfoId(Long physicalValueInfoId) {
        this.physicalValueInfoId = physicalValueInfoId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public int getArrayNum() {
        return arrayNum;
    }

    public void setArrayNum(int arrayNum) {
        this.arrayNum = arrayNum;
    }

    public int getChannel(){
        return channel;
    }
    private void setChannel(int channel) {
        this.channel = channel;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    /**
     * 建造者
     */
    public static final class Builder{
        private final FbgValue fbgValue;

        /**
         * 构造函数
         */
        public Builder(){
            fbgValue = new FbgValue();
        }

        /**
         * 构建 建造者
         *
         * @return 返回 建造者
         */
        public static Builder builder(){
            return new Builder();
        }

        /**
         * 赋值
         * @param physicalValueInfoId
         * @return 返回 建造者
         */
        public Builder withPhysicalValueInfoId(Long physicalValueInfoId){
            fbgValue.setPhysicalValueInfoId(physicalValueInfoId);
            return this;
        }

        /**
         * 赋值
         * @param value
         * @return 返回 建造者
         */
        public Builder withValue(BigDecimal value){
            fbgValue.setValue(value);
            return this;
        }

        /**
         * 赋值
         * @param arrayNum
         * @return 返回 建造者
         */
        public Builder withArrayNum(int arrayNum){
            fbgValue.setArrayNum(arrayNum);
            return this;
        }

        /**
         * 赋值
         * @param createTime
         * @return 返回 建造者
         */
        public Builder withCreateTime(Long createTime){
            fbgValue.setCreateTime(createTime);
            return this;
        }
        public FbgValue build(){
            return fbgValue;
        }

        public Builder withChannel(int channel) {
            fbgValue.setChannel(channel);
            return this;
        }
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FbgValue{");
        sb.append("id=").append(id);
        sb.append(", physicalValueInfoId=").append(physicalValueInfoId);
        sb.append(", value=").append(value);
        sb.append(", arrayNum=").append(arrayNum);
        sb.append(", createTime=").append(createTime);
        sb.append(", channel=").append(channel);
        sb.append('}');
        return sb.toString();
    }
}