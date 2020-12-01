package com.example.offline_companion_android;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class SurveillanceContact {

  private GeoPoint deviceLocation;

  private List<SurveillanceCamera> camerasInRange;

  public SurveillanceContact(GeoPoint deviceLocation,
                             List<SurveillanceCamera> cameras){
    this.deviceLocation = deviceLocation;
    this.camerasInRange = cameras;
  }

  public GeoPoint getDeviceLocation() {
    return deviceLocation;
  }

  public List<SurveillanceCamera> getCamerasInRange() {
    return camerasInRange;
  }

  public void setDeviceLocation(GeoPoint deviceLocation) {
    this.deviceLocation = deviceLocation;
  }

  public void setCamerasInRange(List<SurveillanceCamera> camerasInRange) {
    this.camerasInRange = camerasInRange;
  }
}
