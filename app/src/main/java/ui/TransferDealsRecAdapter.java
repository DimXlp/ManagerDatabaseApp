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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.R;
import com.dimxlp.managerdb.TransferDealsActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import model.Manager;
import model.Transfer;
import util.UserApi;

public class TransferDealsRecAdapter extends RecyclerView.Adapter<TransferDealsRecAdapter.ViewHolder> {

    private Context context;
    private List<Transfer> transferList;
    private long managerId;
    private String team;
    private int buttonInt;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference transfersColRef = db.collection("Transfers");
    private CollectionReference managersColRef = db.collection("Managers");

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
        holder.transferText.setText(transfer.getType());
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
                                holder.transferFee.setText(String.format("%s %s", currency, transfer.getTransferFee()));
                            } else {
                                holder.transferFee.setText(String.format("%s ???????", currency));
                            }
                            if (transfer.getWage() > 0) {
                                holder.wage.setText(String.format("%s %s", currency, transfer.getWage()));
                            } else {
                                holder.wage.setText(String.format("%s ???", currency));
                            }
                            if (transfer.getPlusPlayerName() != null) {
                                holder.plusPlayerName.setText(transfer.getPlusPlayerName());
                            } else {
                                holder.plusImage.setVisibility(View.GONE);
                                holder.plusPlayerName.setVisibility(View.GONE);
                            }

                        }
                    }
                });

        if (transfer.isFormerPlayer() == true) {
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
        private TextView wage;
        private TextView contractYearsText;
        private TextView contractYears;
        private ImageView line5;
        private TextView yearText;
        private TextView year;
        private TextView comments;
        //private Button editButton;
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
            wage = itemView.findViewById(R.id.wage_trf);
            contractYearsText = itemView.findViewById(R.id.contract_years_text_trf);
            contractYears = itemView.findViewById(R.id.contract_years_trf);
            line5 = itemView.findViewById(R.id.line_trf_5);
            yearText = itemView.findViewById(R.id.year_text_trf);
            year = itemView.findViewById(R.id.year_trf);
            comments = itemView.findViewById(R.id.comments_trf);
            //editButton = itemView.findViewById(R.id.edit_button_trf);
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

//            editButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    editTransfer(transferList.get(getAdapterPosition()));
//                }
//            });

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

//        private void editTransfer(final Transfer transfer) {
//            builder = new AlertDialog.Builder(context);
//            View view = LayoutInflater.from(context)
//                    .inflate(R.layout.edit_transfer_deal_popup, null);
//
//            final EditText firstNameEdit;
//            final EditText lastNameEdit;
//            final Spinner positionSpinnerEdit;
//            final EditText nationalityEdit;
//            final EditText overallEdit;
//            final EditText potentialLowEdit;
//            final EditText potentialHighEdit;
//            final Spinner typeOfTransferEdit;
//            final EditText oldTeamEdit;
//            final EditText newTeamEdit;
//            final TextInputLayout feeTil;
//            final EditText feeEdit;
//            final Spinner plusPlayerSpinnerEdit;
//            final TextInputLayout wageTil;
//            final EditText wageEdit;
//            final EditText contractYearsEdit;
//            final Spinner yearEdit;
//            final EditText commentsEdit;
//            Button editTransferButton;
//
//            firstNameEdit = view.findViewById(R.id.first_name_trf_edit);
//            lastNameEdit = view.findViewById(R.id.last_name_trf_edit);
//            positionSpinnerEdit = view.findViewById(R.id.position_spinner_trf_edit);
//            nationalityEdit = view.findViewById(R.id.nationality_trf_edit);
//            overallEdit = view.findViewById(R.id.overall_trf_edit);
//            potentialLowEdit = view.findViewById(R.id.potential_low_trf_edit);
//            potentialHighEdit = view.findViewById(R.id.potential_high_trf_edit);
//            typeOfTransferEdit = view.findViewById(R.id.type_of_transfer_spinner_trf_edit);
//            oldTeamEdit = view.findViewById(R.id.old_team_trf_edit);
//            newTeamEdit = view.findViewById(R.id.new_team_trf_edit);
//            feeTil = view.findViewById(R.id.fee_til_trf_edit);
//            feeEdit = view.findViewById(R.id.fee_trf_edit);
//            plusPlayerSpinnerEdit = view.findViewById(R.id.plus_player_spinner_trf_edit);
//            wageTil = view.findViewById(R.id.wage_til_trf_edit);
//            wageEdit = view.findViewById(R.id.wage_trf_edit);
//            contractYearsEdit = view.findViewById(R.id.contract_years_trf_edit);
//            yearEdit = view.findViewById(R.id.year_spinner_trf_edit);
//            commentsEdit = view.findViewById(R.id.comments_trf_edit);
//            editTransferButton = view.findViewById(R.id.edit_transfer_button);
//
//            ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
//            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            yearEdit.setAdapter(yearAdapter);
//
//            managersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                    .whereEqualTo("id", managerId)
//                    .get()
//                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//                        @Override
//                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
//                            if (!queryDocumentSnapshots.isEmpty()) {
//                                for (QueryDocumentSnapshot doc: queryDocumentSnapshots) {
//                                    Manager manager = doc.toObject(Manager.class);
//                                    String currency = manager.getCurrency();
//                                    feeTil.setHint("Fee (in " + currency + ")");
//                                    wageTil.setHint("Wage (in " + currency + ")");
//                                }
//                            }
//                        }
//                    });
//
//            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            positionSpinnerEdit.setAdapter(adapter);
//
//            final String[] transferArray = {"BOUGHT FROM ANOTHER TEAM",
//                                        "TRANSFER TO ANOTHER TEAM",
//                                        "FREE TRANSFER",
//                                        "SHORT TERM LOAN",
//                                        "ONE-YEAR LOAN",
//                                        "TWO-YEAR LOAN"};
//
//            ArrayAdapter<String> transferAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, transferArray);
//            transferAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            typeOfTransferEdit.setAdapter(transferAdapter);
//
//            firstNameEdit.setText(transfer.getFirstName());
//            lastNameEdit.setText(transfer.getLastName());
//            positionSpinnerEdit.setSelection(adapter.getPosition(transfer.getPosition()));
//            nationalityEdit.setText(transfer.getNationality());
//            overallEdit.setText(String.valueOf(transfer.getOverall()));
//            potentialLowEdit.setText(String.valueOf(transfer.getPotentialLow()));
//            potentialHighEdit.setText(String.valueOf(transfer.getPotentialHigh()));
//            typeOfTransferEdit.setSelection(transferAdapter.getPosition(transfer.getType()));
//            oldTeamEdit.setText(transfer.getFormerTeam());
//            newTeamEdit.setText(transfer.getCurrentTeam());
//            feeEdit.setText(String.valueOf(transfer.getTransferFee()));
//            wageEdit.setText(String.valueOf(transfer.getWage()));
//            contractYearsEdit.setText(String.valueOf(transfer.getContractYears()));
//            yearEdit.setSelection(yearAdapter.getPosition(transfer.getYear()));
//            commentsEdit.setText(transfer.getComments());
//
//            builder.setView(view);
//            final AlertDialog dialog = builder.create();
//            dialog.show();
//
//            editTransferButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (!lastNameEdit.getText().toString().isEmpty() &&
//                            !nationalityEdit.getText().toString().isEmpty() &&
//                            !positionSpinnerEdit.getSelectedItem().toString().isEmpty() &&
//                            !overallEdit.getText().toString().isEmpty() &&
//                            !typeOfTransferEdit.getSelectedItem().toString().isEmpty() &&
//                            !oldTeamEdit.getText().toString().isEmpty() &&
//                            !newTeamEdit.getText().toString().isEmpty() &&
//                            !yearEdit.getSelectedItem().toString().equals("0")) {
//                        transfersColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
//                                .whereEqualTo("managerId", managerId)
//                                .get()
//                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                                    @Override
//                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                                        if (task.isSuccessful()) {
//                                            List<DocumentSnapshot> doc =  task.getResult().getDocuments();
//                                            DocumentReference documentReference = null;
//                                            for (DocumentSnapshot ds: doc) {
//                                                Transfer editedTransfer = ds.toObject(Transfer.class);
//                                                if (editedTransfer.getId() == transfer.getId()) {
//                                                    documentReference = transfersColRef.document(ds.getId());
//                                                }
//                                            }
//                                            String ptlLow = potentialLowEdit.getText().toString().trim();
//                                            String ptlHi = potentialHighEdit.getText().toString().trim();
//                                            String trFee = feeEdit.getText().toString().trim();
//                                            String wg = wageEdit.getText().toString().trim();
//                                            String conYears = contractYearsEdit.getText().toString().trim();
//                                            String y = yearEdit.getSelectedItem().toString().trim();
//                                            String transferType = typeOfTransferEdit.getSelectedItem().toString().trim();
//                                            assert documentReference != null;
//                                            documentReference.update("firstName", firstNameEdit.getText().toString().trim(),
//                                                    "lastName", lastNameEdit.getText().toString().trim(),
//                                                    "fullName", firstNameEdit.getText().toString().trim() + " " + lastNameEdit.getText().toString().trim(),
//                                                    "position", positionSpinnerEdit.getSelectedItem().toString().trim(),
//                                                    "nationality", nationalityEdit.getText().toString().trim(),
//                                                    "overall", Integer.parseInt(overallEdit.getText().toString().trim()),
//                                                    "potentialLow", (!ptlLow.isEmpty()) ? Integer.parseInt(ptlLow) : 0,
//                                                    "potentialHigh", (!ptlHi.isEmpty()) ? Integer.parseInt(ptlHi) : 0,
//                                                    "type", typeOfTransferEdit.getSelectedItem().toString().trim(),
//                                                    "formerTeam", oldTeamEdit.getText().toString().trim(),
//                                                    "currentTeam", newTeamEdit.getText().toString().trim(),
//                                                    "transferFee", (!trFee.isEmpty()) ? Integer.parseInt(trFee) : 0,
//                                                    "wage", (!wg.isEmpty()) ? Integer.parseInt(wg) : 0,
//                                                    "contractYears", (!conYears.isEmpty()) ? Integer.parseInt(conYears) : 0,
//                                                    "year", (!y.equals("0")) ? y : "0",
//                                                    "formerPlayer", (transferType.equals(transferArray[1])) ? true : false)
//                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                        @Override
//                                                        public void onSuccess(Void aVoid) {
//                                                            notifyItemChanged(getAdapterPosition(), transfer);
//                                                            dialog.dismiss();
//                                                            Intent intent = new Intent(context, TransferDealsActivity.class);
//                                                            intent.putExtra("managerId", managerId);
//                                                            intent.putExtra("team", team);
//                                                            context.startActivity(intent);
//                                                            ((Activity)context).finish();
//                                                            Toast.makeText(context, "Transfer updated!", Toast.LENGTH_LONG)
//                                                                    .show();
//                                                        }
//                                                    });
//
//                                        }
//                                    }
//                                });
//                    } else {
//                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Type of Transfer, Old Team, New Team and Year Signed/Left are required", Toast.LENGTH_LONG)
//                                .show();
//                    }
//
//                }
//            });
//        }
    }
}
