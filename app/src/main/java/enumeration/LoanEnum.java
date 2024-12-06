package enumeration;

public enum LoanEnum {

    SHORT_TERM("Short Term Loan"),
    ONE_YEAR("One-Year Loan"),
    TWO_YEAR("Two-Year Loan");

    private String description;

    LoanEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
