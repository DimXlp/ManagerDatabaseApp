package enumeration;

public enum PurchaseTransferEnum {
    WITH_TRANSFER_FEE("Bought from Another Team"),
    FREE_TRANSFER("Bought as a Free Transfer");

    private String description;

    PurchaseTransferEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
