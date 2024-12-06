package enumeration;

public enum SaleTransferEnum {
    WITH_TRANSFER_FEE("Transfer to Another Team"),
    RELEASE("Release Player Transfer");

    String description;


    SaleTransferEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
