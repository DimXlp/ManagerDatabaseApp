package model;

import com.google.firebase.Timestamp;

public class FirstTeamPlayer extends Player {

    private int number;
    private String yearSigned;
    private String yearScouted;
    private boolean isLoanPlayer;
    private String team;

    public FirstTeamPlayer() {
        super();
    }

    public FirstTeamPlayer(long id, String firstName, String lastName, String fullName, String position, int number, String team, String nationality, int overall, int potentialLow, int potentialHigh, String yearSigned, String yearScouted, long managerId, boolean isLoanPlayer, String userId, Timestamp timeAdded) {
        super(id, firstName, lastName, fullName, position, nationality, overall, potentialLow, potentialHigh, managerId, userId, timeAdded);
        this.number = number;
        this.yearSigned = yearSigned;
        this.yearScouted = yearScouted;
        this.isLoanPlayer = isLoanPlayer;
        this.team = team;
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

    public boolean isLoanPlayer() {
        return isLoanPlayer;
    }

    public void setLoanPlayer(boolean loanPlayer) {
        isLoanPlayer = loanPlayer;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}
