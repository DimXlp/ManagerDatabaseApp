package model;

import com.google.firebase.Timestamp;

public class Transfer {

    private int id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String position;
    private String formerTeam;
    private String currentTeam;
    private String nationality;
    private int overall;
    private int potentialLow;
    private int potentialHigh;
    private String type;
    private long transferFee;
    private String plusPlayerName;
    private int wage;
    private int contractYears;
    private String year;
    private String comments;
    private boolean isFormerPlayer;
    private long managerId;
    private String userId;
    private Timestamp timeAdded;

    public Transfer() {
    }

    public Transfer(int id, String firstName, String lastName, String fullName, String position, String formerTeam, String currentTeam, String nationality, int overall, int potentialLow, int potentialHigh, String type, long transferFee, String plusPlayerName, int wage, int contractYears, String year, String comments, boolean isFormerPlayer, long managerId, String userId, Timestamp timeAdded) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.position = position;
        this.formerTeam = formerTeam;
        this.currentTeam = currentTeam;
        this.nationality = nationality;
        this.overall = overall;
        this.potentialLow = potentialLow;
        this.potentialHigh = potentialHigh;
        this.type = type;
        this.transferFee = transferFee;
        this.plusPlayerName = plusPlayerName;
        this.wage = wage;
        this.contractYears = contractYears;
        this.year = year;
        this.comments = comments;
        this.isFormerPlayer = isFormerPlayer;
        this.managerId = managerId;
        this.userId = userId;
        this.timeAdded = timeAdded;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public String getFormerTeam() {
        return formerTeam;
    }

    public void setFormerTeam(String formerTeam) {
        this.formerTeam = formerTeam;
    }

    public String getCurrentTeam() {
        return currentTeam;
    }

    public void setCurrentTeam(String currentTeam) {
        this.currentTeam = currentTeam;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTransferFee() {
        return transferFee;
    }

    public void setTransferFee(long transferFee) {
        this.transferFee = transferFee;
    }

    public String getPlusPlayerName() {
        return plusPlayerName;
    }

    public void setPlusPlayerName(String plusPlayerName) {
        this.plusPlayerName = plusPlayerName;
    }

    public int getWage() {
        return wage;
    }

    public void setWage(int wage) {
        this.wage = wage;
    }

    public int getContractYears() {
        return contractYears;
    }

    public void setContractYears(int contractYears) {
        this.contractYears = contractYears;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isFormerPlayer() {
        return isFormerPlayer;
    }

    public void setFormerPlayer(boolean formerPlayer) {
        isFormerPlayer = formerPlayer;
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
