package com.isw.payapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.isw.payapp.Adapters.CustomDividerItemDecoration;
import com.isw.payapp.Adapters.IndexAdapter;
import com.isw.payapp.Adapters.TransactionAdapter;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentIndexPageBinding;
import com.isw.payapp.databinding.FragmentTransactionBinding;

import java.util.ArrayList;
import java.util.List;


public class Transaction extends Fragment {

    private FragmentTransactionBinding binding;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private ImageView imageViewBack,imageViewExit;

    public Transaction() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.tranrecyclerView.findViewById(R.id.tranrecyclerView);
        adapter = new TransactionAdapter();

        // Set GridLayout with 3 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new CustomDividerItemDecoration(getContext()));

        // Populate RecyclerView with images
        List<Integer> imageList = getImageList(); // Your method to get image resources
        List<String> titles = getTitles();
        adapter.setImageList(imageList, titles);

        imageViewBack = binding.imageViewBack.findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Transaction.this)
                        .navigate(R.id.transaction_to_index);
            }
        });
        imageViewExit = binding.imageViewCancel.findViewById(R.id.imageViewCancel);
        imageViewExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Transaction.this)
                        .navigate(R.id.transaction_to_index);
            }
        });

    }

    private List<Integer> getImageList() {
        List<Integer> imageList = new ArrayList<>();
        imageList.add(R.drawable.shopping_cart_simple);
        imageList.add(R.drawable.arrows_reversal);
        imageList.add(R.drawable.arrows_refund);
        return imageList;
    }

    private List<String> getTitles(){
        List<String>title = new ArrayList<>();
        title.add("Purchase");
        title.add("Reversal");
        title.add("Refund");

        return  title;
    }
}