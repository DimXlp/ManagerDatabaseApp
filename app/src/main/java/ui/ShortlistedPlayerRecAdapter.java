package ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
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

import com.dimxlp.managerdb.R;
import com.dimxlp.managerdb.ShortlistActivity;
import com.dimxlp.managerdb.ShortlistPlayersActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import model.Manager;
import model.ShortlistedPlayer;
import model.Transfer;
import util.UserApi;

public class ShortlistedPlayerRecAdapter extends RecyclerView.Adapter<ShortlistedPlayerRecAdapter.ViewHolder> {

    private Context context;
    private List<ShortlistedPlayer> playerList;
    private long managerId;
    private String team;
    private String position;
    private int buttonInt;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference shPlayersColRef = db.collection("ShortlistedPlayers");
    private CollectionReference ftPlayersColRef = db.collection("FirstTeamPlayers");
    private CollectionReference managersColRef = db.collection("Managers");
    private CollectionReference transfersColRef = db.collection("Transfers");
    private CollectionReference frmPlayersColRef = db.collection("FormerPlayers");

    private Animation slideLeft;
    private Animation slideRight;

    public ShortlistedPlayerRecAdapter(Context context, List<ShortlistedPlayer> playerList, long managerId, String team, String position, int buttonInt) {
        this.context = context;
        this.playerList = playerList;
        this.managerId = managerId;
        this.team = team;
        this.position = position;
        this.buttonInt = buttonInt;
    }

