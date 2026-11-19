/**
 * Assignment 11
 * File Name: SavedTripsAdapter.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.charlotte.assignment11.databinding.ItemSavedTripBinding;

/**
 * SavedTripsAdapter binds the list of saved trips to the RecyclerView in SavedTripsFragment.
 * Handles user clicks on trip items (to view on map) and delete button clicks.
 */
public class SavedTripsAdapter extends RecyclerView.Adapter<SavedTripsAdapter.TripViewHolder> {

    private final List<Trip> tripList;
    private final OnTripClickListener listener;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
        void onDeleteClick(Trip trip);
    }

    public SavedTripsAdapter(List<Trip> tripList, OnTripClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSavedTripBinding binding = ItemSavedTripBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TripViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        holder.bind(trip, listener);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        private final ItemSavedTripBinding binding;

        public TripViewHolder(ItemSavedTripBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Trip trip, OnTripClickListener listener) {
            binding.textViewTripTitle.setText(trip.getTripTitle());
            binding.textViewTripStart.setText("Start: " + trip.getStartAddress());
            binding.textViewTripEnd.setText("End: " + trip.getEndAddress());

            // Format epoch timestamp into readable date/time string
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(trip.getCreatedAt()));
            binding.textViewDate.setText("Saved on: " + dateStr);

            // Click trip to view route on map
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTripClick(trip);
                }
            });

            // Click delete button to trigger deletion flow
            binding.imageButtonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(trip);
                }
            });
        }
    }
}
