package ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.LoanedOutPlayersActivity;
import com.dimxlp.managerdb.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import enumeration.LoanEnum;
import model.FirstTeamPlayer;
import model.LoanedOutPlayer;
import util.AnimationUtil;
import util.NationalityFlagUtil;
import util.UserApi;

public class LoanedOutPlayerRecAdapter extends RecyclerView.Adapter<LoanedOutPlayerRecAdapter.ViewHolder> {

    private static final String LOG_TAG = "RAFI|LoanedOutPlayerRecAdapter";
    private Context context;
    private List<LoanedOutPlayer> loanedOutPlayerList;
    private long managerId;
    private String team;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference loPlayersColRef = db.collection("LoanedOutPlayers");
    private CollectionReference ftPlayersColRef = db.collection("FirstTeamPlayers");
    private CollectionReference transfersColRef = db.collection("Transfers");

    public LoanedOutPlayerRecAdapter(Context context, List<LoanedOutPlayer> loanedOutPlayerList, long managerId, String team) {
        this.context = context;
        this.loanedOutPlayerList = loanedOutPlayerList;
        this.managerId = managerId;
        this.team = team;
    }

    @NonNull
    @Override
    public LoanedOutPlayerRecAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.lo_player_row, parent, false);
        return new LoanedOutPlayerRecAdapter.ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull LoanedOutPlayerRecAdapter.ViewHolder holder, int position) {
        LoanedOutPlayer player = loanedOutPlayerList.get(position);

        holder.playerTopBar.setOnClickListener(v -> {
            boolean isVisible = holder.details.getVisibility() == View.VISIBLE;
            if (isVisible) {
                AnimationUtil.collapseView(holder.details);
            } else {
                AnimationUtil.expandView(holder.details);
            }
        });

        String fullName = player.getFullName();
        if (fullName.length() > 16 && fullName.contains(" ")) {
            String[] parts = fullName.split(" ");
            fullName = parts[0].charAt(0) + ". " + parts[1];
        }

        holder.numberText.setText(String.valueOf(player.getNumber()));
        holder.fullNameText.setText(fullName);
        StringBuilder basic = new StringBuilder();
        basic.append(player.getPosition());
        basic.append(" · ").append(player.getOverall());
        if (player.getPotentialLow() != 0 && player.getPotentialHigh() != 0) {
            basic.append(" · ").append(player.getPotentialLow()).append("–").append(player.getPotentialHigh());
        }
        basic.append(" · ");
        holder.basicInfo.setText(basic);

        String nationality = player.getNationality();
        String iso = NationalityFlagUtil.getNationalityToIsoMap().getOrDefault(nationality, "un");
        int flagResId = NationalityFlagUtil.getFlagResId(context, iso);

        holder.playerFlag.setImageResource(flagResId);
        holder.playerNationality.setText(nationality);

        boolean hasSigned = player.getYearSigned() != null && !player.getYearSigned().equals("0");
        boolean hasScouted = player.getYearScouted() != null && !player.getYearScouted().equals("0");

        holder.yearSignedText.setText(hasSigned ? player.getYearSigned() : null);
        holder.yearSignedText.setVisibility(hasSigned ? View.VISIBLE : View.GONE);
        holder.yearSignedIcon.setVisibility(hasSigned ? View.VISIBLE : View.GONE);

        holder.yearScoutedText.setText(hasScouted ? player.getYearScouted() : null);
        holder.yearScoutedText.setVisibility(hasScouted ? View.VISIBLE : View.GONE);
        holder.yearScoutedIcon.setVisibility(hasScouted ? View.VISIBLE : View.GONE);

        holder.yearSeparator.setVisibility((hasSigned && hasScouted) ? View.VISIBLE : View.GONE);
        holder.yearContainer.setVisibility((hasSigned || hasScouted) ? View.VISIBLE : View.GONE);

        holder.loanToText.setText("Loan to " + player.getTeam());
        holder.yearLoanedText.setText(player.getYearLoanedOut());
        holder.loanType.setText(player.getTypeOfLoan());

        holder.details.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return loanedOutPlayerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private LinearLayout playerTopBar;
        private TextView numberText;
        private TextView fullNameText;
        private TextView basicInfo;
        private ImageView playerFlag;
        private TextView playerNationality;
        private TextView yearSignedText;
        private TextView yearScoutedText;
        private View yearSignedIcon;
        private View yearScoutedIcon;
        private TextView yearSeparator;
        private View yearContainer;
        private TextView loanToText;
        private ImageView yearLoanedIcon;
        private TextView yearLoanedText;
        private TextView loanSeparator;
        private TextView loanType;
        private LinearLayout details;
        private ImageView actionMenu;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            playerTopBar = itemView.findViewById(R.id.player_top_bar_lop);
            numberText = itemView.findViewById(R.id.player_number_lop);
            fullNameText = itemView.findViewById(R.id.player_full_name_lop);
            basicInfo = itemView.findViewById(R.id.player_basic_text_lop);
            playerFlag = itemView.findViewById(R.id.player_flag_lop);
            playerNationality = itemView.findViewById(R.id.player_nationality_lop);
            yearSignedText = itemView.findViewById(R.id.year_signed_text_lop);
            yearScoutedText = itemView.findViewById(R.id.year_scouted_text_lop);
            yearSignedIcon = itemView.findViewById(R.id.year_signed_icon_lop);
            yearScoutedIcon = itemView.findViewById(R.id.year_scouted_icon_lop);
            yearSeparator = itemView.findViewById(R.id.year_separator_lop);
            yearContainer = itemView.findViewById(R.id.player_year_info_container_lop);
            loanToText = itemView.findViewById(R.id.player_loan_to_info_lop);
            yearLoanedIcon = itemView.findViewById(R.id.year_loaned_icon_lop);
            yearLoanedText = itemView.findViewById(R.id.year_loaned_text_lop);
            loanSeparator = itemView.findViewById(R.id.loan_separator_lop);
            loanType = itemView.findViewById(R.id.loan_type_text_lop);
            details = itemView.findViewById(R.id.expandable_section_lop);
            actionMenu = itemView.findViewById(R.id.player_action_menu_lop);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (details.getVisibility() == View.GONE) {
                        details.setVisibility(View.VISIBLE);
                    } else if (details.getVisibility() == View.VISIBLE){
                        details.setVisibility(View.GONE);
                    }
                }
            });

            actionMenu.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(context, actionMenu);
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.loaned_out_player_actions_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.action_edit:
                            clickEditPlayerButton(loanedOutPlayerList.get(getAdapterPosition()));
                            return true;
                        case R.id.action_recall:
                            clickRecallPlayerButton();
                            return true;
                        case R.id.action_delete:
                            clickDeletePlayerButton();
                            return true;
                        default:
                            return false;
                    }
                });

                popupMenu.show();
            });
        }

        private void clickDeletePlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            deletePlayer(loanedOutPlayerList.get(getAdapterPosition()));
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Do you want to delete this player?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        private void clickRecallPlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            recallPlayerFromLoan(loanedOutPlayerList.get(getAdapterPosition()));
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Do you want to terminate this player's loan deal?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        private void deletePlayer(final LoanedOutPlayer player) {
            Log.d(LOG_TAG, "deletePlayer called for player: " + player.getFullName());

            loPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "LoanedOutPlayers collection fetched successfully.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    LoanedOutPlayer loPlayer = ds.toObject(LoanedOutPlayer.class);
                                    if (loPlayer.getId() == player.getId()) {
                                        documentReference = loPlayersColRef.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Player successfully deleted: " + player.getFullName());
                                                Toast.makeText(context, "Player deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                Intent intent = new Intent(context, LoanedOutPlayersActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                context.startActivity(intent);
                                                ((Activity)context).finish();
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error deleting player.", e));
                            } else {
                                Log.e(LOG_TAG, "Error fetching LoanedOutPlayers collection.", task.getException());
                            }

                        }
                    });
        }

        private void recallPlayerFromLoan(final LoanedOutPlayer player) {
            Log.d(LOG_TAG, "terminateLoanDeal called for player: " + player.getFullName());

            loPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "LoanedOutPlayers collection fetched successfully.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    LoanedOutPlayer loPlayer = ds.toObject(LoanedOutPlayer.class);
                                    if (loPlayer.getId() == player.getId()) {
                                        documentReference = loPlayersColRef.document(ds.getId());
                                        break;
                                    }
                                }

                                if (documentReference == null) {
                                    Log.e(LOG_TAG, "No matching document found for player ID: " + player.getId());
                                    Toast.makeText(context, "Error: Player not found in the database.", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Loan deal successfully terminated for player: " + player.getFullName());

                                                FirstTeamPlayer ftPlayer = new FirstTeamPlayer();
                                                ftPlayer.setId(0);
                                                ftPlayer.setFirstName(player.getFirstName());
                                                ftPlayer.setLastName(player.getLastName());
                                                ftPlayer.setFullName(player.getFullName());
                                                ftPlayer.setPosition(player.getPosition());
                                                ftPlayer.setNumber(player.getNumber());
                                                ftPlayer.setTeam(team);
                                                ftPlayer.setNationality(player.getNationality());
                                                ftPlayer.setOverall(player.getOverall());
                                                ftPlayer.setPotentialLow(player.getPotentialLow());
                                                ftPlayer.setPotentialHigh(player.getPotentialHigh());
                                                ftPlayer.setYearSigned(player.getYearSigned());
                                                ftPlayer.setYearScouted(player.getYearScouted());
                                                ftPlayer.setManagerId(managerId);
                                                ftPlayer.setUserId(UserApi.getInstance().getUserId());
                                                ftPlayer.setTimeAdded(new Timestamp(new Date()));
                                                ftPlayersColRef.add(ftPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Log.d(LOG_TAG, "Player returned to First Team: " + ftPlayer.getFullName());
                                                                Toast.makeText(context, "Player has returned!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error returning player to First Team.", e));

                                                Intent intent = new Intent(context, LoanedOutPlayersActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                context.startActivity(intent);
                                                ((Activity)context).finish();
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error terminating loan deal.", e));
                            } else {
                                Log.e(LOG_TAG, "Error fetching LoanedOutPlayers collection.", task.getException());
                            }
                        }
                    });
        }

        private void clickEditPlayerButton(final LoanedOutPlayer player) {
            Log.d(LOG_TAG, "editPlayer called for player: " + player.getFullName());

            BottomSheetDialog editDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.edit_loaned_out_player_popup, null);
            editDialog.setContentView(view);

            final EditText firstName = view.findViewById(R.id.first_name_lop_edit);
            final EditText lastName = view.findViewById(R.id.last_name_lop_edit);
            final TextView positionPicker = view.findViewById(R.id.position_picker_lop_edit);
            final EditText number = view.findViewById(R.id.number_lop_edit);
            final AutoCompleteTextView nationality = view.findViewById(R.id.nationality_lop_edit);
            final EditText overall = view.findViewById(R.id.overall_lop_edit);
            final EditText potentialLow = view.findViewById(R.id.potential_low_lop_edit);
            final EditText potentialHigh = view.findViewById(R.id.potential_high_lop_edit);
            final TextView yearSigned = view.findViewById(R.id.year_signed_picker_lop_edit);
            final TextView yearScouted = view.findViewById(R.id.year_scouted_picker_lop_edit);
            final EditText teamText = view.findViewById(R.id.team_lop_edit);
            final TextView yearLoaned = view.findViewById(R.id.year_loaned_picker_lop_edit);
            final TextView typeOfLoanPicker = view.findViewById(R.id.loan_type_lop_edit);
            Button editPlayerButton = view.findViewById(R.id.edit_lop_button);

            String[] countrySuggestions = context.getResources().getStringArray(R.array.nationalities);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_dropdown_item_1line, countrySuggestions);

            nationality.setAdapter(adapter);

            nationality.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) nationality.showDropDown();
            });

            String[] positions = context.getResources().getStringArray(R.array.position_array);
            String[] years = context.getResources().getStringArray(R.array.years_array);

            positionPicker.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Position")
                        .setItems(positions, (pickerDialog, which) -> positionPicker.setText(positions[which]))
                        .show();
            });

            yearSigned.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Signed")
                        .setItems(years, (pickerDialog, which) -> yearSigned.setText(years[which]))
                        .show();
            });

            yearScouted.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Scouted")
                        .setItems(years, (pickerDialog, which) -> yearScouted.setText(years[which]))
                        .show();
            });

            yearLoaned.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Loaned")
                        .setItems(years, (pickerDialog, which) -> yearLoaned.setText(years[which]))
                        .show();
            });

            String[] loanTypes = getLoans();

            typeOfLoanPicker.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(context)
                        .setTitle("Select Loan Type")
                        .setItems(loanTypes, (pickerDialog, loan) -> typeOfLoanPicker.setText(loanTypes[loan]))
                        .show();
            });

            firstName.setText(player.getFirstName());
            lastName.setText(player.getLastName());
            positionPicker.setText(player.getPosition());
            number.setText(String.valueOf(player.getNumber()));
            nationality.setText(player.getNationality());
            overall.setText(String.valueOf(player.getOverall()));
            potentialLow.setText(String.valueOf(player.getPotentialLow()));
            potentialHigh.setText(String.valueOf(player.getPotentialHigh()));
            yearSigned.setText(player.getYearSigned());
            yearScouted.setText(player.getYearScouted());
            teamText.setText(player.getTeam());
            yearLoaned.setText(player.getYearLoanedOut());
            typeOfLoanPicker.setText(player.getTypeOfLoan());
            Log.d(LOG_TAG, "Player data populated into fields for editing: " + player.getFullName());

            editDialog.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Save Player button clicked for player: " + player.getFullName());

                    if (!lastName.getText().toString().isEmpty() &&
                            !nationality.getText().toString().isEmpty() &&
                            !positionPicker.getText().toString().isEmpty() &&
                            !overall.getText().toString().isEmpty() &&
                            !yearSigned.getText().toString().isEmpty() &&
                            !yearLoaned.getText().toString().isEmpty()) {
                        Log.d(LOG_TAG, "Validation successful. Proceeding to update player.");

                        loPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(LOG_TAG, "Loaned out players fetched successfully.");

                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                LoanedOutPlayer loPlayer = ds.toObject(LoanedOutPlayer.class);
                                                if (loPlayer.getId() == player.getId()) {
                                                    documentReference = loPlayersColRef.document(ds.getId());
                                                }
                                            }
                                            String no = number.getText().toString().trim();
                                            String nationalityPlayer = nationality.getText().toString().trim();
                                            Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
                                            String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            String yScouted = yearScouted.getText().toString().trim();
                                            assert documentReference != null;
                                            Log.d(LOG_TAG, "Document reference found for player: " + player.getFullName());
                                            documentReference.update("firstName", firstName.getText().toString().trim(),
                                                    "lastName", lastName.getText().toString().trim(),
                                                    "fullName", firstName.getText().toString().trim() + " " + lastName.getText().toString().trim(),
                                                    "position", positionPicker.getText().toString().trim(),
                                                    "number", (!no.isEmpty()) ? Integer.parseInt(no) : 99,
                                                    "nationality", nationalityInput,
                                                    "overall", Integer.parseInt(overall.getText().toString().trim()),
                                                    "potentialLow", (!ptlLow.isEmpty()) ? Integer.parseInt(ptlLow) : 0,
                                                    "potentialHigh", (!ptlHi.isEmpty()) ? Integer.parseInt(ptlHi) : 0,
                                                    "yearSigned", yearSigned.getText().toString().trim(),
                                                    "yearScouted", yScouted,
                                                    "team", teamText.getText().toString().trim(),
                                                    "yearLoanedOut", yearLoaned.getText().toString().trim(),
                                                    "typeOfLoan", typeOfLoanPicker.getText().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(LOG_TAG, "Player successfully updated in Firestore: " + player.getFullName());
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            editDialog.dismiss();
                                                            Intent intent = new Intent(context, LoanedOutPlayersActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            context.startActivity(intent);
                                                            ((Activity) context).finish();
                                                            Toast.makeText(context, "Player edited! You should also edit the corresponding transfer deal!", Toast.LENGTH_LONG)
                                                                    .show();

                                                        }
                                                    })
                                                    .addOnFailureListener(e -> Log.e(LOG_TAG, "Error updating player in Firestore.", e));
                                        } else {
                                            Log.e(LOG_TAG, "Error fetching First Team players.", task.getException());
                                        }
                                    }
                                });
                    } else {
                        Log.w(LOG_TAG, "Validation failed: Required fields are missing.");
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Year Signed and Year Loaned are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }

        private String[] getLoans() {
            return Arrays.stream(LoanEnum.values())
                    .map(LoanEnum::getDescription)
                    .toArray(String[]::new);
        }
    }
}
