package model;


import com.google.firebase.Timestamp;

public class Manager {

    private long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String nationality;
    private String team;
    private String currency;
    private String teamBadgeUrl;
    private Timestamp timeAdded;
    private String userId;

    public Manager() {
    }

    public Manager(long id, String firstName, String lastName, String nationality, String managerPhotoUrl, String team, String currency, String teamBadgeUrl, Timestamp timeAdded, String userId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.nationality = nationality;
        this.team = team;
        this.currency = currency;
        this.teamBadgeUrl = teamBadgeUrl;
        this.timeAdded = timeAdded;
        this.userId = userId;
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

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getTeamBadgeUrl() {
        return teamBadgeUrl;
    }

    public void setTeamBadgeUrl(String teamBadgeUrl) {
        this.teamBadgeUrl = teamBadgeUrl;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
