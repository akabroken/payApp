package com.isw.payapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentLoginBinding;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.terminal.config.TerminalConfig;
import com.isw.payapp.terminal.controllers.LoginController;
import com.isw.payapp.model.UserModel;
import com.isw.payapp.utils.NetworkExecutor;
import com.isw.payapp.utils.UnsafeOkHttpClient;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import okhttp3.OkHttpClient;

public class Login extends Fragment {

    private FragmentLoginBinding binding;
    private SessionManager sessionManager;
    private TerminalConfig terminalConfig;
    private LoginController loginController;
    private static final String TAG = "LoginFragment";
    private static final String LOGIN_URL = "https://smarttrans.interswitch-ke.com:81/";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        // Load logo with error handling
        try {
            Glide.with(this)
                    .load(BuildConfig.APP_LOGO)
                    .into(binding.imageLogo);
        } catch (Exception e) {
            Log.e(TAG, "Error loading logo", e);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        setupClickListeners();

        // Redirect if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToHome();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clean up binding to prevent memory leaks
    }

    private void initializeComponents() {
        try {
            sessionManager = new SessionManager(requireContext());
            terminalConfig = new TerminalConfig();
            loginController = new LoginController(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
            showError("Initialization failed. Please restart the app.");
        }
    }

    private void setupClickListeners() {
        binding.buttonLogin.setOnClickListener(v -> {
            hideKeyboard(v);
            validateCredentials();
        });

        binding.textViewResetPass.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigate(R.id.login_to_resetpassword);
            } catch (Exception e) {
                Log.e(TAG, "Navigation error", e);
                showToast("Navigation error occurred");
            }
        });
    }

    private void hideKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding keyboard", e);
        }
    }

    private void validateCredentials() {
        String username = binding.textUsername.getText().toString().trim();
        String password = binding.textPassword.getText().toString().trim();

        if (!validateInputs(username, password)) {
            return;
        }

        showLoading(true);

        try {
            ConfigManager.refreshConfig(getActivity());
            TerminalConfigModel config = ConfigManager.getConfig(getActivity());
            UserModel userModel = createUserModel(username, password);
            String requestXml = generateTerminalUsersRequest(userModel);
            Log.i(TAG, "Login request: " + requestXml);
            ExecutorService networkExecutor = NetworkExecutor.getExecutor();
            networkExecutor.execute(() -> {
                try {
                    OkHttpClient unsafeClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
                    String login_URL = "https://"+config.getLoginurl()+":"+config.getLoginport()+"/";
                    NetworkService.initialize(requireContext(), login_URL);
                    NetworkService networkService = NetworkService.getInstance();
                    String response = networkService.postPayLoadSyncLogin(requestXml);
                    Log.i(TAG, "Login response: " + response);

                    requireActivity().runOnUiThread(() -> {
                        try {
                            handleLoginResponse(response, username);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling login response", e);
                            showError("Login processing failed");
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Network error", e);
                    requireActivity().runOnUiThread(() -> {
                        showError("Network error. Please check your connection.");
                    });
                } finally {
                    requireActivity().runOnUiThread(() -> showLoading(false));
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Login error", e);
            showError("Login failed. Please try again.");
            showLoading(false);
        }
    }

    private boolean validateInputs(String username, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(username)) {
            binding.textUsername.setError("Username is required");
            isValid = false;
        } else {
            binding.textUsername.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.textPassword.setError("Password is required");
            isValid = false;
        } else {
            binding.textPassword.setError(null);
        }

        // Hide error message if inputs are valid
        if (isValid) {
            binding.errorMessage.setVisibility(View.GONE);
        }

        return isValid;
    }

    private UserModel createUserModel(String username, String password) {
        UserModel userModel = new UserModel();
        ConfigManager.refreshConfig(getActivity());
        TerminalConfigModel config = ConfigManager.getConfig(getActivity());
        userModel.setUsername(username);
        userModel.setPassword(password);
        if(getSelectedRole().isEmpty()){
            showError("Invalid User");
            return null;
        }
        userModel.setRole(getSelectedRole());

        try {
            userModel.setTid(config.getTid());
            userModel.setMid(config.getMid());
        } catch (Exception e) {
            Log.e(TAG, "Error loading terminal data", e);
            throw new RuntimeException("Failed to load terminal configuration");
        }

        userModel.setRequestType(getRequestType());
        return userModel;
    }

    private String getSelectedRole() {
        if (binding.checkBoxAdmin.isChecked()) {
            return "ADMIN";
        }
        else if (binding.checkBoxSuper.isChecked()) {
            return "SUPERVISOR";
       }
        return null;
    }

    private String getRequestType() {
        return (binding.checkBoxAdmin.isChecked() || binding.checkBoxSuper.isChecked())
                ? "Admin"
                : "SUPERVISOR";
    }

    private void handleLoginResponse(String response, String username) throws Exception {
        if (TextUtils.isEmpty(response)) {
            showError("Empty response from server");
            return;
        }

        Document doc = parseXmlResponse(response);
        String responseCode = getValue(doc, "responseCode");
        String responseMessage = getValue(doc, "responseMessage");

        if ("00".equals(responseCode)) {
            String names = getValue(doc, "names");
            sessionManager.createSession(username, names, getSelectedRole());
            showToast("Login successful!");
            navigateToHome();
        } else {
            String errorMsg = TextUtils.isEmpty(responseMessage)
                    ? "Invalid username or password"
                    : responseMessage;
            showError(errorMsg);
        }
    }

    private Document parseXmlResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Security: disable external entities to prevent XXE attacks
//        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private void showLoading(boolean isLoading) {
        binding.buttonLogin.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setText(isLoading ? "Logging in..." : "Login");
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        if (binding != null) {
            binding.errorMessage.setText(message);
            binding.errorMessage.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToHome() {
        try {
            NavHostFragment.findNavController(this).navigate(R.id.login_to_home);
        } catch (Exception e) {
            Log.e(TAG, "Navigation to home failed", e);
            showToast("Navigation error occurred");
        }
    }

    // XML generation helper methods
    public static String generateTerminalUsersRequest(UserModel userModel) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Security: disable external entities
//        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element terminalUsersRequest = doc.createElement("terminalUsersRequest");
        doc.appendChild(terminalUsersRequest);

        createElement(doc, terminalUsersRequest, "username", userModel.getUsername());
        createElement(doc, terminalUsersRequest, "password", userModel.getPassword());
        createElement(doc, terminalUsersRequest, "firstName", "");
        createElement(doc, terminalUsersRequest, "lastName", "");
        createElement(doc, terminalUsersRequest, "terminalId", userModel.getTid());
        createElement(doc, terminalUsersRequest, "merchantId", userModel.getMid());
        createElement(doc, terminalUsersRequest, "role", userModel.getRole());
        createElement(doc, terminalUsersRequest, "reqType", userModel.getRequestType());

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        return writer.toString();
    }

    private static void createElement(Document doc, Element parent, String tagName, String value) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(value != null ? value : ""));
        parent.appendChild(element);
    }

    private static String getValue(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String content = element.getTextContent();
                return content != null ? content.trim() : "";
            }
        }
        return "";
    }
}