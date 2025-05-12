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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import enumeration.LoanEnum;
import enumeration.PurchaseTransferEnum;
import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.Manager;
import model.Player;
import model.ShortlistedPlayer;
import model.Transfer;
import util.AnimationUtil;
import util.NationalityFlagUtil;
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

    private BottomSheetDialog createDialog;
    private BottomSheetDialog transferDialog;

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

        holder.teamNameText.setText(player.getTeam());

        boolean hasValue = player.getValue() > 0;
        boolean hasWage = player.getWage() > 0;

        holder.valueIcon.setVisibility(hasValue ? View.VISIBLE : View.GONE);
        holder.valueText.setVisibility(hasValue ? View.VISIBLE : View.GONE);
        holder.wageIcon.setVisibility(hasWage? View.VISIBLE : View.GONE);
        holder.wageText.setVisibility(hasWage? View.VISIBLE : View.GONE);

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
                            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
                            if (hasValue) {
                                holder.valueText.setText(String.format("%s %s", currency, formatter.format(player.getValue())));
                            }
                            if (hasWage) {
                                holder.wageText.setText(String.format("%s %s", currency,formatter.format(player.getWage())));
                            }

                            switch (currency) {
                                case "€":
                                    holder.valueIcon.setImageResource(R.drawable.ic_euros);
                                    break;
                                case "$":
                                    holder.valueIcon.setImageResource(R.drawable.ic_dollars);
                                    break;
                                case "£":
                                    holder.valueIcon.setImageResource(R.drawable.ic_pounds);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });

        holder.financeSeparator.setVisibility((hasValue && hasWage) ? View.VISIBLE : View.GONE);
        holder.playerFinancialInfo.setVisibility((hasValue || hasWage) ? View.VISIBLE : View.GONE);

        boolean hasComments = !player.getComments().isEmpty();

        holder.commentsText.setText(player.getComments());
        holder.commentsText.setVisibility(hasComments ? View.VISIBLE : View.GONE);

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

        private LinearLayout playerTopBar;
        private TextView fullNameText;
        private TextView basicInfo;
        private ImageView playerFlag;
        private TextView playerNationality;
        private TextView teamNameText;
        private LinearLayout playerFinancialInfo;
        private ImageView valueIcon;
        private TextView valueText;
        private TextView financeSeparator;
        private ImageView wageIcon;
        private TextView wageText;
        private TextView commentsText;
        private LinearLayout details;
        private ImageView actionMenu;

        private TextView typeOfTransferPicker;
        private TextInputLayout transferFeeTil;
        private EditText transferFee;
        private TextInputLayout playerPickerText;
        private TextView playerPicker;
        private TextInputLayout wageTil;
        private EditText wage;
        private EditText contractYears;
        private TextView yearSigned;
        private EditText comments;
        private Button transferButton;
        private long ftPlayerId;
        private boolean hasPlusPlayer = false;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);
            context = ctx;

            playerTopBar = itemView.findViewById(R.id.player_top_bar_shp);
            fullNameText = itemView.findViewById(R.id.player_full_name_shp);
            basicInfo = itemView.findViewById(R.id.player_basic_text_shp);
            playerFlag = itemView.findViewById(R.id.player_flag_shp);
            playerNationality = itemView.findViewById(R.id.player_nationality_shp);
            teamNameText = itemView.findViewById(R.id.team_text_shp);
            playerFinancialInfo = itemView.findViewById(R.id.player_financial_info_container_shp);
            valueIcon = itemView.findViewById(R.id.value_icon_shp);
            valueText = itemView.findViewById(R.id.value_text_shp);
            financeSeparator = itemView.findViewById(R.id.finance_separator_shp);
            wageIcon = itemView.findViewById(R.id.wage_icon_shp);
            wageText = itemView.findViewById(R.id.wage_text_shp);
            commentsText = itemView.findViewById(R.id.comments_shp);
            details = itemView.findViewById(R.id.expandable_section_shp);
            actionMenu = itemView.findViewById(R.id.player_action_menu_shp);

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
                inflater.inflate(R.menu.shortlist_players_action_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.action_edit:
                            clickEditPlayerButton(playerList.get(getAdapterPosition()));
                            return true;
                        case R.id.action_buy:
                            clickBuyPlayerButton();
                            return true;
                        case R.id.action_loan:
                            clickLoanPlayerButton();
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

        private void clickLoanPlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            transferDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
                            View view = LayoutInflater.from(context).inflate(R.layout.transfer_popup, null);
                            transferDialog.setContentView(view);

                            typeOfTransferPicker = view.findViewById(R.id.type_of_transfer_picker_buy);
                            transferFeeTil = view.findViewById(R.id.transfer_fee_til_buy);
                            transferFee = view.findViewById(R.id.transfer_fee_buy);
                            playerPickerText = view.findViewById(R.id.plus_player_picker_til_buy);
                            playerPicker = view.findViewById(R.id.plus_player_picker_buy);
                            wageTil = view.findViewById(R.id.wage_til_buy);
                            wage = view.findViewById(R.id.wage_buy);
                            contractYears = view.findViewById(R.id.contract_years_buy);
                            yearSigned = view.findViewById(R.id.year_signed_picker_buy);
                            comments = view.findViewById(R.id.comments_buy);
                            transferButton = view.findViewById(R.id.transfer_button);
                            transferButton.setText("Loan Player");

                            ValueFormatter.formatValue(wage);

                            transferFeeTil.setVisibility(View.GONE);
                            transferFee.setVisibility(View.GONE);
                            playerPickerText.setVisibility(View.GONE);
                            playerPicker.setVisibility(View.GONE);
                            contractYears.setVisibility(View.GONE);

                            typeOfTransferPicker.setText(LoanEnum.SHORT_TERM.getDescription());

                            String[] transferTypes = getLoans();

                            typeOfTransferPicker.setOnClickListener(v -> {
                                new android.app.AlertDialog.Builder(context)
                                        .setTitle("Select Loan Type")
                                        .setItems(transferTypes, (pickerDialog, transfer) -> typeOfTransferPicker.setText(transferTypes[transfer]))
                                        .show();
                            });

                            String[] years = context.getResources().getStringArray(R.array.years_array);

                            yearSigned.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Year Signed")
                                        .setItems(years, (pickerDialog, item) -> yearSigned.setText(years[item]))
                                        .show();
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
                                                wageTil.setHint("Wage (in " + currency + ")");
                                            }
                                        }
                                    });

                            transferButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!yearSigned.getText().toString().isEmpty() &&
                                            !typeOfTransferPicker.getText().toString().isEmpty()) {
                                        transferPlayer(playerList.get(getAdapterPosition()), true);
                                    } else {
                                        Toast.makeText(context, "Transfer Type & Year Signed are required!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                            transferDialog.show();

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

        private void clickBuyPlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            transferDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
                            View view = LayoutInflater.from(context).inflate(R.layout.transfer_popup, null);
                            transferDialog.setContentView(view);

                            typeOfTransferPicker = view.findViewById(R.id.type_of_transfer_picker_buy);
                            transferFeeTil = view.findViewById(R.id.transfer_fee_til_buy);
                            transferFee = view.findViewById(R.id.transfer_fee_buy);
                            playerPickerText = view.findViewById(R.id.plus_player_picker_til_buy);
                            playerPicker = view.findViewById(R.id.plus_player_picker_buy);
                            wageTil = view.findViewById(R.id.wage_til_buy);
                            wage = view.findViewById(R.id.wage_buy);
                            contractYears = view.findViewById(R.id.contract_years_buy);
                            yearSigned = view.findViewById(R.id.year_signed_picker_buy);
                            comments = view.findViewById(R.id.comments_buy);
                            transferButton = view.findViewById(R.id.transfer_button);

                            typeOfTransferPicker.setText(PurchaseTransferEnum.WITH_TRANSFER_FEE.getTransferType());

                            ValueFormatter.formatValue(transferFee);
                            ValueFormatter.formatValue(wage);

                            String[] transferTypes = getPurchaseTransfers();

                            typeOfTransferPicker.setOnClickListener(v -> {
                                new android.app.AlertDialog.Builder(context)
                                        .setTitle("Select Purchase Type")
                                        .setItems(transferTypes, (pickerDialog, transfer) -> {
                                            String selectedType = transferTypes[transfer];
                                            typeOfTransferPicker.setText(selectedType);

                                            if (PurchaseTransferEnum.FREE_TRANSFER.getTransferType().equals(selectedType)) {
                                                transferFeeTil.setVisibility(View.GONE);
                                                transferFee.setEnabled(false);
                                                playerPickerText.setVisibility(View.GONE);
                                                playerPicker.setVisibility(View.GONE);
                                            } else {
                                                transferFeeTil.setVisibility(View.VISIBLE);
                                                transferFee.setEnabled(true);
                                                playerPickerText.setVisibility(View.VISIBLE);
                                                playerPicker.setVisibility(View.VISIBLE);
                                            }
                                        })
                                        .show();
                            });

                            String[] years = context.getResources().getStringArray(R.array.years_array);

                            yearSigned.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Year Signed")
                                        .setItems(years, (pickerDialog, item) -> yearSigned.setText(years[item]))
                                        .show();
                            });

                            playerPicker.setText("None");

                            ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                    .whereEqualTo("managerId", managerId)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            if (!queryDocumentSnapshots.isEmpty()) {
                                                List<FirstTeamPlayer> ftPlayerList = new ArrayList<>();
                                                var firstPlayer = new FirstTeamPlayer();
                                                firstPlayer.setFullName("None");
                                                ftPlayerList.add(firstPlayer);
                                                queryDocumentSnapshots.getDocuments().stream()
                                                        .map(doc -> doc.toObject(FirstTeamPlayer.class))
                                                        .forEach(ftPlayerList::add);

                                                List<String> playerNames = ftPlayerList.stream()
                                                        .map(Player::getFullName)
                                                        .collect(Collectors.toList());

                                                playerPicker.setOnClickListener(v -> {
                                                    if (!playerNames.isEmpty()) {
                                                        String[] nameArray = playerNames.toArray(new String[0]);

                                                        new AlertDialog.Builder(context)
                                                                .setTitle("Select First Team Player")
                                                                .setItems(nameArray, (dialog, which) -> {
                                                                    FirstTeamPlayer selectedPlayer = ftPlayerList.get(which);
                                                                    playerPicker.setText(selectedPlayer.getFullName());

                                                                    ftPlayerId = selectedPlayer.getId();
                                                                    hasPlusPlayer = ftPlayerId > 0 && !selectedPlayer.getFullName().equals("None");
                                                                })
                                                                .show();
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
                                    if (!yearSigned.getText().toString().isEmpty() &&
                                        !typeOfTransferPicker.getText().toString().isEmpty()) {
                                        transferPlayer(playerList.get(getAdapterPosition()), false);
                                    } else {
                                        Toast.makeText(context, "Transfer Type & Year Signed are required!", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                            transferDialog.show();

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

        private String[] getLoans() {
            return Arrays.stream(LoanEnum.values())
                    .map(LoanEnum::getDescription)
                    .toArray(String[]::new);
        }

        private String[] getPurchaseTransfers() {
            return Arrays.stream(PurchaseTransferEnum.values())
                    .map(PurchaseTransferEnum::getTransferType)
                    .toArray(String[]::new);
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
                                        Log.d(LOG_TAG, "Matching player ID: " + shPlayer.getId() + " with " + player.getId());
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
                                                newTransfer.setType(typeOfTransferPicker.getText().toString());
                                                newTransfer.setManagerId(managerId);
                                                newTransfer.setNationality(player.getNationality());
                                                String trFee = transferFee.getText().toString().trim().replaceAll(",", "");
                                                newTransfer.setTransferFee((!trFee.isEmpty()) ? Integer.parseInt(trFee) : 0);

                                                if (isLoan) {
                                                    newTransfer.setPlusPlayerName(null);
                                                    Log.d(LOG_TAG, "Transfer is a loan. No additional player involved.");
                                                } else {
                                                    newTransfer.setHasPlusPlayer(hasPlusPlayer);
                                                    String plusPlayerName = playerPicker.getText().toString().trim();
                                                    newTransfer.setPlusPlayerName((!plusPlayerName.isEmpty()) ? plusPlayerName : "");
                                                    newTransfer.setPlusPlayerId(ftPlayerId);
                                                    Log.d(LOG_TAG, "Transfer involves another player: " + plusPlayerName);
                                                }

                                                String wg = wage.getText().toString().trim().replaceAll(",", "");
                                                newTransfer.setWage((!wg.isEmpty()) ? Integer.parseInt(wg) : 0);
                                                String conYears = contractYears.getText().toString().trim();
                                                newTransfer.setContractYears((!conYears.isEmpty()) ? Integer.parseInt(conYears) : 0);
                                                newTransfer.setYear(yearSigned.getText().toString().trim());
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
                                                                        fmPlayer.setYearLeft(yearSigned.getText().toString().trim());
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
                                                ftPlayer.setYearSigned(yearSigned.getText().toString().trim());
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
                                                                boolean isPurchase = checkIfPurchaseType();
                                                                if (isPurchase) {
                                                                    Toast.makeText(context, "Player bought!", Toast.LENGTH_LONG)
                                                                            .show();
                                                                } else {
                                                                    Toast.makeText(context, "Player loaned in!", Toast.LENGTH_LONG)
                                                                            .show();
                                                                }
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
                                                                        transferDialog.dismiss();
                                                                        Intent intent = new Intent(context, ShortlistPlayersActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barPosition", position);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Log.d(LOG_TAG, "Navigating to ShortlistActivity.");
                                                                        transferDialog.dismiss();
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

        private boolean checkIfPurchaseType() {
            String selectedText = typeOfTransferPicker.getText().toString().trim();

            boolean isPurchaseTransfer = false;
            boolean isLoanTransfer = false;

            for (PurchaseTransferEnum transferEnum : PurchaseTransferEnum.values()) {
                if (transferEnum.getTransferType().equals(selectedText)) {
                    isPurchaseTransfer = true;
                    break;
                }
            }

            if (!isPurchaseTransfer) {
                for (LoanEnum loanEnum : LoanEnum.values()) {
                    if (loanEnum.getDescription().equals(selectedText)) {
                        isLoanTransfer = true;
                        break;
                    }
                }
            }

            if (isPurchaseTransfer) {
                return true;
            } else if (isLoanTransfer) {
                return false;
            } else {
                return false;
            }
        }

        private void clickEditPlayerButton(final ShortlistedPlayer player) {
            Log.d(LOG_TAG, "editPlayer called for player: " + player.getFullName());

            createDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.create_shortlisted_player_popup, null);
            createDialog.setContentView(view);

            TextView title = view.findViewById(R.id.create_sh_player);
            final EditText firstName = view.findViewById(R.id.first_name_shp_create);
            final EditText lastName = view.findViewById(R.id.last_name_shp_create);
            final TextView positionPicker = view.findViewById(R.id.position_picker_shp_create);
            final AutoCompleteTextView nationality = view.findViewById(R.id.nationality_shp_create);
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

            ValueFormatter.formatValue(value);
            ValueFormatter.formatValue(wage);

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

            String[] positions = context.getResources().getStringArray(R.array.position_array);
            positionPicker.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(context)
                        .setTitle("Select Position")
                        .setItems(positions, (pickerDialog, which) -> positionPicker.setText(positions[which]))
                        .show();
            });

            firstName.setText(player.getFirstName());
            lastName.setText(player.getLastName());
            positionPicker.setText(player.getPosition());
            teamText.setText(player.getTeam());
            nationality.setText(player.getNationality());
            overall.setText(String.valueOf(player.getOverall()));
            potentialLow.setText(String.valueOf(player.getPotentialLow()));
            potentialHigh.setText(String.valueOf(player.getPotentialHigh()));

            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
            String formattedValue = formatter.format(player.getValue());
            value.setText(formattedValue);

            String formattedWage = formatter.format(player.getWage());
            wage.setText(formattedWage);

            comments.setText(player.getComments());

            createDialog.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionPicker.getText().toString().isEmpty() &&
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
                                            String nationalityPlayer = nationality.getText().toString().trim();
                                            Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
                                            String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            String v = value.getText().toString().trim().replaceAll(",", "");
                                            String w = wage.getText().toString().trim().replaceAll(",", "");
                                            assert documentReference != null;
                                            documentReference.update("firstName", firstName.getText().toString().trim(),
                                                    "lastName", lastName.getText().toString().trim(),
                                                    "fullName", firstName.getText().toString().trim() + " " + lastName.getText().toString().trim(),
                                                    "position", positionPicker.getText().toString().trim(),
                                                    "nationality", nationalityInput,
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
                                                            createDialog.dismiss();
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
