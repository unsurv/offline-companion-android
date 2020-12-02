package com.example.offline_companion_android;

import androidx.annotation.Nullable;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class SurveillanceContact {

  private GeoPoint deviceLocation;

  private List<SurveillanceCamera> camerasInRange;

  public SurveillanceContact(GeoPoint deviceLocation,
                             @Nullable  List<SurveillanceCamera> cameras){
    this.deviceLocation = deviceLocation;
    this.camerasInRange = cameras;
  }


  public List<SurveillanceCamera> getAllCameras() {
    return camerasInRange;
  }

  public void addCamera(SurveillanceCamera camera) {
    this.camerasInRange.add(camera);
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
