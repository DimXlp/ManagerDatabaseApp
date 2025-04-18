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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import enumeration.LoanEnum;
import enumeration.PurchaseTransferEnum;
import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.Manager;
import model.ShortlistedPlayer;
import model.Transfer;
import util.UserApi;
import util.ValueFormatter;

public class ShortlistedPlayerRecAdapter extends RecyclerView.Adapter<ShortlistedPlayerRecAdapter.ViewHolder> {

    private static final String LOG_TAG = "RAFI|ShortlistedPlayersRecAdapter";
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
                            if (player.getValue() != 0) {
                                holder.valueNoText.setText(String.format("%s %s", currency, NumberFormat.getInstance().format(player.getValue())));
                            } else {
                                holder.valueNoText.setText(String.format("%s ???", currency));
                            }
                            if (player.getWage() != 0) {
                                holder.wageNoText.setText(String.format("%s %s", currency, NumberFormat.getInstance().format(player.getWage())));
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
        private TextView playerSpinnerText;
        private Spinner playerSpinner;
        private TextInputLayout wageTil;
        private EditText wage;
        private EditText noOfContractYears;
        private Spinner yearSigned;
        private EditText comments;
        private Button transferButton;
        private long ftPlayerId;
        private boolean hasPlusPlayer = false;

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
                                    playerSpinnerText = view.findViewById(R.id.plus_player_text_buy);
                                    playerSpinner = view.findViewById(R.id.plus_player_spinner_buy);
                                    wageTil = view.findViewById(R.id.wage_til_buy);
                                    wage = view.findViewById(R.id.wage_buy);
                                    noOfContractYears = view.findViewById(R.id.contract_years_buy);
                                    yearSigned = view.findViewById(R.id.year_signed_spinner_buy);
                                    comments = view.findViewById(R.id.comments_buy);
                                    transferButton = view.findViewById(R.id.transfer_button);

                                    ValueFormatter.formatValue(transferFee);
                                    ValueFormatter.formatValue(wage);

                                    List<String> transferTypes = Arrays.stream(PurchaseTransferEnum.values())
                                            .map(PurchaseTransferEnum::getDescription)
                                            .collect(Collectors.toList());

                                    ArrayAdapter<String> transferAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, transferTypes);
                                    transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    typeOfTransferSpinner.setAdapter(transferAdapter);

                                    typeOfTransferSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                        @Override
                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                            String selectedTransferType = parent.getItemAtPosition(position).toString();

