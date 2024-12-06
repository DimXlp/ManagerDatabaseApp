package model;

import com.google.firebase.Timestamp;

public class YouthTeamPlayer extends Player {
    private int number;
    private String team;
    private String yearScouted;

    public YouthTeamPlayer() {
    }

    public YouthTeamPlayer(long id, String firstName, String lastName, String fullName, String position, int number, String team, String nationality, int overall, int potentialLow, int potentialHigh, String yearScouted, long managerId, String userId, Timestamp timeAdded) {
        super(id, firstName, lastName, fullName, position, nationality, overall, potentialLow, potentialHigh, managerId, userId, timeAdded);
        this.number = number;
        this.team = team;
        this.yearScouted = yearScouted;
    }



    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getYearScouted() {
        return yearScouted;
    }

    public void setYearScouted(String yearScouted) {
        this.yearScouted = yearScouted;
    }
}
