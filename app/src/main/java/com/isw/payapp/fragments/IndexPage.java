package com.isw.payapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.isw.payapp.Adapters.IndexAdapter;
import com.isw.payapp.BuildConfig;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentIndexPageBinding;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.GridMenuItem;

import java.util.Arrays;
import java.util.List;

public class IndexPage extends Fragment {

    private static final long IDLE_TIMEOUT = 10 * 60 * 1000; // 10 minutes
    private static final long IDLE_CHECK_INTERVAL = 30 * 1000; // Check every 30 seconds
    private static final String TAG = "IndexPage";

    private Handler idleHandler;
    private Runnable idleRunnable;
    private FragmentIndexPageBinding binding;
    private SessionManager sessionManager;
    private IndexAdapter adapter;
    private AlertDialog timeoutDialog;
    private NavController navController;

    // Track if we're currently showing the timeout dialog to prevent duplicates
    private boolean isShowingTimeoutDialog = false;
    private boolean isFragmentActive = false;

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

        // Initialize handler
        idleHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view");
        binding = FragmentIndexPageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: View created");

        try {
            navController = Navigation.findNavController(view);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get NavController", e);
        }

        // Initialize components first
        initializeComponents();

        // Check authentication before proceeding
        if (!isSessionValid()) {
            Log.i(TAG, "User not logged in or session invalid, navigating to login");
            navigateToLogin();
            return;
        }

        // Setup UI components
        setupUI();
        setupIdleTimer();

        // Start idle monitoring
        startIdleMonitoring();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Fragment started");
        isFragmentActive = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");
        isFragmentActive = true;

        if (!isAdded() || getView() == null) {
            Log.w(TAG, "Fragment not attached, skipping resume operations");
            return;
        }

        // Check session validity
        if (!isSessionValid()) {
            Log.i(TAG, "Session invalid on resume, navigating to login");
            navigateToLogin();
            return;
        }

        // Check if session expired while fragment was paused
        if (sessionManager != null && sessionManager.shouldLogoutDueToInactivity()) {
            Log.d(TAG, "Session expired due to inactivity");
            showTimeoutDialog();
        } else {
            // Update activity time and restart monitoring
            if (sessionManager != null) {
                sessionManager.updateLastActivityTime();
            }
            startIdleMonitoring();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Fragment paused");
        isFragmentActive = false;

        // Stop idle monitoring when not visible
        stopIdleMonitoring();

        // Update last activity time
        if (sessionManager != null && sessionManager.isLoggedIn()) {
            sessionManager.updateLastActivityTime();
        }

        // Dismiss dialog if showing
        if (timeoutDialog != null && timeoutDialog.isShowing()) {
            timeoutDialog.dismiss();
            isShowingTimeoutDialog = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Fragment stopped");
        isFragmentActive = false;
        stopIdleMonitoring();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Fragment view destroyed");

        stopIdleMonitoring();

        if (timeoutDialog != null && timeoutDialog.isShowing()) {
            timeoutDialog.dismiss();
        }

        timeoutDialog = null;
        binding = null; // Clean up view binding
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Fragment destroyed");

        stopIdleMonitoring();

        if (timeoutDialog != null) {
            timeoutDialog.dismiss();
            timeoutDialog = null;
        }

        idleHandler = null;
        idleRunnable = null;
        sessionManager = null;
        adapter = null;
    }

    private boolean isSessionValid() {
        if (sessionManager == null) {
            Log.e(TAG, "SessionManager is null");
            return false;
        }

        boolean isValid = sessionManager.isLoggedIn();
        Log.d(TAG, "isSessionValid: " + isValid);

        if (!isValid) {
            Log.d(TAG, "Session invalid. Session info: \n" + sessionManager.getSessionInfo());
        }

        return isValid;
    }

    private void initializeComponents() {
        if (sessionManager == null) {
            sessionManager = new SessionManager(requireContext());
        }

        if (adapter == null) {
            adapter = new IndexAdapter(requireContext());
        }

        if (idleHandler == null) {
            idleHandler = new Handler(Looper.getMainLooper());
        }
    }

    private void setupUI() {
        // Load logo
        try {
            Glide.with(this)
                    .load(BuildConfig.APP_LOGO)
                    .into(binding.imageView);
        } catch (Exception e) {
            Log.e(TAG, "Error loading logo", e);
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Display welcome message
        displayWelcomeMessage();
    }

    private void setupRecyclerView() {
        if (binding == null) return;

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        List<GridMenuItem> menuItems = Arrays.asList(
                new GridMenuItem(R.drawable.pos, getString(R.string.transaction_index), R.id.index_to_transaction),
                new GridMenuItem(R.drawable.security_warning, getString(R.string.pin_select), R.id.index_to_pinselect),
                new GridMenuItem(R.drawable.gearsix, getString(R.string.settings), R.id.index_to_settings),
                new GridMenuItem(R.drawable.exit, getString(R.string.logout), R.id.index_to_login)
        );

        adapter.setMenuItems(menuItems);
        adapter.setOnItemClickListener(this::handleMenuItemClick);
    }

    private void handleMenuItemClick(GridMenuItem menuItem) {
        if (binding == null || !isAdded()) return;

        Log.d(TAG, "Menu item clicked: " + menuItem.getTitle());

        if (getString(R.string.logout).equals(menuItem.getTitle())) {
            showLogoutConfirmationDialog();
        } else {
            navigateTo(menuItem.getActionId());
        }
    }

    private void showLogoutConfirmationDialog() {
        if (!isAdded() || getContext() == null) return;

        // Using MaterialAlertDialogBuilder for better appearance
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.confirm_logout_title))
                .setMessage(getString(R.string.confirm_logout_message))
                .setPositiveButton(getString(R.string.logout), (dialog, which) -> {
                    // User confirmed logout
                    performLogout();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    // User cancelled, just dismiss the dialog
                    dialog.dismiss();
                })
                .setCancelable(true) // Allows dismissing by tapping outside
                .setOnDismissListener(dialog -> resetIdleTimer()) // Reset timer when dialog is dismissed
                .show();
    }

