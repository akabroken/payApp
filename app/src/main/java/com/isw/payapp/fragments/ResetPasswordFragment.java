package com.isw.payapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentResetPasswordBinding;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.terminal.controllers.LoginController;
import com.isw.payapp.model.UserModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class ResetPasswordFragment extends Fragment {


    private FragmentResetPasswordBinding binding;
    private EditText username, password, repassword;
    private Button submit;
    public ResetPasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentResetPasswordBinding.inflate(inflater,container,false);

        return binding.getRoot();
    }

    @Override
    public  void onViewCreated(@NonNull View view, Bundle saveBundle) {
        super.onViewCreated(view, saveBundle);

        submit = binding.buttonSubmit.findViewById(R.id.buttonSubmit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                resetCredentials();
            }
        });

    }
    private void hideKeyboard(View view) {
        // Get the InputMethodManager using the public API
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(getContext().INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void resetCredentials() {
        username = binding.textUsername.findViewById(R.id.textUsername);
        password = binding.textPassword.findViewById(R.id.textPassword);
        repassword = binding.textRePassword.findViewById(R.id.textRePassword);
        UserModel userModel = new UserModel();
        CheckBox checkBoxAdmin, checkBoxSuper;
        TerminalConfig terminalConfig = new TerminalConfig();
        LoginController loginController = new LoginController();
        TextView err = binding.errorMessage.findViewById(R.id.errorMessage);
        try {
            if (TextUtils.isEmpty(username.getText())) {
                username.setError("Username is required");
                return;
            }
            if (TextUtils.isEmpty(password.getText())) {
                password.setError("Password is required");
                return;
            }
            if (TextUtils.isEmpty(repassword.getText())) {
                password.setError("Password is required");
                return;
            }
            if(!password.getText().toString().equals(repassword.getText().toString())){
                password.setError("Password Mismatch!!");
                return;
            }

            String roleSelectType = "";
            String requestType ="";
            checkBoxAdmin = binding.checkBoxAdmin.findViewById(R.id.checkBoxAdmin);
            checkBoxSuper = binding.checkBoxSuper.findViewById(R.id.checkBoxSuper);
            if(checkBoxAdmin.isChecked()){
                roleSelectType = "ADMIN";
                requestType = "UPDATE";
            }else if (checkBoxSuper.isChecked()){
                roleSelectType = "SUPERVISOR";
                requestType = "UPDATE";
            }else{
                roleSelectType = "TELLER";
                requestType ="UPDATE";
            }
            userModel.setUsername(username.getText().toString());
            userModel.setPassword(password.getText().toString());
            userModel.setRole(roleSelectType);
            userModel.setRequestType(requestType);
            userModel.setTid(terminalConfig.loadTerminalDataFromJson(getContext(),"__tid"));
            userModel.setMid(terminalConfig.loadTerminalDataFromJson(getContext(),"__mid"));

            userModel.setRequestType("UPDATE");
            String in  = generateTerminalUsersRequest(userModel);
            System.out.println(in);
            Log.i("TEST",in);
            String url = "https://smarttrans.interswitch-ke.com:81/SmartControlSvc.svc/pos/user/request";
            String out  = loginController.sendPostRequest(url, in,"");
            System.out.println("Test: " + out);

            // Parse the XML string into a Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(out.getBytes("UTF-8"));
            Document doc = builder.parse(input);

            // Normalize the XML structure
            doc.getDocumentElement().normalize();

            // Extract values from the XML
            String responseCode = getValue(doc, "responseCode");
            String responseMessage = getValue(doc, "responseMessage");
            if(responseCode.equals("00")){
               // sessionManager.createSession(username.getText().toString(),getValue(doc, "names"));
                Toast.makeText(requireActivity(), responseMessage, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(ResetPasswordFragment.this).navigate(R.id.resetPass_to_login);
            }else{
                err.setText("Invalid username or password");
                err.setVisibility(View.VISIBLE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String generateTerminalUsersRequest(UserModel userModel) throws Exception {

        // Create a new Document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Root element <terminalUsersRequest>
        Element terminalUsersRequest = doc.createElement("terminalUsersRequest");
        doc.appendChild(terminalUsersRequest);

        // Add child elements with dynamic values
        createElement(doc, terminalUsersRequest, "username", userModel.getUsername());
        createElement(doc, terminalUsersRequest, "password", userModel.getPassword());
        createElement(doc, terminalUsersRequest, "firstName", "");//userModel.getFirstName()
        createElement(doc, terminalUsersRequest, "lastName", "");//userModel.getLastName()
        createElement(doc, terminalUsersRequest, "terminalId", userModel.getTid());
        createElement(doc, terminalUsersRequest, "merchantId", userModel.getMid());
        createElement(doc, terminalUsersRequest, "role", userModel.getRole());
        createElement(doc, terminalUsersRequest, "reqType", userModel.getRequestType());

        // Convert Document to String
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        // Return the generated XML payload
        return writer.toString();
    }

    // Helper method to create and append elements
    private static void createElement(Document doc, Element parent, String tagName, String value) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value));
        parent.appendChild(element);
    }

    /**
     * Helper method to extract the text content of an element by its tag name.
     *
     * @param doc       The XML document.
     * @param tagName   The tag name of the element to extract.
     * @return          The text content of the element, or an empty string if not found.
     */
    private static String getValue(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                return element.getTextContent().trim();
            }
        }
        return ""; // Return empty string if the tag is not found or has no content
    }
}