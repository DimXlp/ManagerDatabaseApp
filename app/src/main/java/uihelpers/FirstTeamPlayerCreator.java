package uihelpers;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.dimxlp.managerdb.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class FirstTeamPlayerCreator {

    private TextView title;
    private EditText firstName;
    private EditText lastName;
    private TextView position;
    private EditText number;
    private EditText nationality;
    private EditText overall;
    private EditText potentialLow;
    private EditText potentialHigh;
    private TextView yearSigned;
    private TextView yearScouted;
    private SwitchMaterial loanSwitch;
    private Button savePlayerButton;

    public FirstTeamPlayerCreator(View view) {
        this.title = view.findViewById(R.id.create_ft_player);
        this.firstName = view.findViewById(R.id.first_name_ftp_create);
        this.lastName = view.findViewById(R.id.last_name_ftp_create);
        this.position = view.findViewById(R.id.position_picker_ftp_create);
        this.number = view.findViewById(R.id.number_ftp_create);
        this.nationality = view.findViewById(R.id.nationality_ftp_create);
        this.overall = view.findViewById(R.id.overall_ftp_create);
        this.potentialLow = view.findViewById(R.id.potential_low_ftp_create);
        this.potentialHigh = view.findViewById(R.id.potential_high_ftp_create);
        this.yearSigned = view.findViewById(R.id.year_signed_picker_ftp_create);
        this.yearScouted = view.findViewById(R.id.year_scouted_picker_ftp_create);
        this.loanSwitch = view.findViewById(R.id.loan_player_switch_ftp_create);
        this.savePlayerButton = view.findViewById(R.id.create_ft_player_button);
    }

    public TextView getTitle() {
        return title;
    }

    public void setTitle(TextView title) {
        this.title = title;
    }

    public EditText getFirstName() {
        return firstName;
    }

    public void setFirstName(EditText firstName) {
        this.firstName = firstName;
    }

    public EditText getLastName() {
        return lastName;
    }

    public void setLastName(EditText lastName) {
        this.lastName = lastName;
    }

    public TextView getPosition() {
        return position;
    }

    public void setPosition(TextView position) {
        this.position = position;
    }

    public EditText getNumber() {
        return number;
    }

    public void setNumber(EditText number) {
        this.number = number;
    }

    public EditText getNationality() {
        return nationality;
    }

    public void setNationality(EditText nationality) {
        this.nationality = nationality;
    }

    public EditText getOverall() {
        return overall;
    }

    public void setOverall(EditText overall) {
        this.overall = overall;
    }

    public EditText getPotentialLow() {
        return potentialLow;
    }

    public void setPotentialLow(EditText potentialLow) {
        this.potentialLow = potentialLow;
    }

    public EditText getPotentialHigh() {
        return potentialHigh;
    }

    public void setPotentialHigh(EditText potentialHigh) {
        this.potentialHigh = potentialHigh;
    }

    public TextView getYearSigned() {
        return yearSigned;
    }

    public void setYearSigned(TextView yearSigned) {
        this.yearSigned = yearSigned;
    }

    public TextView getYearScouted() {
        return yearScouted;
    }

    public void setYearScouted(TextView yearScouted) {
        this.yearScouted = yearScouted;
    }

    public SwitchMaterial getLoanSwitch() {
        return loanSwitch;
    }

    public void setLoanSwitch(SwitchMaterial loanSwitch) {
        this.loanSwitch = loanSwitch;
    }

    public Button getSavePlayerButton() {
        return savePlayerButton;
    }

    public void setSavePlayerButton(Button savePlayerButton) {
        this.savePlayerButton = savePlayerButton;
    }

    public void changePropertiesForExchange() {
        title.setText("Add Exchange Player");
        savePlayerButton.setText(R.string.save_player);
        loanSwitch.setVisibility(View.GONE);
        yearScouted.setVisibility(View.GONE);
    }
}
