package org.unsurv.offline_companion_android;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.Nullable;

import org.osmdroid.util.GeoPoint;


/**
 * Object representing a local camera capture by the user. Either manual, via object detection or
 * a training capture
 */

@Entity(tableName = "local_surveillance_cameras")
public class SurveillanceCamera {

  @PrimaryKey(autoGenerate = true)
  private int id;


  // openstreetmap tags
  private int cameraType; // fixed, dome, panning

  private int area; // unknown, outdoor, public, indoor, traffic
  private int mount; // unknown, pole, wall, ceiling, streetlamp
  private int direction; // -1 = unknown, 0 - 360 degrees
  private int height; // -1 = unknown,  0 - 20 m default 5 m
  private int angle; // -1 = unknown,  15 to 90 Degree from horizontal


  @Nullable
  private String imagePath;

  @Nullable
  private long externalId;

  private double latitude;
  private double longitude;

  private String comment;

  private String timestamp; // can be disabled in settings

  private boolean isShown;

  public SurveillanceCamera(int cameraType,
                            int area,
                            int direction,
                            int mount,
                            int height,
                            int angle,
                            @Nullable String imagePath,
                            long externalId,
                            double latitude,
                            double longitude,
                            @Nullable String comment,
                            @Nullable String timestamp){

    this.cameraType = cameraType;
    this.area = area;
    this.mount = mount;
    this.direction = direction;
    this.height = height;
    this.angle = angle;

    this.imagePath = imagePath;

    this.externalId = externalId;

    this.latitude = latitude;
    this.longitude = longitude;

    this.comment = comment;

    this.timestamp = timestamp;

    this.isShown = false;

  }



  public int getId() {
    return id;
  }

  public int getCameraType(){return cameraType;}

  public int getArea() {
    return area;
  }

  public int getMount() {
    return mount;
  }

  public int getDirection() {
    return direction;
  }

  public int getHeight() {
    return height;
  }

  public int getAngle() {
    return angle;
  }

  @Nullable
  public String getImagePath() {
    return imagePath;
  }

  public long getExternalId() {
    return externalId;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }


  public String getComment() {
    return comment;
  }

  public String getTimestamp() { return timestamp; }

  public boolean isShown() {
    return isShown;
  }

  public GeoPoint getPositionAsGeoPoint() {
    return  new GeoPoint(latitude, longitude);
  }


  public void setId(int id) {
    this.id = id;
  }

  public void setCameraType(int type){this.cameraType = type;}

  public void setArea(int area) {
    this.area = area;
  }

  public void setDirection(int direction) {
    this.direction = direction;
  }

  public void setMount(int mount) {
    this.mount = mount;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public void setAngle(int angle) {
    this.angle = angle;
  }



  public void setImagePath(String ImagePath) {
    this.imagePath = ImagePath;
  }

  public void setExternalId(@Nullable Long externalId) {
    this.externalId = externalId;
  }

  public void setLatitude(double Latitude) {
    this.latitude = Latitude;
  }

  public void setLongitude(double Longitude) {
    this.longitude = Longitude;
  }

  public void setComment(String Comment) {
    this.comment = Comment;
  }

  public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

  public void setShown(boolean shown) {
    isShown = shown;
  }

  //TODO add delete function to delete all Files connected to specific camera

}
