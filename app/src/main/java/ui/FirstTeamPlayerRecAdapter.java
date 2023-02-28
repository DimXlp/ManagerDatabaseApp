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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

import com.dimxlp.managerdb.FirstTeamActivity;
import com.dimxlp.managerdb.FirstTeamListActivity;
import com.dimxlp.managerdb.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.LoanedOutPlayer;
import model.Manager;
import model.Transfer;
import util.UserApi;

public class FirstTeamPlayerRecAdapter extends RecyclerView.Adapter<FirstTeamPlayerRecAdapter.ViewHolder> {

    private Context context;
    private List<FirstTeamPlayer> playerList;
    private long managerId;
    private String team;
    private String barYear;
    private int buttonInt;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference ftPlayersReference = db.collection("FirstTeamPlayers");
    private CollectionReference fmPlayersReference = db.collection("FormerPlayers");
    private CollectionReference loPlayersReference = db.collection("LoanedOutPlayers");
    private CollectionReference managersReference = db.collection("Managers");
    private CollectionReference transfersReference = db.collection("Transfers");

    private Animation slideLeft;
    private Animation slideRight;

    public FirstTeamPlayerRecAdapter(Context context, List<FirstTeamPlayer> playerList, long managerId, String team, String barYear, int buttonInt) {
        this.context = context;
        this.playerList = playerList;
        this.managerId = managerId;
        this.team = team;
        this.barYear = barYear;
        this.buttonInt = buttonInt;
    }

