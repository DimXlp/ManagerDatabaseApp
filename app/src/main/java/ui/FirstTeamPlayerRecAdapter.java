package ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import enumeration.LoanEnum;
import enumeration.SaleTransferEnum;
import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.LoanedOutPlayer;
import model.Manager;
import model.Transfer;
import util.AnimationUtil;
import util.NationalityFlagUtil;
import util.UserApi;
import util.ValueFormatter;

public class FirstTeamPlayerRecAdapter extends RecyclerView.Adapter<FirstTeamPlayerRecAdapter.ViewHolder> {

    private static final String LOG_TAG = "RAFI|FirstTeamPlayerRecAdapter";
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
    private long maxId;
    private int maxTransferId;

    private BottomSheetDialog editDialog;
    private BottomSheetDialog departDialog;
    private BottomSheetDialog loanDialog;

    public FirstTeamPlayerRecAdapter(Context context, List<FirstTeamPlayer> playerList, long managerId, String team, String barYear, int buttonInt, long maxId) {
        this.context = context;
        this.playerList = playerList;
        this.managerId = managerId;
        this.team = team;
        this.barYear = barYear;
        this.buttonInt = buttonInt;
        this.maxId = maxId;
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

        holder.signedText.setText(hasSigned ? player.getYearSigned() : null);
        holder.signedText.setVisibility(hasSigned ? View.VISIBLE : View.GONE);
        holder.signedIcon.setVisibility(hasSigned ? View.VISIBLE : View.GONE);

        holder.scoutedText.setText(hasScouted ? player.getYearScouted() : null);
        holder.scoutedText.setVisibility(hasScouted ? View.VISIBLE : View.GONE);
        holder.scoutedIcon.setVisibility(hasScouted ? View.VISIBLE : View.GONE);

        holder.separator.setVisibility((hasSigned && hasScouted) ? View.VISIBLE : View.GONE);
        holder.yearContainer.setVisibility((hasSigned || hasScouted) ? View.VISIBLE : View.GONE);

        if (player.isLoanPlayer()) {
            holder.loanInfo.setText("Loaned to " + player.getTeam());
            holder.loanInfo.setVisibility(View.VISIBLE);
        } else {
            holder.loanInfo.setVisibility(View.GONE);
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

        private LinearLayout playerTopBar;
        private TextView numberText;
        private TextView fullNameText;
        private TextView basicInfo;
        private ImageView playerFlag;
        private TextView playerNationality;
        private TextView signedText;
        private TextView scoutedText;
        private View signedIcon;
        private View scoutedIcon;
        private TextView separator;
        private View yearContainer;
        private TextView loanInfo;
        private LinearLayout details;
        private ImageView actionMenu;

        private EditText teamLeft;
        private TextView yearLeft;
        private TextView typeOfTransfer;
        private EditText transferFee;
        private SwitchMaterial playerExchangeSwitch;
        private EditText comments;
        private Button transferButton;
        private EditText teamLoan;
        private TextView yearLoaned;
        private TextView loanPicker;
        private Button loanPlayerButton;
        private TextInputLayout transferFeeTil;
        private Button setTerminationYearButton;
        private boolean hasExchangePlayer = false;

        public ViewHolder(@NonNull View itemView, final Context ctx) {
            super(itemView);
            context = ctx;

            playerTopBar = itemView.findViewById(R.id.player_top_bar);
            numberText = itemView.findViewById(R.id.player_number);
            fullNameText = itemView.findViewById(R.id.player_full_name);
            basicInfo = itemView.findViewById(R.id.player_basic_text);
            playerFlag = itemView.findViewById(R.id.player_flag);
            playerNationality = itemView.findViewById(R.id.player_nationality);
            signedText = itemView.findViewById(R.id.year_signed_text);
            scoutedText = itemView.findViewById(R.id.year_scouted_text);
            signedIcon = itemView.findViewById(R.id.year_signed_icon);
            scoutedIcon = itemView.findViewById(R.id.year_scouted_icon);
            separator = itemView.findViewById(R.id.year_separator);
            yearContainer = itemView.findViewById(R.id.player_year_info_container);
            loanInfo = itemView.findViewById(R.id.player_loan_info);
            details = itemView.findViewById(R.id.expandable_section);
            actionMenu = itemView.findViewById(R.id.player_action_menu);

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
                inflater.inflate(R.menu.first_player_actions_menu, popupMenu.getMenu());

                var position = getAdapterPosition();
                var player = playerList.get(position);

                if (!player.isLoanPlayer()) {
                    popupMenu.getMenu().findItem(R.id.action_terminate_loan).setVisible(false);
                }

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.action_edit:
                            clickEditPlayerButton(playerList.get(position));
                            return true;
                        case R.id.action_depart:
                            clickDepartPlayerButton();
                            return true;
                        case R.id.action_loan:
                            clickLoanPlayerButton();
                            return true;
                        case R.id.action_terminate_loan:
                            clickTerminateLoanButton();
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

        private void clickTerminateLoanButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            aBuilder = new AlertDialog.Builder(context);
                            View view = LayoutInflater.from(context).inflate(R.layout.year_left_popup, null);

                            yearLeft = view.findViewById(R.id.year_left_picker_left);
                            setTerminationYearButton = view.findViewById(R.id.set_year_left_button);

                            String[] years = context.getResources().getStringArray(R.array.years_array);

                            yearLeft.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Year Left")
                                        .setItems(years, (pickerDialog, item) -> yearLeft.setText(years[item]))
                                        .show();
                            });

                            setTerminationYearButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!yearLeft.getText().toString().isEmpty()) {
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

        private void clickLoanPlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            loanDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
                            View view = LayoutInflater.from(context).inflate(R.layout.loan_popup, null);
                            loanDialog.setContentView(view);

                            teamLoan = view.findViewById(R.id.team_loan);
                            yearLoaned = view.findViewById(R.id.year_loaned_picker);
                            loanPicker = view.findViewById(R.id.type_of_loan_picker);
                            comments = view.findViewById(R.id.comments_loan);
                            loanPlayerButton = view.findViewById(R.id.loan_player_button);

                            String[] years = context.getResources().getStringArray(R.array.years_array);

                            yearLoaned.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Year Loaned")
                                        .setItems(years, (pickerDialog, item) -> yearLoaned.setText(years[item]))
                                        .show();
                            });

                            List<String> loanTypes = Arrays.stream(LoanEnum.values())
                                    .map(LoanEnum::getDescription)
                                    .collect(Collectors.toList());

                            loanPicker.setOnClickListener(v -> {
                                        new AlertDialog.Builder(context)
                                                .setTitle("Select Type of Loan")
                                                .setItems(loanTypes.toArray(new String[0]), (pickerDialog, loan) -> {
                                                    loanPicker.setText(loanTypes.get(loan));
                                                })
                                                .show();
                                    });

                            loanPlayerButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!teamLoan.getText().toString().isEmpty() &&
                                        !yearLoaned.getText().toString().isEmpty() &&
                                        !loanPicker.getText().toString().isEmpty()) {
                                        loanPlayer(playerList.get(getAdapterPosition()));
                                    } else {
                                        Toast.makeText(context, "Team, Year Loaned and Type of Loan are required", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                            loanDialog.show();

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

        private void clickDepartPlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            departDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
                            View view = LayoutInflater.from(context).inflate(R.layout.depart_popup, null);
                            departDialog.setContentView(view);

                            teamLeft = view.findViewById(R.id.team_left);
                            yearLeft = view.findViewById(R.id.year_left_picker_depart);
                            typeOfTransfer = view.findViewById(R.id.type_of_transfer_picker_depart);
                            transferFeeTil = view.findViewById(R.id.transfer_fee_til_depart);
                            transferFee = view.findViewById(R.id.transfer_fee_depart);
                            playerExchangeSwitch = view.findViewById(R.id.player_exchange_switch_depart);
                            comments = view.findViewById(R.id.comments_depart);
                            transferButton = view.findViewById(R.id.transfer_player_button);

                            String[] years = context.getResources().getStringArray(R.array.years_array);

                            yearLeft.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Year Left")
                                        .setItems(years, (pickerDialog, item) -> yearLeft.setText(years[item]))
                                        .show();
                            });

                            ValueFormatter.formatValue(transferFee);

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

                            List<String> transferTypes = Arrays.stream(SaleTransferEnum.values())
                                    .map(SaleTransferEnum::getDescription)
                                    .collect(Collectors.toList());

                            typeOfTransfer.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Type of Transfer")
                                        .setItems(transferTypes.toArray(new String[0]), (pickerDialog, transfer) -> {
                                            String selectedTransferType = transferTypes.get(transfer);
                                            typeOfTransfer.setText(transferTypes.get(transfer));

                                            if (SaleTransferEnum.RELEASE.getDescription().equals(selectedTransferType)) {
                                                transferFeeTil.setVisibility(View.GONE);
                                                transferFee.setEnabled(false);
                                                teamLeft.setVisibility(View.GONE);
                                                playerExchangeSwitch.setVisibility(View.GONE);
                                            } else {
                                                transferFeeTil.setVisibility(View.VISIBLE);
                                                transferFee.setEnabled(true);
                                                teamLeft.setVisibility(View.VISIBLE);
                                                playerExchangeSwitch.setVisibility(View.VISIBLE);
                                            }
                                        })
                                        .show();
                            });

                            playerExchangeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                        hasExchangePlayer = isChecked;
                                }
                            });

                            transferButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String selectedTransferType = typeOfTransfer.getText().toString();
                                    String selectedYearLeft = yearLeft.getText().toString();
                                    String teamLeftText = teamLeft.getText().toString();
                                    boolean isWithTransferFeeType = SaleTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(selectedTransferType);
                                    boolean isReleaseType = SaleTransferEnum.RELEASE.getDescription().equals(selectedTransferType);
                                    boolean isYearLeftZero = "0".equals(selectedYearLeft);

                                    if (isReleaseType) {
                                        if (isYearLeftZero) {
                                            Toast.makeText(context, "Year Left required!", Toast.LENGTH_LONG).show();
                                        } else {
                                            int position = getAdapterPosition();
                                            if (position != RecyclerView.NO_POSITION) {
                                                letPlayerLeave(playerList.get(position));
                                            }
                                        }
                                    } else if (isWithTransferFeeType) {
                                        if (teamLeftText.isEmpty() && isYearLeftZero) {
                                            Toast.makeText(context, "Team and Year Left fields required!", Toast.LENGTH_LONG).show();
                                        } else {
                                            int position = getAdapterPosition();
                                            if (position != RecyclerView.NO_POSITION) {
                                                letPlayerLeave(playerList.get(position));
                                            }
                                        }
                                    }
                                }
                            });

                            departDialog.show();

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

        private void terminateLoan(final FirstTeamPlayer player) {
            Log.d(LOG_TAG, "terminateLoan called for player: " + player.getFullName());

            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "First Team players fetched successfully.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                Log.d(LOG_TAG, "Document reference found for player: " + player.getFullName());
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Player successfully removed from First Team: " + player.getFullName());

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
                                                fmPlayer.setYearLeft(yearLeft.getText().toString().trim());
                                                fmPlayer.setManagerId(managerId);
                                                fmPlayer.setUserId(UserApi.getInstance().getUserId());
                                                fmPlayer.setTimeAdded(new Timestamp(new Date()));
                                                fmPlayer.setFirstTeamId(player.getId());

                                                fmPlayersReference.add(fmPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Log.d(LOG_TAG, "Player added to FormerPlayer collection: " + fmPlayer.getFullName());
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
                                                                        Log.d(LOG_TAG, "First Team list refreshed. Remaining players: " + task.getResult().size());
                                                                        Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Log.d(LOG_TAG, "No players left in First Team. Navigating to FirstTeamActivity.");
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
            Log.d(LOG_TAG, "loanPlayer called for player: " + player.getFullName());

            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "First Team players fetched successfully.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                Log.d(LOG_TAG, "Document reference found for player: " + player.getFullName());
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Player successfully removed from First Team: " + player.getFullName());

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
                                                newTransfer.setType(loanPicker.getText().toString());
                                                newTransfer.setYear(yearLoaned.getText().toString().trim());
                                                String coms = comments.getText().toString().trim();
                                                newTransfer.setComments((!coms.isEmpty()) ? coms : "");
                                                newTransfer.setFormerPlayer(true);
                                                newTransfer.setManagerId(managerId);
                                                newTransfer.setUserId(UserApi.getInstance().getUserId());
                                                newTransfer.setTimeAdded(new Timestamp(new Date()));
                                                Log.d(LOG_TAG, "Transfer object created for player: " + player.getFullName());

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
                                                loPlayer.setYearLoanedOut(yearLoaned.getText().toString().trim());
                                                loPlayer.setTypeOfLoan(loanPicker.getText().toString());
                                                loPlayer.setManagerId(managerId);
                                                loPlayer.setUserId(UserApi.getInstance().getUserId());
                                                loPlayer.setTimeAdded(new Timestamp(new Date()));
                                                Log.d(LOG_TAG, "LoanedOutPlayer object created for player: " + player.getFullName());

                                                transfersReference.add(newTransfer)
                                                        .addOnSuccessListener(docRef -> Log.d(LOG_TAG, "Transfer added to Firestore: " + newTransfer.getFullName()))
                                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding transfer to Firestore.", e));


                                                loPlayersReference.add(loPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Log.d(LOG_TAG, "Player added to LoanedOutPlayer collection: " + loPlayer.getFullName());
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
                                                                        Log.d(LOG_TAG, "First Team list refreshed. Remaining players: " + task.getResult().size());
                                                                        loanDialog.dismiss();
                                                                        Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Log.d(LOG_TAG, "No players left in First Team. Navigating to FirstTeamActivity.");
                                                                        loanDialog.dismiss();
                                                                        Intent intent = new Intent(context, FirstTeamActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    }
                                                                } else {
                                                                    Log.e(LOG_TAG, "Error refreshing First Team list.", task.getException());
                                                                }
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding player to LoanedOutPlayer collection.", e));
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error removing player from First Team.", e));
                            }
                        }
                    });
        }

        private void deletePlayer(final FirstTeamPlayer player) {
            Log.d(LOG_TAG, "deletePlayer called for player: " + player.getFullName());

            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "First Team players fetched successfully.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                Log.d(LOG_TAG, "Document reference found for player: " + player.getFullName());
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Player successfully deleted from First Team: " + player.getFullName());
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
                                                                        Log.d(LOG_TAG, "First Team list refreshed. Remaining players: " + task.getResult().size());
                                                                        Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Log.d(LOG_TAG, "No players left in First Team. Navigating to FirstTeamActivity.");
                                                                        Intent intent = new Intent(context, FirstTeamActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    }
                                                                } else {
                                                                    Log.e(LOG_TAG, "Error refreshing First Team list.", task.getException());
                                                                }
                                                            }
                                                        });
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error deleting player from First Team.", e));
                            }
                        }
                    });
        }

        private void letPlayerLeave(final FirstTeamPlayer player) {
            Log.d(LOG_TAG, "letPlayerLeave called for player: " + player.getFullName());

            ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "First Team players fetched successfully.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FirstTeamPlayer ftPlayer = ds.toObject(FirstTeamPlayer.class);
                                    assert ftPlayer != null;
                                    if (ftPlayer.getId() == player.getId()) {
                                        documentReference = ftPlayersReference.document(ds.getId());
                                        Log.d(LOG_TAG, "Matching player ID: " + ftPlayer.getId() + " with " + player.getId());
                                    }
                                }
                                assert documentReference != null;
                                Log.d(LOG_TAG, "Document reference found for player: " + player.getFullName());
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Player successfully removed from First Team: " + player.getFullName());

                                                final Transfer newTransfer = new Transfer();
                                                findMaxTransferId();
                                                newTransfer.setId(maxTransferId);
                                                newTransfer.setFirstName(player.getFirstName());
                                                newTransfer.setLastName(player.getLastName());
                                                newTransfer.setFullName(player.getFullName());
                                                newTransfer.setPosition(player.getPosition());
                                                newTransfer.setFormerTeam(player.getTeam());
                                                newTransfer.setCurrentTeam(!teamLeft.getText().toString().trim().isEmpty()
                                                                            ? teamLeft.getText().toString().trim()
                                                                            : "Free Agent");
                                                newTransfer.setNationality(player.getNationality());
                                                newTransfer.setOverall(player.getOverall());
                                                newTransfer.setPotentialLow(player.getPotentialLow());
                                                newTransfer.setPotentialHigh(player.getPotentialHigh());
                                                newTransfer.setType(typeOfTransfer.getText().toString());
                                                String trFee = transferFee.getText().toString().trim().replaceAll(",", "");
                                                newTransfer.setTransferFee((!trFee.isEmpty()) ? Integer.parseInt(trFee) : 0);
                                                newTransfer.setYear(yearLeft.getText().toString().trim());
                                                String coms = comments.getText().toString().trim();
                                                newTransfer.setComments((!coms.isEmpty()) ? coms : "");
                                                newTransfer.setFormerPlayer(true);
                                                newTransfer.setHasPlayerExchange(hasExchangePlayer);
                                                newTransfer.setManagerId(managerId);
                                                newTransfer.setUserId(UserApi.getInstance().getUserId());
                                                newTransfer.setTimeAdded(new Timestamp(new Date()));
                                                Log.d(LOG_TAG, "Transfer object created: " + newTransfer.getFullName());

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
                                                fmPlayer.setYearLeft(yearLeft.getText().toString().trim());
                                                fmPlayer.setManagerId(managerId);
                                                fmPlayer.setUserId(UserApi.getInstance().getUserId());
                                                fmPlayer.setTimeAdded(new Timestamp(new Date()));
                                                fmPlayer.setFirstTeamId(player.getId());
                                                Log.d(LOG_TAG, "FormerPlayer object created: " + fmPlayer.getFullName());

                                                fmPlayersReference.add(fmPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Log.d(LOG_TAG, "Player added to FormerPlayer collection: " + fmPlayer.getFullName());
                                                                Toast.makeText(context, "Player transferred!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding player to FormerPlayer collection.", e));
                                                if (hasExchangePlayer) {
                                                    addExchangePlayer(newTransfer);
                                                } else {
                                                    transfersReference.add(newTransfer)
                                                            .addOnSuccessListener(docRef2 -> Log.d(LOG_TAG, "Transfer added to Firestore: " + newTransfer.getFullName()))
                                                            .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding transfer to Firestore.", e));

                                                    ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                            .whereEqualTo("managerId", managerId)
                                                            .get()
                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                    if (task.isSuccessful()) {
                                                                        if (!task.getResult().isEmpty()) {
                                                                            Log.d(LOG_TAG, "First Team list refreshed. Remaining players: " + task.getResult().size());
                                                                            departDialog.dismiss();
                                                                            Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                                            intent.putExtra("managerId", managerId);
                                                                            intent.putExtra("team", team);
                                                                            intent.putExtra("barYear", barYear);
                                                                            context.startActivity(intent);
                                                                            ((Activity) context).finish();
                                                                        } else {
                                                                            Log.d(LOG_TAG, "No players left in First Team. Navigating to FirstTeamActivity.");
                                                                            departDialog.dismiss();
                                                                            Intent intent = new Intent(context, FirstTeamActivity.class);
                                                                            intent.putExtra("managerId", managerId);
                                                                            intent.putExtra("team", team);
                                                                            context.startActivity(intent);
                                                                            ((Activity) context).finish();
                                                                        }
                                                                    } else {
                                                                        Log.e(LOG_TAG, "Error refreshing First Team list.", task.getException());
                                                                    }
                                                                }
                                                            });
                                                }

                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error removing player from First Team.", e));
                            }
                        }
                    });
        }

        private void addExchangePlayer(Transfer transfer) {
            Log.d(LOG_TAG, "addExchangePlayer called for transfer: " + transfer.getFullName());

            BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View view = LayoutInflater.from(context).inflate(R.layout.create_first_team_player_popup, null);
            dialog.setContentView(view);

            TextView title = view.findViewById(R.id.create_ft_player);
            final EditText firstName = view.findViewById(R.id.first_name_ftp_create);
            final EditText lastName = view.findViewById(R.id.last_name_ftp_create);
            final TextView positionPicker = view.findViewById(R.id.position_picker_ftp_create);
            final EditText number = view.findViewById(R.id.number_ftp_create);
            final EditText nationality = view.findViewById(R.id.nationality_ftp_create);
            final EditText overall = view.findViewById(R.id.overall_ftp_create);
            final EditText potentialLow = view.findViewById(R.id.potential_low_ftp_create);
            final EditText potentialHigh = view.findViewById(R.id.potential_high_ftp_create);
            final TextView yearSignedPicker = view.findViewById(R.id.year_signed_picker_ftp_create);
            final TextView yearScoutedPicker = view.findViewById(R.id.year_scouted_picker_ftp_create);
            final SwitchMaterial loanSwitch = view.findViewById(R.id.loan_player_switch_ftp_create);
            Button savePlayerButton = view.findViewById(R.id.create_ft_player_button);

            title.setText("Add Exchange Player");
            savePlayerButton.setText(R.string.save_player);
            loanSwitch.setVisibility(View.GONE);
            yearScoutedPicker.setVisibility(View.GONE);

            String[] positions = context.getResources().getStringArray(R.array.position_array);
            String[] years = context.getResources().getStringArray(R.array.years_array);

            positionPicker.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Select Position")
                        .setItems(positions, (pickerDialog, which) -> positionPicker.setText(positions[which]))
                        .show();
            });

            yearSignedPicker.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Select Year Signed")
                        .setItems(years, (pickerDialog, which) -> yearSignedPicker.setText(years[which]))
                        .show();
            });

            savePlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Save Exchange Player button clicked.");

                    if (!lastName.getText().toString().isEmpty() &&
                            !nationality.getText().toString().isEmpty() &&
                            !positionPicker.getText().toString().isEmpty() &&
                            !overall.getText().toString().isEmpty() &&
                            !yearSignedPicker.getText().toString().isEmpty()) {
                        Log.d(LOG_TAG, "Validation successful. Proceeding to create exchange player.");
                        createPlayer(transfer);
                    } else {
                        Log.w(LOG_TAG, "Validation failed: Required fields are missing.");
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }

                private void createPlayer(Transfer newTransfer) {
                    Log.d(LOG_TAG, "Creating exchange player for transfer: " + newTransfer.getFullName());

                    String firstNamePlayer = firstName.getText().toString().trim();
                    String lastNamePlayer = lastName.getText().toString().trim();
                    String fullNamePlayer;
                    if (!firstNamePlayer.isEmpty()) {
                        fullNamePlayer = firstNamePlayer + " " + lastNamePlayer;
                    } else {
                        fullNamePlayer = lastNamePlayer;
                    }
                    String positionPlayer = positionPicker.getText().toString().trim();
                    String numberPlayer = number.getText().toString().trim();
                    String nationalityPlayer = nationality.getText().toString().trim();
                    String overallPlayer = overall.getText().toString().trim();
                    String potentialLowPlayer = potentialLow.getText().toString().trim();
                    String potentialHiPlayer = potentialHigh.getText().toString().trim();
                    final String ySignedPlayer = yearSignedPicker.getText().toString().trim();
                    Log.d(LOG_TAG, "Exchange player details: Full Name = " + fullNamePlayer + ", Position = " + positionPlayer);

                    FirstTeamPlayer player = new FirstTeamPlayer();
                    player.setId(maxId+1);
                    newTransfer.setExchangePlayerId(maxId+1);
                    player.setFirstName(firstNamePlayer);
                    player.setLastName(lastNamePlayer);
                    player.setFullName(fullNamePlayer);
                    newTransfer.setExchangePlayerName(fullNamePlayer);
                    player.setPosition(positionPlayer);
                    if (!numberPlayer.isEmpty()) {
                        player.setNumber(Integer.parseInt(numberPlayer));
                    } else {
                        player.setNumber(99);
                    }
                    player.setTeam(team);
                    player.setNationality(nationalityPlayer);
                    player.setOverall(Integer.parseInt(overallPlayer));
                    if (!potentialLowPlayer.isEmpty()) {
                        player.setPotentialLow(Integer.parseInt(potentialLowPlayer));
                    }
                    if (!potentialHiPlayer.isEmpty()) {
                        player.setPotentialHigh(Integer.parseInt(potentialHiPlayer));
                    }
                    player.setYearSigned(ySignedPlayer);
                    player.setYearScouted("0");
                    player.setUserId(UserApi.getInstance().getUserId());
                    player.setTimeAdded(new Timestamp(new Date()));
                    player.setManagerId(managerId);
                    player.setLoanPlayer(loanSwitch.isChecked());
                    Log.d(LOG_TAG, "FirstTeamPlayer object created for exchange player: " + fullNamePlayer);

                    newTransfer.setExchangePlayerId(player.getId());
                    newTransfer.setExchangePlayerName(player.getFullName());

                    transfersReference.add(newTransfer)
                            .addOnSuccessListener(docRef -> Log.d(LOG_TAG, "Transfer added to Firestore: " + newTransfer.getFullName()))
                            .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding transfer to Firestore.", e));

                    ftPlayersReference.add(player)
                            .addOnSuccessListener(docRef -> {
                                        Log.d(LOG_TAG, "Exchange player added to FirstTeamPlayer collection: " + player.getFullName());
                                        Toast.makeText(context, "Exchange Player added!", Toast.LENGTH_LONG).show();
                                    })
                            .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding exchange player to Firestore.", e));

                    ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                            .whereEqualTo("managerId", managerId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        if (!task.getResult().isEmpty()) {
                                            Log.d(LOG_TAG, "Navigating to FirstTeamListActivity.");
                                            Intent intent = new Intent(context, FirstTeamListActivity.class);
                                            intent.putExtra("managerId", managerId);
                                            intent.putExtra("team", team);
                                            intent.putExtra("barYear", ySignedPlayer);
                                            context.startActivity(intent);
                                            ((Activity) context).finish();
                                        } else {
                                            Log.d(LOG_TAG, "No players left. Navigating to FirstTeamActivity.");
                                            Intent intent = new Intent(context, FirstTeamActivity.class);
                                            intent.putExtra("managerId", managerId);
                                            intent.putExtra("team", team);
                                            context.startActivity(intent);
                                            ((Activity) context).finish();
                                        }
                                    }
                                }
                            });
                }
            });

            dialog.show();
        }

        private void clickEditPlayerButton(final FirstTeamPlayer player) {
            Log.d(LOG_TAG, "editPlayer called for player: " + player.getFullName());

            editDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View view = LayoutInflater.from(context).inflate(R.layout.create_first_team_player_popup, null);
            editDialog.setContentView(view);

            TextView title = view.findViewById(R.id.create_ft_player);
            final EditText firstName = view.findViewById(R.id.first_name_ftp_create);
            final EditText lastName = view.findViewById(R.id.last_name_ftp_create);
            final TextView positionPicker = view.findViewById(R.id.position_picker_ftp_create);
            final EditText number = view.findViewById(R.id.number_ftp_create);
            final AutoCompleteTextView nationality = view.findViewById(R.id.nationality_ftp_create);
            final EditText overall = view.findViewById(R.id.overall_ftp_create);
            final EditText potentialLow = view.findViewById(R.id.potential_low_ftp_create);
            final EditText potentialHigh = view.findViewById(R.id.potential_high_ftp_create);
            final TextView yearSignedPicker = view.findViewById(R.id.year_signed_picker_ftp_create);
            final TextView yearScoutedPicker = view.findViewById(R.id.year_scouted_picker_ftp_create);
            final SwitchMaterial loanSwitch = view.findViewById(R.id.loan_player_switch_ftp_create);
            Button savePlayerButton = view.findViewById(R.id.create_ft_player_button);

            title.setText(R.string.edit_player_title);
            savePlayerButton.setText(R.string.save_player);

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

            yearSignedPicker.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Signed")
                        .setItems(years, (pickerDialog, which) -> yearSignedPicker.setText(years[which]))
                        .show();
            });

            yearScoutedPicker.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Scouted")
                        .setItems(years, (pickerDialog, which) -> yearScoutedPicker.setText(years[which]))
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
            yearSignedPicker.setText(player.getYearSigned());
            yearScoutedPicker.setText(player.getYearScouted());
            loanSwitch.setChecked(player.isLoanPlayer());
            Log.d(LOG_TAG, "Player data populated into fields for editing: " + player.getFullName());

            editDialog.show();

            savePlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Save Player button clicked for player: " + player.getFullName() + ", id: " + player.getId());

                    if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionPicker.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty() &&
                        !yearSignedPicker.getText().toString().isEmpty()) {
                        Log.d(LOG_TAG, "Validation successful. Proceeding to update player.");

                        ftPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(LOG_TAG, "First Team players fetched successfully.");

                                            List<DocumentSnapshot> doc =  task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds: doc) {
                                                FirstTeamPlayer ftplayer = ds.toObject(FirstTeamPlayer.class);
                                                if (ftplayer.getId() == player.getId()) {
                                                    documentReference = ftPlayersReference.document(ds.getId());
                                                    Log.d(LOG_TAG, "Matching player ID: " + ftplayer.getId() + " with " + player.getId());
                                                }
                                            }
                                            String no = number.getText().toString().trim();
                                            String nationalityPlayer = nationality.getText().toString().trim();
                                            Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
                                            String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            String yScouted = yearScoutedPicker.getText().toString().trim();
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
                                                    "yearSigned", yearSignedPicker.getText().toString().trim(),
                                                    "yearScouted", (!yScouted.equals("0")) ? yScouted : "0",
                                                    "loanPlayer", loanSwitch.isChecked())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(LOG_TAG, "Player successfully updated in Firestore: " + player.getFullName());
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            editDialog.dismiss();
                                                            Intent intent = new Intent(context, FirstTeamListActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barYear", barYear);
                                                            context.startActivity(intent);
                                                            ((Activity)context).finish();
                                                            Toast.makeText(context, "Player updated!", Toast.LENGTH_LONG)
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
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position and Overall are required", Toast.LENGTH_LONG)
                                .show();
                    }

                }
            });
        }
    }

    public void findMaxTransferId() {
        db.collection("Transfers")
                .whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(LOG_TAG, "Transfers fetched successfully for calculating max transfer ID.");

                        maxTransferId = queryDocumentSnapshots.toObjects(Transfer.class).get(0).getId();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Transfer transfer = doc.toObject(Transfer.class);
                            assert transfer != null;
                            if (transfer.getId() > maxTransferId) {
                                maxTransferId = transfer.getId();
                            }
                        }
                    } else {
                        Log.d(LOG_TAG, "No transfers found. Defaulting maxTransferId to 0.");
                        maxTransferId = 0;
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching transfers for maxTransferId.", e));
    }
}
