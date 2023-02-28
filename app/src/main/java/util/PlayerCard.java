package util;

public class PlayerCard {

    private String number;
    private String fullName;
    private String position;
    private String overall;
    private String potential;
    private String nationality;
    private String yearSigned;
    private String yearScouted;

    public PlayerCard() {
    }

    public PlayerCard(String number, String fullName, String position, String overall, String potential, String nationality, String yearSigned, String yearScouted) {
        this.number = number;
        this.fullName = fullName;
        this.position = position;
        this.overall = overall;
        this.potential = potential;
        this.nationality = nationality;
        this.yearSigned = yearSigned;
        this.yearScouted = yearScouted;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public String getOverall() {
        return overall;
    }

    public void setOverall(String overall) {
        this.overall = overall;
    }

    public String getPotential() {
        return potential;
    }

    public void setPotential(String potential) {
        this.potential = potential;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
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
}
