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

import com.dimxlp.managerdb.FormerPlayersListActivity;
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

import java.util.Date;
import java.util.List;

import model.FormerPlayer;
import model.ShortlistedPlayer;
import util.UserApi;

public class FormerPlayerRecAdapter extends RecyclerView.Adapter<FormerPlayerRecAdapter.ViewHolder> {

    private Context context;
    private List<FormerPlayer> formerPlayerList;
    private long managerId;
    private String team;
    private String barTeam;
    private int buttonInt;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference frmPlayerColRef = db.collection("FormerPlayers");
    private CollectionReference ftPlayerColRef = db.collection("FirstTeamPlayers");
    private CollectionReference managersColRef = db.collection("Managers");
    private CollectionReference transfersColRef = db.collection("TransferDeals");

    private Animation slideLeft;
    private Animation slideRight;

    public FormerPlayerRecAdapter(Context context, List<FormerPlayer> formerPlayerList, long managerId, String team, String barTeam, int buttonInt) {
        this.context = context;
        this.formerPlayerList = formerPlayerList;
        this.managerId = managerId;
        this.team = team;
        this.barTeam = barTeam;
        this.buttonInt = buttonInt;
    }

    @NonNull
    @Override
    public FormerPlayerRecAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.former_player_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull FormerPlayerRecAdapter.ViewHolder holder, int position) {

        FormerPlayer player = formerPlayerList.get(position);

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
        String ySigned = player.getYearSigned();
        if (!ySigned.equals("0")) {
            holder.yearSignedDateText.setText(ySigned);
        } else {
            holder.yearSignedDateText.setText("????/??");
        }
        String yScouted = player.getYearScouted();
        if (!yScouted.equals("0")) {
            holder.yearScoutedDateText.setText(yScouted);
        } else {
            holder.yearScoutedDateText.setText("????/??");
        }
        holder.yearLeftDateText.setText(player.getYearLeft());

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
        return formerPlayerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView numberText;
        private TextView fullNameText;
        private TextView positionText;
        private TextView overallNoText;
        private TextView potentialNoText;
        private TextView countryText;
        private TextView yearSignedDateText;
        private TextView yearScoutedDateText;
        private TextView yearLeftDateText;
        private ImageView positionOval;
        private Button listAddButton;
        private Button deleteButton;
        private Button editButton;
        private EditText currentTeam;
        private Button setTeamButton;
        private RelativeLayout details;
        private AlertDialog dialog;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            numberText = itemView.findViewById(R.id.number_fpl);
            fullNameText = itemView.findViewById(R.id.full_name_fpl);
            positionText = itemView.findViewById(R.id.position_fpl);
            overallNoText = itemView.findViewById(R.id.overall_fpl);
            potentialNoText = itemView.findViewById(R.id.potential_fpl);
            countryText = itemView.findViewById(R.id.nationality_fpl);
            yearSignedDateText = itemView.findViewById(R.id.year_signed_fpl);
            yearScoutedDateText = itemView.findViewById(R.id.year_scouted_fpl);
            yearLeftDateText = itemView.findViewById(R.id.year_left_fpl);
            listAddButton = itemView.findViewById(R.id.list_add_button_fpl);
            positionOval = itemView.findViewById(R.id.position_oval_fpl);
            deleteButton = itemView.findViewById(R.id.delete_button_fpl);
            editButton = itemView.findViewById(R.id.edit_button_fpl);
            details = itemView.findViewById(R.id.details_fpl);

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
                    int position = getAdapterPosition();
                    editPlayer(formerPlayerList.get(position));
                }
            });

            listAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    AlertDialog.Builder aBuilder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.current_team_popup, null);

                                    currentTeam = view.findViewById(R.id.current_team_add_list);
                                    setTeamButton = view.findViewById(R.id.set_team_button_add_list);

                                    setTeamButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!currentTeam.getText().toString().isEmpty()) {
                                                addToList(formerPlayerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Field required", Toast.LENGTH_LONG).show();
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
                    builder.setMessage("Do you want to add this player to the shortlist?").setPositiveButton("Yes", dialogClickListener)
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
                                    deletePlayer(formerPlayerList.get(getAdapterPosition()));
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

        private void addToList(final FormerPlayer player) {
            frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                for (DocumentSnapshot ds : doc) {
                                    FormerPlayer frmPlayer = ds.toObject(FormerPlayer.class);
                                    if (frmPlayer.getId() == player.getId()) {
                                        ShortlistedPlayer shPlayer = new ShortlistedPlayer();
                                        shPlayer.setId(0);
                                        shPlayer.setFirstName(player.getFirstName());
                                        shPlayer.setLastName(player.getLastName());
                                        shPlayer.setFullName(player.getFullName());
                                        shPlayer.setPosition(player.getPosition());
                                        shPlayer.setNationality(player.getNationality());
                                        shPlayer.setOverall(player.getOverall());
                                        shPlayer.setPotentialLow(player.getPotentialLow());
                                        shPlayer.setPotentialHigh(player.getPotentialHigh());
                                        shPlayer.setTeam(currentTeam.getText().toString().trim());
                                        shPlayer.setComments("");
                                        shPlayer.setManagerId(managerId);
                                        shPlayer.setUserId(UserApi.getInstance().getUserId());
                                        shPlayer.setTimeAdded(new Timestamp(new Date()));
                                        db.collection("ShortlistedPlayers").add(shPlayer)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {
                                                        Toast.makeText(context, "Player added to the shortlist!", Toast.LENGTH_LONG)
                                                                .show();
                                                    }
                                                });
                                        Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                        intent.putExtra("managerId", managerId);
                                        intent.putExtra("team", team);
                                        intent.putExtra("barTeam", barTeam);
                                        context.startActivity(intent);
                                        ((Activity)context).finish();
                                    }
                                }
                            }
                        }
                    });
        }

        private void deletePlayer(final FormerPlayer player) {
            frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    FormerPlayer frmPlayer = ds.toObject(FormerPlayer.class);
                                    if (frmPlayer.getId() == player.getId()) {
                                        documentReference = frmPlayerColRef.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(context, "Player deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                intent.putExtra("barTeam", barTeam);
                                                context.startActivity(intent);
                                                ((Activity) context).finish();
                                            }
                                        });
                            }
                        }
                    });
        }

        private void editPlayer(final FormerPlayer player) {
            if (barTeam.equals("First Team")) {
                editFormerFirstTeamPlayer(player);
            } else {
                editFormerYouthTeamPlayer(player);
            }

        }

        private void editFormerYouthTeamPlayer(final FormerPlayer player) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.edit_former_player_popup, null);

            TextView title;
            final EditText firstName;
            final EditText lastName;
            final Spinner positionSpinner;
            final EditText number;
            final EditText nationality;
            final EditText overall;
            final EditText potentialLow;
            final EditText potentialHigh;
            final TextView yearSignedTitle;
            final Spinner yearSigned;
            final Spinner yearScouted;
            final Spinner yearLeft;
            Button editPlayerButton;

            title = view.findViewById(R.id.edit_frm_player);
            firstName = view.findViewById(R.id.first_name_frm_edit);
            lastName = view.findViewById(R.id.last_name_frm_edit);
            positionSpinner = view.findViewById(R.id.position_spinner_frm_edit);
            number = view.findViewById(R.id.number_frm_edit);
            nationality = view.findViewById(R.id.nationality_frm_edit);
            overall = view.findViewById(R.id.overall_frm_edit);
            potentialLow = view.findViewById(R.id.potential_low_frm_edit);
            potentialHigh = view.findViewById(R.id.potential_high__frm_edit);
            yearSignedTitle = view.findViewById(R.id.year_signed_text_frm_edit);
            yearSigned = view.findViewById(R.id.year_signed_spinner_frm_edit);
            yearScouted = view.findViewById(R.id.year_scouted_spinner_frm_edit);
            yearLeft = view.findViewById(R.id.year_left_spinner_frm_edit);
            editPlayerButton = view.findViewById(R.id.edit_frm_player_button);

            yearSignedTitle.setVisibility(View.GONE);
            yearSigned.setVisibility(View.GONE);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(adapter);

            ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            yearSigned.setAdapter(yearAdapter);
            yearScouted.setAdapter(yearAdapter);
            yearLeft.setAdapter(yearAdapter);

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
            yearLeft.setSelection(yearAdapter.getPosition(player.getYearLeft()));

            builder.setView(view);
            dialog = builder.create();
            dialog.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!lastName.getText().toString().isEmpty() &&
                            !nationality.getText().toString().isEmpty() &&
                            !positionSpinner.getSelectedItem().toString().isEmpty() &&
                            !overall.getText().toString().isEmpty() &&
                            !yearScouted.getSelectedItem().toString().equals("0") &&
                            !yearLeft.getSelectedItem().toString().equals("0")) {
                        frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                FormerPlayer frmplayer = ds.toObject(FormerPlayer.class);
                                                if (frmplayer.getId() == player.getId()) {
                                                    documentReference = frmPlayerColRef.document(ds.getId());
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
                                                    "yearSigned", "0",
                                                    "yearScouted", yScouted,
                                                    "yearLeft", yearLeft.getSelectedItem().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            dialog.dismiss();
                                                            Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barTeam", barTeam);
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
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Year Signed and Year Left are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }

        private void editFormerFirstTeamPlayer(final FormerPlayer player) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.edit_former_player_popup, null);

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
            final Spinner yearLeft;
            Button editPlayerButton;

            title = view.findViewById(R.id.edit_frm_player);
            firstName = view.findViewById(R.id.first_name_frm_edit);
            lastName = view.findViewById(R.id.last_name_frm_edit);
            positionSpinner = view.findViewById(R.id.position_spinner_frm_edit);
            number = view.findViewById(R.id.number_frm_edit);
            nationality = view.findViewById(R.id.nationality_frm_edit);
            overall = view.findViewById(R.id.overall_frm_edit);
            potentialLow = view.findViewById(R.id.potential_low_frm_edit);
            potentialHigh = view.findViewById(R.id.potential_high__frm_edit);
            yearSigned = view.findViewById(R.id.year_signed_spinner_frm_edit);
            yearScouted = view.findViewById(R.id.year_scouted_spinner_frm_edit);
            yearLeft = view.findViewById(R.id.year_left_spinner_frm_edit);
            editPlayerButton = view.findViewById(R.id.edit_frm_player_button);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(adapter);

            ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            yearSigned.setAdapter(yearAdapter);
            yearScouted.setAdapter(yearAdapter);
            yearLeft.setAdapter(yearAdapter);

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
            yearLeft.setSelection(yearAdapter.getPosition(player.getYearLeft()));

            builder.setView(view);
            final AlertDialog dialog1 = builder.create();
            dialog1.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!lastName.getText().toString().isEmpty() &&
                            !nationality.getText().toString().isEmpty() &&
                            !positionSpinner.getSelectedItem().toString().isEmpty() &&
                            !overall.getText().toString().isEmpty() &&
                            !yearSigned.getSelectedItem().toString().equals("0") &&
                            !yearLeft.getSelectedItem().toString().equals("0")) {
                        frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                FormerPlayer frmplayer = ds.toObject(FormerPlayer.class);
                                                if (frmplayer.getId() == player.getId()) {
                                                    documentReference = frmPlayerColRef.document(ds.getId());
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
                                                    "yearLeft", yearLeft.getSelectedItem().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            dialog1.dismiss();
                                                            Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barTeam", barTeam);
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
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Year Signed and Year Left are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }

    }
}
