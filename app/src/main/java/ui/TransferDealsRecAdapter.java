package ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.R;
import com.dimxlp.managerdb.TransferDealsActivity;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import enumeration.LoanEnum;
import enumeration.PurchaseTransferEnum;
import enumeration.SaleTransferEnum;
import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.Manager;
import model.Transfer;
import uihelpers.FirstTeamPlayerCreator;
import uihelpers.TransferEditor;
import util.AnimationUtil;
import util.NationalityFlagUtil;
import util.UserApi;
import util.ValueFormatter;

public class TransferDealsRecAdapter extends RecyclerView.Adapter<TransferDealsRecAdapter.ViewHolder> {

    private static final String LOG_TAG = "RAFI|TransferDealsRecAdapter";
    private Context context;
    private List<Transfer> transferList;
    private long managerId;
    private String team;
    private int buttonInt;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference transfersColRef = db.collection("Transfers");
    private CollectionReference managersColRef = db.collection("Managers");
    private CollectionReference ftPlayersColRef = db.collection("FirstTeamPlayers");
    private CollectionReference fmPlayersColRef = db.collection("FormerPlayers");
    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private Animation slideLeft;
    private Animation slideRight;
    private long maxFTPlayerId;
    private boolean needToChangeLoanedOutPlayer = false;

    public TransferDealsRecAdapter(Context context, List<Transfer> transferList, long managerId, String team, int buttonInt) {
        this.context = context;
        this.transferList = transferList;
        this.managerId = managerId;
        this.team = team;
        this.buttonInt = buttonInt;
    }

    @NonNull
    @Override
    public TransferDealsRecAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.transfer_row, parent, false);
        return new TransferDealsRecAdapter.ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull final TransferDealsRecAdapter.ViewHolder holder, int position) {
        findMaxFTPlayerId();
        final Transfer transfer = transferList.get(position);

        holder.playerTopBar.setOnClickListener(v -> {
            boolean isVisible = holder.details.getVisibility() == View.VISIBLE;
            if (isVisible) {
                AnimationUtil.collapseView(holder.details);
            } else {
                AnimationUtil.expandView(holder.details);
            }
        });

        String fullName = transfer.getFullName();
        if (fullName.length() > 16 && fullName.contains(" ")) {
            String[] parts = fullName.split(" ");
            fullName = parts[0].charAt(0) + ". " + parts[1];
        }

        holder.fullNameText.setText(fullName);
        StringBuilder basic = new StringBuilder();
        basic.append(transfer.getPosition());
        basic.append(" · ").append(transfer.getOverall());
        if (transfer.getPotentialLow() != 0 && transfer.getPotentialHigh() != 0) {
            basic.append(" · ").append(transfer.getPotentialLow()).append("–").append(transfer.getPotentialHigh());
        }
        basic.append(" · ");
        holder.basicInfo.setText(basic);

        String nationality = transfer.getNationality();
        String iso = NationalityFlagUtil.getNationalityToIsoMap().getOrDefault(nationality, "un");
        int flagResId = NationalityFlagUtil.getFlagResId(context, iso);

        holder.playerFlag.setImageResource(flagResId);
        holder.playerNationality.setText(nationality);

        String type = transfer.getType();
        String finalText = "";

        if (PurchaseTransferEnum.WITH_TRANSFER_FEE.getTransferType().equals(type)) {
            finalText = "Bought from " + transfer.getFormerTeam();
        } else if (PurchaseTransferEnum.FREE_TRANSFER.getTransferType().equals(type)) {
            finalText = "Free Transfer";
        } else if (SaleTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(type)) {
            finalText = "Transferred to " + transfer.getCurrentTeam();
        } else if (SaleTransferEnum.RELEASE.getDescription().equals(type)) {
            finalText = "Released Player";
        } else if (LoanEnum.SHORT_TERM.getDescription().equals(type)) {
            if (transfer.getCurrentTeam().equals(team)) {
                finalText = "Short-Term Loan from " + transfer.getFormerTeam();
            } else if (transfer.getFormerTeam().equals(team)) {
                finalText = "Short-Term Loan to " + transfer.getCurrentTeam();
            }
        } else if (LoanEnum.ONE_YEAR.getDescription().equals(type)) {
            if (transfer.getCurrentTeam().equals(team)) {
                finalText = "One-Year Loan from " + transfer.getFormerTeam();
            } else if (transfer.getFormerTeam().equals(team)) {
                finalText = "One-Year Loan to " + transfer.getCurrentTeam();
            }
        } else if (LoanEnum.TWO_YEAR.getDescription().equals(type)) {
            if (transfer.getCurrentTeam().equals(team)) {
                finalText = "Two-Year Loan from " + transfer.getFormerTeam();
            } else if (transfer.getFormerTeam().equals(team)) {
                finalText = "Two-Year Loan to " + transfer.getCurrentTeam();
            }
        } else {
            finalText = type;
        }

        holder.transferText.setText(finalText);


        boolean isFormerPlayer = transfer.isFormerPlayer();
        boolean hasTransferFee = transfer.getTransferFee() > 0;
        boolean hasWage = transfer.getWage() > 0 && !isFormerPlayer;
        boolean hasContractYears = transfer.getContractYears() != 0 && !isFormerPlayer;
        boolean hasYear = transfer.getYear() != null && !transfer.getYear().isEmpty();
        boolean hasPlusPlayer = transfer.getPlusPlayerName() != null && !transfer.getPlusPlayerName().isEmpty() && !"None".equals(transfer.getPlusPlayerName());
        boolean noFeeButHasPlusPlayer = !hasTransferFee && hasPlusPlayer
                && !PurchaseTransferEnum.FREE_TRANSFER.getTransferType().equals(type)
                && !SaleTransferEnum.RELEASE.getDescription().equals(type);

        holder.transferFee.setVisibility(hasTransferFee ? View.VISIBLE : View.GONE);
        holder.transferFeeIcon.setVisibility(hasTransferFee ? View.VISIBLE : View.GONE);

        holder.wage.setVisibility(hasWage ? View.VISIBLE : View.GONE);
        holder.wageIcon.setVisibility(hasWage ? View.VISIBLE : View.GONE);

        holder.contractYears.setVisibility(hasContractYears ? View.VISIBLE : View.GONE);
        holder.signedText.setVisibility(hasYear ? View.VISIBLE : View.GONE);

        // Separator logic for financial row (Fee · Wage)
        holder.financeSeparator1.setVisibility((hasTransferFee && hasWage) ? View.VISIBLE : View.GONE);

        // Group: Fee or Wage or Contract
        holder.financialInfoContainer.setVisibility((hasTransferFee || hasWage || hasContractYears) ? View.VISIBLE : View.GONE);

        // Contract Years now grouped with Year Signed
        holder.contractYearsSeparator.setVisibility((hasYear && hasContractYears) ? View.VISIBLE : View.GONE);

        // Set year signed
        holder.signedText.setText(hasYear ? transfer.getYear() : "");
        holder.contractYears.setText(hasContractYears ? transfer.getContractYears() + "y" : "");

        // Set currency and icons
        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Manager manager = queryDocumentSnapshots.getDocuments().get(0).toObject(Manager.class);
                        if (manager != null) {
                            String currency = manager.getCurrency();

                            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
                            if (hasTransferFee) {
                                holder.transferFee.setText(String.format("%s %s", currency, formatter.format(transfer.getTransferFee())));
                                switch (currency) {
                                    case "€":
                                        holder.transferFeeIcon.setImageResource(R.drawable.ic_euros); break;
                                    case "$":
                                        holder.transferFeeIcon.setImageResource(R.drawable.ic_dollars); break;
                                    case "£":
                                        holder.transferFeeIcon.setImageResource(R.drawable.ic_pounds); break;
                                }
                            }

                            if (hasWage) {
                                holder.wage.setText(String.format("%s %s", currency, formatter.format(transfer.getWage())));
                            }
                        }
                    }
                });

        // Handle Plus Player logic (visually aligned with transfer type)
        if (hasPlusPlayer && hasTransferFee) {
            holder.plusPlayerName.setVisibility(View.VISIBLE);
            holder.plusPlayerName.setText("+ " + transfer.getPlusPlayerName());
        } else if (noFeeButHasPlusPlayer) {
            holder.plusPlayerName.setVisibility(View.VISIBLE);
            holder.plusPlayerName.setText("Exchanged for " + transfer.getPlusPlayerName());
        } else {
            holder.plusPlayerName.setVisibility(View.GONE);
        }


        var hasComments = transfer.getComments().isEmpty();
        holder.comments.setVisibility(hasComments ? View.GONE : View.VISIBLE);
        holder.comments.setText(transfer.getComments());

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
        return transferList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout playerTopBar;
        private TextView fullNameText;
        private TextView basicInfo;
        private ImageView playerFlag;
        private TextView playerNationality;
        private TextView transferText;
        private ImageView signedIcon;
        private TextView signedText;
        private LinearLayout financialInfoContainer;
        private ImageView transferFeeIcon;
        private TextView transferFee;
        private TextView financeSeparator1;
        private ImageView wageIcon;
        private TextView wage;
        private TextView contractYearsSeparator;
        private TextView contractYears;
        private TextView plusPlayerName;
        private TextView comments;
        private LinearLayout details;
        private ImageView actionMenu;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            playerTopBar = itemView.findViewById(R.id.player_top_bar_trf);
            fullNameText = itemView.findViewById(R.id.full_name_trf);
            basicInfo = itemView.findViewById(R.id.player_basic_text_trf);
            playerFlag = itemView.findViewById(R.id.player_flag_trf);
            playerNationality = itemView.findViewById(R.id.player_nationality_trf);
            transferText = itemView.findViewById(R.id.transfer_text_trf);
            signedIcon = itemView.findViewById(R.id.transfer_year_icon_trf);
            signedText = itemView.findViewById(R.id.year_trf);
            financialInfoContainer = itemView.findViewById(R.id.player_financial_info_container_trf);
            transferFeeIcon = itemView.findViewById(R.id.transfer_fee_icon_trf);
            transferFee = itemView.findViewById(R.id.transfer_fee_trf);
            financeSeparator1 = itemView.findViewById(R.id.finance_separator1_trf);
            wageIcon = itemView.findViewById(R.id.wage_icon_trf);
            wage = itemView.findViewById(R.id.wage_trf);
            contractYearsSeparator = itemView.findViewById(R.id.year_contract_separator);
            contractYears = itemView.findViewById(R.id.contract_years_trf);
            plusPlayerName = itemView.findViewById(R.id.plus_player_trf);
            comments = itemView.findViewById(R.id.comments_trf);
            details = itemView.findViewById(R.id.expandable_section_trf);
            actionMenu = itemView.findViewById(R.id.player_action_menu_trf);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (details.getVisibility() == View.GONE) {
                        details.setVisibility(View.VISIBLE);
                    } else if (details.getVisibility() == View.VISIBLE) {
                        details.setVisibility(View.GONE);
                    }
                }
            });

            actionMenu.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(context, actionMenu);
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.transfer_deals_action_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
//                        case R.id.action_edit:
//                            clickEditTransferButton(transferList.get(getAdapterPosition()));
//                            return true;
                        case R.id.action_delete:
                            clickDeleteTransferButton();
                            return true;
                        default:
                            return false;
                    }
                });

                popupMenu.show();
            });
        }

        private void clickDeleteTransferButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            deleteTransfer(transferList.get(getAdapterPosition()));
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Do you want to delete this transfer deal?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }

        private void deleteTransfer(final Transfer transfer) {
            Log.d(LOG_TAG, "deleteTransfer called for transfer ID: " + transfer.getId());

            transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "Successfully fetched Transfers collection.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    Transfer deletedTransfer = ds.toObject(Transfer.class);
                                    if (deletedTransfer.getId() == transfer.getId()) {
                                        documentReference = transfersColRef.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Transfer successfully deleted. ID: " + transfer.getId());
                                                Toast.makeText(context, "Transfer deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                Intent intent = new Intent(context, TransferDealsActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                context.startActivity(intent);
                                                ((Activity)context).finish();
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error deleting transfer. ID: " + transfer.getId(), e));
                            } else {
                                Log.e(LOG_TAG, "Error fetching Transfers collection.", task.getException());
                            }
                        }
                    });
        }

