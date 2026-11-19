/**
 * Assignment 11
 * File Name: MainActivity.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity implements
        LoginFragment.LoginListener,
        SignUpFragment.SignUpListener,
        TripMapFragment.TripMapListener,
        SavedTripsFragment.SavedTripsListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                replaceFragment(TripMapFragment.newInstance(null), false);
            } else {
                replaceFragment(new LoginFragment(), false);
            }
        }
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        var transaction = getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public void onLoginSuccess() {
        replaceFragment(TripMapFragment.newInstance(null), false);
    }

    @Override
    public void onGoToSignUp() {
        replaceFragment(new SignUpFragment(), true);
    }

    @Override
    public void onSignUpSuccess() {
        getSupportFragmentManager().popBackStack();
        replaceFragment(TripMapFragment.newInstance(null), false);
    }

    @Override
    public void onCancelSignUp() {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onLogout() {
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        replaceFragment(new LoginFragment(), false);
    }

    @Override
    public void onViewSavedTrips() {
        replaceFragment(new SavedTripsFragment(), true);
    }

    @Override
    public void onTripSelected(Trip trip) {
        getSupportFragmentManager().popBackStack();
        replaceFragment(TripMapFragment.newInstance(trip), false);
    }

    @Override
    public void onBackToMap() {
        getSupportFragmentManager().popBackStack();
    }
}