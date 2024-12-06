package enumeration;

public enum CurrencyEnum {

    EUROS("€"),
    DOLLARS("$"),
    POUNDS("£");

    private String symbol;

    CurrencyEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
