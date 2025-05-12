package ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

import com.dimxlp.managerdb.FormerPlayersListActivity;
import com.dimxlp.managerdb.R;
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

import model.FormerPlayer;
import model.ShortlistedPlayer;
import util.AnimationUtil;
import util.NationalityFlagUtil;
import util.UserApi;

public class FormerPlayerRecAdapter extends RecyclerView.Adapter<FormerPlayerRecAdapter.ViewHolder> {

    private static final String LOG_TAG = "RAFI|FormerPlayerRecAdapter";
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

    private BottomSheetDialog editDialog;

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
        boolean hasLeft = player.getYearLeft() != null && !player.getYearLeft().equals("0");

        holder.signedText.setText(hasSigned ? player.getYearSigned() : null);
        holder.signedText.setVisibility(hasSigned ? View.VISIBLE : View.GONE);
        holder.signedIcon.setVisibility(hasSigned ? View.VISIBLE : View.GONE);

        holder.scoutedText.setText(hasScouted ? player.getYearScouted() : null);
        holder.scoutedText.setVisibility(hasScouted ? View.VISIBLE : View.GONE);
        holder.scoutedIcon.setVisibility(hasScouted ? View.VISIBLE : View.GONE);

        holder.leftText.setText(hasLeft ? player.getYearLeft() : null);
        holder.leftText.setVisibility(hasLeft ? View.VISIBLE : View.GONE);
        holder.leftIcon.setVisibility(hasLeft ? View.VISIBLE : View.GONE);

        holder.separator1.setVisibility((hasSigned && hasScouted) ? View.VISIBLE : View.GONE);
        holder.separator2.setVisibility(((hasScouted || hasSigned) && hasLeft) ? View.VISIBLE : View.GONE);
        holder.yearContainer.setVisibility((hasSigned || hasScouted || hasLeft) ? View.VISIBLE : View.GONE);

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

