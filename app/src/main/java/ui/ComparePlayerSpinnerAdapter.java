package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;

import com.dimxlp.managerdb.R;

import java.util.ArrayList;
import java.util.List;

public class ComparePlayerSpinnerAdapter extends ArrayAdapter<String> {
    private final List<String> items;

    public ComparePlayerSpinnerAdapter(@NonNull Context context, List<String> items) {
        super(context, R.layout.spinner_player_item, items);
        this.items = items;
    }

    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.spinner_player_item, parent, false);
        }

        TextView textView = (TextView) convertView;
        String item = items.get(position);

        // Apply a different style for headers
        if (item.equals("First Team") || item.equals("Youth Team")) {
            textView.setText(item);
            textView.setBackgroundColor(0xFF616161); // Gray background
            textView.setTextColor(0xFFFFFFFF); // White text color
            textView.setPadding(16, 16, 16, 16); // Extra padding for headers
            textView.setTextSize(16);
            textView.setTypeface(null, android.graphics.Typeface.BOLD); // Bold for headers
        } else {
            textView.setText(item);
            textView.setBackgroundColor(0x00000000); // Transparent for regular items
            textView.setTextColor(0xFF000000); // Black text color for items
            textView.setPadding(16, 8, 16, 8); // Standard padding for players
            textView.setTextSize(14);
            textView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        return convertView;
    }
}
