package enumeration;

public enum PositionEnum {

    GK("GK", "Goalkeeper", "Goalkeepers", "GKs"),
    CB("CB", "Center Back", "Center Backs", "Defs"),
    RB("RB", "Right Back", "Right Backs", "Defs"),
    RWB("RWB", "Right Wing Back", "Right Backs", "Defs"),
    LB("LB", "Left Back", "Left Backs", "Defs"),
    LWB("LWB", "Left Wing Back", "Left Backs", "Defs"),
    CDM("CDM", "Center Defensive Midfielder", "Center Defensive Mids", "Mids"),
    CM("CM", "Center Midfielder", "Center Midfielders", "Mids"),
    CAM("CAM", "Center Attacking Midfielder", "Center Attacking Mids", "Mids"),
    RM("RM", "Right Midfielder", "Right Wingers", "Mids"),
    RW("RW", "Right Winger", "Right Wingers", "Atts"),
    LM("LM", "Left Midfielder", "Left Wingers", "Mids"),
    LW("LW", "Left Winger", "Left Wingers", "Atts"),
    ST("ST", "Striker", "Strikers", "Atts"),
    CF("CF", "Center Forward", "Strikers", "Atts"),
    RF("RF", "Right Forward", "Strikers", "Atts"),
    LF("LF", "Left Forwards", "Strikers", "Atts");

    private String initials;
    private String positionName;
    private String category;

    private String group;

    PositionEnum(String initials, String positionName, String category, String group) {
        this.initials = initials;
        this.positionName = positionName;
        this.category = category;
        this.group = group;
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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
