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
import com.dimxlp.managerdb.YouthTeamActivity;
import com.dimxlp.managerdb.YouthTeamListActivity;
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

import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.YouthTeamPlayer;
import util.UserApi;

public class YouthTeamPlayerRecAdapter extends RecyclerView.Adapter<YouthTeamPlayerRecAdapter.ViewHolder> {

    private static final String LOG_TAG = "RAFI|YouthTeamPlayerRecAdapter";
    private Context context;
    private List<YouthTeamPlayer> playerList;
    private long managerId;
    private String team;
    private String barYear;
    private int buttonInt;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference ytPlayersReference = db.collection("YouthTeamPlayers");
    private CollectionReference ftPlayersReference = db.collection("FirstTeamPlayers");
    private CollectionReference fmPlayersReference = db.collection("FormerPlayers");

    private Animation slideLeft;
    private Animation slideRight;

    public YouthTeamPlayerRecAdapter(Context context, List<YouthTeamPlayer> playerList, long managerId, String team, String barYear, int buttonInt) {
        this.context = context;
        this.playerList = playerList;
        this.managerId = managerId;
        this.team = team;
        this.barYear = barYear;
        this.buttonInt = buttonInt;
    }

    @NonNull
    @Override
    public YouthTeamPlayerRecAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.yt_player_row, parent, false);
        return new YouthTeamPlayerRecAdapter.ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull YouthTeamPlayerRecAdapter.ViewHolder holder, int position) {
        YouthTeamPlayer player = playerList.get(position);

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
        holder.yearScoutedDateText.setText(player.getYearScouted());

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
        private ImageView positionOval;
        private TextView positionText;
        private TextView overallNoText;
        private TextView potentialNoText;
        private TextView countryText;
        private TextView yearScoutedDateText;
        private RelativeLayout details;
        private Button promoteButton;
        private Button deleteButton;
        private Button editButton;
        private Button departButton;

        private Spinner yearSigned;
        private Button saveButton;

        private Spinner yearLeft;
        private Button setYearButton;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            numberText = itemView.findViewById(R.id.number_ytp);
            fullNameText = itemView.findViewById(R.id.full_name_ytp);
            positionOval = itemView.findViewById(R.id.position_oval_ytp);
            positionText = itemView.findViewById(R.id.position_ytp);
            overallNoText = itemView.findViewById(R.id.overall_ytp);
            potentialNoText = itemView.findViewById(R.id.potential_ytp);
            countryText = itemView.findViewById(R.id.nationality_ytp);
            yearScoutedDateText = itemView.findViewById(R.id.year_scouted_ytp);
            details = itemView.findViewById(R.id.details_ytp);
            promoteButton = itemView.findViewById(R.id.promote_button_ytp);
            deleteButton = itemView.findViewById(R.id.delete_button_ytp);
            editButton = itemView.findViewById(R.id.edit_button_ytp);
            departButton = itemView.findViewById(R.id.depart_button_ytp);

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

            promoteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    aBuilder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.year_signed_popup, null);

                                    yearSigned = view.findViewById(R.id.year_signed_spinner_ytp_promote);
                                    saveButton = view.findViewById(R.id.promote_button_year_signed);

                                    ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
                                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    yearSigned.setAdapter(yearAdapter);

                                    aBuilder.setView(view);
                                    aDialog = aBuilder.create();
                                    aDialog.show();

                                    saveButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!yearSigned.getSelectedItem().toString().equals("0")) {
                                                promotePlayer(playerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Field required", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to promote this player to the First Team?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
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
                                    View view = LayoutInflater.from(context).inflate(R.layout.year_left_popup, null);

                                    yearLeft = view.findViewById(R.id.year_left_spinner_left);
                                    saveButton = view.findViewById(R.id.set_year_left_button);

                                    ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
                                    yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                    yearLeft.setAdapter(yearAdapter);

                                    aBuilder.setView(view);
                                    aDialog = aBuilder.create();
                                    aDialog.show();

                                    saveButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (!yearLeft.getSelectedItem().toString().equals("0")) {
                                                letPlayerLeave(playerList.get(getAdapterPosition()));
                                            } else {
                                                Toast.makeText(context, "Field required", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to let this player leave the Youth Team?").setPositiveButton("Yes", dialogClickListener)
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

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editPlayer(playerList.get(getAdapterPosition()));
                }
            });
        }

        private void letPlayerLeave(final YouthTeamPlayer player) {
            Log.d(LOG_TAG, "Attempting to let player leave: " + player.getFullName());

            ytPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    YouthTeamPlayer ytPlayer = ds.toObject(YouthTeamPlayer.class);
                                    if (ytPlayer.getId() == player.getId()) {
                                        documentReference = ytPlayersReference.document(ds.getId());
                                        Log.d(LOG_TAG, "Found document for player: " + ytPlayer.getFullName());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Successfully deleted player document: " + player.getFullName());

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
                                                fmPlayer.setYearSigned("0");
                                                fmPlayer.setYearScouted(player.getYearScouted());
                                                fmPlayer.setYearLeft(yearLeft.getSelectedItem().toString().trim());
                                                fmPlayer.setManagerId(managerId);
                                                fmPlayer.setUserId(UserApi.getInstance().getUserId());
                                                fmPlayer.setTimeAdded(new Timestamp(new Date()));
                                                fmPlayersReference.add(fmPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Log.d(LOG_TAG, "FormerPlayer added successfully: " + player.getFullName());
                                                                Toast.makeText(context, "Player transferred!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        });
                                                ytPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, YouthTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, YouthTeamActivity.class);
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
                                Log.e(LOG_TAG, "Failed to fetch player document: " + task.getException());
                            }
                        }
                    });
        }

        private void promotePlayer(final YouthTeamPlayer player) {
            Log.d(LOG_TAG, "Attempting to promote player: " + player.getFullName());

            ytPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    YouthTeamPlayer ytPlayer = ds.toObject(YouthTeamPlayer.class);
                                    if (ytPlayer.getId() == player.getId()) {
                                        documentReference = ytPlayersReference.document(ds.getId());
                                        Log.d(LOG_TAG, "Found document for player: " + ytPlayer.getFullName());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Successfully deleted YouthTeamPlayer document: " + player.getFullName());

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
                                                ftPlayer.setYearSigned(yearSigned.getSelectedItem().toString().trim());
                                                ftPlayer.setYearScouted(player.getYearScouted());
                                                ftPlayer.setManagerId(managerId);
                                                ftPlayer.setLoanPlayer(false);
                                                ftPlayer.setUserId(UserApi.getInstance().getUserId());
                                                ftPlayer.setTimeAdded(new Timestamp(new Date()));
                                                ftPlayersReference.add(ftPlayer)
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                            @Override
                                                            public void onSuccess(DocumentReference documentReference) {
                                                                Log.d(LOG_TAG, "FirstTeamPlayer added successfully: " + player.getFullName());
                                                                Toast.makeText(context, "Player was promoted!", Toast.LENGTH_LONG)
                                                                        .show();
                                                            }
                                                        });

                                                ytPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, YouthTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, YouthTeamActivity.class);
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

        private void deletePlayer(final YouthTeamPlayer player) {
            Log.d(LOG_TAG, "Attempting to delete player: " + player.getFullName());

            ytPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    YouthTeamPlayer ytPlayer = ds.toObject(YouthTeamPlayer.class);
                                    if (ytPlayer.getId() == player.getId()) {
                                        documentReference = ytPlayersReference.document(ds.getId());
                                        Log.d(LOG_TAG, "Found document for player: " + ytPlayer.getFullName());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Successfully deleted player: " + player.getFullName());
                                                Toast.makeText(context, "Player deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                ytPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                                        .whereEqualTo("managerId", managerId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    if (task.getResult().size() > 0) {
                                                                        Intent intent = new Intent(context, YouthTeamListActivity.class);
                                                                        intent.putExtra("managerId", managerId);
                                                                        intent.putExtra("team", team);
                                                                        intent.putExtra("barYear", barYear);
                                                                        context.startActivity(intent);
                                                                        ((Activity)context).finish();
                                                                    } else {
                                                                        Intent intent = new Intent(context, YouthTeamActivity.class);
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
                                Log.e(LOG_TAG, "Failed to fetch player document: " + task.getException());
                            }
                        }
                    });
        }

        private void editPlayer(final YouthTeamPlayer player) {
            Log.d(LOG_TAG, "Opening edit dialog for player: " + player.getFullName());

            builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.create_youth_team_player_popup, null);

            TextView title;
            final EditText firstName;
            final EditText lastName;
            final Spinner positionSpinner;
            final EditText number;
            final EditText nationality;
            final EditText overall;
            final EditText potentialLow;
            final EditText potentialHigh;
            final Spinner yearScouted;
            Button savePlayerButton;

            title = view.findViewById(R.id.create_yt_player);
            title.setText(R.string.edit_player_title);
            firstName = view.findViewById(R.id.first_name_ytp_create);
            lastName = view.findViewById(R.id.last_name_ytp_create);
            positionSpinner = view.findViewById(R.id.position_spinner_ytp_create);
            number = view.findViewById(R.id.number_ytp_create);
            nationality = view.findViewById(R.id.nationality_ytp_create);
            overall = view.findViewById(R.id.overall_ytp_create);
            potentialLow = view.findViewById(R.id.potential_low_ytp_create);
            potentialHigh = view.findViewById(R.id.potential_high__ytp_create);
            yearScouted = view.findViewById(R.id.year_scouted_spinner_ytp_create);
            savePlayerButton = view.findViewById(R.id.create_yt_player_button);
            savePlayerButton.setText(R.string.save_player);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.position_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            positionSpinner.setAdapter(adapter);

            ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(context, R.array.years_array, android.R.layout.simple_spinner_item);
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            yearScouted.setAdapter(yearAdapter);

            firstName.setText(player.getFirstName());
            lastName.setText(player.getLastName());
            positionSpinner.setSelection(adapter.getPosition(player.getPosition()));
            number.setText(String.valueOf(player.getNumber()));
            nationality.setText(player.getNationality());
            overall.setText(String.valueOf(player.getOverall()));
            potentialLow.setText(String.valueOf(player.getPotentialLow()));
            potentialHigh.setText(String.valueOf(player.getPotentialHigh()));
            yearScouted.setSelection(yearAdapter.getPosition(player.getYearScouted()));

            builder.setView(view);
            dialog = builder.create();
            dialog.show();

            savePlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Attempting to save changes for player: " + player.getFullName());

                    if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionSpinner.getSelectedItem().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty() &&
                        !yearScouted.getSelectedItem().toString().equals("0")) {
                        ytPlayersReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                YouthTeamPlayer ytplayer = ds.toObject(YouthTeamPlayer.class);
                                                if (ytplayer.getId() == player.getId()) {
                                                    documentReference = ytPlayersReference.document(ds.getId());
                                                    Log.d(LOG_TAG, "Document found for player: " + player.getFullName());
                                                }
                                            }
                                            String no = number.getText().toString().trim();
                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
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
                                                    "yearScouted", yearScouted.getSelectedItem().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(LOG_TAG, "Successfully updated player: " + player.getFullName());
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            dialog.dismiss();
                                                            Intent intent = new Intent(context, YouthTeamListActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barYear", barYear);
                                                            context.startActivity(intent);
                                                            ((Activity) context).finish();
                                                            Toast.makeText(context, "Player updated!", Toast.LENGTH_LONG)
                                                                    .show();
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(LOG_TAG, "Failed to update player: " + e.getMessage());
                                                    });
                                        }
                                    }
                                });
                    } else {
                        Log.w(LOG_TAG, "Validation failed. Fields are missing for player: " + player.getFullName());
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall and Year Scouted are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }
    }
}