//        private void clickEditTransferButton(final Transfer transfer) {
//            Log.d(LOG_TAG, "editTransfer called for transfer ID: " + transfer.getId());
//
//            if (transfer.getPlusPlayerName() != null && !"None".equals(transfer.getPlusPlayerName()) && !transfer.getPlusPlayerName().isEmpty()) {
//                Log.w(LOG_TAG, "Editing transfers with former players involved is not yet supported.");
//                Toast.makeText(context, "Editing transfers with former players involved is not yet supported.", Toast.LENGTH_LONG).show();
//                return;
//            }
//
//            if (transfer.getExchangePlayerName() != null && !"None".equals(transfer.getExchangePlayerName()) && !transfer.getExchangePlayerName().isEmpty()) {
//                Log.w(LOG_TAG, "Editing transfers with exchange players is not yet supported.");
//                Toast.makeText(context, "Editing transfers with exchange players is not yet supported.", Toast.LENGTH_LONG).show();
//                return;
//            }
//
//            BottomSheetDialog editDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
//            View view = LayoutInflater.from(context)
//                    .inflate(R.layout.edit_transfer_deal_popup, null);
//            editDialog.setContentView(view);
//
//            TextInputLayout feeInputLayout = view.findViewById(R.id.fee_til_trf_edit);
//            TextInputLayout wageInputLayout = view.findViewById(R.id.wage_til_trf_edit);
//
//            managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                    .whereEqualTo("id", managerId)
//                    .get()
//                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                        @Override
//                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                            if (!queryDocumentSnapshots.isEmpty()) {
//                                List<Manager> managerList = new ArrayList<>();
//                                for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
//                                    Manager manager = doc.toObject(Manager.class);
//                                    managerList.add(manager);
//                                }
//                                Manager manager = managerList.get(0);
//                                String currency = manager.getCurrency();
//                                feeInputLayout.setHint("Transfer Fee (in " + currency + ")");
//                                wageInputLayout.setHint("Wage (in " + currency + ")");
//                            }
//                        }
//                    });
//
//            TransferEditor transferEditor = new TransferEditor(view);
//
//            transferEditor.formatValue(transferEditor.getFeeEdit());
//            transferEditor.formatValue(transferEditor.getWageEdit());
//
//            setEditTransferFields(transfer, transferEditor);
//            setFieldsBasedOnTransferType(transfer.getType(), transferEditor, transfer);
//
//            editDialog.show();
//
//            boolean wasExchangeTransfer = transferEditor.getPlayerExchangeSwitch().isChecked();
//            boolean wasPlusPlayerTransfer = !transferEditor.getPlusPlayerPickerEdit().toString().isEmpty();
//
//            ArrayAdapter<String> transferAdapter = populateTransferAdapter(transfer);
//
//            transferEditor.getTypeOfTransferPickerEdit().setOnClickListener(v -> {
//                List<String> transferTypesList = new ArrayList<>();
//                for (int i = 0; i < transferAdapter.getCount(); i++) {
//                    transferTypesList.add(transferAdapter.getItem(i));
//                }
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                builder.setTitle("Select Transfer Type");
//
//                builder.setItems(transferTypesList.toArray(new String[0]), (transferDialog, which) -> {
//                    String selectedType = transferTypesList.get(which);
//                    transferEditor.getTypeOfTransferPickerEdit().setText(selectedType);
//                    Log.d(LOG_TAG, "Transfer type changed to: " + selectedType);
//                    setFieldsBasedOnTransferType(selectedType, transferEditor, transfer);
//                });
//
//                builder.show();
//            });
//
//
//            transferEditor.getPlayerExchangeSwitch().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    Log.d(LOG_TAG, "Player exchange switch changed. IsChecked: " + isChecked);
//                    transferEditor.setIsExchangePlayer(isChecked);
//                    transfer.setHasPlayerExchange(isChecked);
//                }
//            });
//
//            String initialPlusPlayerName = transfer.getPlusPlayerName();
//            long initialPlusPlayerId = transfer.getPlusPlayerId();
//
//            populatePlusPlayerDropdown(transfer.getPlusPlayerName(), plusPlayersList -> {
//                String[] playerNames = plusPlayersList.stream()
//                        .map(FirstTeamPlayer::getFullName)
//                        .toArray(String[]::new);
//
//                transferEditor.getPlusPlayerPickerEdit().setOnClickListener(v -> {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                    builder.setTitle("Select Plus Player");
//
//                    builder.setItems(playerNames, (plusPlayerDialog, which) -> {
//                        FirstTeamPlayer selected = plusPlayersList.get(which);
//
//                        transferEditor.getPlusPlayerPickerEdit().setText(selected.getFullName());
//                        transferEditor.setPlayerSpinnerId(selected.getId());
//                        transferEditor.setIsPlusPlayer(which > 0);
//                        transferEditor.setPlusPlayerName(which > 0 ? selected.getFullName() : null);
//
//                        Log.d(LOG_TAG, "Plus player selected: " + selected.getFullName());
//                    });
//
//                    builder.show();
//                });
//            });
//
//            transferEditor.getEditTransferButton().setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Log.d(LOG_TAG, "Edit transfer button clicked.");
//                    if (!transferEditor.getLastNameEdit().getText().toString().isEmpty()
//                        && !transferEditor.getNationalityEdit().getText().toString().isEmpty()
//                        && !transferEditor.getPositionPickerEdit().getText().toString().isEmpty()
//                        && !transferEditor.getOverallEdit().getText().toString().isEmpty()
//                        && !transferEditor.getTypeOfTransferPickerEdit().getText().toString().isEmpty()
//                        && !transferEditor.getOldTeamEdit().getText().toString().isEmpty()
//                        && !transferEditor.getNewTeamEdit().getText().toString().isEmpty()
//                        && !transferEditor.getYearEdit().getText().toString().equals("0")) {
//                        Log.d(LOG_TAG, "All required fields validated. Fetching transfer document for update.");
//
//                        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                                .whereEqualTo("managerId", managerId)
//                                .get()
//                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                                    @Override
//                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                        if (task.isSuccessful()) {
//                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
//                                            DocumentReference documentReference = null;
//                                            for (DocumentSnapshot ds : doc) {
//                                                Transfer editedTransfer = ds.toObject(Transfer.class);
//                                                if (editedTransfer.getId() == transfer.getId()) {
//                                                    documentReference = transfersColRef.document(ds.getId());
//                                                    break;
//                                                }
//                                            }
//                                            if (documentReference == null) {
//                                                Log.d(LOG_TAG, "Document reference found for transfer ID: " + transfer.getId());
//                                                Toast.makeText(context, "Failed to find the transfer document.", Toast.LENGTH_LONG).show();
//                                                return;
//                                            }
//                                            assignNewTransferValues(transferEditor, documentReference);
//
//                                            if (transferEditor.isExchangePlayer()) {
//                                                Log.d(LOG_TAG, "Transfer involves exchange player.");
//                                                addExchangePlayer(documentReference);
////                                            } else if (wasExchangeTransfer) {
////                                                Log.d(LOG_TAG, "Transfer previously involved exchange player.");
////                                                if (transferEditor.isExchangePlayer()) {
////                                                    askToChangeExchangePlayer(documentReference, transfer);
////                                                } else {
////                                                    askToRemoveExchangePlayer(transfer, transferEditor);
////                                                }
////                                            } else if (wasPlusPlayerTransfer) {
////                                                Log.d(LOG_TAG, "Transfer previously involved plus player.");
////                                                if (transferEditor.isPlusPlayer()) {
////                                                    if (!initialPlusPlayerName.equals(transferEditor.getPlusPlayerName())) {
////                                                        askToChangePlusPlayer(transferEditor, initialPlusPlayerId, transfer);
////                                                    }
////                                                } else {
////                                                    askToRejoinPlusPlayer(initialPlusPlayerId, transfer, transferEditor);
////                                                }
//                                            } else {
//                                                Log.d(LOG_TAG, "Finalizing transfer update.");
//                                                Intent intent = new Intent(context, TransferDealsActivity.class);
//                                                intent.putExtra("managerId", managerId);
//                                                intent.putExtra("team", team);
//                                                context.startActivity(intent);
//                                                ((Activity) context).finish();
//
//                                                if (needToChangeLoanedOutPlayer) {
//                                                    Toast.makeText(context, "Transfer updated! You should also edit the LoanedOut player!", Toast.LENGTH_LONG).show();
//                                                } else {
//                                                    Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG).show();
//                                                }
//                                            }
//                                        }
//                                    }
//                                })
//                                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error fetching transfer document.", e));
//
//                    } else {
//                        Log.w(LOG_TAG, "Validation failed. Required fields are missing.");
//                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Type of Transfer, Old Team, New Team and Year Signed/Left are required", Toast.LENGTH_LONG)
//                                .show();
//                    }
//
//                }
//            });
//        }
    }

