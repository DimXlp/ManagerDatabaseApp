package util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Locale;

public class ValueFormatter {
    
    public static EditText formatValue(EditText value) {
        value.addTextChangedListener(new TextWatcher() {
            private String currentText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(currentText)) {
                    value.removeTextChangedListener(this);

                    // Remove commas to parse the input as a raw number
                    String cleanString = s.toString().replaceAll(",", "");

                    try {
                        // Parse the input as a long and format it with commas
                        long parsed = Long.parseLong(cleanString);
                        String formatted = String.format(Locale.US, "%,d", parsed);

                        currentText = formatted;
                        value.setText(formatted);
                        value.setSelection(formatted.length()); // Move cursor to the end
                    } catch (NumberFormatException e) {
                        // Handle any parsing errors here if needed
                        e.printStackTrace();
                    }

                    value.addTextChangedListener(this);
                }
            }
        });

        return value;
    }
}
