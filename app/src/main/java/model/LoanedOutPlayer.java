package model;

import com.google.firebase.Timestamp;

public class LoanedOutPlayer {

    private long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String position;
    private int number;
    private String nationality;
    private int overall;
    private int potentialLow;
    private int potentialHigh;
    private String yearSigned;
    private String yearScouted;
    private String team;
    private String yearLoanedOut;
    private String typeOfLoan;
    private long managerId;
    private String userId;
    private Timestamp timeAdded;

    public LoanedOutPlayer() {
    }

    public LoanedOutPlayer(long id, String firstName, String lastName, String position, int number, String nationality, int overall, int potentialLow, int potentialHigh, String yearSigned, String yearScouted, String userId, Timestamp timeAdded, String team, String yearLoanedOut, String typeOfLoan, long managerId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.position = position;
        this.number = number;
        this.nationality = nationality;
        this.overall = overall;
        this.potentialLow = potentialLow;
        this.potentialHigh = potentialHigh;
        this.yearSigned = yearSigned;
        this.yearScouted = yearScouted;
        this.team = team;
        this.yearLoanedOut = yearLoanedOut;
        this.typeOfLoan = typeOfLoan;
        this.managerId = managerId;
        this.userId = userId;
        this.timeAdded = timeAdded;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public int getOverall() {
        return overall;
    }

    public void setOverall(int overall) {
        this.overall = overall;
    }

    public int getPotentialLow() {
        return potentialLow;
    }

    public void setPotentialLow(int potentialLow) {
        this.potentialLow = potentialLow;
    }

    public int getPotentialHigh() {
        return potentialHigh;
    }

    public void setPotentialHigh(int potentialHigh) {
        this.potentialHigh = potentialHigh;
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

    public long getManagerId() {
        return managerId;
    }

    public void setManagerId(long managerId) {
        this.managerId = managerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }
}