//    private void askToRejoinPlusPlayer(long initialPlusPlayerId, Transfer transfer, TransferEditor transferEditor) {
//        Log.d(LOG_TAG, "askToRejoinPlusPlayer called for transfer ID: " + transfer.getId() + ", initialPlusPlayerId: " + initialPlusPlayerId);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setMessage("This action will result in the former player who was part of this transfer rejoining the first team. Do you want to continue?")
//                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Log.d(LOG_TAG, "User confirmed to rejoin former player with ID: " + initialPlusPlayerId);
//                        rejoinFormerPlayer(initialPlusPlayerId, transfer, new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void unused) {
//                                Log.d(LOG_TAG, "Former player successfully rejoined the first team.");
//                            }
//                        });
//                    }
//                })
//                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .show();
//    }

//    private void askToChangePlusPlayer(TransferEditor transferEditor, long initialPlusPlayerId, Transfer transfer) {
//        Log.d(LOG_TAG, "askToChangePlusPlayer called for transfer ID: " + transfer.getId() + ", initialPlusPlayerId: " + initialPlusPlayerId);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setMessage("Do you want to change the player that was part of this transfer?")
//                .setPositiveButton("Yes", (dialog, which) -> {
//                    Log.d(LOG_TAG, "User confirmed to change plus player.");
//
//                    // Move the current plus player to FormerPlayers
//                    letPlayerLeave(transferEditor, new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//                            Log.d(LOG_TAG, "Current plus player successfully moved to FormerPlayers.");
//
//                            // Rejoin the former plus player
//                            rejoinFormerPlayer(initialPlusPlayerId, transfer, new OnSuccessListener<Void>() {
//                                @Override
//                                public void onSuccess(Void aVoid) {
//                                    Log.d(LOG_TAG, "Former player successfully rejoined the first team.");
//                                    updateTransferDocument(transfer, transferEditor);
//                                    Log.d(LOG_TAG, "Transfer document successfully updated with new plus player.");
//                                }
//                            });
//                        }
//                    });
//                })
//                .setNegativeButton("No", (dialog, which) -> {
//                    Log.d(LOG_TAG, "User canceled changing plus player.");
//                    dialog.dismiss();
//                })
//                .show();
//    }

