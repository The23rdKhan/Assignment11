/**
 * Assignment 11
 * File Name: SavedTripsFragment.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import edu.charlotte.assignment11.databinding.FragmentSavedTripsBinding;

/**
 * SavedTripsFragment fetches and displays the authenticated user's saved trips from Cloud Firestore.
 * Allows viewing selected trips on the map or deleting trips with a confirmation dialog.
 */
public class SavedTripsFragment extends Fragment implements SavedTripsAdapter.OnTripClickListener {

    private FragmentSavedTripsBinding binding;
    private SavedTripsListener mListener;
    private SavedTripsAdapter adapter;
    private final List<Trip> tripList = new ArrayList<>();

    public interface SavedTripsListener {
        void onTripSelected(Trip trip);
        void onBackToMap();
    }

    public SavedTripsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SavedTripsListener) {
            mListener = (SavedTripsListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement SavedTripsListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSavedTripsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.recyclerViewSavedTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SavedTripsAdapter(tripList, this);
        binding.recyclerViewSavedTrips.setAdapter(adapter);

        binding.buttonBack.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onBackToMap();
            }
        });

        loadTripsFromFirestore();
    }

    /**
     * Query Cloud Firestore for trips belonging to the currently authenticated user (UID).
     */
    private void loadTripsFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("trips")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null || !isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    tripList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Trip trip = doc.toObject(Trip.class);
                        if (trip != null) {
                            trip.setDocId(doc.getId()); // Store Firestore Document ID
                            tripList.add(trip);
                        }
                    }
                    // Sort in memory by createdAt descending to show newest trips first
                    java.util.Collections.sort(tripList, (t1, t2) -> Long.compare(t2.getCreatedAt(), t1.getCreatedAt()));
                    adapter.notifyDataSetChanged();

                    if (tripList.isEmpty()) {
                        binding.textViewEmpty.setVisibility(View.VISIBLE);
                    } else {
                        binding.textViewEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null || !isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load trips: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onTripClick(Trip trip) {
        if (mListener != null) {
            mListener.onTripSelected(trip);
        }
    }

    /**
     * Requires confirmation before deleting a saved trip from Cloud Firestore.
     */
    @Override
    public void onDeleteClick(Trip trip) {
        if (trip.getDocId() == null) return;

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Trip?")
                .setMessage("Are you sure you want to delete this saved trip?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (binding == null || !isAdded()) return;
                    binding.progressBar.setVisibility(View.VISIBLE);
                    FirebaseFirestore.getInstance().collection("trips").document(trip.getDocId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (binding == null || !isAdded()) return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Trip deleted successfully.", Toast.LENGTH_SHORT).show();
                                loadTripsFromFirestore();
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null || !isAdded()) return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed to delete trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
