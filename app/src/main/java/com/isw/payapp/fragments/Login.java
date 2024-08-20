package com.isw.payapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.isw.payapp.R;
import com.isw.payapp.databinding.FragmentLoginBinding;


public class Login extends Fragment {

    // TODO: Declare login fragment Binding
    private FragmentLoginBinding binding;
    private EditText password,username;
    private CheckBox reme;
    private TextView fPassword;
    private Button login;

    public Login() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(inflater,container,false);

        return binding.getRoot();
    }


    public void onViewCreated(@NonNull View view, Bundle saveBundle){
        super.onViewCreated(view,saveBundle);

        login = binding.buttonLogin;
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(Login.this).navigate(R.id.login_to_home);
            }
        });

    }
}