package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dimxlp.managerdb.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import model.Transfer;

public class DashboardTransferAdapter extends RecyclerView.Adapter<DashboardTransferAdapter.ViewHolder> {

    private Context context;
    private List<Transfer> transfers;

    public DashboardTransferAdapter(Context context, List<Transfer> transfers) {
        this.context = context;
        this.transfers = transfers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transfer_dashboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transfer transfer = transfers.get(position);
        
        holder.playerName.setText(transfer.getFullName().toUpperCase());
        
        String transferDetails = "";
        if ("Transfer In".equalsIgnoreCase(transfer.getType())) {
            transferDetails = transfer.getFormerTeam() + " → " + transfer.getCurrentTeam();
        } else if ("Transfer Out".equalsIgnoreCase(transfer.getType())) {
            transferDetails = transfer.getFormerTeam() + " → " + transfer.getCurrentTeam();
        } else {
            transferDetails = transfer.getType();
        }
        holder.transferDetails.setText(transferDetails);
        
        // Format transfer fee
        if (transfer.getTransferFee() > 0) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
            formatter.setMaximumFractionDigits(1);
            String fee = "€" + (transfer.getTransferFee() / 1_000_000.0) + "M";
            holder.transferFee.setText(fee);
        } else if ("Free Transfer".equalsIgnoreCase(transfer.getType())) {
            holder.transferFee.setText("FREE");
        } else if ("Loan".equalsIgnoreCase(transfer.getType())) {
            holder.transferFee.setText("LOAN");
        } else {
            holder.transferFee.setText("€0");
        }
        
        holder.transferType.setText(transfer.getType().toUpperCase());
    }

    @Override
    public int getItemCount() {
        return transfers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView playerName;
        TextView transferDetails;
        TextView transferFee;
        TextView transferType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            playerName = itemView.findViewById(R.id.player_name);
            transferDetails = itemView.findViewById(R.id.transfer_details);
            transferFee = itemView.findViewById(R.id.transfer_fee);
            transferType = itemView.findViewById(R.id.transfer_type);
        }
    }
}

