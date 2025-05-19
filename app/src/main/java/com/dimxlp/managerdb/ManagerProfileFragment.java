package com.dimxlp.managerdb;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import enumeration.CurrencyEnum;
import model.Manager;
import util.UserApi;

public class ManagerProfileFragment extends Fragment {

    private static final String LOG_TAG = "RAFI|ManagerProfile";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Managers");
    private StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    private ImageView badgeImage;
    private TextView fullName, teamText, nationality;
    private Button editButton;
    private LinearLayout selectManagerButton;

    private long managerId;
    private String team;

    private Uri teamBadgeUriEdit;
    private AlertDialog dialog;

    private BottomSheetDialog editDialog;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    teamBadgeUriEdit = uri;
                    if (dialog != null) {
                        ImageView badgeImageEdit = dialog.findViewById(R.id.team_badge_image_edit);
                        Picasso.get().load(uri).into(badgeImageEdit);
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manager_profile, container, false);

        badgeImage = view.findViewById(R.id.manager_photo_profile);
        fullName = view.findViewById(R.id.full_name_profile);
        teamText = view.findViewById(R.id.team_profile);
        nationality = view.findViewById(R.id.nationality_profile);
        editButton = view.findViewById(R.id.edit_button_profile);
        selectManagerButton = view.findViewById(R.id.select_manager_section);

        managerId = requireActivity().getIntent().getLongExtra("managerId", -1);
        team = requireActivity().getIntent().getStringExtra("team");

        loadManagerData();

        editButton.setOnClickListener(v -> createPopupDialog());
        selectManagerButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), SelectManagerActivity.class)));

        return view;
    }

    private void loadManagerData() {
        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Manager manager = queryDocumentSnapshots.getDocuments().get(0).toObject(Manager.class);
                        if (manager != null) {
                            fullName.setText(manager.getFullName());
                            teamText.setText(manager.getTeam());
                            nationality.setText(manager.getNationality());
                            Picasso.get().load(manager.getTeamBadgeUrl()).into(badgeImage);
                        }
                    }
                });
    }

    private void createPopupDialog() {
        editDialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.edit_manager_popup, null);
        editDialog.setContentView(view);

        EditText firstNameEdit = view.findViewById(R.id.first_name_edit);
        EditText lastNameEdit = view.findViewById(R.id.last_name_edit);
        EditText teamEdit = view.findViewById(R.id.team_edit);
        EditText nationalityEdit = view.findViewById(R.id.nationality_edit);
        ImageView badgeImageEdit = view.findViewById(R.id.team_badge_image_edit);
        Button uploadBadgeEdit = view.findViewById(R.id.upload_button_edit);
        TextView currencyPicker = view.findViewById(R.id.currency_text_edit);
        Button saveManagerButton = view.findViewById(R.id.save_manager_button);

        teamEdit.setEnabled(false);
        teamEdit.setTextColor(Color.GRAY);

        List<String> currencies = Arrays.stream(CurrencyEnum.values())
                .map(CurrencyEnum::getDescription)
                .collect(Collectors.toList());

        String[] currencyArray = currencies.toArray(new String[0]);

        currencyPicker.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Currency")
                    .setItems(currencyArray, (pickerDialog, item) -> currencyPicker.setText(currencyArray[item]))
                    .show();
        });

        collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                .whereEqualTo("id", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Manager manager = queryDocumentSnapshots.getDocuments().get(0).toObject(Manager.class);
                        if (manager != null) {
                            firstNameEdit.setText(manager.getFirstName());
                            lastNameEdit.setText(manager.getLastName());
                            teamEdit.setText(manager.getTeam());
                            nationalityEdit.setText(manager.getNationality());
                            Picasso.get().load(manager.getTeamBadgeUrl()).into(badgeImageEdit);
                            currencyPicker.setText(manager.getCurrency());
                        }
                    }
                });

        uploadBadgeEdit.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        saveManagerButton.setOnClickListener(v -> {
            if (!firstNameEdit.getText().toString().isEmpty() &&
                    !lastNameEdit.getText().toString().isEmpty() &&
                    !nationalityEdit.getText().toString().isEmpty()) {

                collectionReference.whereEqualTo("userId", UserApi.getInstance().getUserId())
                        .whereEqualTo("id", managerId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                                DocumentReference ref = collectionReference.document(doc.getId());

                                if (teamBadgeUriEdit != null) {
                                    StorageReference filepath = storageReference
                                            .child("team_badges")
                                            .child(team + "_" + Timestamp.now().getSeconds());

                                    filepath.putFile(teamBadgeUriEdit)
                                            .addOnSuccessListener(taskSnapshot -> filepath.getDownloadUrl().addOnSuccessListener(uri -> {
                                                ref.update("teamBadgeUrl", uri.toString());
                                            }));
                                }

                                ref.update("firstName", firstNameEdit.getText().toString().trim(),
                                                "lastName", lastNameEdit.getText().toString().trim(),
                                                "fullName", firstNameEdit.getText().toString().trim() + " " + lastNameEdit.getText().toString().trim(),
                                                "team", teamEdit.getText().toString().trim(),
                                                "nationality", nationalityEdit.getText().toString().trim(),
                                                "currency", currencyPicker.getText().toString().trim().split(" - ")[0])
                                        .addOnSuccessListener(aVoid -> {
                                            loadManagerData();
                                            editDialog.dismiss();
                                            Toast.makeText(getContext(), "Manager updated!", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        });
            } else {
                Toast.makeText(getContext(), "All fields are required!", Toast.LENGTH_SHORT).show();
            }
        });

        editDialog.show();
    }
}
