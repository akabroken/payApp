package com.isw.payapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.isw.payapp.Adapters.ImageAdapter;
import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentHomeBinding;
import com.isw.payapp.model.ItemData;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<ItemData> itemDataList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        itemDataList = generateItemData();
        imageAdapter = new ImageAdapter(requireContext(), itemDataList);
        recyclerView.setAdapter(imageAdapter);

        imageAdapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ItemData itemData, int position) {
                String clickedTitle = itemData.getTitle();

                switch (clickedTitle) {
                    case "Purchase":
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.action_HomeFragment_to_PaymentFragment);
                        break;

                    case "Keymgnt":
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.homeFragment_to_keydownloadFragement);
                        break;
                    case "Reversal":
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.homeFragment_to_reversal);
                        break;

                    case "Preauth":
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.homeFragment_to_preauth);
                        break;

                    case "Authcomp":
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.homeFragment_to_authcompite);
                        break;
                    case "Refund":
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.homeFragment_to_refund);
                        break;
                    case "Reports":
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.index_to_report); // Add this navigation action
                        break;
                    default:
                        Toast.makeText(requireContext(), "Clicked: " + clickedTitle, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private List<ItemData> generateItemData() {
        List<ItemData> itemDataList = new ArrayList<>();

        int[] imageId = {R.drawable.new_purchase, R.drawable.new_reversal, R.drawable.new_preauth,
                R.drawable.new_auth_complete, R.drawable.new_refund,
                R.drawable.new_key_mgnt, R.drawable.new_reports};

        String[] titleData = {"Purchase", "Reversal", "Preauth", "Authcomp", "Refund",
                "Keymgnt", "Reports"};

        // Validate arrays have same length
        if (imageId.length != titleData.length) {
            throw new IllegalStateException("Image and title arrays must have the same length");
        }

        for (int i = 0; i < imageId.length; i++) {
            ItemData itemData = new ItemData(imageId[i], titleData[i]);
            itemDataList.add(itemData);
        }

        return itemDataList;
    }
}