    @NonNull
    @Override
    public ShortlistedPlayerRecAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.shortlisted_player_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull final ShortlistedPlayerRecAdapter.ViewHolder holder, int position) {

        final ShortlistedPlayer player = playerList.get(position);

        holder.fullNameText.setText(player.getFullName());
        holder.positionText.setText(player.getPosition());
        holder.overallNoText.setText(String.valueOf(player.getOverall()));
        if (player.getPotentialLow() != 0 && player.getPotentialHigh() != 0) {
            holder.potentialNoText.setText(String.format("%d-%d", player.getPotentialLow(), player.getPotentialHigh()));
        } else {
            holder.potentialNoText.setText("??-??");
        }
        holder.teamNameText.setText(player.getTeam());
        holder.countryText.setText(player.getNationality());

        if (player.getComments().isEmpty()) {
            holder.commentsText.setVisibility(View.GONE);
        } else {
            holder.commentsText.setText(player.getComments());
        }

        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                            Log.d("RAFI", "onSuccess: Currency: " + currency);
                            if (player.getValue() != 0) {
                                holder.valueNoText.setText(String.format("%s %s", currency, player.getValue()));
                            } else {
                                holder.valueNoText.setText(String.format("%s ???", currency));
                            }
                            if (player.getWage() != 0) {
                                holder.wageNoText.setText(String.format("%s %s", currency, player.getWage()));
                            } else {
                                holder.wageNoText.setText(String.format("%s ???", currency));
                            }

                        }
                    }
                });

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

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView fullNameText;
        private ImageView positionOval;
        private TextView positionText;
        private TextView overallNoText;
        private TextView potentialNoText;
        private TextView teamNameText;
        private TextView countryText;
        private TextView valueNoText;
        private TextView wageNoText;
        private TextView commentsText;
        private RelativeLayout details;
        private Button buyButton;
        private Button loanButton;
        private Button deleteButton;
        private Button editButton;
        private AlertDialog.Builder builder;
        private AlertDialog dialog;
        private Spinner typeOfTransferSpinner;
        private TextInputLayout transferFeeTil;
        private EditText transferFee;
        private Spinner playerSpinner;
        private TextInputLayout wageTil;
        private EditText wage;
        private EditText noOfContractYears;
        private Spinner yearSigned;
        private EditText comments;
        private Button transferButton;
        private long ftPlayerId;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            fullNameText = itemView.findViewById(R.id.full_name_shp);
            positionOval = itemView.findViewById(R.id.position_oval_shp);
            positionText = itemView.findViewById(R.id.position_shp);
            overallNoText = itemView.findViewById(R.id.overall_shp);
            potentialNoText = itemView.findViewById(R.id.potential_shp);
            teamNameText = itemView.findViewById(R.id.team_shp);
            countryText = itemView.findViewById(R.id.nationality_shp);
            valueNoText = itemView.findViewById(R.id.value_shp);
            wageNoText = itemView.findViewById(R.id.wage_shp);
            commentsText = itemView.findViewById(R.id.comments_shp);
            details = itemView.findViewById(R.id.details_shp);
            buyButton = itemView.findViewById(R.id.buy_button_shp);
            loanButton = itemView.findViewById(R.id.loan_button_shp);
            deleteButton = itemView.findViewById(R.id.delete_button_shp);
            editButton = itemView.findViewById(R.id.edit_button_shp);

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

            buyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    AlertDialog.Builder aBuilder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.transfer_popup, null);

                                    typeOfTransferSpinner = view.findViewById(R.id.type_of_transfer_spinner);
                                    transferFeeTil = view.findViewById(R.id.transfer_fee_til_buy);
                                    transferFee = view.findViewById(R.id.transfer_fee_buy);
                                    playerSpinner = view.findViewById(R.id.plus_player_spinner_buy);
                                    wageTil = view.findViewById(R.id.wage_til_buy);
                                    wage = view.findViewById(R.id.wage_buy);
                                    noOfContractYears = view.findViewById(R.id.contract_years_buy);
                                    yearSigned = view.findViewById(R.id.year_signed_spinner_buy);
                                    comments = view.findViewById(R.id.comments_buy);
                                    transferButton = view.findViewById(R.id.transfer_button);

                                    String[] transferArray = {"BOUGHT FROM ANOTHER TEAM",
                                                                "FREE TRANSFER"};

                                    ArrayAdapter<String> transferAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, transferArray);
                                    transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    typeOfTransferSpinner.setAdapter(transferAdapter);

                                    ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
                                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    yearSigned.setAdapter(yearAdapter);

                                    ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                            .whereEqualTo("managerId", managerId)
                                            .get()
                                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                @Override
                                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                    if (!queryDocumentSnapshots.isEmpty()) {
                                                        List<FirstTeamPlayer> ftPlayerList = new ArrayList<>();
                                                        ftPlayerList.add(new FirstTeamPlayer());
                                                        for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                                            FirstTeamPlayer ftPlayer = doc.toObject(FirstTeamPlayer.class);
                                                            ftPlayerList.add(ftPlayer);
                                                        }
                                                        ArrayAdapter<FirstTeamPlayer> playerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, ftPlayerList);
                                                        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                        playerSpinner.setAdapter(playerAdapter);
                                                        playerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                            @Override
                                                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                                FirstTeamPlayer player = (FirstTeamPlayer) parent.getSelectedItem();
                                                                Log.d("RAFI", "name = " + player.getFullName());
                                                                Log.d("RAFI", "id = " + player.getId());
                                                                ftPlayerId = player.getId();

                                                            }

                                                            @Override
                                                            public void onNothingSelected(AdapterView<?> parent) {

                                                            }
                                                        });
                                                    }
                                                }
                                            });

                                    managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                                                        wageTil.setHint("Wage (in " + currency + ")");
                                                    }
                                                }
                                            });

                                    transferButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!yearSigned.getSelectedItem().toString().equals("0") &&
                                                !typeOfTransferSpinner.getSelectedItem().toString().isEmpty()) {
                                                Log.d("ID", "Id = " + ftPlayerId);
                                                transferPlayer(playerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Transfer Type & Year Signed are required!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                                    aBuilder.setView(view);
                                    AlertDialog aDialog = aBuilder.create();
                                    aDialog.show();

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to buy this player?").setPositiveButton("Yes", dialogClickListener)
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
                                    AlertDialog.Builder aBuilder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.transfer_popup, null);

                                    typeOfTransferSpinner = view.findViewById(R.id.type_of_transfer_spinner);
                                    transferFeeTil = view.findViewById(R.id.transfer_fee_til_buy);
                                    transferFee = view.findViewById(R.id.transfer_fee_buy);
                                    playerSpinner = view.findViewById(R.id.plus_player_spinner_buy);
                                    wageTil = view.findViewById(R.id.wage_til_buy);
                                    wage = view.findViewById(R.id.wage_buy);
                                    noOfContractYears = view.findViewById(R.id.contract_years_buy);
                                    yearSigned = view.findViewById(R.id.year_signed_spinner_buy);
                                    comments = view.findViewById(R.id.comments_buy);
                                    transferButton = view.findViewById(R.id.transfer_button);
                                    transferButton.setText("Loan Player");

                                    transferFeeTil.setVisibility(View.GONE);
                                    transferFee.setVisibility(View.GONE);
                                    playerSpinner.setVisibility(View.GONE);
                                    noOfContractYears.setVisibility(View.GONE);

                                    String[] transferArray = {"SHORT TERM LOAN",
                                                                "ONE-YEAR LOAN",
                                                                "TWO-YEAR LOAN"};

                                    ArrayAdapter<String> transferAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, transferArray);
                                    transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    typeOfTransferSpinner.setAdapter(transferAdapter);

                                    ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
                                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    yearSigned.setAdapter(yearAdapter);

                                    managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                                                        wageTil.setHint("Wage (in " + currency + ")");
                                                    }
                                                }
                                            });

                                    transferButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!yearSigned.getSelectedItem().toString().equals("0") &&
                                                    !typeOfTransferSpinner.getSelectedItem().toString().isEmpty()) {
                                                transferPlayer(playerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Transfer Type & Year Signed are required!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                                    aBuilder.setView(view);
                                    AlertDialog aDialog = aBuilder.create();
                                    aDialog.show();

                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to loan this player?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            });
            
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editPlayer(playerList.get(getAdapterPosition()));
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

        private void deletePlayer(final ShortlistedPlayer player) {
            shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    ShortlistedPlayer shPlayer = ds.toObject(ShortlistedPlayer.class);
                                    if (shPlayer.getId() == player.getId()) {
                                        documentReference = shPlayersColRef.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(context, "Player deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, ShortlistPlayersActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barPosition", position);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, ShortlistActivity.class);
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

        private void transferPlayer(final ShortlistedPlayer player) {


            shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    ShortlistedPlayer shPlayer = ds.toObject(ShortlistedPlayer.class);
                                    if (shPlayer.getId() == player.getId()) {
                                        documentReference = shPlayersColRef.document(ds.getId());
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
                                                newTransfer.setCurrentTeam(team);
                                                newTransfer.setOverall(player.getOverall());
                                                newTransfer.setPotentialLow(player.getPotentialLow());
                                                newTransfer.setPotentialHigh(player.getPotentialHigh());
                                                newTransfer.setType(typeOfTransferSpinner.getSelectedItem().toString().trim());
                                                newTransfer.setManagerId(managerId);
                                                newTransfer.setNationality(player.getNationality());
                                                String trFee = transferFee.getText().toString().trim();
                                                newTransfer.setTransferFee((!trFee.isEmpty()) ? Integer.parseInt(trFee) : 0);
                                                String plusPlayerName = playerSpinner.getSelectedItem().toString().trim();
                                                newTransfer.setPlusPlayerName((!plusPlayerName.isEmpty()) ? plusPlayerName : "");
                                                String wg = wage.getText().toString().trim();
                                                newTransfer.setWage((!wg.isEmpty()) ? Integer.parseInt(wg) : 0);
                                                String conYears = noOfContractYears.getText().toString().trim();
                                                newTransfer.setContractYears((!conYears.isEmpty()) ? Integer.parseInt(conYears) : 0);
                                                newTransfer.setYear(yearSigned.getSelectedItem().toString().trim());
                                                String coms = comments.getText().toString().trim();
                                                newTransfer.setComments((!coms.isEmpty()) ? coms : "");
                                                newTransfer.setFormerPlayer(false);
                                                newTransfer.setUserId(UserApi.getInstance().getUserId());
                                                newTransfer.setTimeAdded(new Timestamp(new Date()));

                                                if (ftPlayerId != 0) {

                                                    ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                            .whereEqualTo("managerId", managerId)
                                                            .whereEqualTo("id", ftPlayerId)
                                                            .get()
                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                    if (task.isSuccessful()) {

                                                                        List<FirstTeamPlayer> ftPlayerList = new ArrayList<>();
                                                                        DocumentReference ftPlayerDocRef = null;
                                                                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                                                            FirstTeamPlayer ftPlayer = doc.toObject(FirstTeamPlayer.class);
                                                                            ftPlayerList.add(ftPlayer);
                                                                            ftPlayerDocRef = ftPlayersColRef.document(doc.getId());
                                                                        }
                                                                        // Only one player in this list
                                                                        FirstTeamPlayer thePlayer = ftPlayerList.get(0);
                                                                        Log.d("RAFI", "nameeee = " + thePlayer.getFullName());


                                                                        FormerPlayer fmPlayer = new FormerPlayer();
                                                                        fmPlayer.setId(0);
                                                                        fmPlayer.setFirstName(thePlayer.getFirstName());
                                                                        fmPlayer.setLastName(thePlayer.getLastName());
                                                                        fmPlayer.setFullName(thePlayer.getFullName());
                                                                        fmPlayer.setPosition(thePlayer.getPosition());
                                                                        fmPlayer.setNumber(thePlayer.getNumber());
                                                                        fmPlayer.setNationality(thePlayer.getNationality());
                                                                        fmPlayer.setOverall(thePlayer.getOverall());
                                                                        fmPlayer.setPotentialLow(thePlayer.getPotentialLow());
                                                                        fmPlayer.setPotentialHigh(thePlayer.getPotentialHigh());
                                                                        fmPlayer.setYearSigned(thePlayer.getYearSigned());
                                                                        fmPlayer.setYearScouted(thePlayer.getYearScouted());
                                                                        // Player left when new player was signed
                                                                        fmPlayer.setYearLeft(yearSigned.getSelectedItem().toString().trim());
                                                                        fmPlayer.setManagerId(managerId);
                                                                        fmPlayer.setUserId(UserApi.getInstance().getUserId());
                                                                        fmPlayer.setTimeAdded(new Timestamp(new Date()));
                                                                        frmPlayersColRef.add(fmPlayer);

                                                                        ftPlayerDocRef.delete();
                                                                    }
                                                                }
                                                            });
                                                }

                                                FirstTeamPlayer ftPlayer = new FirstTeamPlayer();
                                                ftPlayer.setId(0);
                                                ftPlayer.setFirstName(player.getFirstName());
                                                ftPlayer.setLastName(player.getLastName());
                                                ftPlayer.setFullName(player.getFullName());
                                                ftPlayer.setPosition(player.getPosition());
                                                ftPlayer.setNumber(99);
                                                ftPlayer.setTeam(team);
                                                ftPlayer.setNationality(player.getNationality());
                                                ftPlayer.setOverall(player.getOverall());
                                                ftPlayer.setYearSigned(yearSigned.getSelectedItem().toString().trim());
                                                ftPlayer.setYearScouted("0");
                                                ftPlayer.setManagerId(managerId);
                                                ftPlayer.setUserId(UserApi.getInstance().getUserId());
                                                ftPlayer.setTimeAdded(new Timestamp(new Date()));
                                                ftPlayer.setLoanPlayer(!typeOfTransferSpinner.getSelectedItem().toString().equals("BOUGHT FROM ANOTHER TEAM") &&
                                                        !typeOfTransferSpinner.getSelectedItem().toString().equals("FREE TRANSFER"));

                                                transfersColRef.add(newTransfer);

                                                ftPlayersColRef.add(ftPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Toast.makeText(context, "Player bought!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        });
                                                shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, ShortlistPlayersActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barPosition", position);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, ShortlistActivity.class);
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

        private void editPlayer(final ShortlistedPlayer player) {
            builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.create_shortlisted_player_popup, null);

            TextView title;
            final EditText firstName;
            final EditText lastName;
            final Spinner positionSpinner;
            final EditText nationality;
            final EditText overall;
            final EditText potentialLow;
            final EditText potentialHigh;
            final EditText teamText;
            final TextInputLayout valueTil;
            final EditText value;
            final TextInputLayout wageTil;
            final EditText wage;
            final EditText comments;
            Button editPlayerButton;

            title = view.findViewById(R.id.create_sh_player);
            title.setText("Edit Player");
            firstName = view.findViewById(R.id.first_name_shp_create);
            lastName = view.findViewById(R.id.last_name_shp_create);
            positionSpinner = view.findViewById(R.id.position_spinner_shp_create);
            teamText = view.findViewById(R.id.team_shp_create);
            nationality = view.findViewById(R.id.nationality_shp_create);
            overall = view.findViewById(R.id.overall_shp_create);
            potentialLow = view.findViewById(R.id.potential_low_shp_create);
            potentialHigh = view.findViewById(R.id.potential_high_shp_create);
            valueTil = view.findViewById(R.id.value_til_shp_create);
            value = view.findViewById(R.id.value_shp_create);
            wageTil = view.findViewById(R.id.wage_til_shp_create);
            wage = view.findViewById(R.id.wage_shp_create);
            comments = view.findViewById(R.id.comments_shp_create);
            editPlayerButton = view.findViewById(R.id.create_sh_player_button);
            editPlayerButton.setText("Edit Player");

            managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
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
                                valueTil.setHint("Value (in " + currency + ")");
                                wageTil.setHint("Wage (in " + currency + ")");
                            }
                        }
                    });

            ArrayAdapter<CharSequence> positionAdapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
            positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(positionAdapter);

            firstName.setText(player.getFirstName());
            lastName.setText(player.getLastName());
            positionSpinner.setSelection(positionAdapter.getPosition(player.getPosition()));
            teamText.setText(player.getTeam());
            nationality.setText(player.getNationality());
            overall.setText(String.valueOf(player.getOverall()));
            potentialLow.setText(String.valueOf(player.getPotentialLow()));
            potentialHigh.setText(String.valueOf(player.getPotentialHigh()));
            value.setText(String.valueOf(player.getValue()));
            wage.setText(String.valueOf(player.getWage()));
            comments.setText(player.getComments());

            builder.setView(view);
            dialog = builder.create();
            dialog.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionSpinner.getSelectedItem().toString().isEmpty() &&
                        !teamText.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty()) {
                        shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                ShortlistedPlayer shPlayer = ds.toObject(ShortlistedPlayer.class);
                                                if (shPlayer.getId() == player.getId()) {
                                                    documentReference = shPlayersColRef.document(ds.getId());
                                                }
                                            }
                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            String v = value.getText().toString().trim();
                                            String w = wage.getText().toString().trim();
                                            assert documentReference != null;
                                            documentReference.update("firstName", firstName.getText().toString().trim(),
                                                    "lastName", lastName.getText().toString().trim(),
                                                    "fullName", firstName.getText().toString().trim() + " " + lastName.getText().toString().trim(),
                                                    "position", positionSpinner.getSelectedItem().toString().trim(),
                                                    "nationality", nationality.getText().toString().trim(),
                                                    "overall", Integer.parseInt(overall.getText().toString().trim()),
                                                    "potentialLow", (!ptlLow.isEmpty()) ? Integer.parseInt(ptlLow) : 0,
                                                    "potentialHigh", (!ptlHi.isEmpty()) ? Integer.parseInt(ptlHi) : 0,
                                                    "team", teamText.getText().toString().trim(),
                                                    "value", (!v.isEmpty()) ? Integer.parseInt(v) : 0,
                                                    "wage", (!w.isEmpty()) ? Integer.parseInt(w) : 0,
                                                    "comments", comments.getText().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            dialog.dismiss();
                                                            Intent intent = new Intent(context, ShortlistPlayersActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barPosition", position);
                                                            context.startActivity(intent);
                                                            ((Activity) context).finish();
                                                            Toast.makeText(context, "Player updated!", Toast.LENGTH_LONG)
                                                                    .show();
                                                        }
                                                    });

                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Team and Overall are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }
    }
}