//    private void rejoinFormerPlayer(long initialPlusPlayerId, Transfer transfer, OnSuccessListener<Void> onSuccessListener) {
//        Log.d(LOG_TAG, "rejoinFormerPlayer called for transfer ID: " + transfer.getId() + ", initialPlusPlayerId: " + initialPlusPlayerId);
//
//        fmPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                .whereEqualTo("managerId", managerId)
//                .whereEqualTo("firstTeamId", initialPlusPlayerId)
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    if (!queryDocumentSnapshots.isEmpty()) {
//                        Log.d(LOG_TAG, "Former player documents found for rejoining: " + queryDocumentSnapshots.size());
//                        DocumentSnapshot latestDocument = queryDocumentSnapshots.getDocuments()
//                                .stream()
//                                .max((d1, d2) -> {
//                                    Timestamp t1 = d1.toObject(FormerPlayer.class).getTimeAdded();
//                                    Timestamp t2 = d2.toObject(FormerPlayer.class).getTimeAdded();
//                                    return t1.compareTo(t2);
//                                }).orElse(null);
//
//                        if (latestDocument != null) {
//                            Log.d(LOG_TAG, "Processing latest FormerPlayer document for rejoining.");
//                            processFormerPlayerDocument(latestDocument, transfer, onSuccessListener);
//                        } else {
//                            Log.w(LOG_TAG, "No latest document found for rejoining.");
//                            onSuccessListener.onSuccess(null);
//                        }
//                    } else {
//                        Log.w(LOG_TAG, "No FormerPlayer documents found for rejoining.");
//                        onSuccessListener.onSuccess(null);
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.w(LOG_TAG, "No FormerPlayer documents found for rejoining.");
//                    onSuccessListener.onSuccess(null);
//                });
//    }

//    private void processFormerPlayerDocument(DocumentSnapshot document, Transfer transfer, OnSuccessListener<Void> onSuccessListener) {
//        Log.d(LOG_TAG, "processFormerPlayerDocument called for document ID: " + document.getId());
//
//        FormerPlayer formerPlayer = document.toObject(FormerPlayer.class);
//        if (formerPlayer != null) {
//            Log.d(LOG_TAG, "FormerPlayer object retrieved: " + formerPlayer.getFullName());
//
//            // Remove from FormerPlayers
//            fmPlayersColRef.document(document.getId()).delete()
//                    .addOnSuccessListener(aVoid -> {
//                        Log.d(LOG_TAG, "FormerPlayer successfully deleted from collection.");
//
//                        // Add the player back to FirstTeamPlayers
//                        addRejoinedPlayerToFirstTeam(formerPlayer, transfer, onSuccessListener);
//                    })
//                    .addOnFailureListener(e -> {
//                        Log.e(LOG_TAG, "Failed to delete FormerPlayer document.", e);
//                        onSuccessListener.onSuccess(null);
//                    });
//        } else {
//            Log.w(LOG_TAG, "FormerPlayer object is null.");
//            onSuccessListener.onSuccess(null);
//        }
//    }

//    private void addRejoinedPlayerToFirstTeam(FormerPlayer formerPlayer, Transfer transfer, OnSuccessListener<Void> onSuccessListener) {
//        Log.d(LOG_TAG, "addRejoinedPlayerToFirstTeam called for FormerPlayer: " + formerPlayer.getFullName());
//
//        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                .whereEqualTo("managerId", managerId)
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    long maxId = queryDocumentSnapshots.getDocuments().stream()
//                            .map(doc -> doc.toObject(FirstTeamPlayer.class))
//                            .mapToLong(FirstTeamPlayer::getId)
//                            .max()
//                            .orElse(0);
//                    Log.d(LOG_TAG, "Max ID in FirstTeamPlayers: " + maxId);
//
//                    FirstTeamPlayer rejoinedPlayer = new FirstTeamPlayer();
//                    rejoinedPlayer.setId(maxId + 1);
//                    rejoinedPlayer.setFirstName(formerPlayer.getFirstName());
//                    rejoinedPlayer.setLastName(formerPlayer.getLastName());
//                    rejoinedPlayer.setFullName(formerPlayer.getFullName());
//                    rejoinedPlayer.setPosition(formerPlayer.getPosition());
//                    rejoinedPlayer.setNumber(formerPlayer.getNumber());
//                    rejoinedPlayer.setNationality(formerPlayer.getNationality());
//                    rejoinedPlayer.setOverall(formerPlayer.getOverall());
//                    rejoinedPlayer.setPotentialLow(formerPlayer.getPotentialLow());
//                    rejoinedPlayer.setPotentialHigh(formerPlayer.getPotentialHigh());
//                    rejoinedPlayer.setYearSigned(formerPlayer.getYearSigned());
//                    rejoinedPlayer.setYearScouted(formerPlayer.getYearScouted());
//                    rejoinedPlayer.setTeam(team);
//                    rejoinedPlayer.setUserId(UserApi.getInstance().getUserId());
//                    rejoinedPlayer.setManagerId(managerId);
//                    rejoinedPlayer.setTimeAdded(new Timestamp(new Date()));
//
//                    ftPlayersColRef.add(rejoinedPlayer)
//                            .addOnSuccessListener(aVoid -> {
//                                Log.d(LOG_TAG, "Player successfully rejoined the First Team: " + formerPlayer.getFullName());
//                                onSuccessListener.onSuccess(null);
//                            })
//                            .addOnFailureListener(e -> {
//                                Log.e(LOG_TAG, "Failed to add player to First TeamPlayers collection.", e);
//                                onSuccessListener.onSuccess(null);
//                            });
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(LOG_TAG, "Error fetching FirstTeamPlayers collection.", e);
//                    onSuccessListener.onSuccess(null);
//                });
//    }

//    private void updateTransferDocument(Transfer transfer, TransferEditor transferEditor) {
//        Log.d(LOG_TAG, "updateTransferDocument called for transfer ID: " + transfer.getId());
//
//        transfersColRef.whereEqualTo("id", transfer.getId())
//                .whereEqualTo("managerId", managerId)
//                .whereEqualTo("userId", UserApi.getInstance().getUserId())
//                .get()
//                .addOnSuccessListener(transferSnapshots -> {
//                    if (!transferSnapshots.isEmpty()) {
//                        Log.d(LOG_TAG, "Transfer documents found for update.");
//
//                        for (DocumentSnapshot doc : transferSnapshots) {
//                            DocumentReference transferDocRef = transfersColRef.document(doc.getId());
//                            transferDocRef.update(
//                                    "plusPlayerName", transferEditor.getPlusPlayerPickerEdit().getSelectedItem().toString().trim(),
//                                    "plusPlayerId", transferEditor.getPlayerSpinnerId(),
//                                    "hasPlusPlayer", true
//                            ).addOnSuccessListener(aVoid -> {
//                                Log.d(LOG_TAG, "Transfer document successfully updated.");
//
//                                 // Navigate back to the TransferDealsActivity
//                                Intent intent = new Intent(context, TransferDealsActivity.class);
//                                intent.putExtra("managerId", managerId);
//                                intent.putExtra("team", team);
//                                context.startActivity(intent);
//                                ((Activity) context).finish();
//                            }).addOnFailureListener(e -> {
//                                Log.e(LOG_TAG, "Error updating transfer document.", e);
//                            });
//                        }
//                    } else {
//                        Log.w(LOG_TAG, "No transfer documents found for update.");
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(LOG_TAG, "Error fetching transfer documents.", e);
//                });
//    }

//    private void askToRemoveExchangePlayer(Transfer transfer, TransferEditor transferEditor) {
//        Log.d(LOG_TAG, "askToRemoveExchangePlayer called for transfer ID: " + transfer.getId());
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setMessage("Unchecking the exchange player toggle will remove the exchange player. Do you want to continue?")
//                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Log.d(LOG_TAG, "User confirmed to remove the exchange player for transfer ID: " + transfer.getId());
//                        removeOldExchangePlayer(transfer, new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                Log.d(LOG_TAG, "Exchange player removed successfully.");
//
//                                transfer.setExchangePlayerName(null);
//                                transfer.setExchangePlayerId(0);
//                                transfer.setHasPlayerExchange(false);
//                                transferEditor.setIsExchangePlayer(false);
//
//                                transfersColRef.whereEqualTo("id", transfer.getId())
//                                        .whereEqualTo("managerId", managerId)
//                                        .whereEqualTo("userId", UserApi.getInstance().getUserId())
//                                        .get()
//                                        .addOnSuccessListener(queryDocumentSnapshots -> {
//                                            if (!queryDocumentSnapshots.isEmpty()) {
//                                                for (DocumentSnapshot document : queryDocumentSnapshots) {
//                                                    DocumentReference transferDocRef = transfersColRef.document(document.getId());
//                                                    transferDocRef.update(
//                                                            "exchangePlayerName", null,
//                                                            "exchangePlayerId", 0,
//                                                            "hasPlayerExchange", false
//                                                    ).addOnSuccessListener(aVoid1 -> {
//                                                        Log.d(LOG_TAG, "Transfer document updated successfully.");
//                                                        Toast.makeText(context, "Exchange player removed and transfer updated successfully!", Toast.LENGTH_LONG).show();
//
//                                                        Intent intent = new Intent(context, TransferDealsActivity.class);
//                                                        intent.putExtra("managerId", managerId);
//                                                        intent.putExtra("team", team);
//                                                        context.startActivity(intent);
//                                                        ((Activity) context).finish();
//                                                    }).addOnFailureListener(e -> {
//                                                        Log.e(LOG_TAG, "Failed to update transfer document.", e);
//                                                        Toast.makeText(context, "Failed to update transfer document", Toast.LENGTH_LONG).show();
//                                                    });
//                                                }
//                                            } else {
//                                                Log.w(LOG_TAG, "No transfer document found for update.");
//                                                Toast.makeText(context, "Transfer document not found", Toast.LENGTH_LONG).show();
//                                            }
//                                        });
//                            }
//                        });
//                    }
//                })
//                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Log.d(LOG_TAG, "User canceled removing exchange player.");
//                        dialog.dismiss();
//                        // Reset the toggle back to checked if the user cancels
////                                                                    buttonView.setChecked(true);
//                    }
//                })
//                .show();
//    }

//    private void askToChangeExchangePlayer(DocumentReference finalDocumentReference, Transfer transfer) {
//        Log.d(LOG_TAG, "askToChangeExchangePlayer called for transfer ID: " + transfer.getId());
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setMessage("Do you want to change the player that was exchanged?")
//                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Log.d(LOG_TAG, "User confirmed to change exchange player.");
//                        removeOldExchangePlayer(transfer, new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                Log.d(LOG_TAG, "Old exchange player removed successfully.");
//                                addNewExchangePlayer(finalDocumentReference);
//                            }
//                        });
//                    }
//                })
//                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Log.d(LOG_TAG, "User canceled changing exchange player.");
//                        dialog.dismiss();
//                    }
//                })
//                .show();
//    }

//    private void addNewExchangePlayer(DocumentReference transferDocRef) {
//        Log.d(LOG_TAG, "addNewExchangePlayer called.");
//
//        builder = new AlertDialog.Builder(context);
//        View view = LayoutInflater.from(context)
//                .inflate(R.layout.create_first_team_player_popup, null);
//        builder.setView(view);
//        builder.setCancelable(false);
//
//        FirstTeamPlayerCreator firstTeamPlayerCreator = new FirstTeamPlayerCreator(view);
//
//        firstTeamPlayerCreator.changePropertiesForExchange();
//
//        String[] years = context.getResources().getStringArray(R.array.years_array);
//        firstTeamPlayerCreator.getYearSigned().setOnClickListener(v -> {
//            new AlertDialog.Builder(context)
//                    .setTitle("Select Year Signed")
//                    .setItems(years, (dialog, which) -> firstTeamPlayerCreator.getYearSigned().setText(years[which]))
//                    .show();
//        });
//
//        firstTeamPlayerCreator.getSavePlayerButton().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(LOG_TAG, "Save player button clicked for exchange player.");
//
//                if (!firstTeamPlayerCreator.getLastName().getText().toString().isEmpty()
//                        && !firstTeamPlayerCreator.getNationality().getText().toString().isEmpty()
//                        && !firstTeamPlayerCreator.getPosition().getText().toString().isEmpty()
//                        && !firstTeamPlayerCreator.getOverall().getText().toString().isEmpty()
//                        && !firstTeamPlayerCreator.getYearSigned().getText().toString().isEmpty()) {
//                    createNewExchangePlayer(firstTeamPlayerCreator, transferDocRef);
//                } else {
//                    Log.w(LOG_TAG, "Validation failed. Required fields are missing.");
//                    Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
//                            .show();
//                }
//            }
//
//        });
//
//        dialog = builder.create();
//        dialog.setCanceledOnTouchOutside(false);
//        dialog.show();
//    }

//    private void createNewExchangePlayer(FirstTeamPlayerCreator firstTeamPlayerCreator, DocumentReference transferDocRef) {
//        Log.d(LOG_TAG, "createNewExchangePlayer called.");
//
//        String firstNamePlayer = firstTeamPlayerCreator.getFirstName().getText().toString().trim();
//        String lastNamePlayer = firstTeamPlayerCreator.getLastName().getText().toString().trim();
//        String fullNamePlayer;
//        if (!firstNamePlayer.isEmpty()) {
//            fullNamePlayer = firstNamePlayer + " " + lastNamePlayer;
//        } else {
//            fullNamePlayer = lastNamePlayer;
//        }
//        String positionPlayer = firstTeamPlayerCreator.getPosition().getText().toString().trim();
//        String numberPlayer = firstTeamPlayerCreator.getNumber().getText().toString().trim();
//        String nationalityPlayer = firstTeamPlayerCreator.getNationality().getText().toString().trim();
//        String overallPlayer = firstTeamPlayerCreator.getOverall().getText().toString().trim();
//        String potentialLowPlayer = firstTeamPlayerCreator.getPotentialLow().getText().toString().trim();
//        String potentialHiPlayer = firstTeamPlayerCreator.getPotentialHigh().getText().toString().trim();
//        final String ySignedPlayer = firstTeamPlayerCreator.getYearSigned().getText().toString().trim();
//
//        FirstTeamPlayer player = new FirstTeamPlayer();
//
//        player.setId(++maxFTPlayerId);
//        player.setFirstName(firstNamePlayer);
//        player.setLastName(lastNamePlayer);
//        player.setFullName(fullNamePlayer);
//        player.setPosition(positionPlayer);
//        if (!numberPlayer.isEmpty()) {
//            player.setNumber(Integer.parseInt(numberPlayer));
//        } else {
//            player.setNumber(99);
//        }
//        player.setTeam(team);
//        player.setNationality(nationalityPlayer);
//        player.setOverall(Integer.parseInt(overallPlayer));
//        if (!potentialLowPlayer.isEmpty()) {
//            player.setPotentialLow(Integer.parseInt(potentialLowPlayer));
//        }
//        if (!potentialHiPlayer.isEmpty()) {
//            player.setPotentialHigh(Integer.parseInt(potentialHiPlayer));
//        }
//        player.setYearSigned(ySignedPlayer);
//        player.setYearScouted("0");
//        player.setUserId(UserApi.getInstance().getUserId());
//        player.setTimeAdded(new Timestamp(new Date()));
//        player.setManagerId(managerId);
//        player.setLoanPlayer(false);
//        Log.d(LOG_TAG, "Adding new exchange player: " + fullNamePlayer);
//
//        ftPlayersColRef.add(player)
//                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentReference> task) {
//                        if (task.isSuccessful()) {
//                            Log.d(LOG_TAG, "New exchange player added successfully.");
//                            Intent intent = new Intent(context, TransferDealsActivity.class);
//                            intent.putExtra("managerId", managerId);
//                            intent.putExtra("team", team);
//                            context.startActivity(intent);
//                            ((Activity) context).finish();
//                            Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG).show();
//                        }
//                    }
//                });
//
//        transferDocRef.update("exchangePlayerId", player.getId(), "exchangePlayerName", fullNamePlayer, "hasPlayerExchange", true)
//                .addOnSuccessListener(aVoid -> {
//                    Log.d(LOG_TAG, "Transfer document updated with new exchange player.");
//                    Toast.makeText(context, "Transfer updated with new exchanged player!", Toast.LENGTH_LONG).show();
//                    Intent intent = new Intent(context, TransferDealsActivity.class);
//                    intent.putExtra("managerId", managerId);
//                    intent.putExtra("team", team);
//                    context.startActivity(intent);
//                    ((Activity) context).finish();
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(LOG_TAG, "Failed to update transfer document with exchange player.", e);
//                });
//    }

//    private void removeOldExchangePlayer(Transfer transfer, OnSuccessListener<Void> onSuccessListener) {
//        Log.d(LOG_TAG, "removeOldExchangePlayer called for exchange player ID: " + transfer.getExchangePlayerId());
//
//        long exchangePlayerId = transfer.getExchangePlayerId();
//
//        ftPlayersColRef.whereEqualTo("managerId", managerId)
//                .whereEqualTo("userId", UserApi.getInstance().getUserId())
//                .whereEqualTo("id", exchangePlayerId)
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    if (!queryDocumentSnapshots.isEmpty()) {
//                        for (DocumentSnapshot document : queryDocumentSnapshots) {
//                            ftPlayersColRef.document(document.getId()).delete()
//                                    .addOnSuccessListener(aVoid -> {
//                                        Log.d(LOG_TAG, "Exchange player removed successfully.");
//                                        onSuccessListener.onSuccess(null);
//                                    })
//                                    .addOnFailureListener(e -> {
//                                        Log.e(LOG_TAG, "Failed to remove exchange player.", e);
//                                        onSuccessListener.onSuccess(null); // Proceed even on failure
//                                    });
//                        }
//                    } else {
//                        Log.w(LOG_TAG, "No exchange player document found for removal.");
//                        onSuccessListener.onSuccess(null);
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(LOG_TAG, "Error fetching exchange player documents.", e);
//                    onSuccessListener.onSuccess(null);
//                });
//    }

//    private void letPlayerLeave(TransferEditor transferEditor, OnSuccessListener<Void> onSuccessListener) {
//        Log.d(LOG_TAG, "letPlayerLeave called for player ID: " + transferEditor.getPlayerSpinnerId());
//
//        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                .whereEqualTo("managerId", managerId)
//                .whereEqualTo("id", transferEditor.getPlayerSpinnerId())
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful() && task.getResult() != null) {
//                        Log.d(LOG_TAG, "Query successful. Processing FirstTeamPlayer documents.");
//                        for (DocumentSnapshot ds : task.getResult().getDocuments()) {
//                            FirstTeamPlayer player = ds.toObject(FirstTeamPlayer.class);
//                            if (player != null) {
//                                DocumentReference documentReference = ftPlayersColRef.document(ds.getId());
//
//                                // Check for existing FormerPlayer documents with the same firstTeamId
//                                fmPlayersColRef.whereEqualTo("firstTeamId", player.getId())
//                                        .get()
//                                        .addOnSuccessListener(queryDocumentSnapshots -> {
//                                            Log.d(LOG_TAG, "Removing duplicates from FormerPlayers for player ID: " + player.getId());
//
//                                            // Delete all duplicates
//                                            for (DocumentSnapshot formerDoc : queryDocumentSnapshots) {
//                                                fmPlayersColRef.document(formerDoc.getId()).delete();
//                                            }
//
//                                            Log.d(LOG_TAG, "Adding player to FormerPlayers.");
//                                            // Add player to FormerPlayers
//                                            FormerPlayer fmPlayer = new FormerPlayer();
//                                            fmPlayer.setId(0);
//                                            fmPlayer.setFirstName(player.getFirstName());
//                                            fmPlayer.setLastName(player.getLastName());
//                                            fmPlayer.setFullName(player.getFullName());
//                                            fmPlayer.setPosition(player.getPosition());
//                                            fmPlayer.setNumber(player.getNumber());
//                                            fmPlayer.setNationality(player.getNationality());
//                                            fmPlayer.setOverall(player.getOverall());
//                                            fmPlayer.setPotentialLow(player.getPotentialLow());
//                                            fmPlayer.setPotentialHigh(player.getPotentialHigh());
//                                            fmPlayer.setYearSigned(player.getYearSigned());
//                                            fmPlayer.setYearScouted(player.getYearScouted());
//                                            fmPlayer.setYearLeft(transferEditor.getYearEdit().getSelectedItem().toString().trim());
//                                            fmPlayer.setManagerId(managerId);
//                                            fmPlayer.setUserId(UserApi.getInstance().getUserId());
//                                            fmPlayer.setTimeAdded(new Timestamp(new Date()));
//                                            fmPlayer.setFirstTeamId(player.getId());
//
//                                            fmPlayersColRef.add(fmPlayer)
//                                                    .addOnSuccessListener(aVoid -> {
//                                                        Log.d(LOG_TAG, "Player successfully added to FormerPlayers. Deleting from FirstTeamPlayers.");
//
//                                                        // Delete from FirstTeamPlayers
//                                                        documentReference.delete()
//                                                                .addOnSuccessListener(onSuccessListener)
//                                                                .addOnFailureListener(e -> {
//                                                                    Log.e(LOG_TAG, "Failed to delete player from FirstTeamPlayers.", e);
//                                                                    onSuccessListener.onSuccess(null); // Proceed even if delete fails
//                                                                });
//                                                    })
//                                                    .addOnFailureListener(e -> {
//                                                        Log.e(LOG_TAG, "Failed to add player to FormerPlayers.", e);
//                                                        onSuccessListener.onSuccess(null); // Proceed even if add fails
//                                                    });
//                                        });
//                            }
//                        }
//                    } else {
//                        Log.w(LOG_TAG, "No matching FirstTeamPlayer found.");
//                        onSuccessListener.onSuccess(null); // No matching player, but proceed
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(LOG_TAG, "Error querying FirstTeamPlayers.", e);
//                    onSuccessListener.onSuccess(null); // Proceed even on query failure
//                });
//    }

//    private void addExchangePlayer(DocumentReference documentReference) {
//        Log.d(LOG_TAG, "addExchangePlayer called.");
//
//        builder = new AlertDialog.Builder(context);
//        View view = LayoutInflater.from(context)
//                .inflate(R.layout.create_first_team_player_popup, null);
//
//        builder.setView(view);
//        builder.setCancelable(false);
//
//        FirstTeamPlayerCreator firstTeamPlayerCreator = new FirstTeamPlayerCreator(view);
//
//        firstTeamPlayerCreator.changePropertiesForExchange();
//
//        String[] years = context.getResources().getStringArray(R.array.years_array);
//        firstTeamPlayerCreator.getYearSigned().setOnClickListener(v -> {
//            new AlertDialog.Builder(context)
//                    .setTitle("Select Year Signed")
//                    .setItems(years, (dialog, which) -> firstTeamPlayerCreator.getYearSigned().setText(years[which]))
//                    .show();
//        });
//
//        firstTeamPlayerCreator.getSavePlayerButton().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(LOG_TAG, "Save player button clicked for exchange player.");
//
//                if (!firstTeamPlayerCreator.getLastName().getText().toString().isEmpty()
//                    && !firstTeamPlayerCreator.getNationality().getText().toString().isEmpty()
//                    && !firstTeamPlayerCreator.getPosition().getText().toString().isEmpty()
//                    && !firstTeamPlayerCreator.getOverall().getText().toString().isEmpty()
//                    && !firstTeamPlayerCreator.getYearSigned().getText().toString().isEmpty()) {
//                    createPlayer(firstTeamPlayerCreator, documentReference);
//                } else {
//                    Log.w(LOG_TAG, "Validation failed for exchange player. Required fields missing.");
//                    Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
//                            .show();
//                }
//            }
//
//        });
//
//        dialog = builder.create();
//        dialog.setCanceledOnTouchOutside(false);
//        dialog.show();
//    }

//    private void createPlayer(FirstTeamPlayerCreator firstTeamPlayerCreator, DocumentReference documentReference) {
//        Log.d(LOG_TAG, "createPlayer called for new exchange player.");
//
//        String firstNamePlayer = firstTeamPlayerCreator.getFirstName().getText().toString().trim();
//        String lastNamePlayer = firstTeamPlayerCreator.getLastName().getText().toString().trim();
//        String fullNamePlayer;
//        if (!firstNamePlayer.isEmpty()) {
//            fullNamePlayer = firstNamePlayer + " " + lastNamePlayer;
//        } else {
//            fullNamePlayer = lastNamePlayer;
//        }
//        String positionPlayer = firstTeamPlayerCreator.getPosition().getText().toString().trim();
//        String numberPlayer = firstTeamPlayerCreator.getNumber().getText().toString().trim();
//        String nationalityPlayer = firstTeamPlayerCreator.getNationality().getText().toString().trim();
//        String overallPlayer = firstTeamPlayerCreator.getOverall().getText().toString().trim();
//        String potentialLowPlayer = firstTeamPlayerCreator.getPotentialLow().getText().toString().trim();
//        String potentialHiPlayer = firstTeamPlayerCreator.getPotentialHigh().getText().toString().trim();
//        final String ySignedPlayer = firstTeamPlayerCreator.getYearSigned().getText().toString().trim();
//
//        FirstTeamPlayer player = new FirstTeamPlayer();
//
//        player.setId(++maxFTPlayerId);
//        player.setFirstName(firstNamePlayer);
//        player.setLastName(lastNamePlayer);
//        player.setFullName(fullNamePlayer);
//        player.setPosition(positionPlayer);
//        if (!numberPlayer.isEmpty()) {
//            player.setNumber(Integer.parseInt(numberPlayer));
//        } else {
//            player.setNumber(99);
//        }
//        player.setTeam(team);
//        player.setNationality(nationalityPlayer);
//        player.setOverall(Integer.parseInt(overallPlayer));
//        if (!potentialLowPlayer.isEmpty()) {
//            player.setPotentialLow(Integer.parseInt(potentialLowPlayer));
//        }
//        if (!potentialHiPlayer.isEmpty()) {
//            player.setPotentialHigh(Integer.parseInt(potentialHiPlayer));
//        }
//        player.setYearSigned(ySignedPlayer);
//        player.setYearScouted("0");
//        player.setUserId(UserApi.getInstance().getUserId());
//        player.setTimeAdded(new Timestamp(new Date()));
//        player.setManagerId(managerId);
//        player.setLoanPlayer(false);
//
////                newTransfer.setPlusPlayerName(player.getFullName());
//
////                transfersReference.add(newTransfer);
//        ftPlayersColRef.add(player)
//                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentReference> task) {
//                        if (task.isSuccessful()) {
//                            Log.d(LOG_TAG, "New exchange player added successfully.");
//                            Intent intent = new Intent(context, TransferDealsActivity.class);
//                            intent.putExtra("managerId", managerId);
//                            intent.putExtra("team", team);
//                            context.startActivity(intent);
//                            ((Activity) context).finish();
//                            Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG).show();
//                        } else {
//                            Log.e(LOG_TAG, "Failed to add exchange player.", task.getException());
//                        }
//                    }
//                });
//
//        documentReference.update("exchangePlayerId", player.getId(), "exchangePlayerName", fullNamePlayer, "hasPlayerExchange", true)
//                .addOnSuccessListener(aVoid -> {
//                    Log.d(LOG_TAG, "Transfer document updated with exchange player.");
//                    Toast.makeText(context, "Transfer updated with new exchanged player!", Toast.LENGTH_LONG).show();
//                    Intent intent = new Intent(context, TransferDealsActivity.class);
//                    intent.putExtra("managerId", managerId);
//                    intent.putExtra("team", team);
//                    context.startActivity(intent);
//                    ((Activity) context).finish();
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(LOG_TAG, "Failed to update transfer document with exchange player.", e);
//                });
//    }

    private void findMaxFTPlayerId() {
        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            maxFTPlayerId = queryDocumentSnapshots.toObjects(Transfer.class).get(0).getId();
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                                assert player != null;
                                if (player.getId() > maxFTPlayerId) {
                                    maxFTPlayerId = player.getId();
                                }
                            }
                        }
                });
    }
    
