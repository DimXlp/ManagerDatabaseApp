package ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.FirstTeamListActivity;
import com.dimxlp.managerdb.FormerPlayersListActivity;
import com.dimxlp.managerdb.LoanedOutPlayersActivity;
import com.dimxlp.managerdb.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import enumeration.LoanEnum;
import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.LoanedOutPlayer;
import model.Transfer;
import util.UserApi;

public class LoanedOutPlayerRecAdapter extends RecyclerView.Adapter<LoanedOutPlayerRecAdapter.ViewHolder> {

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

        holder.numberText.setText(String.valueOf(player.getNumber()));
        holder.fullNameText.setText(player.getFullName());
        holder.positionText.setText(player.getPosition());
        holder.overallNoText.setText(String.valueOf(player.getOverall()));
        if (player.getPotentialLow() != 0 && player.getPotentialHigh() != 0) {
            holder.potentialNoText.setText(String.format("%d-%d", player.getPotentialLow(), player.getPotentialHigh()));
        } else {
            holder.potentialNoText.setText("??-??");
        }
        holder.countryText.setText(player.getNationality());
        holder.yearSignedDateText.setText(player.getYearSigned());
        String yScouted = player.getYearScouted();
        if (!yScouted.equals("0")) {
            holder.yearScoutedDateText.setText(yScouted);
        } else {
            holder.yearScoutedDateText.setText("????/??");
        }
        holder.teamText.setText(player.getTeam() + "\n(" + player.getYearLoanedOut() + ", " + player.getTypeOfLoan() + ")");

        GradientDrawable gradientDrawable = (GradientDrawable) holder.positionOval.getBackground();
        String pos = holder.positionText.getText().toString().trim();
        if (pos.equals("CB") ||
                pos.equals("RB") ||
                pos.equals("RWB") ||
                pos.equals("LB") ||
                pos.equals("LWB")) {
            gradientDrawable.setColor(Color.YELLOW);
        } else if (pos.equals("CM") ||
                pos.equals("CDM") ||
                pos.equals("CAM") ||
                pos.equals("RM") ||
                pos.equals("LM")) {
            gradientDrawable.setColor(Color.GREEN);
        } else if (pos.equals("ST") ||
                pos.equals("CF") ||
                pos.equals("LF") ||
                pos.equals("RF") ||
                pos.equals("RW") ||
                pos.equals("LW")) {
            gradientDrawable.setColor(Color.BLUE);
        } else {
            gradientDrawable.setColor(Color.parseColor("#FFA500"));
        }

