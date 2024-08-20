package com.isw.payapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.isw.payapp.Adapters.IndexAdapter;
import com.isw.payapp.Adapters.SettingAdapter;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentSettingsBinding;

import java.util.ArrayList;
import java.util.List;


public class Settings extends Fragment {

    private FragmentSettingsBinding binding;
    private RecyclerView recyclerView;
    private SettingAdapter adapter;
    private ImageView imageViewBack, imageViewExit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView.findViewById(R.id.recyclerView);
        adapter = new SettingAdapter();

        // Set GridLayout with 3 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Populate RecyclerView with images
        List<Integer> imageList = getImageList(); // Your method to get image resources
        List<String> titles = getTitles();
        adapter.setImageList(imageList, titles);

        imageViewBack = binding.imageViewBack.findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Settings.this)
                        .navigate(R.id.settings_to_index);
            }
        });
        imageViewExit = binding.imageViewCancel.findViewById(R.id.imageViewCancel);
        imageViewExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Settings.this)
                        .navigate(R.id.settings_to_index);
            }
        });
    }

    private List<Integer> getImageList() {
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.pos);
        imageList.add(R.drawable.pos);
        imageList.add(R.drawable.pos);
//        imageList.add(R.drawable.office_editing);
//        imageList.add(R.drawable.exit);
        // Add more images as needed
        return imageList;
    }

    private List<String> getTitles() {
        List<String> title = new ArrayList<>();
        title.add("Key Download");
        title.add("Terminal Setting");
        title.add("Network");
//        title.add("Reports");
//        title.add("Logout");
        return title;
    }

}