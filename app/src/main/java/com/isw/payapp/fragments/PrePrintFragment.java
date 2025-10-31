package com.isw.payapp.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.isw.payapp.R;

import com.isw.payapp.databinding.FragmentPrePrintBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.interfaces.IPrinterProcessor;
//import com.telpo.tps550.api.printer.UsbThermalPrinter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class PrePrintFragment extends Fragment {

    private String print_data_in;
    private FragmentPrePrintBinding binding;
    private TextView amount, responseCode, transactionId, dateTime,carname, car_card;
    private Button print;
    private IPrinterProcessor printerAction;
 //   private UsbThermalPrinter usbThermalPrinter;



//    public PrePrintFragment(String print_data_in) {
//        this.print_data_in = print_data_in;
//        // Required empty public constructor
//    }

    private static final String ARG_PRE_PAYLOAD = "";
    private static final String ARG_NAME = "";
    private static final String ARG_CARD = "";

    public static PrePrintFragment newInstance(String prePayload, String cardNum, String name) {
        PrePrintFragment fragment = new PrePrintFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRE_PAYLOAD, prePayload);
        args.putString(ARG_CARD,cardNum);
        args.putString(ARG_NAME,name);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPrePrintBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle saveInstantState) {
        super.onViewCreated(view, saveInstantState);
        responseCode = binding.responseCode.findViewById(R.id.responseCode);
        transactionId = binding.responseCode.findViewById(R.id.transactionId);
        amount = binding.responseCode.findViewById(R.id.amount);
        dateTime = binding.responseCode.findViewById(R.id.dateTime);
        carname = binding.name.findViewById(R.id.name);
        car_card = binding.card.findViewById(R.id.card);

        // Get data from Intent
        Intent intent = getActivity().getIntent();
        String respCode = intent.getStringExtra("response_code");
        String txnId = intent.getStringExtra("transaction_id");
        String txnAmount = intent.getStringExtra("amount");
        String txnDateTime = intent.getStringExtra("date_time");
        try {
            if (getArguments() != null) {
                String xmlString = getArguments().getString(ARG_PRE_PAYLOAD);
                String pan_ = getArguments().getString(ARG_CARD);
                String cname = getArguments().getString(ARG_NAME);
                // Use prePayload as needed (e.g., display it in a TextView)
                // Populate the views
                car_card.setText(pan_);
                carname.setText(cname);
                responseCode.setText("Response Code: " + getValueByTagName(xmlString, "field39"));
                transactionId.setText("Transaction ID: " + getValueByTagName(xmlString, "referenceNumber"));
                amount.setText("Amount: " + getNestedValue(xmlString, "hostEmvData", "AmountAuthorized"));
                dateTime.setText("Date and Time: " + txnDateTime);

                System.out.println("Description: " + getValueByTagName(xmlString, "description"));
                System.out.println("Field39: " + getValueByTagName(xmlString, "field39"));
                System.out.println("Reference Number: " + getValueByTagName(xmlString, "referenceNumber"));
                System.out.println("Transaction Channel Name: " + getValueByTagName(xmlString, "transactionChannelName"));

                // For nested values, use a more specific XPath-like approach
                System.out.println("Amount Authorized: " + getNestedValue(xmlString, "hostEmvData", "AmountAuthorized"));
                System.out.println("ATC: " + getNestedValue(xmlString, "hostEmvData", "atc"));

                // Extract structured data by key
                System.out.println("Postilion:ExtendedResponseCode: " + getStructuredDataValue(xmlString, "Postilion:ExtendedResponseCode"));

                Button printButton = binding.printButton.findViewById(R.id.printButton);
                printButton.setOnClickListener(
                        v -> printReceipt(car_card.getText().toString(), carname.getText().toString(),responseCode.getText().toString(),
                                transactionId.getText().toString(),amount.getText().toString(),dateTime.getText().toString()));
            }

        }catch (Exception e){

        }


        // Handle print button click

    }

    public void printReceipt(String car_card,String carname,String responseCode,String transactionId,String amount, String dateTime) {
        printerAction = DeviceFactory.createPrinter(getContext());
       // printerAction.PrintFuel(usbThermalPrinter, car_card, carname, amount, transactionId, responseCode, transactionId);
    }

    /**
     * Extracts a value by tag name from the XML string.
     *
     * @param xmlString The XML string to parse.
     * @param tagName   The tag name to search for.
     * @return The value of the tag, or null if not found.
     * @throws Exception If parsing fails.
     */
    public static String getValueByTagName(String xmlString, String tagName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));

        NodeList nodeList = document.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    /**
     * Extracts a nested value by parent and child tag names.
     *
     * @param xmlString   The XML string to parse.
     * @param parentTag   The parent tag name.
     * @param childTag    The child tag name.
     * @return The value of the child tag, or null if not found.
     * @throws Exception If parsing fails.
     */
    public static String getNestedValue(String xmlString, String parentTag, String childTag) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));

        NodeList parentNodes = document.getElementsByTagName(parentTag);
        for (int i = 0; i < parentNodes.getLength(); i++) {
            Node parentNode = parentNodes.item(i);
            if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element) parentNode;
                NodeList childNodes = parentElement.getElementsByTagName(childTag);
                if (childNodes.getLength() > 0) {
                    return childNodes.item(0).getTextContent();
                }
            }
        }
        return null;
    }

    /**
     * Extracts a structured data value by key.
     *
     * @param xmlString The XML string to parse.
     * @param key       The key to search for in structuredData.
     * @return The value associated with the key, or null if not found.
     * @throws Exception If parsing fails.
     */
    public static String getStructuredDataValue(String xmlString, String key) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));

        NodeList structuredDataNodes = document.getElementsByTagName("structuredData");
        for (int i = 0; i < structuredDataNodes.getLength(); i++) {
            Node structuredDataNode = structuredDataNodes.item(i);
            if (structuredDataNode.getNodeType() == Node.ELEMENT_NODE) {
                Element structuredDataElement = (Element) structuredDataNode;
                NodeList keyNodes = structuredDataElement.getElementsByTagName("key");
                if (keyNodes.getLength() > 0 && keyNodes.item(0).getTextContent().equals(key)) {
                    NodeList valueNodes = structuredDataElement.getElementsByTagName("value");
                    if (valueNodes.getLength() > 0) {
                        return valueNodes.item(0).getTextContent();
                    }
                }
            }
        }
        return null;
    }
}