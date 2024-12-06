package model;

import com.google.firebase.Timestamp;

public class FormerPlayer extends Player {

    private int number;
    private String yearSigned;
    private String yearScouted;
    private String yearLeft;
    private String team;

    public FormerPlayer() {
    }

    public FormerPlayer(long id, String firstName, String lastName, String fullName, String position, int number, String nationality, int overall, int potentialLow, int potentialHigh, String yearSigned, String yearScouted, String yearLeft, long managerId, String userId, Timestamp timeAdded) {
        super(id, firstName, lastName, fullName, position, nationality, overall, potentialLow, potentialHigh, managerId, userId, timeAdded);
        this.number = number;
        this.yearSigned = yearSigned;
        this.yearScouted = yearScouted;
        this.yearLeft = yearLeft;
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

    public String getYearLeft() {
        return yearLeft;
    }

    public void setYearLeft(String yearLeft) {
        this.yearLeft = yearLeft;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}