//    private void assignNewTransferValues(TransferEditor transferEditor, DocumentReference documentReference) {
//        Log.d(LOG_TAG, "assignNewTransferValues called for transfer ID: " + documentReference.getId());
//
//        var firstNameEdit = transferEditor.getFirstNameEdit().getText().toString().trim();
//        var lastNameEdit = transferEditor.getLastNameEdit().getText().toString().trim();
//        var positionSpinnerEdit = transferEditor.getPositionPickerEdit().getText().toString().trim();
//        var nationalityEdit = transferEditor.getNationalityEdit().getText().toString().trim();
//        var overallEdit = transferEditor.getOverallEdit().getText().toString().trim();
//        var potentialLowEdit = transferEditor.getPotentialLowEdit().getText().toString().trim();
//        var potentialHighEdit = transferEditor.getPotentialHighEdit().getText().toString().trim();
//        var typeOfTransferSpinnerEdit = transferEditor.getTypeOfTransferPickerEdit().getText().toString().trim();
//        var oldTeamEdit = transferEditor.getOldTeamEdit().getText().toString().trim();
//        var newTeamEdit = transferEditor.getNewTeamEdit().getText().toString().trim();
//        var transferFeeEdit = transferEditor.getFeeEdit().getText().toString().trim().replaceAll(",", "");
//        var wageEdit = transferEditor.getWageEdit().getText().toString().trim().replaceAll(",", "");
//        var contractYearsEdit = transferEditor.getContractYearsEdit().getText().toString().trim();
//        var yearEdit = transferEditor.getYearEdit().getText().toString().trim();
//
//        if (PurchaseTransferEnum.FREE_TRANSFER.getDescription().equals(typeOfTransferSpinnerEdit)) {
//            transferFeeEdit = "0";
//            oldTeamEdit = "Free Agent";
//        } else if (SaleTransferEnum.RELEASE.getDescription().equals(typeOfTransferSpinnerEdit)) {
//            transferFeeEdit = "0";
//            newTeamEdit = "Free Agent";
//        } else if (LoanEnum.SHORT_TERM.getDescription().equals(typeOfTransferSpinnerEdit)
//                 || LoanEnum.ONE_YEAR.getDescription().equals(typeOfTransferSpinnerEdit)
//                 || LoanEnum.TWO_YEAR.getDescription().equals(typeOfTransferSpinnerEdit)) {
//             transferFeeEdit = "0";
//             contractYearsEdit = "0";
//             if (team.equals(oldTeamEdit)) {
//                 wageEdit = "0";
//                 needToChangeLoanedOutPlayer = true;
//             }
//        }
//
//        Log.d(LOG_TAG, "Updating transfer document in Firestore.");
//        documentReference.update("firstName", firstNameEdit,
//                        "lastName", lastNameEdit,
//                        "fullName", firstNameEdit + " " + lastNameEdit,
//                        "position", positionSpinnerEdit,
//                        "nationality", nationalityEdit,
//                        "overall", Integer.parseInt(overallEdit),
//                        "potentialLow", (!potentialLowEdit.isEmpty()) ? Integer.parseInt(potentialLowEdit) : 0,
//                        "potentialHigh", (!potentialHighEdit.isEmpty()) ? Integer.parseInt(potentialHighEdit) : 0,
//                        "type", typeOfTransferSpinnerEdit,
//                        "formerTeam", oldTeamEdit,
//                        "currentTeam", newTeamEdit,
//                        "transferFee", (!transferFeeEdit.isEmpty()) ? Integer.parseInt(transferFeeEdit) : 0,
//                        "wage", (!wageEdit.isEmpty()) ? Integer.parseInt(wageEdit) : 0,
//                        "contractYears", (!contractYearsEdit.isEmpty()) ? Integer.parseInt(contractYearsEdit) : 0,
//                        "year", (!yearEdit.equals("0")) ? yearEdit : "0",
//                        "playerExchange", transferEditor.isExchangePlayer()
//                ).addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Transfer document updated successfully."))
//                .addOnFailureListener(e -> Log.e(LOG_TAG, "Failed to update transfer document.", e));
//    }

