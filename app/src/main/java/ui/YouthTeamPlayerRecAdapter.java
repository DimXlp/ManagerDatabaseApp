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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.List;
import java.util.Map;

import model.FirstTeamPlayer;
import model.FormerPlayer;
import model.YouthTeamPlayer;
import util.NationalityFlagUtil;
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

        holder.playerTopBar.setOnClickListener(v -> {
            boolean isVisible = holder.details.getVisibility() == View.VISIBLE;
            if (isVisible) {
                collapseView(holder.details);
            } else {
                expandView(holder.details);
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

        boolean hasScouted = player.getYearScouted() != null && !player.getYearScouted().equals("0");

        holder.scoutedText.setText(hasScouted ? player.getYearScouted() : null);
        holder.scoutedText.setVisibility(hasScouted ? View.VISIBLE : View.GONE);
        holder.scoutedIcon.setVisibility(hasScouted ? View.VISIBLE : View.GONE);
        holder.yearContainer.setVisibility(hasScouted ? View.VISIBLE : View.GONE);

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

    public static void expandView(final View view) {
        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = view.getMeasuredHeight();

        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.setDuration(200); // ms
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.start();
    }

    public static void collapseView(final View view) {
        final int initialHeight = view.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.setDuration(200); // ms
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
        animator.start();
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
        private TextView scoutedText;
        private View scoutedIcon;
        private View yearContainer;
        private LinearLayout details;
        private ImageView actionMenu;

        private TextView yearSigned;
        private Button saveButton;

        private TextView yearLeft;
        private Button setYearButton;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            playerTopBar = itemView.findViewById(R.id.player_top_bar_ytp);
            numberText = itemView.findViewById(R.id.player_number_ytp);
            fullNameText = itemView.findViewById(R.id.player_full_name_ytp);
            basicInfo = itemView.findViewById(R.id.player_basic_text_ytp);
            playerFlag = itemView.findViewById(R.id.player_flag_ytp);
            playerNationality = itemView.findViewById(R.id.player_nationality_ytp);
            scoutedText = itemView.findViewById(R.id.year_scouted_text_ytp);
            scoutedIcon = itemView.findViewById(R.id.year_scouted_icon_ytp);
            yearContainer = itemView.findViewById(R.id.player_year_info_container_ytp);
            details = itemView.findViewById(R.id.expandable_section_ytp);
            actionMenu = itemView.findViewById(R.id.player_action_menu_ytp);

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
                inflater.inflate(R.menu.youth_player_actions_menu, popupMenu.getMenu());

                var position = getAdapterPosition();
                var player = playerList.get(position);

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.action_edit_ytp:
                            clickEditPlayerButton(player);
                            return true;
                        case R.id.action_depart_ytp:
                            clickDepartPlayerButton();
                            return true;
                        case R.id.action_promote_ytp:
                            clickPromotePlayerButton();
                            return true;
                        case R.id.action_delete_ytp:
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

        private void clickPromotePlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            BottomSheetDialog promoteDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
                            View view = LayoutInflater.from(context).inflate(R.layout.year_signed_popup, null);
                            promoteDialog.setContentView(view);

                            yearSigned = view.findViewById(R.id.year_signed_picker_ytp_promote);
                            saveButton = view.findViewById(R.id.promote_button_year_signed);

                            String[] years = context.getResources().getStringArray(R.array.years_array);

                            yearSigned.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Year Signed")
                                        .setItems(years, (pickerDialog, item) -> yearSigned.setText(years[item]))
                                        .show();
                            });

                            promoteDialog.show();

                            saveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!yearSigned.getText().toString().isEmpty()) {
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

        private void clickDepartPlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            BottomSheetDialog departDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
                            View view = LayoutInflater.from(context).inflate(R.layout.year_left_popup, null);
                            departDialog.setContentView(view);

                            yearLeft = view.findViewById(R.id.year_left_picker_left);
                            saveButton = view.findViewById(R.id.set_year_left_button);

                            String[] years = context.getResources().getStringArray(R.array.years_array);

                            yearLeft.setOnClickListener(v -> {
                                new AlertDialog.Builder(context)
                                        .setTitle("Select Year Left")
                                        .setItems(years, (pickerDialog, item) -> yearLeft.setText(years[item]))
                                        .show();
                            });

                            departDialog.show();

                            saveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!yearLeft.getText().toString().isEmpty()) {
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
                                                fmPlayer.setYearLeft(yearLeft.getText().toString().trim());
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
                                                ftPlayer.setYearSigned(yearSigned.getText().toString().trim());
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

        private void clickEditPlayerButton(final YouthTeamPlayer player) {
            Log.d(LOG_TAG, "Opening edit dialog for player: " + player.getFullName());

            BottomSheetDialog editDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.create_youth_team_player_popup, null);
            editDialog.setContentView(view);

            TextView title = view.findViewById(R.id.create_yt_player);
            title.setText(R.string.edit_player_title);
            final EditText firstName = view.findViewById(R.id.first_name_ytp_create);
            final EditText lastName = view.findViewById(R.id.last_name_ytp_create);
            final TextView positionPicker = view.findViewById(R.id.position_picker_ytp_create);
            final EditText number = view.findViewById(R.id.number_ytp_create);
            final AutoCompleteTextView nationality = view.findViewById(R.id.nationality_ytp_create);
            final EditText overall = view.findViewById(R.id.overall_ytp_create);
            final EditText potentialLow = view.findViewById(R.id.potential_low_ytp_create);
            final EditText potentialHigh = view.findViewById(R.id.potential_high_ytp_create);
            final TextView yearScoutedPicker = view.findViewById(R.id.year_scouted_picker_ytp_create);
            Button savePlayerButton = view.findViewById(R.id.create_yt_player_button);
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
            yearScoutedPicker.setText(player.getYearScouted());

            editDialog.show();

            savePlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Attempting to save changes for player: " + player.getFullName());

                    if (!lastName.getText().toString().isEmpty() &&
                        !nationality.getText().toString().isEmpty() &&
                        !positionPicker.getText().toString().isEmpty() &&
                        !overall.getText().toString().isEmpty() &&
                        !yearScoutedPicker.getText().toString().isEmpty()) {
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
                                            String nationalityPlayer = nationality.getText().toString().trim();
                                            Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
                                            String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            assert documentReference != null;
                                            documentReference.update("firstName", firstName.getText().toString().trim(),
                                                    "lastName", lastName.getText().toString().trim(),
                                                    "fullName", firstName.getText().toString().trim() + " " + lastName.getText().toString().trim(),
                                                    "position", positionPicker.getText().toString().trim(),
                                                    "number", (!no.isEmpty()) ? Integer.parseInt(no) : 99,
                                                    "nationality", nationalityInput,
                                                    "overall", Integer.parseInt(overall.getText().toString().trim()),
                                                    "potentialLow", (!ptlLow.isEmpty()) ? Integer.parseInt(ptlLow) : 0,
                                                    "potentialHigh", (!ptlHi.isEmpty()) ? Integer.parseInt(ptlHi) : 0,
                                                    "yearScouted", yearScoutedPicker.getText().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(LOG_TAG, "Successfully updated player: " + player.getFullName());
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            editDialog.dismiss();
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
