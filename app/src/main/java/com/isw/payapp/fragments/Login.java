package com.isw.payapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentLoginBinding;
import com.isw.payapp.helpers.SessionManager;
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

public class Login extends Fragment {

    private FragmentLoginBinding binding;
    private SessionManager sessionManager;
    private TerminalConfig terminalConfig;
    private LoginController loginController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        Glide.with(this)
                .load(R.drawable.sidian_bank_logo)
                .into(binding.imageLogo);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupClickListeners();

        // Redirect if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToHome();
        }
    }

    private void initializeComponents() {
        sessionManager = new SessionManager(requireContext());
        terminalConfig = new TerminalConfig();
        loginController = new LoginController(requireContext());
    }

    private void setupClickListeners() {
        binding.buttonLogin.setOnClickListener(v -> {
            hideKeyboard(v);
            validateCredentials();
        });

        binding.textViewResetPass.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.login_to_resetpassword)
        );
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(requireContext().INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void validateCredentials() {
        String username = binding.textUsername.getText().toString().trim();
        String password = binding.textPassword.getText().toString().trim();

        if (!validateInputs(username, password)) {
            return;
        }

        try {
            UserModel userModel = createUserModel(username, password);
            String requestXml = generateTerminalUsersRequest(userModel);
            String response = loginController.sendPostRequest(
                    "https://smarttrans.interswitch-ke.com:81/SmartControlSvc.svc/pos/user/request",
                    requestXml,
                    ""
            );

            handleLoginResponse(response, username);
        } catch (Exception e) {
            showError("Login failed. Please try again.");
            Log.e("LoginFragment", "Login error", e);
        }
    }

    private boolean validateInputs(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            binding.textUsername.setError("Username is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.textPassword.setError("Password is required");
            return false;
        }
        return true;
    }

    private UserModel createUserModel(String username, String password) {
        UserModel userModel = new UserModel();
        userModel.setUsername(username);
        userModel.setPassword(password);
        userModel.setRole(getSelectedRole());
        userModel.setTid(terminalConfig.loadTerminalDataFromJson(requireContext(), "__tid"));
        userModel.setMid(terminalConfig.loadTerminalDataFromJson(requireContext(), "__mid"));
        userModel.setRequestType(getRequestType());
        return userModel;
    }

    private String getSelectedRole() {
        if (binding.checkBoxAdmin.isChecked()) {
            return "ADMIN";
        } else if (binding.checkBoxSuper.isChecked()) {
            return "SUPERVISOR";
        }
        return "TELLER";
    }

    private String getRequestType() {
        return (binding.checkBoxAdmin.isChecked() || binding.checkBoxSuper.isChecked())
                ? "Admin"
                : "LOGIN";
    }

    private void handleLoginResponse(String response, String username) throws Exception {
//        Document doc = parseXmlResponse(response);
//        String responseCode = getValue(doc, "responseCode");
//        String responseMessage = getValue(doc, "responseMessage");

//        if ("00".equals(responseCode)) {
//            String names = getValue(doc, "names");
//            sessionManager.createSession(username, names);
//            showToast("Login successful!");
//            navigateToHome();
//        } else {
//            showError(responseMessage.isEmpty() ? "Invalid username or password" : responseMessage);
//
//        }
        //to be removed
        //sessionManager.createSession(username, "names");
        sessionManager.createSession("username", "names");
        showToast("Login successful!");
        navigateToHome();
    }

    private Document parseXmlResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        binding.errorMessage.setText(message);
        binding.errorMessage.setVisibility(View.VISIBLE);
    }

    private void navigateToHome() {
        NavHostFragment.findNavController(this).navigate(R.id.login_to_home);
    }

    // XML generation helper methods remain the same as in your original code
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