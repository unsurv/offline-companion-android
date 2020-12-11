package org.unsurv.offline_companion_android;

import androidx.annotation.Nullable;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class SurveillanceContact {

  private GeoPoint deviceLocation;

  private List<SurveillanceCamera> camerasInRange;

  private boolean isShown;

  public SurveillanceContact(GeoPoint deviceLocation,
                             @Nullable  List<SurveillanceCamera> cameras){
    this.deviceLocation = deviceLocation;
    this.camerasInRange = cameras;
    this.isShown = false;
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

  public boolean isShown() {
    return isShown;
  }

  public void setDeviceLocation(GeoPoint deviceLocation) {
    this.deviceLocation = deviceLocation;
  }

  public void setCamerasInRange(List<SurveillanceCamera> camerasInRange) {
    this.camerasInRange = camerasInRange;
  }

  public void setShown(boolean shown) {
    isShown = shown;
  }
}
