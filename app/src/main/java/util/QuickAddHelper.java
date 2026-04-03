package util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dimxlp.managerdb.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;

import java.util.Date;

import model.ShortlistedPlayer;

/**
 * Helper class for quick player entry with minimal fields
 */
public class QuickAddHelper {
    
    /**
     * Show a simplified quick-add dialog for shortlisted players
     * Only requires: Name, Overall, Team (optional: Position, Nationality)
     */
    public static void showQuickAddDialog(Activity activity, 
                                          long managerId, 
                                          String currency,
                                          OnPlayerCreatedListener listener) {
        
        BottomSheetDialog quickDialog = new BottomSheetDialog(activity, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(activity).inflate(R.layout.quick_add_player_popup, null);
        quickDialog.setContentView(view);
        
        EditText fullName = view.findViewById(R.id.full_name_quick);
        TextView positionPicker = view.findViewById(R.id.position_picker_quick);
        EditText team = view.findViewById(R.id.team_quick);
        AutoCompleteTextView nationality = view.findViewById(R.id.nationality_quick);
        EditText overall = view.findViewById(R.id.overall_quick);
        
        // Setup nationality autocomplete
        String[] countrySuggestions = activity.getResources().getStringArray(R.array.nationalities);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_dropdown_item_1line, countrySuggestions);
        nationality.setAdapter(adapter);
        
        // Setup position picker
        String[] positions = activity.getResources().getStringArray(R.array.position_array);
        positionPicker.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(activity)
                    .setTitle("Select Position")
                    .setItems(positions, (dialog, which) -> positionPicker.setText(positions[which]))
                    .show();
        });
        
        // Quick add button
        view.findViewById(R.id.quick_add_button).setOnClickListener(v -> {
            String nameText = fullName.getText().toString().trim();
            String teamText = team.getText().toString().trim();
            String overallText = overall.getText().toString().trim();
            
            if (nameText.isEmpty() || overallText.isEmpty()) {
                Toast.makeText(activity, "Name and Overall are required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create player with minimal data
            ShortlistedPlayer player = new ShortlistedPlayer();
            
            // Parse name (assume last word is last name if multiple words)
            String[] nameParts = nameText.split("\\s+");
            if (nameParts.length > 1) {
                player.setFirstName(nameText.substring(0, nameText.lastIndexOf(" ")).trim());
                player.setLastName(nameParts[nameParts.length - 1]);
            } else {
                player.setFirstName("");
                player.setLastName(nameText);
            }
            
            player.setFullName(nameText);
            player.setOverall(Integer.parseInt(overallText));
            player.setTeam(teamText.isEmpty() ? "Unknown" : teamText);
            player.setPosition(positionPicker.getText().toString().isEmpty() ? "Unknown" : positionPicker.getText().toString());
            player.setNationality(nationality.getText().toString().isEmpty() ? "Unknown" : nationality.getText().toString());
            player.setManagerId(managerId);
            player.setUserId(UserApi.getInstance().getUserId());
            player.setTimeAdded(new Timestamp(new Date()));
            player.setComments("Quick add");
            
            quickDialog.dismiss();
            listener.onPlayerCreated(player);
        });
        
        quickDialog.show();
    }
    
    public interface OnPlayerCreatedListener {
        void onPlayerCreated(ShortlistedPlayer player);
    }
}

