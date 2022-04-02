package top.kaluna.pub.domain;

import java.math.BigDecimal;

/**
 * @author 86158
 */
public class FbgValue {
    private Long id;

    private Long physicalValueInfoId;

    private BigDecimal value;

    private Byte tag;

    private Long createTime;
    public FbgValue(){

    }

    public FbgValue(Long id, Long physicalValueInfoId, BigDecimal value, Byte tag, Long createTime) {
        this.id = id;
        this.physicalValueInfoId = physicalValueInfoId;
        this.value = value;
        this.tag = tag;
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

    public Byte getTag() {
        return tag;
    }

    public void setTag(Byte tag) {
        this.tag = tag;
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
         * @param tag
         * @return 返回 建造者
         */
        public Builder withTag(Byte tag){
            fbgValue.setTag(tag);
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
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", physicalValueInfoId=").append(physicalValueInfoId);
        sb.append(", value=").append(value);
        sb.append(", tag=").append(tag);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}