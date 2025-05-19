package com.dimxlp.managerdb;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserProfileFragment extends Fragment {

    private static final String LOG_TAG = "RAFI|UserProfile";
    private AutoCompleteTextView emailInput;
    private TextView unverifiedNotice;
    private TextView resendVerificationLink;
    private TextView changePasswordText;
    private Button saveChangesButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        emailInput = view.findViewById(R.id.email_user_profile);
        unverifiedNotice = view.findViewById(R.id.email_unverified_notice);
        resendVerificationLink = view.findViewById(R.id.resend_verification_link);
        changePasswordText = view.findViewById(R.id.change_password_text);
        saveChangesButton = view.findViewById(R.id.login_button);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            emailInput.setText(firebaseUser.getEmail());

            if (!firebaseUser.isEmailVerified()) {
                unverifiedNotice.setVisibility(View.VISIBLE);
            } else {
                unverifiedNotice.setVisibility(View.GONE);
            }

            resendVerificationLink.setOnClickListener(v -> {
                firebaseUser.sendEmailVerification()
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(getContext(), "Verification email sent again.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        saveChangesButton.setOnClickListener(v -> saveUserChanges());
        changePasswordText.setOnClickListener(v -> showChangePasswordDialog());

        return view;
    }

    private void saveUserChanges() {
        if (firebaseUser == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.email_change_enter_password, null);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.reauth_password_input);
        Button continueButton = dialogView.findViewById(R.id.reauth_continue_button);
        String newEmail = emailInput.getText().toString().trim();

        if (!newEmail.isEmpty() && !newEmail.equals(firebaseUser.getEmail())) {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            continueButton.setOnClickListener(v -> {
                String password = passwordInput.getText().toString().trim();
                if (!password.isEmpty()) {
                    dialog.dismiss();
                    reauthenticateAndUpdateEmail(password, newEmail);
                } else {
                    passwordInput.setError("Enter your current password");
                }
            });

            dialog.show();
        }

        Toast.makeText(getContext(), "Changes saved.", Toast.LENGTH_SHORT).show();
    }

    private void reauthenticateAndUpdateEmail(String currentPassword, String newEmail) {
        AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPassword);
        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(authResult -> {
                    firebaseUser.updateEmail(newEmail)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(LOG_TAG, "Email updated safely");
                                verifyNewEmail();

                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Email update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Re-authentication failed: " + e.getMessage());
                    Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void verifyNewEmail() {
        firebaseUser.reload().addOnSuccessListener(aVoid -> {
            FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();

            if (refreshedUser != null) {
                if (!refreshedUser.isEmailVerified()) {
                    firebaseUser.sendEmailVerification()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(getContext(), "Verification email sent to your new address.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to send verification email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    Toast.makeText(getContext(), "Email updated and verified.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showChangePasswordDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);

        TextView currentPass = dialogView.findViewById(R.id.current_password);
        TextView newPass = dialogView.findViewById(R.id.new_password);
        TextView confirmPass = dialogView.findViewById(R.id.confirm_password);
        Button confirmBtn = dialogView.findViewById(R.id.confirm_password_button);

        confirmBtn.setOnClickListener(v -> {
            String current = currentPass.getText().toString().trim();
            String newP = newPass.getText().toString().trim();
            String confirm = confirmPass.getText().toString().trim();

            if (current.isEmpty() || newP.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newP.equals(confirm)) {
                Toast.makeText(getContext(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseUser.reauthenticate(EmailAuthProvider.getCredential(firebaseUser.getEmail(), current))
                    .addOnSuccessListener(authResult -> {
                        firebaseUser.updatePassword(newP)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Password updated.", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }
}
