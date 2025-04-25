package enumeration;

public enum PurchaseTransferEnum {
    WITH_TRANSFER_FEE("Bought from Another Team", "Buy with Transfer Fee"),
    FREE_TRANSFER("Bought as a Free Transfer", "Buy as a Free Agent");

    private String description;
    private String transferType;

    PurchaseTransferEnum(String description, String transferType) {
        this.description = description;
        this.transferType = transferType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }
}
