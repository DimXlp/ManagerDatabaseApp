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
import android.widget.CompoundButton;
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
import com.dimxlp.managerdb.TransferDealsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import enumeration.SaleTransferEnum;
import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.Manager;
import model.Transfer;
import uihelpers.FirstTeamPlayerCreator;
import uihelpers.TransferEditor;
import util.UserApi;
import util.ValueFormatter;

public class TransferDealsRecAdapter extends RecyclerView.Adapter<TransferDealsRecAdapter.ViewHolder> {

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
        final Transfer transfer = transferList.get(position);

        holder.fullName.setText(transfer.getFullName());
        holder.position.setText(transfer.getPosition());
        holder.overall.setText(String.valueOf(transfer.getOverall()));
        if (transfer.getPotentialLow() != 0 && transfer.getPotentialHigh() != 0) {
            holder.potential.setText(String.format("%d-%d", transfer.getPotentialLow(), transfer.getPotentialHigh()));
        } else {
            holder.potential.setText("??-??");
        }
        holder.country.setText(transfer.getNationality());
        holder.transferText.setText(transfer.getType().toUpperCase());
        holder.oldTeam.setText(transfer.getFormerTeam());
        holder.newTeam.setText(transfer.getCurrentTeam());
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
                            if (transfer.getTransferFee() > 0) {
                                holder.transferFee.setText(String.format("%s %s", currency, NumberFormat.getInstance().format(transfer.getTransferFee())));
                            } else {
                                holder.transferFee.setText(String.format("%s ???????", currency));
                            }
                            if (transfer.getWage() > 0) {
                                holder.wage.setText(String.format("%s %s", currency, NumberFormat.getInstance().format(transfer.getWage())));
                            } else {
                                holder.wage.setText(String.format("%s ???", currency));
                            }
                            if (transfer.getPlusPlayerName() != null && !"".equals(transfer.getPlusPlayerName())) {
                                holder.plusImage.setVisibility(View.VISIBLE);
                                holder.plusPlayerName.setVisibility(View.VISIBLE);
                                holder.plusPlayerName.setText(transfer.getPlusPlayerName());
                            } else {
                                holder.plusImage.setVisibility(View.GONE);
                                holder.plusPlayerName.setVisibility(View.GONE);
                            }

                        }
                    }
                });

        if (transfer.isFormerPlayer()) {
            holder.wageText.setVisibility(View.GONE);
            holder.wage.setVisibility(View.GONE);
            holder.contractYearsText.setVisibility(View.GONE);
            holder.contractYears.setVisibility(View.GONE);
            holder.line5.setVisibility(View.GONE);
            holder.yearText.setText("YR LFT");
            holder.year.setText(String.valueOf(transfer.getYear()));
        } else {
            if (transfer.getContractYears() != 0) {
                holder.contractYears.setText(String.valueOf(transfer.getContractYears()));
            } else {
                holder.contractYears.setText("?");
            }
            holder.yearText.setText("YR SGN");
            holder.year.setText(transfer.getYear());
        }

        if (transfer.getComments().isEmpty()) {
            holder.comments.setVisibility(View.GONE);
        } else {
            holder.comments.setText(transfer.getComments());
        }

        GradientDrawable gradientDrawable = (GradientDrawable) holder.positionOval.getBackground();
        String pos = holder.position.getText().toString().trim();
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
        return transferList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView fullName;
        private ImageView positionOval;
        private TextView position;
        private TextView overall;
        private TextView potential;
        private TextView country;
        private TextView transferText;
        private TextView oldTeam;
        private TextView newTeam;
        private TextView transferFee;
        private ImageView plusImage;
        private TextView plusPlayerName;
        private TextView wageText;
        private TextView wage;
        private TextView contractYearsText;
        private TextView contractYears;
        private ImageView line5;
        private TextView yearText;
        private TextView year;
        private TextView comments;
        private Button editButton;
        private Button deleteButton;
        private RelativeLayout details;
        private AlertDialog.Builder builder;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            fullName = itemView.findViewById(R.id.full_name_trf);
            positionOval = itemView.findViewById(R.id.position_oval_trf);
            position = itemView.findViewById(R.id.position_trf);
            overall = itemView.findViewById(R.id.overall_trf);
            potential = itemView.findViewById(R.id.potential_trf);
            country = itemView.findViewById(R.id.nationality_trf);
            transferText = itemView.findViewById(R.id.transfer_text_trf);
            oldTeam = itemView.findViewById(R.id.old_team_trf);
            newTeam = itemView.findViewById(R.id.new_team_trf);
            transferFee = itemView.findViewById(R.id.transfer_fee_trf);
            plusImage = itemView.findViewById(R.id.plus_trf);
            plusPlayerName = itemView.findViewById(R.id.plus_player_trf);
            wageText = itemView.findViewById(R.id.wage_text_trf);
            wage = itemView.findViewById(R.id.wage_trf);
            contractYearsText = itemView.findViewById(R.id.contract_years_text_trf);
            contractYears = itemView.findViewById(R.id.contract_years_trf);
            line5 = itemView.findViewById(R.id.line_trf_5);
            yearText = itemView.findViewById(R.id.year_text_trf);
            year = itemView.findViewById(R.id.year_trf);
            comments = itemView.findViewById(R.id.comments_trf);
            editButton = itemView.findViewById(R.id.edit_button_trf);
            deleteButton = itemView.findViewById(R.id.delete_button_trf);
            details = itemView.findViewById(R.id.details_trf);

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

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editTransfer(transferList.get(getAdapterPosition()));
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
            });
        }

        private void deleteTransfer(final Transfer transfer) {
            transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
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
                                                Toast.makeText(context, "Transfer deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                Intent intent = new Intent(context, TransferDealsActivity.class);
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

        private void editTransfer(final Transfer transfer) {
            builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.edit_transfer_deal_popup, null);

            TransferEditor transferEditor = new TransferEditor(view);

            transferEditor.formatValue(transferEditor.getFeeEdit());
            transferEditor.formatValue(transferEditor.getWageEdit());

            setEditTransferFields(transfer, transferEditor);
            setFieldsBasedOnTransferType(transfer.getType(), transferEditor, transfer);

            builder.setView(view);
            final AlertDialog dialog = builder.create();
            dialog.show();

            boolean wasExchangeTransfer = transferEditor.getPlayerExchangeSwitch().isChecked();
            boolean wasPlusPlayerTransfer = !transferEditor.getPlusPlayerSpinnerEdit().toString().isEmpty();

            transferEditor.getTypeOfTransferSpinnerEdit().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    setFieldsBasedOnTransferType(parent.getItemAtPosition(position).toString(),
                                                            transferEditor, transfer);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    
                }
            });

            transferEditor.getPlayerExchangeSwitch().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        transferEditor.setExchangePlayer(true);
                    }
                }
            });

            transferEditor.getPlusPlayerSpinnerEdit().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    FirstTeamPlayer player = (FirstTeamPlayer) parent.getSelectedItem();
                    transferEditor.setPlayerSpinnerId(player.getId());
                    transferEditor.setPlusPlayer(true);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            transferEditor.getEditTransferButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!transferEditor.getLastNameEdit().getText().toString().isEmpty()
                        && !transferEditor.getNationalityEdit().getText().toString().isEmpty()
                        && !transferEditor.getPositionSpinnerEdit().getSelectedItem().toString().isEmpty()
                        && !transferEditor.getOverallEdit().getText().toString().isEmpty()
                        && !transferEditor.getTypeOfTransferSpinnerEdit().getSelectedItem().toString().isEmpty()
                        && !transferEditor.getOldTeamEdit().getText().toString().isEmpty()
                        && !transferEditor.getNewTeamEdit().getText().toString().isEmpty()
                        && !transferEditor.getYearEdit().getSelectedItem().toString().equals("0")) {
                        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            List<DocumentSnapshot> doc =  task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds: doc) {
                                                Transfer editedTransfer = ds.toObject(Transfer.class);
                                                if (editedTransfer.getId() == transfer.getId()) {
                                                    documentReference = transfersColRef.document(ds.getId());
                                                }
                                            }

                                            assert documentReference != null;
                                            assignNewTransferValues(transferEditor, documentReference);

                                            if (transferEditor.isExchangePlayer()) {
                                                addExchangePlayer();
                                            } else if (transferEditor.isPlusPlayer()) {
                                                letPlayerLeave(transferEditor);
                                            } else if (wasExchangeTransfer) {
                                                DocumentReference finalDocumentReference = documentReference;
                                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        switch (which) {
                                                            case DialogInterface.BUTTON_POSITIVE ->
                                                                removeOldExchangePlayer(transfer, new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        addNewExchangePlayer(finalDocumentReference);
                                                                    }
                                                                });
                                                            case DialogInterface.BUTTON_NEGATIVE -> {
                                                            }
                                                        }
                                                    }
                                                };

                                                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                                builder.setMessage("Do you want to change the player that was exchanged?").setPositiveButton("Yes", dialogClickListener)
                                                        .setNegativeButton("No", dialogClickListener).show();
                                            } else if (wasPlusPlayerTransfer) {

                                            } else {
                                                Intent intent = new Intent(context, TransferDealsActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                context.startActivity(intent);
                                                ((Activity) context).finish();
                                                Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Type of Transfer, Old Team, New Team and Year Signed/Left are required", Toast.LENGTH_LONG)
                                .show();
                    }

                }
            });
        }
    }

    private void addNewExchangePlayer(DocumentReference transferDocRef) {
        builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.create_first_team_player_popup, null);

        builder.setView(view);
        builder.setCancelable(false);

        Log.d("RAFI", "EXCHANGE");

        FirstTeamPlayerCreator firstTeamPlayerCreator = new FirstTeamPlayerCreator(view);

        firstTeamPlayerCreator.changePropertiesForExchange();

        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        firstTeamPlayerCreator.getYearSigned().setAdapter(yearAdapter);

        firstTeamPlayerCreator.getSavePlayerButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!firstTeamPlayerCreator.getLastName().getText().toString().isEmpty()
                        && !firstTeamPlayerCreator.getNationality().getText().toString().isEmpty()
                        && !firstTeamPlayerCreator.getPositionSpinner().getSelectedItem().toString().isEmpty()
                        && !firstTeamPlayerCreator.getOverall().getText().toString().isEmpty()
                        && !firstTeamPlayerCreator.getYearSigned().getSelectedItem().toString().equals("0")) {
                    createNewExchangePlayer(firstTeamPlayerCreator, transferDocRef);
                } else {
                    Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void createNewExchangePlayer(FirstTeamPlayerCreator firstTeamPlayerCreator, DocumentReference transferDocRef) {
        String firstNamePlayer = firstTeamPlayerCreator.getFirstName().getText().toString().trim();
        String lastNamePlayer = firstTeamPlayerCreator.getLastName().getText().toString().trim();
        String fullNamePlayer;
        if (!firstNamePlayer.isEmpty()) {
            fullNamePlayer = firstNamePlayer + " " + lastNamePlayer;
        } else {
            fullNamePlayer = lastNamePlayer;
        }
        String positionPlayer = firstTeamPlayerCreator.getPositionSpinner().getSelectedItem().toString().trim();
        String numberPlayer = firstTeamPlayerCreator.getNumber().getText().toString().trim();
        String nationalityPlayer = firstTeamPlayerCreator.getNationality().getText().toString().trim();
        String overallPlayer = firstTeamPlayerCreator.getOverall().getText().toString().trim();
        String potentialLowPlayer = firstTeamPlayerCreator.getPotentialLow().getText().toString().trim();
        String potentialHiPlayer = firstTeamPlayerCreator.getPotentialHigh().getText().toString().trim();
        final String ySignedPlayer = firstTeamPlayerCreator.getYearSigned().getSelectedItem().toString().trim();

        FirstTeamPlayer player = new FirstTeamPlayer();

        player.setId(findMaxFTPlayerId()+1);
        player.setFirstName(firstNamePlayer);
        player.setLastName(lastNamePlayer);
        player.setFullName(fullNamePlayer);
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
        player.setLoanPlayer(false);

        ftPlayersColRef.add(player)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(context, TransferDealsActivity.class);
                            intent.putExtra("managerId", managerId);
                            intent.putExtra("team", team);
                            context.startActivity(intent);
                            ((Activity) context).finish();
                            Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        transferDocRef.update("exchangePlayerId", player.getId(), "exchangePlayerName", fullNamePlayer, "hasPlayerExchange", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Transfer updated with new exchanged player!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(context, TransferDealsActivity.class);
                    intent.putExtra("managerId", managerId);
                    intent.putExtra("team", team);
                    context.startActivity(intent);
                    ((Activity) context).finish();
                })
                .addOnFailureListener(e -> Log.e("TransferEdit", "Failed to add new exchanged player", e));
    }

    private void removeOldExchangePlayer(Transfer transfer, OnSuccessListener<Void> onSuccessListener) {
        if (transfer.doesHavePlayerExchange()) {
            DocumentReference oldPlayerRef = ftPlayersColRef.document(String.valueOf(transfer.getExchangePlayerId()));
            oldPlayerRef.delete().addOnSuccessListener(onSuccessListener)
                    .addOnFailureListener(e -> Log.e("TransferEdit", "Failed to remove old exchanged player", e));
        } else {
            onSuccessListener.onSuccess(null);
        }
    }

    private void letPlayerLeave(TransferEditor transferEditor) {
        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                            DocumentReference documentReference = null;
                            for (DocumentSnapshot ds : doc) {
                                FirstTeamPlayer player = ds.toObject(FirstTeamPlayer.class);
                                assert player != null;
                                if (player.getId() == transferEditor.getPlayerSpinnerId()) {
                                    documentReference = ftPlayersColRef.document(ds.getId());

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
                                    fmPlayer.setYearLeft(transferEditor.getYearEdit().getSelectedItem().toString().trim());
                                    fmPlayer.setManagerId(managerId);
                                    fmPlayer.setUserId(UserApi.getInstance().getUserId());
                                    fmPlayer.setTimeAdded(new Timestamp(new Date()));

                                    fmPlayersColRef.add(fmPlayer);
                                }
                            }

                            assert documentReference != null;
                            documentReference.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Intent intent = new Intent(context, TransferDealsActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                context.startActivity(intent);
                                                ((Activity) context).finish();
                                                Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                            }
                    }
                });
    }

    private void addExchangePlayer() {
        builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.create_first_team_player_popup, null);

        builder.setView(view);
        builder.setCancelable(false);

        Log.d("RAFI", "EXCHANGE");

        FirstTeamPlayerCreator firstTeamPlayerCreator = new FirstTeamPlayerCreator(view);

        firstTeamPlayerCreator.changePropertiesForExchange();

        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        firstTeamPlayerCreator.getYearSigned().setAdapter(yearAdapter);

        firstTeamPlayerCreator.getSavePlayerButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!firstTeamPlayerCreator.getLastName().getText().toString().isEmpty()
                    && !firstTeamPlayerCreator.getNationality().getText().toString().isEmpty()
                    && !firstTeamPlayerCreator.getPositionSpinner().getSelectedItem().toString().isEmpty()
                    && !firstTeamPlayerCreator.getOverall().getText().toString().isEmpty()
                    && !firstTeamPlayerCreator.getYearSigned().getSelectedItem().toString().equals("0")) {
                    createPlayer(firstTeamPlayerCreator);
                } else {
                    Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall and Year Signed are required", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void createPlayer(FirstTeamPlayerCreator firstTeamPlayerCreator) {
        String firstNamePlayer = firstTeamPlayerCreator.getFirstName().getText().toString().trim();
        String lastNamePlayer = firstTeamPlayerCreator.getLastName().getText().toString().trim();
        String fullNamePlayer;
        if (!firstNamePlayer.isEmpty()) {
            fullNamePlayer = firstNamePlayer + " " + lastNamePlayer;
        } else {
            fullNamePlayer = lastNamePlayer;
        }
        String positionPlayer = firstTeamPlayerCreator.getPositionSpinner().getSelectedItem().toString().trim();
        String numberPlayer = firstTeamPlayerCreator.getNumber().getText().toString().trim();
        String nationalityPlayer = firstTeamPlayerCreator.getNationality().getText().toString().trim();
        String overallPlayer = firstTeamPlayerCreator.getOverall().getText().toString().trim();
        String potentialLowPlayer = firstTeamPlayerCreator.getPotentialLow().getText().toString().trim();
        String potentialHiPlayer = firstTeamPlayerCreator.getPotentialHigh().getText().toString().trim();
        final String ySignedPlayer = firstTeamPlayerCreator.getYearSigned().getSelectedItem().toString().trim();

        FirstTeamPlayer player = new FirstTeamPlayer();

        player.setId(findMaxFTPlayerId()+1);
        player.setFirstName(firstNamePlayer);
        player.setLastName(lastNamePlayer);
        player.setFullName(fullNamePlayer);
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
        player.setLoanPlayer(false);

//                newTransfer.setPlusPlayerName(player.getFullName());

//                transfersReference.add(newTransfer);
        ftPlayersColRef.add(player)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(context, TransferDealsActivity.class);
                            intent.putExtra("managerId", managerId);
                            intent.putExtra("team", team);
                            context.startActivity(intent);
                            ((Activity) context).finish();
                            Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private long findMaxFTPlayerId() {
        final long[] maxId = {0};
        ftPlayersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            List<FirstTeamPlayer> ftplayers = new ArrayList<>();
                            for (DocumentSnapshot doc : docs) {
                                FirstTeamPlayer player = doc.toObject(FirstTeamPlayer.class);
                                ftplayers.add(player);
                            }
                            maxId[0] = ftplayers.get(0).getId();
                            for (FirstTeamPlayer player : ftplayers) {
                                if (player.getId() > maxId[0]) {
                                    maxId[0] = player.getId();
                                }
                            }
                        }
                    }
                });
        return maxId[0];
    }
    
    private void assignNewTransferValues(TransferEditor transferEditor, DocumentReference documentReference) {
        var firstNameEdit = transferEditor.getFirstNameEdit().getText().toString().trim();
        var lastNameEdit = transferEditor.getLastNameEdit().getText().toString().trim();
        var positionSpinnerEdit = transferEditor.getPositionSpinnerEdit().getSelectedItem().toString().trim();
        var nationalityEdit = transferEditor.getNationalityEdit().getText().toString().trim();
        var overallEdit = transferEditor.getOverallEdit().getText().toString().trim();
        var potentialLowEdit = transferEditor.getPotentialLowEdit().getText().toString().trim();
        var potentialHighEdit = transferEditor.getPotentialHighEdit().getText().toString().trim();
        var typeOfTransferSpinnerEdit = transferEditor.getTypeOfTransferSpinnerEdit().getSelectedItem().toString().trim();
        var oldTeamEdit = transferEditor.getOldTeamEdit().getText().toString().trim();
        var newTeamEdit = transferEditor.getNewTeamEdit().getText().toString().trim();
        var transferFeeEdit = transferEditor.getFeeEdit().getText().toString().trim().replaceAll(",", "");
        var wageEdit = transferEditor.getWageEdit().getText().toString().trim().replaceAll(",", "");
        var contractYearsEdit = transferEditor.getContractYearsEdit().getText().toString().trim();
        var yearEdit = transferEditor.getYearEdit().getSelectedItem().toString().trim();
        // TODO player exchange

         if (PurchaseTransferEnum.FREE_TRANSFER.getDescription().equals(typeOfTransferSpinnerEdit)) {
            transferFeeEdit = "0";
            oldTeamEdit = "Free Agent";
        } else if (SaleTransferEnum.RELEASE.getDescription().equals(typeOfTransferSpinnerEdit)) {
            transferFeeEdit = "0";
            newTeamEdit = "Free Agent";
        } else if (LoanEnum.SHORT_TERM.getDescription().equals(typeOfTransferSpinnerEdit)
                 || LoanEnum.ONE_YEAR.getDescription().equals(typeOfTransferSpinnerEdit)
                 || LoanEnum.TWO_YEAR.getDescription().equals(typeOfTransferSpinnerEdit)) {
             transferFeeEdit = "0";
             contractYearsEdit = "0";
             if (team.equals(oldTeamEdit)) {
                 wageEdit = "0";
             }
         }

        documentReference.update("firstName", firstNameEdit,
                        "lastName", lastNameEdit,
                        "fullName", firstNameEdit + " " + lastNameEdit,
                        "position", positionSpinnerEdit,
                        "nationality", nationalityEdit,
                        "overall", Integer.parseInt(overallEdit),
                        "potentialLow", (!potentialLowEdit.isEmpty()) ? Integer.parseInt(potentialLowEdit) : 0,
                        "potentialHigh", (!potentialHighEdit.isEmpty()) ? Integer.parseInt(potentialHighEdit) : 0,
                        "type", typeOfTransferSpinnerEdit,
                        "formerTeam", oldTeamEdit,
                        "currentTeam", newTeamEdit,
                        "transferFee", (!transferFeeEdit.isEmpty()) ? Integer.parseInt(transferFeeEdit) : 0,
                        "wage", (!wageEdit.isEmpty()) ? Integer.parseInt(wageEdit) : 0,
                        "contractYears", (!contractYearsEdit.isEmpty()) ? Integer.parseInt(contractYearsEdit) : 0,
                        "year", (!yearEdit.equals("0")) ? yearEdit : "0",
                        "playerExchange", transferEditor.isExchangePlayer());
    }

    private void setEditTransferFields(Transfer transfer, TransferEditor transferEditor) {
        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transferEditor.getYearEdit().setAdapter(yearAdapter);

        managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
                                Manager manager = doc.toObject(Manager.class);
                                String currency = manager.getCurrency();
                                transferEditor.getFeeEdit().setHint("Fee (in " + currency + ")");
                                transferEditor.getWageEdit().setHint("Wage (in " + currency + ")");
                            }
                        }
                    }
                });

        ArrayAdapter<CharSequence> positionAdapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transferEditor.getPositionSpinnerEdit().setAdapter(positionAdapter);

        ArrayAdapter<String> transferAdapter = populateTransferAdapter(transfer);
        transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transferEditor.getTypeOfTransferSpinnerEdit().setAdapter(transferAdapter);

        transferEditor.setTransferEditorFields(transfer, yearAdapter, positionAdapter, transferAdapter);
    }

    private void setFieldsBasedOnTransferType(String transferType, TransferEditor transferEditor, Transfer transfer) {
        transferEditor.setAllFieldsVisible();
        transferEditor.setAllFieldsEnabled();
        transferEditor.setAllFieldsTextColor();

        var playerExchangeSwitch = transferEditor.getPlayerExchangeSwitch();
        var oldTeamEdit = transferEditor.getOldTeamEdit();
        var newTeamEdit = transferEditor.getNewTeamEdit();
        var plusPlayerText = transferEditor.getPlusPlayerText();
        var plusPlayerSpinner = transferEditor.getPlusPlayerSpinnerEdit();
        var feeEdit = transferEditor.getFeeEdit();
        var wageEdit = transferEditor.getWageEdit();
        var contractYearsEdit = transferEditor.getContractYearsEdit();
        var plusPlayerSpinnerEdit = transferEditor.getPlusPlayerSpinnerEdit();

        initializeChangingFieldsByTransferType(transfer, transferEditor);

        if (PurchaseTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transferType)) {
            playerExchangeSwitch.setVisibility(View.GONE);
            newTeamEdit.setEnabled(false);
            newTeamEdit.setTextColor(Color.GRAY);
            populatePlusPlayerSpinner(plusPlayerSpinnerEdit);
        } else if (PurchaseTransferEnum.FREE_TRANSFER.getDescription().equals(transferType)) {
            playerExchangeSwitch.setVisibility(View.GONE);
            plusPlayerText.setVisibility(View.GONE);
            plusPlayerSpinner.setVisibility(View.GONE);
            oldTeamEdit.setEnabled(false);
            oldTeamEdit.setTextColor(Color.GRAY);
            oldTeamEdit.setText("Free Agent");
            newTeamEdit.setEnabled(false);
            newTeamEdit.setTextColor(Color.GRAY);
            feeEdit.setEnabled(false);
            feeEdit.setTextColor(Color.GRAY);
            feeEdit.setText("0");
        } else if (SaleTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transferType)) {
            plusPlayerText.setVisibility(View.GONE);
            plusPlayerSpinner.setVisibility(View.GONE);
            wageEdit.setVisibility(View.GONE);
            contractYearsEdit.setVisibility(View.GONE);
            oldTeamEdit.setEnabled(false);
            oldTeamEdit.setTextColor(Color.GRAY);
        } else if (SaleTransferEnum.RELEASE.getDescription().equals(transferType)) {
            plusPlayerText.setVisibility(View.GONE);
            plusPlayerSpinner.setVisibility(View.GONE);
            wageEdit.setVisibility(View.GONE);
            contractYearsEdit.setVisibility(View.GONE);
            playerExchangeSwitch.setVisibility(View.GONE);
            oldTeamEdit.setEnabled(false);
            oldTeamEdit.setTextColor(Color.GRAY);
            newTeamEdit.setEnabled(false);
            newTeamEdit.setTextColor(Color.GRAY);
            newTeamEdit.setText("Free Agent");
            feeEdit.setEnabled(false);
            feeEdit.setTextColor(Color.GRAY);
            feeEdit.setText("0");
        } else if (LoanEnum.SHORT_TERM.getDescription().equals(transferType)
                || LoanEnum.ONE_YEAR.getDescription().equals(transferType)
                || LoanEnum.TWO_YEAR.getDescription().equals(transferType)) {
            plusPlayerText.setVisibility(View.GONE);
            plusPlayerSpinner.setVisibility(View.GONE);
            playerExchangeSwitch.setVisibility(View.GONE);
            feeEdit.setVisibility(View.GONE);
            contractYearsEdit.setVisibility(View.GONE);
            if (team.equals(transfer.getCurrentTeam())) {
                newTeamEdit.setEnabled(false);
                newTeamEdit.setTextColor(Color.GRAY);
            } else if (team.equals(transfer.getFormerTeam())) {
                feeEdit.setVisibility(View.GONE);
                wageEdit.setVisibility(View.GONE);
                oldTeamEdit.setEnabled(false);
                oldTeamEdit.setTextColor(Color.GRAY);
            }
        }
    }

    private static void initializeChangingFieldsByTransferType(Transfer transfer, TransferEditor transferEditor) {
        transferEditor.getFeeEdit().setText(String.valueOf(transfer.getTransferFee()));
        ValueFormatter.formatValue(transferEditor.getFeeEdit());
        transferEditor.getWageEdit().setText(String.valueOf(transfer.getWage()));
        ValueFormatter.formatValue(transferEditor.getWageEdit());
        transferEditor.getOldTeamEdit().setText(transfer.getFormerTeam());
        transferEditor.getNewTeamEdit().setText(transfer.getCurrentTeam());
    }

    private void populatePlusPlayerSpinner(Spinner plusPlayerSpinnerEdit) {

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
                            plusPlayerSpinnerEdit.setAdapter(playerAdapter);
                        }
                    }
                });
    }

    private ArrayAdapter<String> populateTransferAdapter(Transfer transfer) {
        List<String> transferList = new ArrayList<>();

        Log.d("RAFI", "typeOfTransfer: " + transfer.getType());

        if (PurchaseTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transfer.getType())
            || PurchaseTransferEnum.FREE_TRANSFER.getDescription().equals(transfer.getType())) {
            transferList.addAll(Arrays.stream(PurchaseTransferEnum.values())
                    .map(PurchaseTransferEnum::getDescription)
                    .collect(Collectors.toList()));
        } else if (SaleTransferEnum.WITH_TRANSFER_FEE.getDescription().equals(transfer.getType())
                || SaleTransferEnum.RELEASE.getDescription().equals(transfer.getType())) {
            transferList.addAll(Arrays.stream(SaleTransferEnum.values())
                    .map(SaleTransferEnum::getDescription)
                    .collect(Collectors.toList()));
        } else if (LoanEnum.SHORT_TERM.getDescription().equals(transfer.getType())
                || LoanEnum.ONE_YEAR.getDescription().equals(transfer.getType())
                || LoanEnum.TWO_YEAR.getDescription().equals(transfer.getType())) {
            if (team.equals(transfer.getCurrentTeam())) {
                transferList.addAll(Arrays.stream(PurchaseTransferEnum.values())
                        .map(PurchaseTransferEnum::getDescription)
                        .collect(Collectors.toList()));
            } else if (team.equals(transfer.getFormerTeam())) {
                transferList.addAll(Arrays.stream(SaleTransferEnum.values())
                        .map(SaleTransferEnum::getDescription)
                        .collect(Collectors.toList()));
            }
        }

        transferList.addAll(Arrays.stream(LoanEnum.values())
                .map(LoanEnum::getDescription)
                .collect(Collectors.toList()));

        return new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, transferList);
    }
}
