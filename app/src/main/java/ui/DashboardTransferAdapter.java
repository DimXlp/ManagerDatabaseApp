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
        
        // Format transfer details with arrow (from → to)
        String transferDetails = "";
        if ("Transfer In".equalsIgnoreCase(transfer.getType())) {
            transferDetails = transfer.getFormerTeam() + " → " + transfer.getCurrentTeam();
        } else if ("Transfer Out".equalsIgnoreCase(transfer.getType())) {
            transferDetails = transfer.getFormerTeam() + " → " + transfer.getCurrentTeam();
        } else if ("Loan".equalsIgnoreCase(transfer.getType())) {
            transferDetails = transfer.getFormerTeam() + " → " + transfer.getCurrentTeam() + " (Loan)";
        } else if ("Free Transfer".equalsIgnoreCase(transfer.getType())) {
            transferDetails = transfer.getFormerTeam() + " → " + transfer.getCurrentTeam();
        } else {
            transferDetails = transfer.getType();
        }
        holder.transferDetails.setText(transferDetails);
        
        // Format transfer fee
        if (transfer.getTransferFee() > 0) {
            double feeInMillions = transfer.getTransferFee() / 1_000_000.0;
            String fee;
            if (feeInMillions >= 1) {
                fee = String.format(Locale.getDefault(), "€%.1fM", feeInMillions);
            } else {
                // Show in thousands for smaller fees
                double feeInThousands = transfer.getTransferFee() / 1_000.0;
                fee = String.format(Locale.getDefault(), "€%.0fK", feeInThousands);
            }
            holder.transferFee.setText(fee);
        } else if ("Free Transfer".equalsIgnoreCase(transfer.getType())) {
            holder.transferFee.setText("FREE");
        } else if ("Loan".equalsIgnoreCase(transfer.getType())) {
            holder.transferFee.setText("LOAN");
        } else {
            holder.transferFee.setText("—");
        }
        
        // Format status message (more descriptive than just transfer type)
        String status = "";
        if ("Transfer In".equalsIgnoreCase(transfer.getType())) {
            status = "AGREEMENT FINALIZED";
        } else if ("Transfer Out".equalsIgnoreCase(transfer.getType())) {
            status = "TRANSFER COMPLETED";
        } else if ("Loan".equalsIgnoreCase(transfer.getType())) {
            status = "LOAN DEAL ACTIVE";
        } else if ("Free Transfer".equalsIgnoreCase(transfer.getType())) {
            status = "FREE AGENT SIGNING";
        } else {
            status = transfer.getType().toUpperCase();
        }
        holder.transferType.setText(status);
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