        holder.details.setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        return loanedOutPlayerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView numberText;
        private TextView fullNameText;
        private ImageView positionOval;
        private TextView positionText;
        private TextView overallText;
        private TextView overallNoText;
        private TextView potentialText;
        private TextView potentialNoText;
        private TextView nationalityText;
        private TextView countryText;
        private TextView yearSignedText;
        private TextView yearSignedDateText;
        private TextView yearScoutedText;
        private TextView yearScoutedDateText;
        private TextView teamLoanedToText;
        private TextView teamText;
        private RelativeLayout details;
        private Button returnButton;
        private Button deleteButton;
        private Button editButton;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            numberText = itemView.findViewById(R.id.number_lop);
            fullNameText = itemView.findViewById(R.id.full_name_lop);
            positionOval = itemView.findViewById(R.id.position_oval_lop);
            positionText = itemView.findViewById(R.id.position_lop);
            overallText = itemView.findViewById(R.id.overall_text_lop);
            overallNoText = itemView.findViewById(R.id.overall_lop);
            potentialText = itemView.findViewById(R.id.potential_text_lop);
            potentialNoText = itemView.findViewById(R.id.potential_lop);
            nationalityText = itemView.findViewById(R.id.nationality_text_lop);
            countryText = itemView.findViewById(R.id.nationality_lop);
            yearSignedText = itemView.findViewById(R.id.year_signed_text_lop);
            yearSignedDateText = itemView.findViewById(R.id.year_signed_lop);
            yearScoutedText = itemView.findViewById(R.id.year_scouted_text_lop);
            yearScoutedDateText = itemView.findViewById(R.id.year_scouted_lop);
            teamLoanedToText = itemView.findViewById(R.id.loan_to_text_lop);
            teamText = itemView.findViewById(R.id.loan_to_lop);
            details = itemView.findViewById(R.id.details_lop);
            returnButton = itemView.findViewById(R.id.return_button_lop);
            deleteButton = itemView.findViewById(R.id.delete_button_lop);
            editButton = itemView.findViewById(R.id.edit_button_lop);

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

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editPlayer(loanedOutPlayerList.get(getAdapterPosition()));
                }
            });

            returnButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    terminateLoanDeal(loanedOutPlayerList.get(getAdapterPosition()));

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
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
            });
        }

        private void deletePlayer(final LoanedOutPlayer player) {
            loPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
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
                                                Toast.makeText(context, "Player deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                Intent intent = new Intent(context, LoanedOutPlayersActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                context.startActivity(intent);
                                                ((Activity)context).finish();
                                            }
                                        });
                            }
                        }
                    });
        }

        private void terminateLoanDeal(final LoanedOutPlayer player) {
            loPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
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
                                                                Toast.makeText(context, "Player has returned!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        });
                                                Intent intent = new Intent(context, LoanedOutPlayersActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                context.startActivity(intent);
                                                ((Activity)context).finish();
                                            }
                                        });
                            }
                        }
                    });
        }

        private void editPlayer(final LoanedOutPlayer player) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.edit_loaned_out_player_popup, null);

            final EditText firstName;
            final EditText lastName;
            final Spinner positionSpinner;
            final EditText number;
            final EditText nationality;
            final EditText overall;
            final EditText potentialLow;
            final EditText potentialHigh;
            final Spinner yearSigned;
            final Spinner yearScouted;
            final EditText teamText;
            final Spinner yearLoaned;
            final Spinner typeOfLoanSpinner;
            Button editPlayerButton;

            firstName = view.findViewById(R.id.first_name_lop_edit);
            lastName = view.findViewById(R.id.last_name_lop_edit);
            positionSpinner = view.findViewById(R.id.position_spinner_lop_edit);
            number = view.findViewById(R.id.number_lop_edit);
            nationality = view.findViewById(R.id.nationality_lop_edit);
            overall = view.findViewById(R.id.overall_lop_edit);
            potentialLow = view.findViewById(R.id.potential_low_lop_edit);
            potentialHigh = view.findViewById(R.id.potential_high_lop_edit);
            yearSigned = view.findViewById(R.id.year_signed_spinner_lop_edit);
            yearScouted = view.findViewById(R.id.year_scouted_spinner_lop_edit);
            teamText = view.findViewById(R.id.team_lop_edit);
            yearLoaned = view.findViewById(R.id.year_loaned_spinner_lop_edit);
            typeOfLoanSpinner = view.findViewById(R.id.type_of_loan_spinner_lop_edit);
            editPlayerButton = view.findViewById(R.id.edit_lop_button);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(adapter);

            ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            yearSigned.setAdapter(yearAdapter);
            yearScouted.setAdapter(yearAdapter);
            yearLoaned.setAdapter(yearAdapter);

            List<String> loanTypes = Arrays.stream(LoanEnum.values())
                    .map(LoanEnum::getDescription)
                    .collect(Collectors.toList());

            ArrayAdapter<String> loanAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, loanTypes);
            loanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeOfLoanSpinner.setAdapter(loanAdapter);

            firstName.setText(player.getFirstName());
            lastName.setText(player.getLastName());
            positionSpinner.setSelection(adapter.getPosition(player.getPosition()));
            number.setText(String.valueOf(player.getNumber()));
            nationality.setText(player.getNationality());
            overall.setText(String.valueOf(player.getOverall()));
            potentialLow.setText(String.valueOf(player.getPotentialLow()));
            potentialHigh.setText(String.valueOf(player.getPotentialHigh()));
            yearSigned.setSelection(yearAdapter.getPosition(player.getYearSigned()));
            yearScouted.setSelection(yearAdapter.getPosition(player.getYearScouted()));
            teamText.setText(player.getTeam());
            yearLoaned.setSelection(yearAdapter.getPosition(player.getYearLoanedOut()));
            typeOfLoanSpinner.setSelection(loanAdapter.getPosition(player.getTypeOfLoan()));

            builder.setView(view);
            final AlertDialog dialog = builder.create();
            dialog.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lastName.getText().toString().isEmpty() &&
                            !nationality.getText().toString().isEmpty() &&
                            !positionSpinner.getSelectedItem().toString().isEmpty() &&
                            !overall.getText().toString().isEmpty() &&
                            !yearSigned.getSelectedItem().toString().equals("0") &&
                            !yearLoaned.getSelectedItem().toString().equals("0")) {
                        loPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                LoanedOutPlayer loPlayer = ds.toObject(LoanedOutPlayer.class);
                                                if (loPlayer.getId() == player.getId()) {
                                                    documentReference = loPlayersColRef.document(ds.getId());
                                                }
                                            }
                                            String no = number.getText().toString().trim();
                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            String yScouted = yearScouted.getSelectedItem().toString().trim();
                                            assert documentReference != null;
                                            documentReference.update("firstName", firstName.getText().toString().trim(),
                                                    "lastName", lastName.getText().toString().trim(),
                                                    "fullName", firstName.getText().toString().trim() + " " + lastName.getText().toString().trim(),
                                                    "position", positionSpinner.getSelectedItem().toString().trim(),
                                                    "number", (!no.isEmpty()) ? Integer.parseInt(no) : 99,
                                                    "nationality", nationality.getText().toString().trim(),
                                                    "overall", Integer.parseInt(overall.getText().toString().trim()),
                                                    "potentialLow", (!ptlLow.isEmpty()) ? Integer.parseInt(ptlLow) : 0,
                                                    "potentialHigh", (!ptlHi.isEmpty()) ? Integer.parseInt(ptlHi) : 0,
                                                    "yearSigned", yearSigned.getSelectedItem().toString().trim(),
                                                    "yearScouted", yScouted,
                                                    "team", teamText.getText().toString().trim(),
                                                    "yearLoanedOut", yearLoaned.getSelectedItem().toString().trim(),
                                                    "typeOfLoan", typeOfLoanSpinner.getSelectedItem().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            dialog.dismiss();
                                                            Intent intent = new Intent(context, LoanedOutPlayersActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            context.startActivity(intent);
                                                            ((Activity) context).finish();
                                                            Toast.makeText(context, "Player edited!", Toast.LENGTH_LONG)
                                                                    .show();

                                                        }
                                                    });


                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Year Signed and Year Loaned are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }
    }
}
