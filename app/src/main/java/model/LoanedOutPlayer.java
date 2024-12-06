package model;

import com.google.firebase.Timestamp;

public class LoanedOutPlayer extends Player {

    private int number;
    private String yearSigned;
    private String yearScouted;
    private String team;
    private String yearLoanedOut;
    private String typeOfLoan;

    public LoanedOutPlayer() {
    }

    public LoanedOutPlayer(long id, String firstName, String lastName, String fullName, String position, int number, String nationality, int overall, int potentialLow, int potentialHigh, String yearSigned, String yearScouted, String userId, Timestamp timeAdded, String team, String yearLoanedOut, String typeOfLoan, long managerId) {
        super(id, firstName, lastName, fullName, position, nationality, overall, potentialLow, potentialHigh, managerId, userId, timeAdded);
        this.number = number;
        this.yearSigned = yearSigned;
        this.yearScouted = yearScouted;
        this.team = team;
        this.yearLoanedOut = yearLoanedOut;
        this.typeOfLoan = typeOfLoan;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getYearSigned() {
        return yearSigned;
    }

    public void setYearSigned(String yearSigned) {
        this.yearSigned = yearSigned;
    }

    public String getYearScouted() {
        return yearScouted;
    }

    public void setYearScouted(String yearScouted) {
        this.yearScouted = yearScouted;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getYearLoanedOut() {
        return yearLoanedOut;
    }

    public void setYearLoanedOut(String yearLoanedOut) {
        this.yearLoanedOut = yearLoanedOut;
    }

    public String getTypeOfLoan() {
        return typeOfLoan;
    }

    public void setTypeOfLoan(String typeOfLoan) {
        this.typeOfLoan = typeOfLoan;
    }
}