                                            if (PurchaseTransferEnum.FREE_TRANSFER.getDescription().equals(selectedTransferType)) {
                                                transferFeeTil.setVisibility(View.GONE);
                                                transferFee.setEnabled(false);
                                                playerSpinnerText.setVisibility(View.GONE);
                                                playerSpinner.setVisibility(View.GONE);
                                            } else {
                                                transferFeeTil.setVisibility(View.VISIBLE);
                                                transferFee.setEnabled(true);
                                                playerSpinnerText.setVisibility(View.VISIBLE);
                                                playerSpinner.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        @Override
                                        public void onNothingSelected(AdapterView<?> parent) {

                                        }
                                    });

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
                                                        var firstPlayer = new FirstTeamPlayer();
                                                        firstPlayer.setFullName("");
                                                        ftPlayerList.add(firstPlayer);
                                                        queryDocumentSnapshots.getDocuments().stream()
                                                                .map(doc -> doc.toObject(FirstTeamPlayer.class))
                                                                .forEach(ftPlayerList::add);

                                                        ArrayAdapter<FirstTeamPlayer> playerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, ftPlayerList);
                                                        playerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                        playerSpinner.setAdapter(playerAdapter);
                                                        playerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                            @Override
                                                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                                FirstTeamPlayer player = (FirstTeamPlayer) parent.getSelectedItem();
                                                                ftPlayerId = player.getId();
                                                                hasPlusPlayer = ftPlayerId > 0;
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
                                                transferPlayer(playerList.get(getAdapterPosition()), false);
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
                                    playerSpinnerText = view.findViewById(R.id.plus_player_text_buy);
                                    playerSpinner = view.findViewById(R.id.plus_player_spinner_buy);
                                    wageTil = view.findViewById(R.id.wage_til_buy);
                                    wage = view.findViewById(R.id.wage_buy);
                                    noOfContractYears = view.findViewById(R.id.contract_years_buy);
                                    yearSigned = view.findViewById(R.id.year_signed_spinner_buy);
                                    comments = view.findViewById(R.id.comments_buy);
                                    transferButton = view.findViewById(R.id.transfer_button);
                                    transferButton.setText("Loan Player");

                                    ValueFormatter.formatValue(wage);

                                    transferFeeTil.setVisibility(View.GONE);
                                    transferFee.setVisibility(View.GONE);
                                    playerSpinnerText.setVisibility(View.GONE);
                                    playerSpinner.setVisibility(View.GONE);
                                    noOfContractYears.setVisibility(View.GONE);

                                    List<String> loanTypes = Arrays.stream(LoanEnum.values())
                                            .map(LoanEnum::getDescription)
                                            .collect(Collectors.toList());

                                    ArrayAdapter<String> transferAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, loanTypes);
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
                                                transferPlayer(playerList.get(getAdapterPosition()), true);
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
            Log.d(LOG_TAG, "deletePlayer called for player: " + player.getFullName());

            shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "ShortlistedPlayers collection fetched successfully.");

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
                                                Log.d(LOG_TAG, "Player successfully deleted: " + player.getFullName());
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
                                                                        Log.d(LOG_TAG, "Navigating to ShortlistPlayersActivity.");
                                                                        Intent intent = new Intent(context, ShortlistPlayersActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barPosition", position);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Log.d(LOG_TAG, "Navigating to ShortlistActivity.");
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
                            } else {
                                Log.e(LOG_TAG, "Error fetching ShortlistedPlayers collection.", task.getException());
                            }
                        }
                    });
        }

        private void transferPlayer(final ShortlistedPlayer player, boolean isLoan) {
            Log.d(LOG_TAG, "transferPlayer called for player: " + player.getFullName() + ", isLoan: " + isLoan);

            shPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "ShortlistedPlayers collection fetched successfully.");
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
                                                Log.d(LOG_TAG, "Player removed from shortlist: " + player.getFullName());

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
                                                String trFee = transferFee.getText().toString().trim().replaceAll(",", "");
                                                newTransfer.setTransferFee((!trFee.isEmpty()) ? Integer.parseInt(trFee) : 0);

                                                if (isLoan) {
                                                    newTransfer.setPlusPlayerName(null);
                                                    Log.d(LOG_TAG, "Transfer is a loan. No additional player involved.");
                                                } else {
                                                    newTransfer.setHasPlusPlayer(hasPlusPlayer);
                                                    String plusPlayerName = playerSpinner.getSelectedItem().toString().trim();
                                                    newTransfer.setPlusPlayerName((!plusPlayerName.isEmpty()) ? plusPlayerName : "");
                                                    newTransfer.setPlusPlayerId(ftPlayerId);
                                                    Log.d(LOG_TAG, "Transfer involves another player: " + plusPlayerName);
                                                }

                                                String wg = wage.getText().toString().trim().replaceAll(",", "");
                                                newTransfer.setWage((!wg.isEmpty()) ? Integer.parseInt(wg) : 0);
                                                String conYears = noOfContractYears.getText().toString().trim();
                                                newTransfer.setContractYears((!conYears.isEmpty()) ? Integer.parseInt(conYears) : 0);
                                                newTransfer.setYear(yearSigned.getSelectedItem().toString().trim());
                                                String coms = comments.getText().toString().trim();
                                                newTransfer.setComments((!coms.isEmpty()) ? coms : "");
                                                newTransfer.setFormerPlayer(false);
                                                newTransfer.setUserId(UserApi.getInstance().getUserId());
                                                newTransfer.setTimeAdded(new Timestamp(new Date()));

                                                Log.d(LOG_TAG, "New transfer created: " + newTransfer.getFullName());

                                                if (ftPlayerId != 0) {
                                                    Log.d(LOG_TAG, "Processing additional player involved in transfer.");

                                                    ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                            .whereEqualTo("managerId", managerId)
                                                            .whereEqualTo("id", ftPlayerId)
                                                            .get()
                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Log.d(LOG_TAG, "Successfully fetched FirstTeamPlayer collection for additional player.");

                                                                        List<FirstTeamPlayer> ftPlayerList = new ArrayList<>();
                                                                        DocumentReference ftPlayerDocRef = null;
                                                                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                                                            FirstTeamPlayer ftPlayer = doc.toObject(FirstTeamPlayer.class);
                                                                            ftPlayerList.add(ftPlayer);
                                                                            ftPlayerDocRef = ftPlayersColRef.document(doc.getId());
                                                                        }
                                                                        // Only one player in this list
                                                                        FirstTeamPlayer thePlayer = ftPlayerList.get(0);

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
                                                                        fmPlayer.setFirstTeamId(ftPlayerId);
                                                                        frmPlayersColRef.add(fmPlayer);
                                                                        Log.d(LOG_TAG, "Former player added for additional player: " + thePlayer.getFullName());

                                                                        ftPlayerDocRef.delete();
                                                                        Log.d(LOG_TAG, "Additional player removed from First Team.");
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
                                                ftPlayer.setLoanPlayer(isLoan);

                                                transfersColRef.add(newTransfer);
                                                Log.d(LOG_TAG, "Transfer added to Firestore.");

                                                ftPlayersColRef.add(ftPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Log.d(LOG_TAG, "Player successfully added to First Team: " + player.getFullName());
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
                                                                        Log.d(LOG_TAG, "Navigating to ShortlistPlayersActivity.");
                                                                        Intent intent = new Intent(context, ShortlistPlayersActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barPosition", position);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Log.d(LOG_TAG, "Navigating to ShortlistActivity.");
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
            Log.d(LOG_TAG, "editPlayer called for player: " + player.getFullName());

            builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.create_shortlisted_player_popup, null);

            TextView title = view.findViewById(R.id.create_sh_player);
            final EditText firstName = view.findViewById(R.id.first_name_shp_create);
            final EditText lastName = view.findViewById(R.id.last_name_shp_create);
            final Spinner positionSpinner = view.findViewById(R.id.position_spinner_shp_create);
            final EditText nationality = view.findViewById(R.id.nationality_shp_create);
            final EditText overall = view.findViewById(R.id.overall_shp_create);
            final EditText potentialLow = view.findViewById(R.id.potential_low_shp_create);
            final EditText potentialHigh = view.findViewById(R.id.potential_high_shp_create);
            final EditText teamText = view.findViewById(R.id.team_shp_create);
            final TextInputLayout valueTil = view.findViewById(R.id.value_til_shp_create);
            final EditText value = view.findViewById(R.id.value_shp_create);
            final TextInputLayout wageTil = view.findViewById(R.id.wage_til_shp_create);
            final EditText wage = view.findViewById(R.id.wage_shp_create);
            final EditText comments = view.findViewById(R.id.comments_shp_create);
            Button editPlayerButton = view.findViewById(R.id.create_sh_player_button);

            title.setText("Edit Player");
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

            String formattedValue = NumberFormat.getInstance().format(player.getValue());
            value.setText(formattedValue);

            String formattedWage = NumberFormat.getInstance().format(player.getWage());
            wage.setText(formattedWage);

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
                                            String v = value.getText().toString().trim().replaceAll(",", "");
                                            String w = wage.getText().toString().trim().replaceAll(",", "");
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
                                                            Log.d(LOG_TAG, "Player successfully updated: " + player.getFullName());
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