    @NonNull
    @Override
    public FirstTeamPlayerRecAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.ft_player_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull FirstTeamPlayerRecAdapter.ViewHolder holder, int position) {

        FirstTeamPlayer player = playerList.get(position);

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
        if (!player.isLoanPlayer()) {
            holder.line5.setVisibility(View.GONE);
            holder.loanText.setVisibility(View.GONE);
            holder.terminateLoanButton.setVisibility(View.GONE);
        }

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

        if (buttonInt == 1) {
            slideRight = AnimationUtils.loadAnimation(context, R.anim.slide_right);
            holder.itemView.startAnimation(slideRight);
        } else if (buttonInt == 2) {
            slideLeft = AnimationUtils.loadAnimation(context, R.anim.slide_left);
            holder.itemView.startAnimation(slideLeft);
        }

    }

    @Override
    public int getItemCount() {
        return playerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private AlertDialog.Builder aBuilder;
        private AlertDialog aDialog;

        private TextView numberText;
        private TextView fullNameText;
        private TextView positionText;
        private TextView overallNoText;
        private TextView potentialNoText;
        private TextView countryText;
        private TextView yearSignedDateText;
        private TextView yearScoutedDateText;
        private TextView loanText;
        private ImageView line5;
        private ImageView positionOval;
        private RelativeLayout details;
        private Button departButton;
        private Button deleteButton;
        private Button editButton;
        private Button loanButton;
        private Button terminateLoanButton;

        private EditText teamLeft;
        private Spinner yearLeft;
        private Spinner typeOfTransfer;
        private EditText transferFee;
        private EditText comments;
        private Button transferButton;
        private EditText teamLoan;
        private Spinner yearLoaned;
        private Spinner loanSpinner;
        private Button loanPlayerButton;
        private TextInputLayout transferFeeTil;

        public ViewHolder(@NonNull View itemView, final Context ctx) {
            super(itemView);
            context = ctx;

            numberText = itemView.findViewById(R.id.number_ftp);
            fullNameText = itemView.findViewById(R.id.full_name_ftp);
            positionText = itemView.findViewById(R.id.position_ftp);
            overallNoText = itemView.findViewById(R.id.overall_ftp);
            potentialNoText = itemView.findViewById(R.id.potential_ftp);
            countryText = itemView.findViewById(R.id.nationality_ftp);
            yearSignedDateText = itemView.findViewById(R.id.year_signed_ftp);
            yearScoutedDateText = itemView.findViewById(R.id.year_scouted_ftp);
            loanText = itemView.findViewById(R.id.loan_ftp);
            line5 = itemView.findViewById(R.id.line_ftp_5);
            positionOval = itemView.findViewById(R.id.position_oval_ftp);
            details = itemView.findViewById(R.id.details_ftp);
            departButton = itemView.findViewById(R.id.depart_button_ftp);
            deleteButton = itemView.findViewById(R.id.delete_button_ftp);
            editButton = itemView.findViewById(R.id.edit_button_ftp);
            loanButton = itemView.findViewById(R.id.loan_button_ftp);
            terminateLoanButton = itemView.findViewById(R.id.terminate_loan_button_ftp);

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
                    int position = getAdapterPosition();
                    editPlayer(playerList.get(position));
                }
            });

            departButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    aBuilder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.depart_popup, null);

                                    teamLeft = view.findViewById(R.id.team_left);
                                    yearLeft = view.findViewById(R.id.year_left_spinner_depart);
                                    typeOfTransfer = view.findViewById(R.id.type_of_transfer_spinner_depart);
                                    transferFeeTil = view.findViewById(R.id.transfer_fee_til_depart);
                                    transferFee = view.findViewById(R.id.transfer_fee_depart);
                                    comments = view.findViewById(R.id.comments_depart);
                                    transferButton = view.findViewById(R.id.transfer_player_button);

                                    ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
                                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    yearLeft.setAdapter(yearAdapter);

                                    managersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                            .whereEqualTo("id", managerId)
                                            .get()
                                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                @Override
                                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                    if (!queryDocumentSnapshots.isEmpty()) {
                                                        List<Manager> managerList = new ArrayList<>();
                                                        for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                                            Manager manager = doc.toObject(Manager.class);
                                                            managerList.add(manager);
                                                        }
                                                        Manager manager = managerList.get(0);
                                                        String currency = manager.getCurrency();
                                                        transferFeeTil.setHint("Transfer Fee (in " + currency + ")");
                                                    }
                                                }
                                            });

                                    String[] transferArray = {"TRANSFER TO ANOTHER TEAM", "FREE TRANSFER"};

                                    ArrayAdapter<String> transferAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, transferArray);
                                    transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    typeOfTransfer.setAdapter(transferAdapter);

                                    transferButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!yearLeft.getSelectedItem().toString().equals("0") &&
                                                !teamLeft.getText().toString().isEmpty() &&
                                                !typeOfTransfer.getSelectedItem().toString().isEmpty()) {
                                                letPlayerLeave(playerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Fields required", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                                    aBuilder.setView(view);
                                    aDialog = aBuilder.create();
                                    aDialog.show();

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to let this player leave the First Team?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            });

            loanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    aBuilder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.loan_popup, null);

                                    teamLoan = view.findViewById(R.id.team_loan);
                                    yearLoaned = view.findViewById(R.id.year_loaned_spinner_loan);
                                    loanSpinner = view.findViewById(R.id.type_of_loan_spinner);
                                    comments = view.findViewById(R.id.comments_loan);
                                    loanPlayerButton = view.findViewById(R.id.loan_player_button);

                                    ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
                                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    yearLoaned.setAdapter(yearAdapter);

                                    String[] loanArray = {"SHORT TERM LOAN", "ONE-YEAR LOAN", "TWO-YEAR LOAN"};

                                    ArrayAdapter<String> loanAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, loanArray);
                                    loanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    loanSpinner.setAdapter(loanAdapter);

                                    loanPlayerButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!teamLoan.getText().toString().isEmpty() &&
                                                !yearLoaned.getSelectedItem().toString().equals("0") &&
                                                !loanSpinner.getSelectedItem().toString().isEmpty()) {
                                                loanPlayer(playerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Team, Year Loaned and Type of Loan are required", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                                    aBuilder.setView(view);
                                    aDialog = aBuilder.create();
                                    aDialog.show();

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to loan this player out to another team?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            });

            terminateLoanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    aBuilder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.year_left_popup, null);

                                    yearLeft = view.findViewById(R.id.year_left_spinner_left);

                                    ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
                                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    yearLeft.setAdapter(yearAdapter);

                                    terminateLoanButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!yearLeft.getSelectedItem().toString().equals("0")) {
                                                terminateLoan(playerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Field required!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                                    aBuilder.setView(view);
                                    aDialog = aBuilder.create();
                                    aDialog.show();

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to terminate this player's loan deal and return to his team?").setPositiveButton("Yes", dialogClickListener)
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
                                    deletePlayer(playerList.get(getAdapterPosition()));
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

        private void terminateLoan(final FirstTeamPlayer player) {
            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                FormerPlayer fmPlayer = new FormerPlayer();
                                                fmPlayer.setId(0);
                                                fmPlayer.setFirstName(player.getFirstName());
                                                fmPlayer.setLastName(player.getLastName());
                                                fmPlayer.setFullName(player.getFullName());
                                                fmPlayer.setPosition(player.getPosition());
                                                fmPlayer.setNumber(player.getNumber());
                                                fmPlayer.setNationality(player.getNationality());
                                                fmPlayer.setOverall(player.getOverall());
                                                fmPlayer.setPotentialLow(player.getPotentialLow());
                                                fmPlayer.setPotentialHigh(player.getPotentialHigh());
                                                fmPlayer.setYearSigned(player.getYearSigned());
                                                fmPlayer.setYearScouted(player.getYearScouted());
                                                fmPlayer.setYearLeft(yearLeft.getSelectedItem().toString().trim());
                                                fmPlayer.setManagerId(managerId);
                                                fmPlayer.setUserId(UserApi.getInstance().getUserId());
                                                fmPlayer.setTimeAdded(new Timestamp(new Date()));

                                                fmPlayersReference.add(fmPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Toast.makeText(context, "Player returned to his team!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        });
                                                ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, FirstTeamActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    }
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                    });
        }

        private void loanPlayer(final FirstTeamPlayer player) {
            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                final Transfer newTransfer = new Transfer();
                                                newTransfer.setId(0);
                                                newTransfer.setFirstName(player.getFirstName());
                                                newTransfer.setLastName(player.getLastName());
                                                newTransfer.setFullName(player.getFullName());
                                                newTransfer.setPosition(player.getPosition());
                                                newTransfer.setNationality(player.getNationality());
                                                newTransfer.setFormerTeam(player.getTeam());
                                                newTransfer.setCurrentTeam(teamLoan.getText().toString().trim());
                                                newTransfer.setOverall(player.getOverall());
                                                newTransfer.setPotentialLow(player.getPotentialLow());
                                                newTransfer.setPotentialHigh(player.getPotentialHigh());
                                                newTransfer.setType(loanSpinner.getSelectedItem().toString());
                                                newTransfer.setYear(yearLoaned.getSelectedItem().toString().trim());
                                                String coms = comments.getText().toString().trim();
                                                newTransfer.setComments((!coms.isEmpty()) ? coms : "");
                                                newTransfer.setFormerPlayer(true);
                                                newTransfer.setManagerId(managerId);
                                                newTransfer.setUserId(UserApi.getInstance().getUserId());
                                                newTransfer.setTimeAdded(new Timestamp(new Date()));

                                                LoanedOutPlayer loPlayer = new LoanedOutPlayer();
                                                loPlayer.setId(0);
                                                loPlayer.setFirstName(player.getFirstName());
                                                loPlayer.setLastName(player.getLastName());
                                                loPlayer.setFullName(player.getFullName());
                                                loPlayer.setPosition(player.getPosition());
                                                loPlayer.setNumber(player.getNumber());
                                                loPlayer.setNationality(player.getNationality());
                                                loPlayer.setOverall(player.getOverall());
                                                loPlayer.setPotentialLow(player.getPotentialLow());
                                                loPlayer.setPotentialHigh(player.getPotentialHigh());
                                                loPlayer.setYearSigned(player.getYearSigned());
                                                loPlayer.setYearScouted(player.getYearScouted());
                                                loPlayer.setTeam(teamLoan.getText().toString().trim());
                                                loPlayer.setYearLoanedOut(yearLoaned.getSelectedItem().toString().trim());
                                                loPlayer.setTypeOfLoan(loanSpinner.getSelectedItem().toString().trim());
                                                loPlayer.setManagerId(managerId);
                                                loPlayer.setUserId(UserApi.getInstance().getUserId());
                                                loPlayer.setTimeAdded(new Timestamp(new Date()));

                                                transfersReference.add(newTransfer);

                                                loPlayersReference.add(loPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Toast.makeText(context, "Player loaned out!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        });
                                                ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, FirstTeamActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    }
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                    });
        }

        private void deletePlayer(final FirstTeamPlayer player) {
            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(context, "Player deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, FirstTeamActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    }
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                    });
        }

        private void letPlayerLeave(final FirstTeamPlayer player) {
            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                final Transfer newTransfer = new Transfer();
                                                newTransfer.setId(0);
                                                newTransfer.setFirstName(player.getFirstName());
                                                newTransfer.setLastName(player.getLastName());
                                                newTransfer.setFullName(player.getFullName());
                                                newTransfer.setPosition(player.getPosition());
                                                newTransfer.setFormerTeam(player.getTeam());
                                                newTransfer.setCurrentTeam(teamLeft.getText().toString().trim());
                                                newTransfer.setNationality(player.getNationality());
                                                newTransfer.setOverall(player.getOverall());
                                                newTransfer.setPotentialLow(player.getPotentialLow());
                                                newTransfer.setPotentialHigh(player.getPotentialHigh());
                                                newTransfer.setType(typeOfTransfer.getSelectedItem().toString());
                                                String trFee = transferFee.getText().toString().trim();
                                                newTransfer.setTransferFee((!trFee.isEmpty()) ? Integer.parseInt(trFee) : 0);
                                                newTransfer.setYear(yearLeft.getSelectedItem().toString().trim());
                                                String coms = comments.getText().toString().trim();
                                                newTransfer.setComments((!coms.isEmpty()) ? coms : "");
                                                newTransfer.setFormerPlayer(true);
                                                newTransfer.setManagerId(managerId);
                                                newTransfer.setUserId(UserApi.getInstance().getUserId());
                                                newTransfer.setTimeAdded(new Timestamp(new Date()));

                                                FormerPlayer fmPlayer = new FormerPlayer();
                                                fmPlayer.setId(0);
                                                fmPlayer.setFirstName(player.getFirstName());
                                                fmPlayer.setLastName(player.getLastName());
                                                fmPlayer.setFullName(player.getFullName());
                                                fmPlayer.setPosition(player.getPosition());
                                                fmPlayer.setNumber(player.getNumber());
                                                fmPlayer.setNationality(player.getNationality());
                                                fmPlayer.setOverall(player.getOverall());
                                                fmPlayer.setPotentialLow(player.getPotentialLow());
                                                fmPlayer.setPotentialHigh(player.getPotentialHigh());
                                                fmPlayer.setYearSigned(player.getYearSigned());
                                                fmPlayer.setYearScouted(player.getYearScouted());
                                                fmPlayer.setYearLeft(yearLeft.getSelectedItem().toString().trim());
                                                fmPlayer.setManagerId(managerId);
                                                fmPlayer.setUserId(UserApi.getInstance().getUserId());
                                                fmPlayer.setTimeAdded(new Timestamp(new Date()));

                                                transfersReference.add(newTransfer);

                                                fmPlayersReference.add(fmPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Toast.makeText(context, "Player transferred!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        });
                                                ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, FirstTeamActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    }
                                                                }
                                                            }
                                                        });
                                            }
                                        });
                            }
                        }
                    });
        }

        private void editPlayer(final FirstTeamPlayer player) {
            builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.create_first_team_player_popup, null);

            TextView title;
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
            final SwitchMaterial loanSwitch;
            Button savePlayerButton;

            title = view.findViewById(R.id.create_ft_player);
            title.setText(R.string.edit_player_title);
            firstName = view.findViewById(R.id.first_name_ftp_create);
            lastName = view.findViewById(R.id.last_name_ftp_create);
            positionSpinner = view.findViewById(R.id.position_spinner_ftp_create);
            number = view.findViewById(R.id.number_ftp_create);
            nationality = view.findViewById(R.id.nationality_ftp_create);
            overall = view.findViewById(R.id.overall_ftp_create);
            potentialLow = view.findViewById(R.id.potential_low_ftp_create);
            potentialHigh = view.findViewById(R.id.potential_high__ftp_create);
            yearSigned = view.findViewById(R.id.year_signed_spinner_ftp_create);
            yearScouted = view.findViewById(R.id.year_scouted_spinner_ftp_create);
            loanSwitch = view.findViewById(R.id.loan_player_switch_ftp_create);
            savePlayerButton = view.findViewById(R.id.create_ft_player_button);
            savePlayerButton.setText(R.string.save_player);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(adapter);

            ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            yearSigned.setAdapter(yearAdapter);
            yearScouted.setAdapter(yearAdapter);

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
            loanSwitch.setChecked(player.isLoanPlayer());

            builder.setView(view);
            dialog = builder.create();
            dialog.show();

            savePlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionSpinner.getSelectedItem().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty() &&
                        !yearSigned.getSelectedItem().toString().equals("0")) {
                        ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            List<DocumentSnapshot> doc =  task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds: doc) {
                                                FirstTeamPlayer ftplayer = ds.toObject(FirstTeamPlayer.class);
                                                if (ftplayer.getId() == player.getId()) {
                                                    documentReference = ftPlayersReference.document(ds.getId());
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
                                                    "yearScouted", (!yScouted.equals("0")) ? yScouted : "0",
                                                    "loanPlayer", loanSwitch.isChecked())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            dialog.dismiss();
                                                            Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barYear", barYear);
                                                            context.startActivity(intent);
                                                            ((Activity)context).finish();
                                                            Toast.makeText(context, "Player updated!", Toast.LENGTH_LONG)
                                                                    .show();
                                                        }
                                                    });

                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position and Overall are required", Toast.LENGTH_LONG)
                                .show();
                    }

                }
            });
        }
    }
}