        private LinearLayout playerTopBar;
        private TextView numberText;
        private TextView fullNameText;
        private TextView basicInfo;
        private ImageView playerFlag;
        private TextView playerNationality;
        private TextView signedText;
        private TextView scoutedText;
        private TextView leftText;
        private View signedIcon;
        private View scoutedIcon;
        private View leftIcon;
        private TextView separator1;
        private TextView separator2;
        private View yearContainer;
        private LinearLayout details;
        private ImageView actionMenu;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);
            context = ctx;

            playerTopBar = itemView.findViewById(R.id.player_top_bar_fpl);
            numberText = itemView.findViewById(R.id.number_fpl);
            fullNameText = itemView.findViewById(R.id.full_name_fpl);
            basicInfo = itemView.findViewById(R.id.player_basic_text_fpl);
            playerFlag = itemView.findViewById(R.id.player_flag_fpl);
            playerNationality = itemView.findViewById(R.id.player_nationality_fpl);
            signedText = itemView.findViewById(R.id.year_signed_text_fpl);
            scoutedText = itemView.findViewById(R.id.year_scouted_text_fpl);
            leftText = itemView.findViewById(R.id.year_left_text_fpl);
            signedIcon = itemView.findViewById(R.id.year_signed_icon_fpl);
            scoutedIcon = itemView.findViewById(R.id.year_scouted_icon_fpl);
            leftIcon = itemView.findViewById(R.id.year_left_icon_fpl);
            separator1 = itemView.findViewById(R.id.year_separator_1_fpl);
            separator2 = itemView.findViewById(R.id.year_separator_2_fpl);
            yearContainer = itemView.findViewById(R.id.player_year_info_container_fpl);
            details = itemView.findViewById(R.id.expandable_section_fpl);
            actionMenu = itemView.findViewById(R.id.player_action_menu_fpl);

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
                inflater.inflate(R.menu.former_player_actions_menu, popupMenu.getMenu());

                var position = getAdapterPosition();
                var player = formerPlayerList.get(position);

                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.action_edit:
                            clickEditPlayerButton(player);
                            return true;
                        case R.id.action_shortlist:
                            clickShortlistPlayerButton();
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

        private void clickShortlistPlayerButton() {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case DialogInterface.BUTTON_POSITIVE:
                            AlertDialog.Builder aBuilder = new AlertDialog.Builder(context);
                            View view = LayoutInflater.from(context).inflate(R.layout.current_team_popup, null);

                            TextView currentTeam = view.findViewById(R.id.current_team_add_list);
                            Button setTeamButton = view.findViewById(R.id.set_team_button_add_list);

                            setTeamButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (!currentTeam.getText().toString().isEmpty()) {
                                        addToList(formerPlayerList.get(getAdapterPosition()), currentTeam);
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

        private void addToList(final FormerPlayer player, TextView currentTeam) {
            Log.d(LOG_TAG, "addToList called for player: " + player.getFullName());

            frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "FormerPlayer collection fetched successfully.");

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
                                                        Log.d(LOG_TAG, "Player added to ShortlistedPlayers: " + shPlayer.getFullName());
                                                        Toast.makeText(context, "Player added to the shortlist!", Toast.LENGTH_LONG)
                                                                .show();
                                                        Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                                        intent.putExtra("managerId", managerId);
                                                        intent.putExtra("team", team);
                                                        intent.putExtra("barTeam", barTeam);
                                                        context.startActivity(intent);
                                                        ((Activity)context).finish();}
                                                })
                                                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding player to ShortlistedPlayers.", e));
                                    }
                                }
                            } else {
                                Log.e(LOG_TAG, "Error fetching FormerPlayer collection.", task.getException());
                            }
                        }
                    });
        }

        private void deletePlayer(final FormerPlayer player) {
            Log.d(LOG_TAG, "deletePlayer called for player: " + player.getFullName());

            frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .whereEqualTo("managerId", managerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "FormerPlayer collection fetched successfully.");

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
                                                Log.d(LOG_TAG, "Player successfully deleted: " + player.getFullName());
                                                Toast.makeText(context, "Player deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                                intent.putExtra("managerId", managerId);
                                                intent.putExtra("team", team);
                                                intent.putExtra("barTeam", barTeam);
                                                context.startActivity(intent);
                                                ((Activity) context).finish();
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error deleting player.", e));
                            } else {
                                Log.e(LOG_TAG, "Error fetching FormerPlayer collection.", task.getException());
                            }
                        }
                    });
        }

        private void clickEditPlayerButton(final FormerPlayer player) {
            if (barTeam.equals("First Team")) {
                editFormerFirstTeamPlayer(player);
            } else {
                editFormerYouthTeamPlayer(player);
            }

        }

        private void editFormerYouthTeamPlayer(final FormerPlayer player) {
            Log.d(LOG_TAG, "editFormerYouthTeamPlayer called for player: " + player.getFullName());

            editDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.edit_former_player_popup, null);
            editDialog.setContentView(view);

            TextView title = view.findViewById(R.id.edit_frm_player);
            EditText firstName = view.findViewById(R.id.first_name_frm_edit);
            EditText lastName = view.findViewById(R.id.last_name_frm_edit);
            TextView positionPicker = view.findViewById(R.id.position_picker_frm_edit);
            EditText number = view.findViewById(R.id.number_frm_edit);
            AutoCompleteTextView nationality = view.findViewById(R.id.nationality_frm_edit);
            EditText overall = view.findViewById(R.id.overall_frm_edit);
            EditText potentialLow = view.findViewById(R.id.potential_low_frm_edit);
            EditText potentialHigh = view.findViewById(R.id.potential_high_frm_edit);
            TextView yearSigned = view.findViewById(R.id.year_signed_picker_frm_edit);
            TextView yearScouted = view.findViewById(R.id.year_scouted_picker_frm_edit);
            TextView yearLeft = view.findViewById(R.id.year_left_picker_frm_edit);
            Button editPlayerButton = view.findViewById(R.id.edit_frm_player_button);

            yearSigned.setVisibility(View.GONE);

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

            yearScouted.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Scouted")
                        .setItems(years, (pickerDialog, which) -> yearScouted.setText(years[which]))
                        .show();
            });

            yearLeft.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Left")
                        .setItems(years, (pickerDialog, which) -> yearLeft.setText(years[which]))
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
            yearSigned.setText(player.getYearSigned());
            yearScouted.setText(player.getYearScouted());
            yearLeft.setText(player.getYearLeft());
            Log.d(LOG_TAG, "Player data populated for editing: " + player.getFullName());

            editDialog.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Save button clicked for player: " + player.getFullName());

                    if (!lastName.getText().toString().isEmpty() &&
                            !nationality.getText().toString().isEmpty() &&
                            !positionPicker.getText().toString().isEmpty() &&
                            !overall.getText().toString().isEmpty() &&
                            !yearScouted.getText().toString().isEmpty() &&
                            !yearLeft.getText().toString().isEmpty()) {

                        frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(LOG_TAG, "FormerPlayer collection fetched successfully for updating.");

                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                FormerPlayer frmplayer = ds.toObject(FormerPlayer.class);
                                                if (frmplayer.getId() == player.getId()) {
                                                    documentReference = frmPlayerColRef.document(ds.getId());
                                                }
                                            }
                                            String no = number.getText().toString().trim();
                                            String nationalityPlayer = nationality.getText().toString().trim();
                                            Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
                                            String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            String yScouted = yearScouted.getText().toString().trim();
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
                                                    "yearSigned", "0",
                                                    "yearScouted", yScouted,
                                                    "yearLeft", yearLeft.getText().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(LOG_TAG, "Player successfully updated: " + player.getFullName());
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            editDialog.dismiss();
                                                            Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barTeam", barTeam);
                                                            context.startActivity(intent);
                                                            ((Activity) context).finish();
                                                            Toast.makeText(context, "Player edited!", Toast.LENGTH_LONG)
                                                                    .show();
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> Log.e(LOG_TAG, "Error updating player.", e));
                                        } else {
                                            Log.e(LOG_TAG, "Error fetching FormerPlayer collection.", task.getException());
                                        }
                                    }
                                });
                    } else {
                        Log.w(LOG_TAG, "Validation failed: Required fields are missing.");
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Year Signed and Year Left are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }

        private void editFormerFirstTeamPlayer(final FormerPlayer player) {
            Log.d(LOG_TAG, "editFormerFirstTeamPlayer called for player: " + player.getFullName());

            editDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.edit_former_player_popup, null);
            editDialog.setContentView(view);

            TextView title = view.findViewById(R.id.edit_frm_player);
            EditText firstName = view.findViewById(R.id.first_name_frm_edit);
            EditText lastName = view.findViewById(R.id.last_name_frm_edit);
            TextView positionPicker = view.findViewById(R.id.position_picker_frm_edit);
            EditText number = view.findViewById(R.id.number_frm_edit);
            AutoCompleteTextView nationality = view.findViewById(R.id.nationality_frm_edit);
            EditText overall = view.findViewById(R.id.overall_frm_edit);
            EditText potentialLow = view.findViewById(R.id.potential_low_frm_edit);
            EditText potentialHigh = view.findViewById(R.id.potential_high_frm_edit);
            TextView yearSigned = view.findViewById(R.id.year_signed_picker_frm_edit);
            TextView yearScouted = view.findViewById(R.id.year_scouted_picker_frm_edit);
            TextView yearLeft = view.findViewById(R.id.year_left_picker_frm_edit);
            Button editPlayerButton = view.findViewById(R.id.edit_frm_player_button);

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

            yearSigned.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Signed")
                        .setItems(years, (pickerDialog, which) -> yearSigned.setText(years[which]))
                        .show();
            });

            yearScouted.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Scouted")
                        .setItems(years, (pickerDialog, which) -> yearScouted.setText(years[which]))
                        .show();
            });

            yearLeft.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Select Year Left")
                        .setItems(years, (pickerDialog, which) -> yearLeft.setText(years[which]))
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
            yearSigned.setText(player.getYearSigned());
            yearScouted.setText(player.getYearScouted());
            yearLeft.setText(player.getYearLeft());
            Log.d(LOG_TAG, "Player data populated for editing: " + player.getFullName());

            editDialog.show();

            editPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Save button clicked for player: " + player.getFullName());

                    if (!lastName.getText().toString().isEmpty() &&
                            !nationality.getText().toString().isEmpty() &&
                            !positionPicker.getText().toString().isEmpty() &&
                            !overall.getText().toString().isEmpty() &&
                            !yearSigned.getText().toString().isEmpty() &&
                            !yearLeft.getText().toString().isEmpty()) {
                        Log.d(LOG_TAG, "Validation successful. Proceeding to update player.");

                        frmPlayerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                                .whereEqualTo("managerId", managerId)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(LOG_TAG, "FormerPlayer collection fetched successfully for updating.");
                                            List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                            DocumentReference documentReference = null;
                                            for (DocumentSnapshot ds : doc) {
                                                FormerPlayer frmplayer = ds.toObject(FormerPlayer.class);
                                                if (frmplayer.getId() == player.getId()) {
                                                    documentReference = frmPlayerColRef.document(ds.getId());
                                                }
                                            }
                                            String no = number.getText().toString().trim();
                                            String nationalityPlayer = nationality.getText().toString().trim();
                                            Map<String, String> variantMap = NationalityFlagUtil.getVariantToStandardMap();
                                            String nationalityInput = variantMap.getOrDefault(nationalityPlayer, nationalityPlayer);

                                            String ptlLow = potentialLow.getText().toString().trim();
                                            String ptlHi = potentialHigh.getText().toString().trim();
                                            String yScouted = yearScouted.getText().toString().trim();
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
                                                    "yearSigned", yearSigned.getText().toString().trim(),
                                                    "yearScouted", (!yScouted.equals("0")) ? yScouted : "0",
                                                    "yearLeft", yearLeft.getText().toString().trim())
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(LOG_TAG, "Player successfully updated: " + player.getFullName());
                                                            notifyItemChanged(getAdapterPosition(), player);
                                                            editDialog.dismiss();
                                                            Intent intent = new Intent(context, FormerPlayersListActivity.class);
                                                            intent.putExtra("managerId", managerId);
                                                            intent.putExtra("team", team);
                                                            intent.putExtra("barTeam", barTeam);
                                                            context.startActivity(intent);
                                                            ((Activity) context).finish();
                                                            Toast.makeText(context, "Player edited!", Toast.LENGTH_LONG)
                                                                    .show();
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> Log.e(LOG_TAG, "Error updating player.", e));
                                        } else {
                                            Log.e(LOG_TAG, "Error fetching FormerPlayer collection.", task.getException());
                                        }
                                    }
                                });
                    } else {
                        Log.w(LOG_TAG, "Validation failed: Required fields are missing.");
                        Toast.makeText(context, "Last Name/Nickname, Nationality, Position, Overall, Year Signed and Year Left are required", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }

    }
}
