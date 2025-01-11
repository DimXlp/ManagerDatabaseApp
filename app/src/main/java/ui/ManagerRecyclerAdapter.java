package ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.FirstTeamActivity;
import com.dimxlp.managerdb.FirstTeamListActivity;
import com.dimxlp.managerdb.FormerPlayersListActivity;
import com.dimxlp.managerdb.LoanedOutPlayersActivity;
import com.dimxlp.managerdb.ProfileActivity;
import com.dimxlp.managerdb.R;
import com.dimxlp.managerdb.ShortlistActivity;
import com.dimxlp.managerdb.ShortlistPlayersActivity;
import com.dimxlp.managerdb.SupportActivity;
import com.dimxlp.managerdb.TransferDealsActivity;
import com.dimxlp.managerdb.YouthTeamActivity;
import com.dimxlp.managerdb.YouthTeamListActivity;

import java.util.List;

import util.ManageTeamButton;

public class ManagerRecyclerAdapter extends RecyclerView.Adapter<ManagerRecyclerAdapter.ViewHolder> {

    private Context context;
    private List<ManageTeamButton> buttonList;
    private boolean ftPlayersExist;
    private boolean ytPlayersExist;
    private boolean shPlayersExist;
    private long managerId;
    private String team;

    public ManagerRecyclerAdapter(Context context, List<ManageTeamButton> buttonList, boolean ftPlayersExist, boolean ytPlayersExist, boolean shPlayersExist, long managerId, String team) {
        this.context = context;
        this.buttonList = buttonList;
        this.ftPlayersExist = ftPlayersExist;
        this.ytPlayersExist = ytPlayersExist;
        this.shPlayersExist = shPlayersExist;
        this.managerId = managerId;
        this.team = team;
    }

    @NonNull
    @Override
    public ManagerRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.button_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ManagerRecyclerAdapter.ViewHolder holder, int position) {

        switch (position) {
            case 0:
                buttonList.get(position).setTitle("Profile");
                holder.buttonTitle.setText("Profile");
                holder.buttonImage.setBackgroundResource(R.drawable.profile_filled_64);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
            case 1:
                buttonList.get(position).setTitle("First Team");
                holder.buttonTitle.setText("First Team");
                holder.buttonImage.setBackgroundResource(R.drawable.first_team_filled_64);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
            case 2:
                buttonList.get(position).setTitle("Youth Team");
                holder.buttonTitle.setText("Youth Team");
                holder.buttonImage.setBackgroundResource(R.drawable.youth_team_filled_64);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
            case 3:
                buttonList.get(position).setTitle("Former Players");
                holder.buttonTitle.setText("Former Players");
                holder.buttonImage.setBackgroundResource(R.drawable.goodbye_filled_64);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
            case 4:
                buttonList.get(position).setTitle("Shortlist");
                holder.buttonTitle.setText("Shortlist");
                holder.buttonImage.setBackgroundResource(R.drawable.shortlist_filled_64);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
            case 5:
                buttonList.get(position).setTitle("Loaned Out Players");
                holder.buttonTitle.setText("Loaned Out Players");
                holder.buttonImage.setBackgroundResource(R.drawable.loan_filled_64);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
            case 6:
                buttonList.get(position).setTitle("Transfer Deals");
                holder.buttonTitle.setText("Transfer Deals");
                holder.buttonImage.setBackgroundResource(R.drawable.deal_filled_64);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
            case 7:
                buttonList.get(position).setTitle("Support & Info");
                holder.buttonTitle.setText("Support & Info");
                holder.buttonImage.setBackgroundResource(R.drawable.info_filled_64_2);
                holder.buttonImage.getBackground().setTint(Color.WHITE);
                break;
//            case 7:
//                buttonList.get(position).setTitle("Compare Players");
//                holder.buttonTitle.setText("Compare Players");
//                holder.buttonImage.setBackgroundResource(R.drawable.ic_compare);
//                break;
        }

    }

    @Override
    public int getItemCount() {
        return buttonList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView buttonTitle;
        private ImageView buttonImage;

        public ViewHolder(@NonNull View itemView, final Context ctx) {
            super(itemView);
            context = ctx;

            buttonTitle = itemView.findViewById(R.id.button_title);
            buttonImage = itemView.findViewById(R.id.button_image);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    switch (position) {
                        case 0:
                            Intent profileIntent = new Intent(ctx, ProfileActivity.class);
                            profileIntent.putExtra("managerId", managerId);
                            profileIntent.putExtra("team", team);
                            ctx.startActivity(profileIntent);
                            break;
                        case 1:
                            if (ftPlayersExist) {
                                Intent intent = new Intent(ctx, FirstTeamListActivity.class);
                                intent.putExtra("managerId", managerId);
                                intent.putExtra("team", team);
                                ctx.startActivity(intent);
                            } else {
                                Intent intent = new Intent(new Intent(ctx, FirstTeamActivity.class));
                                intent.putExtra("managerId", managerId);
                                intent.putExtra("team", team);
                                ctx.startActivity(intent);
                            }
                            break;
                        case 2:
                            if (ytPlayersExist) {
                                Intent intent = new Intent(ctx, YouthTeamListActivity.class);
                                intent.putExtra("managerId", managerId);
                                intent.putExtra("team", team);
                                ctx.startActivity(intent);
                            } else {
                                Intent intent = new Intent(ctx, YouthTeamActivity.class);
                                intent.putExtra("managerId", managerId);
                                intent.putExtra("team", team);
                                ctx.startActivity(intent);
                            }
                            break;
                        case 3:
                            Intent formerPlayersIntent = new Intent(ctx, FormerPlayersListActivity.class);
                            formerPlayersIntent.putExtra("managerId", managerId);
                            formerPlayersIntent.putExtra("team", team);
                            ctx.startActivity(formerPlayersIntent);
                            break;
                        case 4:
                            if (shPlayersExist) {
                                Intent intent = new Intent(ctx, ShortlistPlayersActivity.class);
                                intent.putExtra("managerId", managerId);
                                intent.putExtra("team", team);
                                ctx.startActivity(intent);
                            } else {
                                Intent intent = new Intent(ctx, ShortlistActivity.class);
                                intent.putExtra("managerId", managerId);
                                intent.putExtra("team", team);
                                ctx.startActivity(intent);
                            }
                            break;
                        case 5:
                            Intent loanIntent = new Intent(ctx, LoanedOutPlayersActivity.class);
                            loanIntent.putExtra("managerId", managerId);
                            loanIntent.putExtra("team", team);
                            ctx.startActivity(loanIntent);
                            break;
                        case 6:
                            Intent transferIntent = new Intent(ctx, TransferDealsActivity.class);
                            transferIntent.putExtra("managerId", managerId);
                            transferIntent.putExtra("team", team);
                            ctx.startActivity(transferIntent);
                            break;
                        case 7:
                            Intent supportIntent = new Intent(ctx, SupportActivity.class);
                            supportIntent.putExtra("managerId", managerId);
                            supportIntent.putExtra("team", team);
                            ctx.startActivity(supportIntent);
                            break;
//                        case 7:
//                            Intent compareIntent = new Intent(ctx, ComparisonActivity.class);
//                            compareIntent.putExtra("managerId", managerId);
//                            compareIntent.putExtra("team", team);
//                            ctx.startActivity(compareIntent);
//                            break;
                    }
                }
            });
        }
    }
}
