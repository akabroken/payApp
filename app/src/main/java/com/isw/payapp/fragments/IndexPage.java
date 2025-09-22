package com.isw.payapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.isw.payapp.Adapters.IndexAdapter;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentIndexPageBinding;
import com.isw.payapp.helpers.SessionManager;
import com.isw.payapp.model.GridMenuItem;
//import com.isw.payapp.activities.Login; // Make sure this import is correct

import java.util.Arrays;
import java.util.List;

public class IndexPage extends Fragment {

    private FragmentIndexPageBinding binding;
    private SessionManager sessionManager;
    private IndexAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentIndexPageBinding.inflate(inflater, container, false);
        Glide.with(this)
                .load(R.drawable.sidian_bank_logo)
                .into(binding.imageView);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        checkAuthentication();
        setupRecyclerView();
        displayWelcomeMessage();
    }

    private void initializeComponents() {
        sessionManager = new SessionManager(requireContext());
        adapter = new IndexAdapter(requireContext());
    }

    private void checkAuthentication() {
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
        }
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        List<GridMenuItem> menuItems = Arrays.asList(
                new GridMenuItem(R.drawable.security_warning, getString(R.string.pin_select), R.id.index_to_pinselect),
                new GridMenuItem(R.drawable.gearsix, getString(R.string.settings), R.id.index_to_settings),
                new GridMenuItem(R.drawable.exit, getString(R.string.logout), R.id.index_to_login)
        );

        adapter.setMenuItems(menuItems);
        adapter.setOnItemClickListener(this::handleMenuItemClick);
    }

    private void handleMenuItemClick(GridMenuItem menuItem) {
        if (menuItem.getTitle().equals(getString(R.string.logout))) {
            sessionManager.logout();
            showToast(getString(R.string.logged_out_successfully));
            navigateToLogin();
        } else {
            navigateTo(menuItem.getActionId());
        }
    }

    private void navigateTo(int actionId) {
        try {
            Navigation.findNavController(requireView()).navigate(actionId);
        } catch (IllegalArgumentException e) {
            Log.e("Navigation", "Invalid navigation action", e);
            showToast(getString(R.string.navigation_error));
        }
    }

//    private void navigateToLogin() {
//        Intent intent = new Intent(requireActivity(), Login.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//        requireActivity().finish();
//        Navigation.findNavController(this).navigate(R.id.index_to_login);
//    }
    private void navigateToLogin() {
        // Use Navigation Component for fragment navigation
        Navigation.findNavController(requireView()).navigate(R.id.index_to_login);
    }

    private void displayWelcomeMessage() {
        String username = sessionManager.getKeyFullname();
        if (username != null) {
            binding.usernameTextView.setText(getString(R.string.welcome_message, username));
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check session expiration when fragment resumes
        if (sessionManager != null && !sessionManager.isLoggedIn()) {
            navigateToLogin();
        }
    }
}