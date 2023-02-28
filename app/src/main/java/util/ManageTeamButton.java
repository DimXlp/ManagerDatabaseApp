package util;

import android.widget.ImageView;
import android.widget.TextView;

public class ManageTeamButton {

    private String title;

    public ManageTeamButton() {
    }

    public ManageTeamButton(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
