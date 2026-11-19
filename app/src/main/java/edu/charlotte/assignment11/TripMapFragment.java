/**
 * Assignment 11
 * File Name: TripMapFragment.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.PlaceAutocomplete;
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.charlotte.assignment11.databinding.FragmentTripMapBinding;

/**
 * TripMapFragment is the core screen of the application.
 * Responsibilities:
 * 1. Initialize Google Map and Places SDK (version 6.0.2 with New Places API enabled).
 * 2. Launch Places Autocomplete intent to let users select start and destination locations.
 * 3. Fetch driving routes between start and end using Routes API v2, and render polyline on the map.
 * 4. Fetch nearby gas stations at the start, midpoint, and destination using Places API (New).
 * 5. Save trip plans to Cloud Firestore or display previously saved trips selected from SavedTripsFragment.
 */
public class TripMapFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_TRIP = "arg_trip";

    private FragmentTripMapBinding binding;
    private GoogleMap mMap;
    private TripMapListener mListener;

    private Place startPlace;
    private Place endPlace;
    private PlacesClient placesClient;
    private String currentEncodedPolyline;
    private final List<GasStation> currentGasStations = new ArrayList<>();

    private Trip loadedTrip;
    private int currentRequestId = 0;
    private int selectionRequestId = 0;

    public interface TripMapListener {
        void onLogout();
        void onViewSavedTrips();
    }

    public static TripMapFragment newInstance(Trip trip) {
        TripMapFragment fragment = new TripMapFragment();
        Bundle args = new Bundle();
        if (trip != null) {
            args.putSerializable(ARG_TRIP, trip);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public TripMapFragment() {
        // Required empty public constructor
    }

    // Activity result launchers for Places Autocomplete (Start and Destination selection)
    private final ActivityResultLauncher<Intent> startAutocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleAutocompleteResult(result.getResultCode(), result.getData(), true));

    private final ActivityResultLauncher<Intent> endAutocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleAutocompleteResult(result.getResultCode(), result.getData(), false));

    /**
     * Handles the result returned from the Places Autocomplete widget intent.
     * Fetches detailed Place info (coordinates, formatted address, display name) using Places SDK.
     */
    private void handleAutocompleteResult(int resultCode, Intent data, boolean isStart) {
        if (resultCode == Activity.RESULT_CANCELED) return;
        if (data == null || resultCode != PlaceAutocompleteActivity.RESULT_OK) {
            if (isAdded()) Toast.makeText(requireContext(), "Location search failed", Toast.LENGTH_SHORT).show();
            return;
        }
        AutocompletePrediction prediction = PlaceAutocomplete.getPredictionFromIntent(data);
        AutocompleteSessionToken token = PlaceAutocomplete.getSessionTokenFromIntent(data);
        if (prediction == null || prediction.getPlaceId() == null) return;

        final int selectionId = ++selectionRequestId;
        currentRequestId++;
        loadedTrip = null;
        currentEncodedPolyline = null;
        currentGasStations.clear();
        binding.buttonSaveTrip.setEnabled(false);
        binding.buttonSaveTrip.setText(R.string.save_trip);
        binding.progressBar.setVisibility(View.VISIBLE);
        if (mMap != null) mMap.clear();

        List<Place.Field> fields = Arrays.asList(Place.Field.ID,
                Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION);
        FetchPlaceRequest.Builder builder = FetchPlaceRequest.builder(prediction.getPlaceId(), fields);
        if (token != null) builder.setSessionToken(token);

        placesClient.fetchPlace(builder.build())
                .addOnSuccessListener(response -> {
                    if (!isAdded() || binding == null || selectionId != selectionRequestId) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Place place = response.getPlace();
                    if (place.getLocation() == null) {
                        Toast.makeText(requireContext(), "Selected place has no coordinates", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String address = place.getFormattedAddress() != null
                            ? place.getFormattedAddress()
                            : (place.getDisplayName() != null ? place.getDisplayName() : "Selected location");
                    if (isStart) {
                        startPlace = place;
                        binding.textViewStartAddress.setText(address);
                    } else {
                        endPlace = place;
                        binding.textViewEndAddress.setText(address);
                    }
                    onLocationsUpdated();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null || selectionId != selectionRequestId) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Could not load location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof TripMapListener) {
            mListener = (TripMapListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement TripMapListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                loadedTrip = getArguments().getSerializable(ARG_TRIP, Trip.class);
            } else {
                loadedTrip = (Trip) getArguments().getSerializable(ARG_TRIP);
            }
        }
        // Initialize Places SDK with New Places API enabled
        String apiKey = getString(R.string.maps_api_key);
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(requireContext().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext().getApplicationContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTripMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Display user greeting in header from Firebase Auth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = currentUser.getEmail();
            }
            binding.textViewUserHeader.setText("Trip Planner (" + name + ")");
        }

        // Logout button click handler
        binding.buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            if (mListener != null) {
                mListener.onLogout();
            }
        });

        // Navigate to Saved Trips screen
        binding.buttonSavedTrips.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onViewSavedTrips();
            }
        });

        binding.buttonSelectStart.setOnClickListener(v -> openPlacesAutocomplete(startAutocompleteLauncher));
        binding.buttonSelectEnd.setOnClickListener(v -> openPlacesAutocomplete(endAutocompleteLauncher));

        binding.buttonClear.setOnClickListener(v -> clearTrip());

        binding.buttonSaveTrip.setOnClickListener(v -> saveCurrentTripToFirestore());

        // Obtain SupportMapFragment and notify when map is ready
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void openPlacesAutocomplete(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new PlaceAutocomplete.IntentBuilder().build(requireContext());
        launcher.launch(intent);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // If a saved trip was passed from SavedTripsFragment, render it on the map
        if (loadedTrip != null) {
            displayLoadedTrip(loadedTrip);
        }
    }

    private void onLocationsUpdated() {
        if (startPlace != null && startPlace.getLocation() != null &&
                endPlace != null && endPlace.getLocation() != null && mMap != null) {
            fetchRouteAndGasStations();
        }
    }

    /**
     * Calls RoutesAndPlacesClient.fetchRoute to get polyline directions between start and destination,
     * then decodes polyline, draws blue route, and triggers gas station searches at start, mid, and end.
     */
    private void fetchRouteAndGasStations() {
        if (startPlace == null || endPlace == null) return;
        LatLng startLatLng = startPlace.getLocation();
        LatLng endLatLng = endPlace.getLocation();
        if (startLatLng == null || endLatLng == null) return;

        currentRequestId++;
        final int requestId = currentRequestId;

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonSaveTrip.setEnabled(false);
        binding.buttonSaveTrip.setText(R.string.save_trip);

        if (mMap != null) {
            mMap.clear();
        }
        currentGasStations.clear();
        currentEncodedPolyline = null;

        String apiKey = getString(R.string.maps_api_key);

        RoutesAndPlacesClient.fetchRoute(requireContext(), startLatLng.latitude, startLatLng.longitude,
                endLatLng.latitude, endLatLng.longitude, apiKey, new RoutesAndPlacesClient.RouteCallback() {
                    @Override
                    public void onSuccess(String encodedPolyline) {
                        if (requestId != currentRequestId || !isAdded()) return;

                        currentEncodedPolyline = encodedPolyline;
                        if (mMap == null) return;

                        mMap.clear();
                        currentGasStations.clear();

                        // Decode polyline points and draw blue route on map
                        List<LatLng> points = PolyUtil.decode(encodedPolyline);
                        if (!points.isEmpty()) {
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(points)
                                    .color(Color.BLUE)
                                    .width(12f));
                        }

                        // Add Start (Green) and Destination (Red) markers
                        mMap.addMarker(new MarkerOptions()
                                .position(startLatLng)
                                .title("Start: " + startPlace.getDisplayName())
                                .snippet(startPlace.getFormattedAddress())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                        mMap.addMarker(new MarkerOptions()
                                .position(endLatLng)
                                .title("End: " + endPlace.getDisplayName())
                                .snippet(endPlace.getFormattedAddress())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                        // Adjust Camera bounds to include start, end, and all route points
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(startLatLng);
                        builder.include(endLatLng);
                        for (LatLng pt : points) {
                            builder.include(pt);
                        }
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));

                        // Calculate Route Midpoint
                        LatLng midLatLng;
                        if (!points.isEmpty()) {
                            midLatLng = points.get(points.size() / 2);
                        } else {
                            midLatLng = SphericalUtil.interpolate(startLatLng, endLatLng, 0.5);
                        }

                        // Fetch Gas Stations at Start, End, and Midpoint locations
                        fetchGasStationsForLocations(startLatLng, midLatLng, endLatLng, apiKey, requestId);
                    }

                    @Override
                    public void onError(String error) {
                        if (requestId != currentRequestId || !isAdded()) return;
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Fetches gas stations concurrently for Start, Mid, and End locations using Places API (New).
     */
    private void fetchGasStationsForLocations(LatLng start, LatLng mid, LatLng end, String apiKey, final int requestId) {
        final int[] completed = {0};
        final boolean[] failed = {false};

        RoutesAndPlacesClient.GasStationCallback callback = new RoutesAndPlacesClient.GasStationCallback() {
            @Override
            public void onSuccess(List<GasStation> stations) {
                if (requestId != currentRequestId || !isAdded()) return;

                currentGasStations.addAll(stations);
                for (GasStation gs : stations) {
                    if (mMap != null) {
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(gs.getLat(), gs.getLng()))
                                .title("[" + gs.getLocationType() + " Gas] " + gs.getName())
                                .snippet(gs.getAddress())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    }
                }

                completed[0]++;
                if (completed[0] == 3) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonSaveTrip.setEnabled(!failed[0]);
                    Toast.makeText(getContext(), failed[0] ? "Some gas station searches failed. Select a location again to retry." : "Route & Gas Stations Loaded!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                if (requestId != currentRequestId || !isAdded()) return;

                completed[0]++;
                failed[0] = true;
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();

                if (completed[0] == 3) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonSaveTrip.setEnabled(false);
                }
            }
        };

        RoutesAndPlacesClient.fetchGasStationsNearby(requireContext(), start, "Start", apiKey, callback);
        RoutesAndPlacesClient.fetchGasStationsNearby(requireContext(), mid, "Mid", apiKey, callback);
        RoutesAndPlacesClient.fetchGasStationsNearby(requireContext(), end, "End", apiKey, callback);
    }

    /**
     * Renders a previously saved trip loaded from Firebase Firestore onto the map.
     */
    private void displayLoadedTrip(Trip trip) {
        if (mMap == null || trip == null) return;
        mMap.clear();

        binding.textViewStartAddress.setText(trip.getStartAddress());
        binding.textViewEndAddress.setText(trip.getEndAddress());

        // Saved trips are read-only views, so disable save button
        binding.buttonSaveTrip.setEnabled(false);
        binding.buttonSaveTrip.setText(R.string.save_trip);

        LatLng startLatLng = new LatLng(trip.getStartLat(), trip.getStartLng());
        LatLng endLatLng = new LatLng(trip.getEndLat(), trip.getEndLng());

        // Add Start and Destination markers
        mMap.addMarker(new MarkerOptions()
                .position(startLatLng)
                .title("Start Location")
                .snippet(trip.getStartAddress())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        mMap.addMarker(new MarkerOptions()
                .position(endLatLng)
                .title("Destination")
                .snippet(trip.getEndAddress())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Decode and render polyline route
        if (trip.getEncodedPolyline() != null && !trip.getEncodedPolyline().isEmpty()) {
            List<LatLng> points = PolyUtil.decode(trip.getEncodedPolyline());
            mMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(12f));

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(startLatLng);
            builder.include(endLatLng);
            for (LatLng pt : points) {
                builder.include(pt);
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
        }

        // Render saved gas station markers
        if (trip.getGasStations() != null) {
            for (GasStation gs : trip.getGasStations()) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(gs.getLat(), gs.getLng()))
                        .title("[" + gs.getLocationType() + " Gas] " + gs.getName())
                        .snippet(gs.getAddress())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            }
        }
    }

    private void clearTrip() {
        currentRequestId++;
        selectionRequestId++;
        binding.progressBar.setVisibility(View.GONE);
        startPlace = null;
        endPlace = null;
        loadedTrip = null;
        currentEncodedPolyline = null;
        currentGasStations.clear();
        binding.textViewStartAddress.setText("No start location selected");
        binding.textViewEndAddress.setText("No destination selected");
        binding.buttonSaveTrip.setEnabled(false);
        binding.buttonSaveTrip.setText(R.string.save_trip);
        if (mMap != null) {
            mMap.clear();
        }
    }

    /**
     * Saves the current trip plan to Cloud Firestore under the "trips" collection,
     * associating it with the authenticated user's UID.
     */
    private void saveCurrentTripToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        Trip tripToSave;

        if (loadedTrip != null) {
            Toast.makeText(getContext(), "This trip is already saved", Toast.LENGTH_SHORT).show();
            return;
        } else if (startPlace != null && endPlace != null && startPlace.getLocation() != null && endPlace.getLocation() != null && currentEncodedPolyline != null) {
            String startName = startPlace.getDisplayName() != null ? startPlace.getDisplayName() : "Start";
            String endName = endPlace.getDisplayName() != null ? endPlace.getDisplayName() : "Destination";
            String title = "Trip: " + startName + " to " + endName;
            String startAddr = startPlace.getFormattedAddress() != null ? startPlace.getFormattedAddress() : startName;
            String endAddr = endPlace.getFormattedAddress() != null ? endPlace.getFormattedAddress() : endName;

            tripToSave = new Trip(
                    user.getUid(),
                    title,
                    startAddr,
                    startPlace.getLocation().latitude,
                    startPlace.getLocation().longitude,
                    endAddr,
                    endPlace.getLocation().latitude,
                    endPlace.getLocation().longitude,
                    currentEncodedPolyline,
                    new ArrayList<>(currentGasStations),
                    System.currentTimeMillis()
            );
        } else {
            Toast.makeText(getContext(), "Please select start and end locations first.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonSaveTrip.setEnabled(false);
        FirebaseFirestore.getInstance().collection("trips")
                .add(tripToSave)
                .addOnSuccessListener(documentReference -> {
                    if (binding == null || !isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Trip saved to Firestore!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null || !isAdded()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonSaveTrip.setEnabled(true);
                    Toast.makeText(getContext(), "Failed to save trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDestroyView() {
        currentRequestId++;
        selectionRequestId++;
        super.onDestroyView();
        binding = null;
    }
}