//    private void setEditTransferFields(Transfer transfer, TransferEditor transferEditor) {
//        Log.d(LOG_TAG, "setEditTransferFields called for transfer: " + transfer.getId());
//
//        String[] positions = context.getResources().getStringArray(R.array.position_array);
//        String[] years = context.getResources().getStringArray(R.array.years_array);
//
//        transferEditor.getYearEdit().setOnClickListener(v -> {
//            new android.app.AlertDialog.Builder(context)
//                    .setTitle("Select Year Signed/Left")
//                    .setItems(years, (pickerDialog, which) -> transferEditor.getYearEdit().setText(years[which]))
//                    .show();
//        });
//
//        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                .whereEqualTo("id", managerId)
//                .get()
//                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                    @Override
//                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                        if (!queryDocumentSnapshots.isEmpty()) {
//                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
//                                Manager manager = doc.toObject(Manager.class);
//                                String currency = manager.getCurrency();
//                                transferEditor.getFeeEdit().setHint("Fee (in " + currency + ")");
//                                transferEditor.getWageEdit().setHint("Wage (in " + currency + ")");
//                            }
//                        }
//                    }
//                });
//
//        transferEditor.getPositionPickerEdit().setOnClickListener(v -> {
//            new android.app.AlertDialog.Builder(context)
//                    .setTitle("Select Position")
//                    .setItems(positions, (pickerDialog, which) -> transferEditor.getPositionPickerEdit().setText(positions[which]))
//                    .show();
//        });
//
//        ArrayAdapter<String> transferAdapter = populateTransferAdapter(transfer);
//        transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//
//        Log.d(LOG_TAG, "Populating transfer editor fields with existing transfer data.");
//        transferEditor.setTransferEditorFields(transfer);
//    }

