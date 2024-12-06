package model;

import com.google.firebase.Timestamp;

public class ShortlistedPlayer extends Player {

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
        super(id, firstName, lastName, fullName, position, nationality, overall, potentialLow, potentialHigh, managerId, userId, timeAdded);
        this.team = team;
        this.value = value;
        this.wage = wage;
        this.comments = comments;
        this.managerId = managerId;
        this.userId = userId;
        this.timeAdded = timeAdded;
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
