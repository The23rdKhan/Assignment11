/**
 * Assignment 11
 * File Name: GasStation.java
 * Full Name of Student: Lucnel Nordelus
 */
package edu.charlotte.assignment11;

import java.io.Serializable;

public class GasStation implements Serializable {
    private String name;
    private String address;
    private double lat;
    private double lng;
    private String locationType; // "Start", "End", "Mid"

    public GasStation() {
        // Required for Firestore
    }

    public GasStation(String name, String address, double lat, double lng, String locationType) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.locationType = locationType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
}