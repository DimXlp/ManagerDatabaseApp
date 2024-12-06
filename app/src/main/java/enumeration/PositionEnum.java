package enumeration;

public enum PositionEnum {

    GK("GK", "Goalkeeper", "Goalkeepers"),
    CB("CB", "Center Back", "Center Backs"),
    RB("RB", "Right Back", "Right Backs"),
    RWB("RWB", "Right Wing Back", "Right Backs"),
    LB("LB", "Left Back", "Left Backs"),
    LWB("LWB", "Left Wing Back", "Left Backs"),
    CDM("CDM", "Center Defensive Midfielder", "Center Defensive Mids"),
    CM("CM", "Center Midfielder", "Center Midfielders"),
    CAM("CAM", "Center Attacking Midfielder", "Center Attacking Mids"),
    RM("RM", "Right Midfielder", "Right Wingers"),
    RW("RW", "Right Winger", "Right Wingers"),
    LM("LM", "Left Midfielder", "Left Wingers"),
    LW("LW", "Left Winger", "Left Wingers"),
    ST("ST", "Striker", "Strikers"),
    CF("CF", "Center Forward", "Strikers"),
    RF("RF", "Right Forward", "Strikers"),
    LF("LF", "Left Forwards", "Strikers");

    private String initials;
    private String positionName;
    private String category;

    PositionEnum(String initials, String positionName, String category) {
        this.initials = initials;
        this.positionName = positionName;
        this.category = category;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
