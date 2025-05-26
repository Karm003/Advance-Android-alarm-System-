package com.unique.simplealarmclock.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.unique.simplealarmclock.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor edit;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "onCreate started");
            setContentView(R.layout.activity_main);

            // Initialize NavController
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.activity_main_nav_host_fragment);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Log.d(TAG, "NavController initialized successfully");
            } else {
                Log.e(TAG, "NavHostFragment not found");
                Toast.makeText(this, "Error initializing navigation", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize SharedPreferences
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            edit = sharedPreferences.edit();
            Log.d(TAG, "SharedPreferences initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error starting app", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating options menu", e);
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if(item.getItemId() == R.id.dayNigthMode) {
                boolean dn = sharedPreferences.getBoolean(getString(R.string.dayNightTheme), true);
                if(dn) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    edit.putBoolean(getString(R.string.dayNightTheme), false).apply();
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    edit.putBoolean(getString(R.string.dayNightTheme), true).apply();
                }
            }
            return super.onOptionsItemSelected(item);
        } catch (Exception e) {
            Log.e(TAG, "Error handling options item selection", e);
            return false;
        }
    }
}