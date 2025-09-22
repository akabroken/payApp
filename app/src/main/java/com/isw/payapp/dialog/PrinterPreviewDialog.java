package com.isw.payapp.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.isw.payapp.R;
import com.isw.payapp.model.CardModel;
import com.isw.payapp.model.EmvModel;
import com.isw.payapp.model.TransactionData;
import com.isw.payapp.terminal.config.TerminalConfig;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PrinterPreviewDialog extends Dialog {
    private Context context;
    private CardModel cardModel;
    private EmvModel emvModel;
    private TransactionData transactionData;
    private String responseMessage;
    private OnPrintClickListener printClickListener;

    public interface OnPrintClickListener {
        void onPrintClick(String previewContent);
        void onCancelClick();
    }

    public PrinterPreviewDialog(@NonNull Context context,
                                CardModel cardModel,
                                EmvModel emvModel,
                                TransactionData transactionData,
                                String responseMessage,
                                OnPrintClickListener listener) {
        super(context);
        this.context = context;
        this.cardModel = cardModel;
        this.emvModel = emvModel;
        this.transactionData = transactionData;
        this.responseMessage = responseMessage;
        this.printClickListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_printer_preview);

        setCancelable(false);
        setTitle("Print Preview");

        initializeViews();
        populatePreviewData();
    }

    private void initializeViews() {
        Button btnPrint = findViewById(R.id.btnPrint);
        Button btnCancel = findViewById(R.id.btnCancel);

        btnPrint.setOnClickListener(v -> {
            if (printClickListener != null) {
                String previewContent = generatePrintContent();
                printClickListener.onPrintClick(previewContent);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (printClickListener != null) {
                printClickListener.onCancelClick();
            }
            dismiss();
        });
    }

    private void populatePreviewData() {
        TextView tvMerchantInfo = findViewById(R.id.tvMerchantInfo);
        TextView tvTransactionDetails = findViewById(R.id.tvTransactionDetails);
        TextView tvCardDetails = findViewById(R.id.tvCardDetails);
        TextView tvEmvData = findViewById(R.id.tvEmvData);
        TextView tvResponseMessage = findViewById(R.id.tvResponseMessage);

        // Merchant Info
        String merchantInfo = "Bank: "+  TerminalConfig.loadTerminalDataFromJson(context,"__bank")+"\n"+
        "\nMerchant: " + TerminalConfig.loadTerminalDataFromJson(context,"__merchantloc") + "\n" +
                "Terminal ID: " + TerminalConfig.loadTerminalDataFromJson(context, "__tid");
        tvMerchantInfo.setText(merchantInfo);

        // Transaction Details
        String transactionDetails = "Amount: " + transactionData.getAmount() + "\n" +
                "Currency: " + transactionData.getCurrency() + "\n" +
                "Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n" +
                "Transaction Type: " + transactionData.getTransactionType();
        tvTransactionDetails.setText(transactionDetails);

        // Card Details
        String cardDetails = "Card: " + (cardModel.getPan() != null ?
                cardModel.getPan().substring(0, 6) + "******" +
                        cardModel.getPan().substring(cardModel.getPan().length() - 4) : "N/A") + "\n" +
                "Entry Mode: Chip";
        tvCardDetails.setText(cardDetails);

        // EMV Data (simplified)
        String emvData = "AID: " + (emvModel.getDedicatedFileName() != null ?
                emvModel.getDedicatedFileName() : "N/A") + "\n" +
                "ATC: " + (emvModel.getAtc() != null ? emvModel.getAtc() : "N/A") + "\n" +
                "TVR: " + (emvModel.getTerminalVerificationResult() != null ?
                emvModel.getTerminalVerificationResult() : "N/A");
        tvEmvData.setText(emvData);

        // Response Message
        tvResponseMessage.setText("Response: " + responseMessage);
    }

    private String generatePrintContent() {
        StringBuilder content = new StringBuilder();

        content.append("================================\n");
        content.append("        TRANSACTION RECEIPT\n");
        content.append("================================\n\n");

        content.append("Bank: ").append(TerminalConfig.loadTerminalDataFromJson(context,"__bank")).append("\n");
        content.append("Branch: ").append(TerminalConfig.loadTerminalDataFromJson(context,"__merchantloc")).append("\n");
        content.append("Terminal ID: ").append(TerminalConfig.loadTerminalDataFromJson(context,"__tid")).append("\n");
        content.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        content.append("--------------------------------\n");

        content.append("Amount: ").append(transactionData.getAmount()).append("\n");
        content.append("Currency: ").append(transactionData.getCurrency()).append("\n");
        content.append("Transaction Type: ").append(transactionData.getTransactionType()).append("\n");
        content.append("--------------------------------\n");

        if (cardModel.getPan() != null) {
            String maskedPan = cardModel.getPan().substring(0, 6) + "******" +
                    cardModel.getPan().substring(cardModel.getPan().length() - 4);
            content.append("Card: ").append(maskedPan).append("\n");
        }

        content.append("Entry Mode: Chip\n");
        content.append("--------------------------------\n");

        content.append("Response: ").append(responseMessage).append("\n");
        content.append("================================\n");
        content.append("     THANK YOU FOR YOUR BUSINESS\n");
        content.append("================================\n");

        return content.toString();
    }
}
