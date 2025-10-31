package com.isw.payapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.isw.payapp.databinding.ActivityMainBinding;
import com.isw.payapp.devices.DeviceFactory;
import com.isw.payapp.devices.interfaces.IPrinterProcessor;
import com.isw.payapp.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private boolean isNetworkAvailable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initial network check

        checkPermissions();
        checkNetworkAndInitialize();
    }

    private void checkNetworkAndInitialize() {
        isNetworkAvailable = NetworkUtils.isNetworkAvailable(this);

        if (!isNetworkAvailable) {
            showNoInternetLayout();
            return;
        }


        initializeMainApp();
    }

    private void showNoInternetLayout() {
        setContentView(R.layout.activity_no_internet);
        findViewById(R.id.retryButton).setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(this)) {
                Log.d("Device", "Current POS brand:------->>>>>>> ");

                initializeMainApp();
            } else {
                Toast.makeText(this,
                        "Still no internet connection. Please try again later.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeMainApp() {
        Log.d("Device", "Current POS brand:------->>>>>>> ");
        //String brand = DeviceDetector.getDeviceBrand();
        String brand = Build.BRAND +"_ " +Build.MANUFACTURER
                +" _"+ Build.MODEL +" _" + Build.DEVICE+"_ "+Build.PRODUCT
                +"_"+Build.MODEL;
        Log.d("Device", "Current POS brand: " + brand);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupNavigation();
        loadImages();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void setupNavigation() {
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);

        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }


    private void loadImages() {
        // Use binding instead of findViewById
        ImageView logo = findViewById(R.id.main_activity_imageView);
        if (logo != null && logo.getParent() == binding.getRoot()) {
            Glide.with(this)
                    .load(R.drawable.sidian_bank_logo)
                    .into(logo); // Using the binding reference
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check network again when activity resumes
        if (!isNetworkAvailable && NetworkUtils.isNetworkAvailable(this)) {
            initializeMainApp();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // Handle settings action
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // In your MainActivity or base activity
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_NETWORK_STATE
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            }
        }
    }
}