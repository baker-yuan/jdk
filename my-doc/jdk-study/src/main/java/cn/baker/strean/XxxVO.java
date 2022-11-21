package cn.baker.strean;

public class XxxVO {
    private Integer bizId;
    private String bizType;
    public XxxVO(Integer bizId, String bizType) {
        this.bizId = bizId;
        this.bizType = bizType;
    }
    /**
     * bizId+bizType唯一
     * @return
     */
    public String getUk() {
        return bizId + bizType;
    }

    public Integer getBizId() {
        return bizId;
    }
    public String getBizType() {
        return bizType;
    }
}