    private void performLogout() {
        Log.d(TAG, "Performing logout");
        stopIdleMonitoring();

        if (sessionManager != null) {
            sessionManager.logout();
        }

        showToast(getString(R.string.logged_out_successfully));
        navigateToLogin();
    }

    private void navigateTo(int actionId) {
        if (!isAdded() || getView() == null) {
            Log.e(TAG, "Cannot navigate - fragment not attached");
            return;
        }

        try {
            resetIdleTimer(); // Reset timer on navigation

            if (navController != null) {
                navController.navigate(actionId);
            } else {
                Navigation.findNavController(requireView()).navigate(actionId);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid navigation action: " + actionId, e);
            showToast(getString(R.string.navigation_error));
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            showToast(getString(R.string.navigation_error));
        }
    }

    private void navigateToLogin() {
        Log.d(TAG, "Navigating to login");

        if (!isAdded() || getView() == null) {
            Log.e(TAG, "Cannot navigate - fragment not attached");
            return;
        }

        try {
            NavController controller = navController != null ? navController : Navigation.findNavController(requireView());

            // Check current destination to prevent circular navigation
            NavDestination currentDestination = controller.getCurrentDestination();
            if (currentDestination != null) {
                int currentId = currentDestination.getId();
                if (currentId == R.id.Login) {
                    Log.d(TAG, "Already at login fragment, skipping navigation");
                    return;
                }

                Log.d(TAG, "Current destination: " + currentId + ", navigating to login");
            }

            // Clear any dialogs first
            dismissTimeoutDialog();

            // Navigate to login with popUpTo to clear back stack
            controller.navigate(R.id.index_to_login);

        } catch (Exception e) {
            Log.e(TAG, "Navigation to login failed", e);
            showToast(getString(R.string.session_expired_please_login));
        }
    }

    private void displayWelcomeMessage() {
        if (binding == null || !isAdded()) return;

        try {
            if (sessionManager != null) {
                String username = sessionManager.getKeyFullname();
                if (username != null && !username.isEmpty()) {
                    binding.usernameTextView.setText(getString(R.string.welcome_message, username));
                    binding.usernameTextView.setVisibility(View.VISIBLE);
                } else {
                    binding.usernameTextView.setVisibility(View.GONE);
                }
            } else {
                binding.usernameTextView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying welcome message", e);
            binding.usernameTextView.setVisibility(View.GONE);
        }
    }

    private void setupIdleTimer() {
        idleRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFragmentActive || !isAdded() || getContext() == null) {
                    Log.d(TAG, "Idle check skipped - fragment not active");
                    return;
                }

                if (sessionManager != null && sessionManager.isLoggedIn()) {
                    if (sessionManager.shouldLogoutDueToInactivity()) {
                        Log.d(TAG, "Session expired, showing timeout dialog");
                        requireActivity().runOnUiThread(() -> {
                            if (isFragmentActive && isAdded()) {
                                showTimeoutDialog();
                            }
                        });
                    } else {
                        // Schedule next check
                        if (idleHandler != null) {
                            idleHandler.postDelayed(this, IDLE_CHECK_INTERVAL);
                        }
                    }
                }
            }
        };
    }

    private void showTimeoutDialog() {
        // Prevent multiple dialogs
        if (isShowingTimeoutDialog || (timeoutDialog != null && timeoutDialog.isShowing())) {
            Log.d(TAG, "Timeout dialog already showing, skipping");
            return;
        }

        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Cannot show timeout dialog - fragment not attached");
            return;
        }

        isShowingTimeoutDialog = true;
        Log.d(TAG, "Showing timeout dialog");

        // Using MaterialAlertDialogBuilder for consistency
        timeoutDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.session_timeout_title))
                .setMessage(getString(R.string.session_timeout_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    isShowingTimeoutDialog = false;
                    Log.d(TAG, "User acknowledged timeout, logging out");
                    performLogout();
                })
                .setOnDismissListener(dialog -> {
                    isShowingTimeoutDialog = false;
                    Log.d(TAG, "Timeout dialog dismissed");
                })
                .create();

        timeoutDialog.show();
    }

    private void dismissTimeoutDialog() {
        if (timeoutDialog != null && timeoutDialog.isShowing()) {
            timeoutDialog.dismiss();
            isShowingTimeoutDialog = false;
        }
    }

    private void startIdleMonitoring() {
        if (sessionManager == null || idleHandler == null || idleRunnable == null) {
            Log.e(TAG, "Cannot start idle monitoring - components not initialized");
            return;
        }

        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "Cannot start idle monitoring - user not logged in");
            return;
        }

        Log.d(TAG, "Starting idle monitoring");

        // Update last activity time to now
        sessionManager.updateLastActivityTime();

        // Remove any existing callbacks and start fresh
        idleHandler.removeCallbacks(idleRunnable);
        idleHandler.postDelayed(idleRunnable, IDLE_CHECK_INTERVAL);
    }

    private void stopIdleMonitoring() {
        Log.d(TAG, "Stopping idle monitoring");
        if (idleHandler != null && idleRunnable != null) {
            idleHandler.removeCallbacks(idleRunnable);
        }
        isShowingTimeoutDialog = false;
    }

    private void resetIdleTimer() {
        Log.d(TAG, "Resetting idle timer");

        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }

        // Update last activity time
        sessionManager.updateLastActivityTime();

        // Restart monitoring with full timeout
        startIdleMonitoring();
    }

    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // Public method to handle user interaction from activity
    public void onUserInteractionDetected() {
        if (isFragmentActive && isAdded() && sessionManager != null && sessionManager.isLoggedIn()) {
            resetIdleTimer();
        }
    }

    // Optional: Add touch listener to the fragment view
    private void setupTouchListener() {
        if (binding != null) {
            binding.getRoot().setOnTouchListener((v, event) -> {
                onUserInteractionDetected();
                return false; // Return false to allow other touch listeners
            });
        }
    }
}