//    private void setFieldsBasedOnTransferType(String transferType, TransferEditor transferEditor, Transfer transfer) {
//        Log.d(LOG_TAG, "setFieldsBasedOnTransferType called with type: " + transferType);
//
//        transferEditor.setAllFieldsVisible();
//        transferEditor.setAllFieldsEnabled();
//        transferEditor.setAllFieldsTextColor();
//
//        var playerExchangeSwitch = transferEditor.getPlayerExchangeSwitch();
//        var oldTeamEdit = transferEditor.getOldTeamEdit();
//        var newTeamEdit = transferEditor.getNewTeamEdit();
//        var plusPlayerSpinner = transferEditor.getPlusPlayerPickerEdit();
//        var feeEdit = transferEditor.getFeeEdit();
//        var wageEdit = transferEditor.getWageEdit();
//        var contractYearsEdit = transferEditor.getContractYearsEdit();
//        var plusPlayerSpinnerEdit = transferEditor.getPlusPlayerPickerEdit();
//
//        initializeChangingFieldsByTransferType(transfer, transferEditor);
//
//        if (PurchaseTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transferType)) {
//            playerExchangeSwitch.setVisibility(View.GONE);
//            newTeamEdit.setEnabled(false);
//            newTeamEdit.setTextColor(Color.GRAY);
//
//            populatePlusPlayerDropdown(transfer.getPlusPlayerName(), plusPlayersList -> {
//                String[] playerNames = plusPlayersList.stream()
//                        .map(FirstTeamPlayer::getFullName)
//                        .toArray(String[]::new);
//
//                plusPlayerSpinner.setOnClickListener(v -> {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                    builder.setTitle("Select Plus Player");
//                    builder.setItems(playerNames, (dialog, which) -> {
//                        FirstTeamPlayer selected = plusPlayersList.get(which);
//
//                        plusPlayerSpinner.setText(selected.getFullName());
//                        transferEditor.setPlayerSpinnerId(selected.getId());
//                        transferEditor.setIsPlusPlayer(which > 0);
//                        transferEditor.setPlusPlayerName(which > 0 ? selected.getFullName() : null);
//
//                        Log.d(LOG_TAG, "Plus player selected: " + selected.getFullName());
//                    });
//                    builder.show();
//                });
//
//                // Pre-fill field if a plus player exists
//                if (transfer.getPlusPlayerName() != null && !transfer.getPlusPlayerName().isBlank()) {
//                    plusPlayerSpinner.setText(transfer.getPlusPlayerName());
//                }
//            });
//        } else if (PurchaseTransferEnum.FREE_TRANSFER.getDescription().equals(transferType)) {
//            playerExchangeSwitch.setVisibility(View.GONE);
//            plusPlayerSpinner.setVisibility(View.GONE);
//            oldTeamEdit.setEnabled(false);
//            oldTeamEdit.setTextColor(Color.GRAY);
//            oldTeamEdit.setText("Free Agent");
//            newTeamEdit.setEnabled(false);
//            newTeamEdit.setTextColor(Color.GRAY);
//            feeEdit.setEnabled(false);
//            feeEdit.setTextColor(Color.GRAY);
//            feeEdit.setText("0");
//        } else if (SaleTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transferType)) {
//            plusPlayerSpinner.setVisibility(View.GONE);
//            wageEdit.setVisibility(View.GONE);
//            contractYearsEdit.setVisibility(View.GONE);
//            oldTeamEdit.setEnabled(false);
//            oldTeamEdit.setTextColor(Color.GRAY);
//        } else if (SaleTransferEnum.RELEASE.getDescription().equals(transferType)) {
//            plusPlayerSpinner.setVisibility(View.GONE);
//            wageEdit.setVisibility(View.GONE);
//            contractYearsEdit.setVisibility(View.GONE);
//            playerExchangeSwitch.setVisibility(View.GONE);
//            oldTeamEdit.setEnabled(false);
//            oldTeamEdit.setTextColor(Color.GRAY);
//            newTeamEdit.setEnabled(false);
//            newTeamEdit.setTextColor(Color.GRAY);
//            newTeamEdit.setText("Free Agent");
//            feeEdit.setEnabled(false);
//            feeEdit.setTextColor(Color.GRAY);
//            feeEdit.setText("0");
//        } else if (LoanEnum.SHORT_TERM.getDescription().equals(transferType)
//                || LoanEnum.ONE_YEAR.getDescription().equals(transferType)
//                || LoanEnum.TWO_YEAR.getDescription().equals(transferType)) {
//            plusPlayerSpinner.setVisibility(View.GONE);
//            playerExchangeSwitch.setVisibility(View.GONE);
//            feeEdit.setVisibility(View.GONE);
//            contractYearsEdit.setVisibility(View.GONE);
//            if (team.equals(transfer.getCurrentTeam())) {
//                newTeamEdit.setEnabled(false);
//                newTeamEdit.setTextColor(Color.GRAY);
//            } else if (team.equals(transfer.getFormerTeam())) {
//                feeEdit.setVisibility(View.GONE);
//                wageEdit.setVisibility(View.GONE);
//                oldTeamEdit.setEnabled(false);
//                oldTeamEdit.setTextColor(Color.GRAY);
//            }
//        }
//    }

