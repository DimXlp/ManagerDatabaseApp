package uihelpers;

import android.graphics.Color;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.dimxlp.managerdb.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import model.Transfer;
import util.ValueFormatter;

public class TransferEditor {
    private final EditText firstNameEdit;
    private final EditText lastNameEdit;
    private final TextView positionPickerEdit;
    private final AutoCompleteTextView nationalityEdit;
    private final EditText overallEdit;
    private final EditText potentialLowEdit;
    private final EditText potentialHighEdit;
    private final TextView typeOfTransferPickerEdit;
    private final SwitchMaterial playerExchangeSwitch;
    private final EditText oldTeamEdit;
    private final EditText newTeamEdit;
    private final TextInputLayout feeTil;
    private final EditText feeEdit;
    private final TextView plusPlayerPickerEdit;
    private long playerSpinnerId = 0;
    private final TextInputLayout wageTil;
    private final EditText wageEdit;
    private final EditText contractYearsEdit;
    private final TextView yearEdit;
    private final EditText commentsEdit;
    private final Button editTransferButton;
    private boolean isPlusPlayer = false;
    private boolean isExchangePlayer = false;
    private String plusPlayerName = null;

    public TransferEditor(View view) {
        this.firstNameEdit = view.findViewById(R.id.first_name_trf_edit);
        this.lastNameEdit = view.findViewById(R.id.last_name_trf_edit);
        this.positionPickerEdit = view.findViewById(R.id.position_picker_trf_edit);
        this.nationalityEdit = view.findViewById(R.id.nationality_trf_edit);
        this.overallEdit = view.findViewById(R.id.overall_trf_edit);
        this.potentialLowEdit = view.findViewById(R.id.potential_low_trf_edit);
        this.potentialHighEdit = view.findViewById(R.id.potential_high_trf_edit);
        this.typeOfTransferPickerEdit = view.findViewById(R.id.type_of_transfer_picker_trf_edit);
        this.playerExchangeSwitch = view.findViewById(R.id.player_exchange_switch_trf_edit);
        this.oldTeamEdit = view.findViewById(R.id.old_team_trf_edit);
        this.newTeamEdit = view.findViewById(R.id.new_team_trf_edit);
        this.feeTil = view.findViewById(R.id.fee_til_trf_edit);
        this.feeEdit = view.findViewById(R.id.fee_trf_edit);
        this.plusPlayerPickerEdit = view.findViewById(R.id.plus_player_picker_trf_edit);
        this.wageTil = view.findViewById(R.id.wage_til_trf_edit);
        this.wageEdit = view.findViewById(R.id.wage_trf_edit);
        this.contractYearsEdit = view.findViewById(R.id.contract_years_trf_edit);
        this.yearEdit = view.findViewById(R.id.year_picker_trf_edit);
        this.commentsEdit = view.findViewById(R.id.comments_trf_edit);
        this.editTransferButton = view.findViewById(R.id.edit_transfer_button);
    }

    public EditText getFirstNameEdit() {
        return firstNameEdit;
    }

    public EditText getLastNameEdit() {
        return lastNameEdit;
    }

    public TextView getPositionPickerEdit() {
        return positionPickerEdit;
    }

    public AutoCompleteTextView getNationalityEdit() {
        return nationalityEdit;
    }

    public EditText getOverallEdit() {
        return overallEdit;
    }

    public EditText getPotentialLowEdit() {
        return potentialLowEdit;
    }

    public EditText getPotentialHighEdit() {
        return potentialHighEdit;
    }

    public TextView getTypeOfTransferPickerEdit() {
        return typeOfTransferPickerEdit;
    }

    public SwitchMaterial getPlayerExchangeSwitch() {
        return playerExchangeSwitch;
    }

    public EditText getOldTeamEdit() {
        return oldTeamEdit;
    }

    public EditText getNewTeamEdit() {
        return newTeamEdit;
    }

    public TextInputLayout getFeeTil() {
        return feeTil;
    }

    public EditText getFeeEdit() {
        return feeEdit;
    }

    public TextView getPlusPlayerPickerEdit() {
        return plusPlayerPickerEdit;
    }

    public long getPlayerSpinnerId() {
        return playerSpinnerId;
    }

    public void setPlayerSpinnerId(long playerSpinnerId) {
        this.playerSpinnerId = playerSpinnerId;
    }

    public TextInputLayout getWageTil() {
        return wageTil;
    }

    public EditText getWageEdit() {
        return wageEdit;
    }

    public EditText getContractYearsEdit() {
        return contractYearsEdit;
    }

    public TextView getYearEdit() {
        return yearEdit;
    }

    public EditText getCommentsEdit() {
        return commentsEdit;
    }

    public void formatValue(EditText value) {
        ValueFormatter.formatValue(value);
    }

    public Button getEditTransferButton() {
        return editTransferButton;
    }

