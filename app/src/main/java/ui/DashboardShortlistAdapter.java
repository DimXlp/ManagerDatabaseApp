package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.R;

import java.util.List;

import model.ShortlistedPlayer;

public class DashboardShortlistAdapter extends RecyclerView.Adapter<DashboardShortlistAdapter.ViewHolder> {

    private Context context;
    private List<ShortlistedPlayer> players;

    public DashboardShortlistAdapter(Context context, List<ShortlistedPlayer> players) {
        this.context = context;
        this.players = players;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shortlist_dashboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortlistedPlayer player = players.get(position);
        
        holder.playerName.setText(player.getFullName().toUpperCase());
        holder.playerPosition.setText(player.getPosition() + " • " + calculateAge(player) + " YRS");
        holder.playerOverall.setText(String.valueOf(player.getOverall()));
        holder.playerPotential.setText("POT: " + player.getPotentialHigh());
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    private int calculateAge(ShortlistedPlayer player) {
        // Simple age calculation - you may need to adjust based on your data structure
        // For now, returning a placeholder
        return 25; // You should implement proper age calculation based on birth year
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView playerName;
        TextView playerPosition;
        TextView playerOverall;
        TextView playerPotential;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            playerName = itemView.findViewById(R.id.player_name);
            playerPosition = itemView.findViewById(R.id.player_position);
            playerOverall = itemView.findViewById(R.id.player_overall);
            playerPotential = itemView.findViewById(R.id.player_potential);
        }
    }
}