//    private static void initializeChangingFieldsByTransferType(Transfer transfer, TransferEditor transferEditor) {
//        Log.d(LOG_TAG, "Initializing fields for transfer: " + transfer.getId());
//
//        transferEditor.getFeeEdit().setText(String.valueOf(transfer.getTransferFee()));
//        ValueFormatter.formatValue(transferEditor.getFeeEdit());
//        transferEditor.getWageEdit().setText(String.valueOf(transfer.getWage()));
//        ValueFormatter.formatValue(transferEditor.getWageEdit());
//        transferEditor.getOldTeamEdit().setText(transfer.getFormerTeam());
//        transferEditor.getNewTeamEdit().setText(transfer.getCurrentTeam());
//    }

//    private void populatePlusPlayerDropdown(String plusPlayerName, Consumer<List<FirstTeamPlayer>> callback) {
//        Log.d(LOG_TAG, "Populating plus player dropdown. Initial player: " + plusPlayerName);
//
//        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                .whereEqualTo("managerId", managerId)
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    if (!queryDocumentSnapshots.isEmpty()) {
//                        List<FirstTeamPlayer> ftPlayerList = new ArrayList<>();
//
//                        // Optional "None" or blank entry
//                        var firstPlayer = new FirstTeamPlayer();
//                        firstPlayer.setFullName("None");
//                        ftPlayerList.add(firstPlayer);
//
//                        // Pre-select if needed
//                        if (plusPlayerName != null && !plusPlayerName.isBlank()) {
//                            FirstTeamPlayer initial = new FirstTeamPlayer();
//                            initial.setFullName(plusPlayerName);
//                            ftPlayerList.add(initial);
//                        }
//
//                        // Add all players from Firestore
//                        queryDocumentSnapshots.getDocuments().stream()
//                                .map(doc -> doc.toObject(FirstTeamPlayer.class))
//                                .forEach(ftPlayerList::add);
//
//                        Log.d(LOG_TAG, "Plus player list populated. Count: " + ftPlayerList.size());
//                        callback.accept(ftPlayerList);
//                    } else {
//                        Log.w(LOG_TAG, "No First Team Players found.");
//                        callback.accept(Collections.emptyList());
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(LOG_TAG, "Failed to populate plus player dropdown.", e);
//                    callback.accept(Collections.emptyList());
//                });
//    }


//    private ArrayAdapter<String> populateTransferAdapter(Transfer transfer) {
//        Log.d(LOG_TAG, "Populating transfer type adapter for transfer type: " + transfer.getType());
//
//        List<String> transferList = new ArrayList<>();
//
//        if (PurchaseTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transfer.getType())
//            || PurchaseTransferEnum.FREE_TRANSFER.getDescription().equals(transfer.getType())) {
//            transferList.addAll(Arrays.stream(PurchaseTransferEnum.values())
//                    .map(PurchaseTransferEnum::getDescription)
//                    .collect(Collectors.toList()));
//        } else if (SaleTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transfer.getType())
//                || SaleTransferEnum.RELEASE.getDescription().equals(transfer.getType())) {
//            transferList.addAll(Arrays.stream(SaleTransferEnum.values())
//                    .map(SaleTransferEnum::getDescription)
//                    .collect(Collectors.toList()));
//        } else if (LoanEnum.SHORT_TERM.getDescription().equals(transfer.getType())
//                || LoanEnum.ONE_YEAR.getDescription().equals(transfer.getType())
//                || LoanEnum.TWO_YEAR.getDescription().equals(transfer.getType())) {
//            if (team.equals(transfer.getCurrentTeam())) {
//                transferList.addAll(Arrays.stream(PurchaseTransferEnum.values())
//                        .map(PurchaseTransferEnum::getDescription)
//                        .collect(Collectors.toList()));
//            } else if (team.equals(transfer.getFormerTeam())) {
//                transferList.addAll(Arrays.stream(SaleTransferEnum.values())
//                        .map(SaleTransferEnum::getDescription)
//                        .collect(Collectors.toList()));
//            }
//        }
//
//        transferList.addAll(Arrays.stream(LoanEnum.values())
//                .map(LoanEnum::getDescription)
//                .collect(Collectors.toList()));
//
//        return new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, transferList);
//    }

}
