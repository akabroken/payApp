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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentResetPasswordBinding;
import com.isw.payapp.devices.services.NetworkService;
import com.isw.payapp.helpers.ConfigManager;
import com.isw.payapp.model.TerminalConfigModel;
import com.isw.payapp.model.UserModel;
import com.isw.payapp.utils.NetworkExecutor;

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

public class ResetPasswordFragment extends Fragment {
    private static final String TAG = "ResetPasswordFragment";

    private FragmentResetPasswordBinding binding;
    private EditText username, password, repassword;
    private Button submit;
    private ProgressBar progressBar;
    private TextView errorMessage;
    private CheckBox checkBoxAdmin, checkBoxSuper, checkBoxTeller;

    private final ExecutorService networkExecutor = NetworkExecutor.getExecutor();

    public ResetPasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false);

        // Load logo
        Glide.with(this)
                .load(BuildConfig.APP_LOGO)
                .into(binding.imageLogo);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle saveBundle) {
        super.onViewCreated(view, saveBundle);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        submit = binding.buttonSubmit;
        username = binding.textUsername;
        password = binding.textPassword;
        repassword = binding.textRePassword;
        progressBar = binding.progressBar;
        errorMessage = binding.errorMessage;
        checkBoxAdmin = binding.checkBoxAdmin;
        checkBoxSuper = binding.checkBoxSuper;
        checkBoxTeller = binding.checkBoxTeller;

        // Hide progress bar initially
        progressBar.setVisibility(View.GONE);
        errorMessage.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        submit.setOnClickListener(v -> {
            hideKeyboard(v);
            resetCredentials();
        });

        // Add text change listeners to clear errors when user starts typing
        username.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearErrors();
            }
        });

        password.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearErrors();
            }
        });

        repassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                clearErrors();
            }
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(getContext().INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void resetCredentials() {
        if (!validateInput()) {
            return;
        }

        showProgress(true);
        clearErrors();

        networkExecutor.execute(() -> {
            try {
                performResetRequest();
            } catch (Exception e) {
                Log.e(TAG, "Reset password failed", e);
                handleResetError("Reset failed: " + e.getMessage());
            }
        });
    }

    private boolean validateInput() {
        boolean isValid = true;

        if (TextUtils.isEmpty(username.getText())) {
            username.setError("Username is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(password.getText())) {
            password.setError("Password is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(repassword.getText())) {
            repassword.setError("Please re-enter password");
            isValid = false;
        }

        if (!TextUtils.isEmpty(password.getText()) &&
                !TextUtils.isEmpty(repassword.getText()) &&
                !password.getText().toString().equals(repassword.getText().toString())) {
            password.setError("Passwords do not match");
            repassword.setError("Passwords do not match");
            isValid = false;
        }

        if (password.getText().length() < 4) {
            password.setError("Password must be at least 4 characters");
            isValid = false;
        }

        if (!checkBoxAdmin.isChecked() && !checkBoxSuper.isChecked() && !checkBoxTeller.isChecked()) {
            showError("Please select a role (Admin or Supervisor or Teller)");
            isValid = false;
        }

        return isValid;
    }

    private void performResetRequest() throws Exception {
        ConfigManager.refreshConfig(requireContext());
        TerminalConfigModel config = ConfigManager.getConfig(requireContext());

        UserModel userModel = createUserModel(config);
        String requestPayload = generateTerminalUsersRequest(userModel);

        Log.d(TAG, "Reset request payload: " + requestPayload);

        String resetUrl = buildResetUrl(config);
        NetworkService.initialize(requireContext(), resetUrl);
        NetworkService networkService = NetworkService.getInstance();

        String response = networkService.postPayLoadSyncLogin(requestPayload);
        Log.d(TAG, "Reset response: " + response);

        processResetResponse(response);
    }

    private UserModel createUserModel(TerminalConfigModel config_) {
        UserModel userModel = new UserModel();
        ConfigManager.refreshConfig(getActivity());
        TerminalConfigModel config = ConfigManager.getConfig(getActivity());
        userModel.setUsername(username.getText().toString().trim());
        userModel.setPassword(password.getText().toString());
        userModel.setRole(getSelectedRole());
        userModel.setRequestType("UPDATE");
        userModel.setTid(config.getTid());
        userModel.setMid(config.getMid());
        return userModel;
    }

    private String getSelectedRole() {
        if (checkBoxAdmin.isChecked()) {
            return "ADMIN";
        } else if (checkBoxSuper.isChecked()) {
            return "SUPERVISOR";
        }else if (checkBoxTeller.isChecked()) {
            return "TELLER";
        }
        else {
            return "";
        }
    }

    private String buildResetUrl(TerminalConfigModel config) {
        return "https://" + config.getLoginurl() + ":" + config.getLoginport() + "/";
    }

    private void processResetResponse(String response) {
        try {
            Document doc = parseXmlResponse(response);
            String responseCode = getValue(doc, "responseCode");
            String responseMessage = getValue(doc, "responseMessage");

            if ("00".equals(responseCode)) {
                handleResetSuccess(responseMessage);
            } else {
                handleResetFailure(responseMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing reset response", e);
            handleResetError("Failed to process server response");
        }
    }

    private Document parseXmlResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private void handleResetSuccess(String message) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });
    }

    private void handleResetFailure(String message) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            showError(message);
        });
    }

    private void handleResetError(String error) {
        requireActivity().runOnUiThread(() -> {
            showProgress(false);
            showError(error);
            Log.e(TAG, error);
        });
    }

    private void showProgress(boolean show) {
        requireActivity().runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            submit.setEnabled(!show);
            submit.setText(show ? "Processing..." : "Submit");
        });
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void clearErrors() {
        errorMessage.setVisibility(View.GONE);
        username.setError(null);
        password.setError(null);
        repassword.setError(null);
    }

    private void navigateToLogin() {
        try {
            NavHostFragment.findNavController(ResetPasswordFragment.this)
                    .navigate(R.id.resetPass_to_login);
        } catch (Exception e) {
            Log.e(TAG, "Navigation failed", e);
            Toast.makeText(requireActivity(), "Reset successful", Toast.LENGTH_SHORT).show();
        }
    }

    public static String generateTerminalUsersRequest(UserModel userModel) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
                return element.getTextContent().trim();
            }
        }
        return "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}