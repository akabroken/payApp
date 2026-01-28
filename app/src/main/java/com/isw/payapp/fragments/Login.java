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
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
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
import java.util.concurrent.RejectedExecutionException;

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
    private static final String TAG = "LoginFragment";
    private boolean isProcessingLogin = false;
    private NavController navController;
    private boolean shouldCheckSession = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Fragment created");

        // Initialize SessionManager
        try {
            sessionManager = new SessionManager(requireContext());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SessionManager", e);
        }

        // Debug session state
        if (sessionManager != null) {
            Log.d(TAG, "SessionManager test - hasData: " + sessionManager.hasSessionData() +
                    ", isLoggedIn: " + sessionManager.isLoggedIn());
            Log.d(TAG, "Session info: \n" + sessionManager.getSessionInfo());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view");
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
        Log.d(TAG, "onViewCreated: View created");

        try {
            navController = Navigation.findNavController(view);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get NavController", e);
        }

        initializeComponents();
        setupClickListeners();

        // Only check session if we haven't already
        if (shouldCheckSession) {
            checkExistingSession();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Fragment started");

        if (shouldCheckSession) {
            checkExistingSession();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");

        // Update last activity time if user is logged in
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            sessionManager.updateLastActivityTime();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Fragment paused");

        // Cancel any ongoing login process
        isProcessingLogin = false;
        showLoading(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Fragment view destroyed");
        binding = null; // Clean up binding to prevent memory leaks
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Fragment destroyed");
    }

    private void checkExistingSession() {
        Log.d(TAG, "checkExistingSession called");

        if (sessionManager == null) {
            Log.e(TAG, "SessionManager is null, initializing...");
            try {
                sessionManager = new SessionManager(requireContext());
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize SessionManager", e);
                return;
            }
        }

        boolean isLoggedIn = sessionManager.isLoggedIn();
        Log.d(TAG, "checkExistingSession: isLoggedIn = " + isLoggedIn +
                ", Fragment isAdded = " + isAdded() +
                ", View = " + getView());

        if (isLoggedIn && isAdded() && getView() != null) {
            Log.d(TAG, "User is logged in, preparing to navigate to home...");

            // Add delay to ensure UI is ready
            getView().postDelayed(() -> {
                if (sessionManager != null && sessionManager.isLoggedIn() && isAdded()) {
                    Log.d(TAG, "Auto-navigating to home from session check");
                    shouldCheckSession = false; // Prevent multiple checks
                    navigateToHome();
                } else {
                    Log.d(TAG, "Session check cancelled - fragment detached or session invalid");
                }
            }, 1000); // Increased delay for better stability
        } else if (!isLoggedIn) {
            Log.d(TAG, "User is not logged in, resetting UI");
            resetLoginUI();
        }
    }

    private void resetLoginUI() {
        if (binding != null) {
            binding.textUsername.setText("");
            binding.textPassword.setText("");
            binding.checkBoxAdmin.setChecked(false);
            binding.checkBoxSuper.setChecked(false);
            binding.errorMessage.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonLogin.setEnabled(true);
            binding.buttonLogin.setText(getString(R.string.login));

            // Focus on username field
            binding.textUsername.requestFocus();
        }
    }

    private void initializeComponents() {
        try {
            if (sessionManager == null) {
                sessionManager = new SessionManager(requireContext());
            }
            terminalConfig = new TerminalConfig();

            // Clear any expired session
            if (sessionManager.isLoggedIn() && sessionManager.isSessionExpired()) {
                Log.w(TAG, "Expired session detected, logging out...");
                sessionManager.logout();
                resetLoginUI();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
            showError(getString(R.string.initialization_failed));
        }
    }

    private void setupClickListeners() {
        binding.buttonLogin.setOnClickListener(v -> {
            if (isProcessingLogin) {
                Log.d(TAG, "Login already in progress, ignoring click");
                showToast(getString(R.string.login_in_progress));
                return;
            }
            hideKeyboard(v);
            validateCredentials();
        });

        binding.textViewResetPass.setOnClickListener(v -> {
            try {
                // Check current destination to avoid navigation conflicts
                if (navController != null) {
                    NavDestination currentDestination = navController.getCurrentDestination();
                    if (currentDestination != null && currentDestination.getId() == R.id.Login) {
                        navController.navigate(R.id.login_to_resetpassword);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Navigation error", e);
                showToast(getString(R.string.navigation_error));
            }
        });

        // Radio button behavior - only one can be selected
        binding.checkBoxAdmin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.checkBoxSuper.setChecked(false);
            }
        });

        binding.checkBoxSuper.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.checkBoxAdmin.setChecked(false);
            }
        });

        // Handle Enter key on password field
        binding.textPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(v);
                validateCredentials();
                return true;
            }
            return false;
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
        if (isProcessingLogin) return;

        String username = binding.textUsername.getText().toString().trim();
        String password = binding.textPassword.getText().toString().trim();

        if (!validateInputs(username, password)) {
            return;
        }

        String selectedRole = getSelectedRole();
        if (selectedRole == null) {
            showError(getString(R.string.select_role));
            return;
        }

        isProcessingLogin = true;
        showLoading(true);

        try {
            ConfigManager.refreshConfig(requireActivity());
            TerminalConfigModel config = ConfigManager.getConfig(requireActivity());

            if (config == null) {
                showError(getString(R.string.configuration_error));
                isProcessingLogin = false;
                showLoading(false);
                return;
            }

            UserModel userModel = createUserModel(username, password, selectedRole);

            if (userModel == null) {
                showError(getString(R.string.user_model_error));
                isProcessingLogin = false;
                showLoading(false);
                return;
            }

            String requestXml = generateTerminalUsersRequest(userModel);
            Log.i(TAG, "Login request: " + requestXml);

            ExecutorService networkExecutor = NetworkExecutor.getExecutor();
            networkExecutor.execute(() -> {
                try {
                    OkHttpClient unsafeClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
                    String login_URL = "https://" + config.getLoginurl() + ":" + config.getLoginport() + "/";
                    Log.d(TAG, "Login URL: " + login_URL);

                    NetworkService.initialize(requireContext(), login_URL);
                    NetworkService networkService = NetworkService.getInstance();
                    String response = networkService.postPayLoadSyncLogin(requestXml);
                    Log.i(TAG, "Login response: " + response);

                    if (!isAdded()) {
                        Log.w(TAG, "Fragment not attached, cannot update UI");
                        return;
                    }

                    requireActivity().runOnUiThread(() -> {
                        try {
                            handleLoginResponse(response, username, selectedRole);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling login response", e);
                            showError(getString(R.string.login_processing_error));
                            isProcessingLogin = false;
                            showLoading(false);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Network error", e);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            showError(getString(R.string.network_error));
                            isProcessingLogin = false;
                            showLoading(false);
                        });
                    }
                }
            });

        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Executor rejected task", e);
            showError(getString(R.string.server_busy));
            isProcessingLogin = false;
            showLoading(false);
        } catch (Exception e) {
            Log.e(TAG, "Login error", e);
            showError(getString(R.string.login_failed));
            isProcessingLogin = false;
            showLoading(false);
        }
    }

    private boolean validateInputs(String username, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(username)) {
            binding.textUsername.setError(getString(R.string.username_required));
            binding.textUsername.requestFocus();
            isValid = false;
        } else {
            binding.textUsername.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            binding.textPassword.setError(getString(R.string.password_required));
            if (isValid) {
                binding.textPassword.requestFocus();
            }
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

    private UserModel createUserModel(String username, String password, String role) {
        UserModel userModel = new UserModel();
        TerminalConfigModel config = ConfigManager.getConfig(requireActivity());

        if (config == null) {
            Log.e(TAG, "Config is null");
            return null;
        }

        userModel.setUsername(username);
        userModel.setPassword(password);
        userModel.setRole(role);

        try {
            userModel.setTid(config.getTid());
            userModel.setMid(config.getMid());
            Log.d(TAG, "Terminal ID: " + config.getTid() + ", Merchant ID: " + config.getMid());
        } catch (Exception e) {
            Log.e(TAG, "Error loading terminal data", e);
            return null;
        }

        userModel.setRequestType(getRequestType(role));
        return userModel;
    }

    private String getSelectedRole() {
        if (binding.checkBoxAdmin.isChecked()) {
            return "ADMIN";
        } else if (binding.checkBoxSuper.isChecked()) {
            return "SUPERVISOR";
        }
        return null;
    }

    private String getRequestType(String role) {
        return "ADMIN".equals(role) ? "Admin" : "Supervisor";
    }

    private void handleLoginResponse(String response, String username, String role) throws Exception {
        isProcessingLogin = false;
        showLoading(false);

        if (TextUtils.isEmpty(response)) {
            showError(getString(R.string.empty_server_response));
            return;
        }

        Document doc = parseXmlResponse(response);
        String responseCode = getValue(doc, "responseCode");
        String responseMessage = getValue(doc, "responseMessage");

        Log.d(TAG, "Login response - Code: " + responseCode + ", Message: " + responseMessage);

        if ("00".equals(responseCode)) {
            String names = getValue(doc, "names");
            if (sessionManager == null) {
                sessionManager = new SessionManager(requireContext());
            }

            boolean sessionCreated = sessionManager.createSession(username, names, role);
            Log.d(TAG, "Session created: " + sessionCreated);
            Log.d(TAG, "Session info after creation: \n" + sessionManager.getSessionInfo());

            if (sessionCreated) {
                showToast(getString(R.string.login_successful));

                // Verify session was actually created
                if (sessionManager.isLoggedIn()) {
                    navigateToHome();
                } else {
                    Log.e(TAG, "Session creation reported success but isLoggedIn() returns false");
                    showError(getString(R.string.session_creation_failed));
                    sessionManager.logout();
                }
            } else {
                showError(getString(R.string.session_creation_failed));
            }
        } else {
            String errorMsg = TextUtils.isEmpty(responseMessage)
                    ? getString(R.string.invalid_credentials)
                    : responseMessage;
            showError(errorMsg);
            Log.e(TAG, "Login failed: " + errorMsg);
        }
    }

    private Document parseXmlResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
//        factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
//        factory.setXIncludeAware(false);
//        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.buttonLogin.setEnabled(!isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.buttonLogin.setText(isLoading ? getString(R.string.logging_in) : getString(R.string.login));

            // Disable other interactive elements during loading
            binding.textUsername.setEnabled(!isLoading);
            binding.textPassword.setEnabled(!isLoading);
            binding.checkBoxAdmin.setEnabled(!isLoading);
            binding.checkBoxSuper.setEnabled(!isLoading);
            binding.textViewResetPass.setEnabled(!isLoading);
        }
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

            // Scroll to error message if needed
           // binding.scrollView.post(() -> binding.scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void navigateToHome() {
        Log.d(TAG, "navigateToHome called");

        if (!isAdded() || getView() == null) {
            Log.e(TAG, "Cannot navigate - fragment not attached");
            return;
        }

        try {
            // Verify session is still valid before navigating
            if (sessionManager != null && sessionManager.isLoggedIn()) {
                Log.d(TAG, "Session is valid, navigating to home...");

                NavController navController = Navigation.findNavController(getView());

                // Check if we're already at home to avoid navigation loops
                NavDestination currentDestination = navController.getCurrentDestination();

                if (currentDestination != null && currentDestination.getId() == R.id.indexpage) {
                    Log.d(TAG, "Already at home, no navigation needed");
                    return;
                }

                Log.d(TAG, "Navigating to home fragment");
                // Use popUpTo to clear back stack
                navController.navigate(R.id.login_to_home);
            } else {
                Log.e(TAG, "Session invalid when trying to navigate to home");
                showError(getString(R.string.session_expired));
                resetLoginUI();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Navigation controller not found", e);
            showToast(getString(R.string.navigation_error));
        } catch (Exception e) {
            Log.e(TAG, "Navigation to home failed", e);
            showToast(getString(R.string.navigation_error));
        }
    }

    // XML generation helper methods
    public static String generateTerminalUsersRequest(UserModel userModel) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
//        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
//        factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

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
        transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);

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