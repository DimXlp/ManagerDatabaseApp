package ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.FirstTeamListActivity;
import com.dimxlp.managerdb.ManageTeamActivity;
import com.dimxlp.managerdb.R;
import com.dimxlp.managerdb.SelectManagerActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import model.FirstTeamPlayer;
import model.Manager;
import util.ManagerSelectionButton;
import util.UserApi;

public class ManagerSelectionRecAdapter extends RecyclerView.Adapter<ManagerSelectionRecAdapter.ViewHolder> {

    private static final String LOG_TAG = "RAFI|ManagerSelectionRecAdapter";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference managerColRef = db.collection("Managers");

    private Context context;
    private List<Manager> managerList;

    public ManagerSelectionRecAdapter(Context context, List<Manager> managerList) {
        this.context = context;
        this.managerList = managerList;
    }

    @NonNull
    @Override
    public ManagerSelectionRecAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.manager_row, parent, false);
        return new ManagerSelectionRecAdapter.ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ManagerSelectionRecAdapter.ViewHolder holder, int position) {

        Manager manager = managerList.get(position);

        holder.managerName.setText(manager.getFullName());
        holder.teamName.setText(manager.getTeam());
    }

    @Override
    public int getItemCount() {
        return managerList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView managerName;
        private TextView teamName;
        private Button deleteButton;

        public ViewHolder(@NonNull View itemView, final Context context) {
            super(itemView);

            managerName = itemView.findViewById(R.id.manager_name_select);
            teamName = itemView.findViewById(R.id.team_name_select);
            deleteButton = itemView.findViewById(R.id.delete_button_select);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Manager clicked at position: " + managerList.get(getAdapterPosition()));
                    Intent intent = new Intent(context, ManageTeamActivity.class);
                    intent.putExtra("managerId", managerList.get(getAdapterPosition()).getId());
                    intent.putExtra("team", managerList.get(getAdapterPosition()).getTeam());
                    context.startActivity(intent);
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, "Delete button clicked for manager: " + managerName.getText());

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    deleteManager(managerList.get(getAdapterPosition()));
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    Log.d(LOG_TAG, "Delete operation canceled.");
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Do you want to delete this manager?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            });
        }

        private void deleteManager(final Manager manager) {
            Log.d(LOG_TAG, "deleteManager called for manager: " + manager.getFullName());

            managerColRef.whereEqualTo("userId", UserApi.getInstance().getUserId())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(LOG_TAG, "Manager collection fetched successfully.");

                                List<DocumentSnapshot> doc = task.getResult().getDocuments();
                                DocumentReference documentReference = null;
                                for (DocumentSnapshot ds : doc) {
                                    Manager mng = ds.toObject(Manager.class);
                                    if (mng.getId() == manager.getId()) {
                                        documentReference = managerColRef.document(ds.getId());
                                    }
                                }
                                assert documentReference != null;
                                documentReference.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(LOG_TAG, "Manager successfully deleted: " + manager.getFullName());
                                                Toast.makeText(context, "Manager deleted!", Toast.LENGTH_LONG)
                                                        .show();
                                                context.startActivity(new Intent(context, SelectManagerActivity.class));
                                                ((Activity)context).finish();
                                            }
                                        });
                            } else {
                                Log.e(LOG_TAG, "Error fetching Managers collection.", task.getException());
                            }
                        }
                    });
        }
    }
}
