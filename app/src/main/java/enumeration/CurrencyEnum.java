package enumeration;

public enum CurrencyEnum {

    EUROS("€", "€ - Euros"),
    DOLLARS("$", "$ - Dollars"),
    POUNDS("£", "£ - Pounds");

    private String symbol;
    private String description;

    CurrencyEnum(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