    public void setIsPlusPlayer(boolean plusPlayer) {
        this.isPlusPlayer = plusPlayer;
    }

    public boolean isPlusPlayer() {
        return isPlusPlayer;
    }

    public boolean isExchangePlayer() {
        return isExchangePlayer;
    }

    public void setIsExchangePlayer(boolean exchangePlayer) {
        this.isExchangePlayer = exchangePlayer;
    }

    public String getPlusPlayerName() {
        return plusPlayerName;
    }

    public void setPlusPlayerName(String plusPlayerName) {
        this.plusPlayerName = plusPlayerName;
    }

    public void setTransferEditorFields(Transfer transfer) {
        firstNameEdit.setText(transfer.getFirstName());
        lastNameEdit.setText(transfer.getLastName());
        positionPickerEdit.setText(transfer.getPosition());
        nationalityEdit.setText(transfer.getNationality());
        overallEdit.setText(String.valueOf(transfer.getOverall()));
        potentialLowEdit.setText(String.valueOf(transfer.getPotentialLow()));
        potentialHighEdit.setText(String.valueOf(transfer.getPotentialHigh()));
        typeOfTransferPickerEdit.setText(transfer.getType());
        oldTeamEdit.setText(transfer.getFormerTeam());
        newTeamEdit.setText(transfer.getCurrentTeam());
        feeEdit.setText(String.valueOf(transfer.getTransferFee()));
        wageEdit.setText(String.valueOf(transfer.getWage()));
        contractYearsEdit.setText(String.valueOf(transfer.getContractYears()));
        yearEdit.setText(transfer.getYear());
        commentsEdit.setText(transfer.getComments());
        plusPlayerPickerEdit.setText(transfer.getPlusPlayerName());
        playerExchangeSwitch.setChecked(transfer.getExchangePlayerName()!=null);
        isExchangePlayer = transfer.getExchangePlayerName() != null;
        isPlusPlayer = transfer.getPlusPlayerName() != null;
        plusPlayerName = transfer.getPlusPlayerName();
    }

    public void setAllFieldsVisible() {
        firstNameEdit.setVisibility(View.VISIBLE);
        lastNameEdit.setVisibility(View.VISIBLE);
        positionPickerEdit.setVisibility(View.VISIBLE);
        nationalityEdit.setVisibility(View.VISIBLE);
        overallEdit.setVisibility(View.VISIBLE);
        potentialLowEdit.setVisibility(View.VISIBLE);
        potentialHighEdit.setVisibility(View.VISIBLE);
        typeOfTransferPickerEdit.setVisibility(View.VISIBLE);
        playerExchangeSwitch.setVisibility(View.VISIBLE);
        oldTeamEdit.setVisibility(View.VISIBLE);
        newTeamEdit.setVisibility(View.VISIBLE);
        feeEdit.setVisibility(View.VISIBLE);
        plusPlayerPickerEdit.setVisibility(View.VISIBLE);
        wageEdit.setVisibility(View.VISIBLE);
        contractYearsEdit.setVisibility(View.VISIBLE);
        yearEdit.setVisibility(View.VISIBLE);
        commentsEdit.setVisibility(View.VISIBLE);
    }

    public void setAllFieldsEnabled() {
        firstNameEdit.setEnabled(true);
        lastNameEdit.setEnabled(true);
        positionPickerEdit.setEnabled(true);
        nationalityEdit.setEnabled(true);
        overallEdit.setEnabled(true);
        potentialLowEdit.setEnabled(true);
        potentialHighEdit.setEnabled(true);
        typeOfTransferPickerEdit.setEnabled(true);
        playerExchangeSwitch.setEnabled(true);
        oldTeamEdit.setEnabled(true);
        newTeamEdit.setEnabled(true);
        feeEdit.setEnabled(true);
        plusPlayerPickerEdit.setEnabled(true);
        wageEdit.setEnabled(true);
        contractYearsEdit.setEnabled(true);
        yearEdit.setEnabled(true);
        commentsEdit.setEnabled(true);
    }

    public void setAllFieldsTextColor() {
        firstNameEdit.setTextColor(Color.BLACK);
        lastNameEdit.setTextColor(Color.BLACK);
        nationalityEdit.setTextColor(Color.BLACK);
        overallEdit.setTextColor(Color.BLACK);
        potentialLowEdit.setTextColor(Color.BLACK);
        potentialHighEdit.setTextColor(Color.BLACK);
        playerExchangeSwitch.setTextColor(Color.BLACK);
        oldTeamEdit.setTextColor(Color.BLACK);
        newTeamEdit.setTextColor(Color.BLACK);
        feeEdit.setTextColor(Color.BLACK);
        wageEdit.setTextColor(Color.BLACK);
        contractYearsEdit.setTextColor(Color.BLACK);
        commentsEdit.setTextColor(Color.BLACK);
    }
}
