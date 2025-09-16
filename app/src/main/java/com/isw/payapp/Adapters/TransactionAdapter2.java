package com.isw.payapp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.isw.payapp.R;
import com.isw.payapp.model.Transaction;

import java.util.List;

public class TransactionAdapter2 extends RecyclerView.Adapter<TransactionAdapter2.TransactionViewHolder>{

    private List<Transaction> transactionList;

    public TransactionAdapter2(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.tvId.setText(transaction.getId());
        holder.tvHolderName.setText(transaction.getHolderName());
        holder.tvCardnum.setText(transaction.getCardNumber());
        holder.tvAmount.setText(transaction.getAmount());
        holder.tvDate.setText(transaction.getDateTime());
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvHolderName, tvCardnum,tvAmount, tvDate;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvId);
            tvCardnum = itemView.findViewById(R.id.tvCardNum);
            tvHolderName = itemView.findViewById(R.id.tvHolderName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
