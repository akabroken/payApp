package com.isw.payapp.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.isw.payapp.Adapters.IndexAdapter;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentIndexPageBinding;

import java.util.ArrayList;
import java.util.List;


public class IndexPage extends Fragment {

    private FragmentIndexPageBinding binding;
    private RecyclerView recyclerView;
    private IndexAdapter adapter;

    public IndexPage() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentIndexPageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView.findViewById(R.id.recyclerView);
        adapter = new IndexAdapter();

        // Set GridLayout with 3 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Populate RecyclerView with images
        List<Integer> imageList = getImageList(); // Your method to get image resources
        List<String> titles = getTitles();
        adapter.setImageList(imageList, titles);

    }

    private List<Integer> getImageList() {
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.pos);
        imageList.add(R.drawable.security_warning);
        imageList.add(R.drawable.gearsix);
        imageList.add(R.drawable.office_editing);
        imageList.add(R.drawable.exit);
        // Add more images as needed
        return imageList;
    }

    private List<String> getTitles(){
        List<String>title = new ArrayList<>();
        title.add("Transaction");
        title.add("PIN Select");
        title.add("Settings");
        title.add("Reports");
        title.add("Logout");
        return  title;
    }

    private ArrayList<ArrayList<Bitmap>> getDataForImageViews() {
        // Add your logic here to create and populate the data
        ArrayList<ArrayList<Bitmap>> icons = new ArrayList<>();

        ArrayList<Bitmap> imageData = new ArrayList<>();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pos);
        imageData.add(bitmap);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.security_warning);
        imageData.add(bitmap);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gearsix);
        imageData.add(bitmap);
        icons.add(imageData);

        ArrayList<Bitmap> imageData2 = new ArrayList<>();
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.office_editing);
        imageData.add(bitmap);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.exit);
        imageData.add(bitmap);
        icons.add(imageData2);

        // Populate imageData with Bitmaps (representing your images)
        return icons;
    }
}