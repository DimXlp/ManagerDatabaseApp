package util;

public class ManagerSelectionButton {

    private String managerName;
    private String teamName;

    public ManagerSelectionButton() {
    }

    public ManagerSelectionButton(String managerName, String teamName) {
        this.managerName = managerName;
        this.teamName = teamName;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
