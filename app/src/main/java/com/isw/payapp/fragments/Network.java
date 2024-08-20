package com.isw.payapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentNetworkBinding;


public class Network extends Fragment {

    private FragmentNetworkBinding binding;
    private ImageView imageViewBack,imageViewExit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNetworkBinding.inflate(inflater,container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle saveInstanceState){
        super.onViewCreated(view,saveInstanceState);



        imageViewBack = binding.imageViewBack.findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Network.this)
                        .navigate(R.id.network_to_settings);
            }
        });
        imageViewExit = binding.imageViewCancel.findViewById(R.id.imageViewCancel);
        imageViewExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Network.this)
                        .navigate(R.id.network_to_settings);
            }
        });

    }
}