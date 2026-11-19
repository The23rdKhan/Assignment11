/**
 * Assignment 11
 * File Name: RoutesAndPlacesClient.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoutesAndPlacesClient {
    private static final String TAG = "RoutesAndPlacesClient";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RouteCallback {
        void onSuccess(String encodedPolyline);
        void onError(String error);
    }

    public interface GasStationCallback {
        void onSuccess(List<GasStation> gasStations);
        void onError(String error);
    }

    /**
     * Dynamically retrieve the uppercase SHA-1 fingerprint of the app's signing certificate.
     */
    public static String getSigningSha1(Context context) {
        if (context == null) return "";
        try {
            PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo.signingInfo != null) {
                    Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();
                    if (signatures != null && signatures.length > 0) {
                        return getSha1FromSignature(signatures[0]);
                    }
                }
            } else {
                packageInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNATURES);
                if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                    return getSha1FromSignature(packageInfo.signatures[0]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error obtaining signing SHA-1", e);
        }
        return "";
    }

    private static String getSha1FromSignature(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(signature.toByteArray());
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                String hex = Integer.toHexString(0xFF & digest[i]).toUpperCase(Locale.US);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Compute Route via Routes API v2
     */
    public static void fetchRoute(Context context, double startLat, double startLng, double endLat, double endLng, String apiKey, RouteCallback callback) {
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";
        String packageName = context != null ? context.getPackageName() : "edu.charlotte.assignment11";
        String certSha1 = getSigningSha1(context);

        JsonObject jsonBody = new JsonObject();

        JsonObject origin = new JsonObject();
        JsonObject originLatLng = new JsonObject();
        JsonObject originLocation = new JsonObject();
        originLatLng.addProperty("latitude", startLat);
        originLatLng.addProperty("longitude", startLng);
        originLocation.add("latLng", originLatLng);
        origin.add("location", originLocation);

        JsonObject destination = new JsonObject();
        JsonObject destLatLng = new JsonObject();
        JsonObject destLocation = new JsonObject();
        destLatLng.addProperty("latitude", endLat);
        destLatLng.addProperty("longitude", endLng);
        destLocation.add("latLng", destLatLng);
        destination.add("location", destLocation);

        jsonBody.add("origin", origin);
        jsonBody.add("destination", destination);
        jsonBody.addProperty("travelMode", "DRIVE");

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Android-Package", packageName)
                .addHeader("X-Goog-FieldMask", "routes.polyline.encodedPolyline")
                .post(body);

        if (!certSha1.isEmpty()) {
            requestBuilder.addHeader("X-Android-Cert", certSha1);
        }

        Request request = requestBuilder.build();

        Log.d(TAG, "Routes API v2 Request -> Package: " + packageName + " | Cert: " + (certSha1.isEmpty() ? "N/A" : "PRESENT"));

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Routes API v2 Network Failure: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Network error fetching route: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Routes API v2 HTTP Status: " + response.code());

                if (!response.isSuccessful()) {
                    String errorMsg = parseErrorMessage(responseData, "Routes API Permission/HTTP Error " + response.code());
                    mainHandler.post(() -> callback.onError(errorMsg));
                    return;
                }

                try {
                    JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                    if (json.has("routes") && !json.getAsJsonArray("routes").isEmpty()) {
                        JsonObject firstRoute = json.getAsJsonArray("routes").get(0).getAsJsonObject();
                        if (firstRoute.has("polyline") && firstRoute.getAsJsonObject("polyline").has("encodedPolyline")) {
                            String encodedPolyline = firstRoute.getAsJsonObject("polyline").get("encodedPolyline").getAsString();
                            mainHandler.post(() -> callback.onSuccess(encodedPolyline));
                            return;
                        }
                    }
                    mainHandler.post(() -> callback.onError("No routes found between selected locations."));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Error parsing Routes API response: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Fetch Gas Stations near a given LatLng using Places API (New)
     */
    public static void fetchGasStationsNearby(Context context, LatLng location, String locationType, String apiKey, GasStationCallback callback) {
        String url = "https://places.googleapis.com/v1/places:searchNearby";
        String packageName = context != null ? context.getPackageName() : "edu.charlotte.assignment11";
        String certSha1 = getSigningSha1(context);

        JsonObject jsonBody = new JsonObject();
        JsonArray types = new JsonArray();
        types.add("gas_station");
        jsonBody.add("includedTypes", types);
        jsonBody.addProperty("maxResultCount", 3);

        JsonObject circle = new JsonObject();
        JsonObject center = new JsonObject();
        center.addProperty("latitude", location.latitude);
        center.addProperty("longitude", location.longitude);
        circle.add("center", center);
        circle.addProperty("radius", 5000.0);

        JsonObject locationRestriction = new JsonObject();
        locationRestriction.add("circle", circle);
        jsonBody.add("locationRestriction", locationRestriction);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Android-Package", packageName)
                .addHeader("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.location")
                .post(body);

        if (!certSha1.isEmpty()) {
            requestBuilder.addHeader("X-Android-Cert", certSha1);
        }

        Request request = requestBuilder.build();

        Log.d(TAG, "Places API (New) Request -> Package: " + packageName + " | Cert: " + (certSha1.isEmpty() ? "N/A" : "PRESENT"));

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Places API (New) Network Failure: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Network error fetching gas stations: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Places API (New) HTTP Status [" + locationType + "]: " + response.code());

                if (!response.isSuccessful()) {
                    String errorMsg = parseErrorMessage(responseData, "Places API Permission/HTTP Error " + response.code());
                    mainHandler.post(() -> callback.onError(errorMsg));
                    return;
                }

                try {
                    JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                    List<GasStation> list = new ArrayList<>();
                    if (json.has("places")) {
                        JsonArray places = json.getAsJsonArray("places");
                        int limit = Math.min(3, places.size());
                        for (int i = 0; i < limit; i++) {
                            JsonObject item = places.get(i).getAsJsonObject();
                            String name = "Gas Station";
                            if (item.has("displayName") && item.getAsJsonObject("displayName").has("text")) {
                                name = item.getAsJsonObject("displayName").get("text").getAsString();
                            }
                            String address = item.has("formattedAddress") ? item.get("formattedAddress").getAsString() : "";
                            if (item.has("location")) {
                                JsonObject loc = item.getAsJsonObject("location");
                                double lat = loc.get("latitude").getAsDouble();
                                double lng = loc.get("longitude").getAsDouble();
                                list.add(new GasStation(name, address, lat, lng, locationType));
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Error parsing Places API response: " + e.getMessage()));
                }
            }
        });
    }

    private static String parseErrorMessage(String responseData, String defaultMsg) {
        if (responseData == null || responseData.isEmpty()) {
            return defaultMsg;
        }
        try {
            JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
            if (json.has("error")) {
                JsonObject errObj = json.getAsJsonObject("error");
                String message = errObj.has("message") ? errObj.get("message").getAsString() : "";
                String status = errObj.has("status") ? errObj.get("status").getAsString() : "";
                if (!message.isEmpty()) {
                    return defaultMsg + ": [" + status + "] " + message;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultMsg + ": " + responseData;
    }
}