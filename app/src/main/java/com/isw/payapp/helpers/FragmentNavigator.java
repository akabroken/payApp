package com.isw.payapp.helpers;

import android.content.Context;
import android.util.Log;

import androidx.fragment.app.FragmentManager;

import com.isw.payapp.R;
import com.isw.payapp.fragments.PrePrintFragment;

public class FragmentNavigator {
    private FragmentManager fragmentManager;
    private Context context;

    // Constructor to accept FragmentManager and Context
    public FragmentNavigator(FragmentManager fragmentManager, Context context) {
        this.fragmentManager = fragmentManager;
        this.context = context;
    }

    // Method to navigate to a Fragment
    public void navigateToFragment(String prePayload) {
        // Log the payload using the passed Context
        Log.i("FragmentNavigator", "Navigating with payload: " + prePayload);

        // Create an instance of PrePrintFragment and pass the payload
        PrePrintFragment prePrintFragment = PrePrintFragment.newInstance(prePayload,"","");

        // Perform the fragment transaction
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragment_complete_to_payment, prePrintFragment)
                .addToBackStack(null) // Optional: Add to back stack
                .commit();
    }
}
