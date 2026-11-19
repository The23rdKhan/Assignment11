/**
 * Assignment 11
 * File Name: Trip.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Trip implements Serializable {
    private String docId;
    private String userId;
    private String tripTitle;
    private String startAddress;
    private double startLat;
    private double startLng;
    private String endAddress;
    private double endLat;
    private double endLng;
    private String encodedPolyline;
    private List<GasStation> gasStations = new ArrayList<>();
    private long createdAt;

    public Trip() {
        // Required for Firestore
    }

    public Trip(String userId, String tripTitle, String startAddress, double startLat, double startLng,
                String endAddress, double endLat, double endLng, String encodedPolyline,
                List<GasStation> gasStations, long createdAt) {
        this.userId = userId;
        this.tripTitle = tripTitle;
        this.startAddress = startAddress;
        this.startLat = startLat;
        this.startLng = startLng;
        this.endAddress = endAddress;
        this.endLat = endLat;
        this.endLng = endLng;
        this.encodedPolyline = encodedPolyline;
        this.gasStations = gasStations;
        this.createdAt = createdAt;
    }

    @Exclude
    public String getDocId() {
        return docId;
    }

    @Exclude
    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTripTitle() {
        return tripTitle;
    }

    public void setTripTitle(String tripTitle) {
        this.tripTitle = tripTitle;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public double getStartLat() {
        return startLat;
    }

    public void setStartLat(double startLat) {
        this.startLat = startLat;
    }

    public double getStartLng() {
        return startLng;
    }

    public void setStartLng(double startLng) {
        this.startLng = startLng;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

    public double getEndLat() {
        return endLat;
    }

    public void setEndLat(double endLat) {
        this.endLat = endLat;
    }

    public double getEndLng() {
        return endLng;
    }

    public void setEndLng(double endLng) {
        this.endLng = endLng;
    }

    public String getEncodedPolyline() {
        return encodedPolyline;
    }

    public void setEncodedPolyline(String encodedPolyline) {
        this.encodedPolyline = encodedPolyline;
    }

    public List<GasStation> getGasStations() {
        return gasStations;
    }

    public void setGasStations(List<GasStation> gasStations) {
        this.gasStations = gasStations;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}