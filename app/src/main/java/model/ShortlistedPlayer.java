package model;

import com.google.firebase.Timestamp;

public class ShortlistedPlayer {
    private long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String position;
    private String nationality;
    private int overall;
    private int potentialLow;
    private int potentialHigh;
    private String team;
    private int value;
    private int wage;
    private String comments;
    private long managerId;
    private String userId;
    private Timestamp timeAdded;

    public ShortlistedPlayer() {
    }

    public ShortlistedPlayer(long id, String firstName, String lastName, String fullName, String position, String nationality, int overall, int potentialLow, int potentialHigh, String team, int value, int wage, String comments, long managerId, String userId, Timestamp timeAdded) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.position = position;
        this.nationality = nationality;
        this.overall = overall;
        this.potentialLow = potentialLow;
        this.potentialHigh = potentialHigh;
        this.team = team;
        this.value = value;
        this.wage = wage;
        this.comments = comments;
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

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getWage() {
        return wage;
    }

    public void setWage(int wage) {
        this.wage = wage;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
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
