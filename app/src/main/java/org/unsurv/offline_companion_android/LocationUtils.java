package org.unsurv.offline_companion_android;

import android.location.Location;

class LocationUtils {

  /**
   * compute a new Location with a latitude / longitude given + distance in meters north / east
   * @param latitude latitude
   * @param longitude longitude
   * @param metersNorth distance north from latitude in meters, can be negative
   * @param metersEast distance east from longitude in meters, can be negative
   * @return Location object with the newly computed location
   */
  static Location getNewLocation(double latitude, double longitude, double metersNorth, double metersEast) {

    double latDiff = metersNorth / 110574;
    double lonDiff = metersEast / longitudeDegreeToMetersRatio(latitude);

    Location updatedLocation = new Location("locationFromLocationAndMeters");

    updatedLocation.setLatitude(latitude + latDiff);
    updatedLocation.setLongitude(longitude + lonDiff);

    return updatedLocation;

  }

  /**
   * Computes distance in meters from degree difference in longitude
   * @param latitude factor is dependant on current latitude
   * @return factor meters per degree difference at given latitude
   */
  private static double longitudeDegreeToMetersRatio(double latitude) {
    // in cameraArray
    int earthRadius = 6371000;

    return Math.PI/180*earthRadius*Math.cos(Math.toRadians(latitude));
  }

  /**
   * @return static factor of meters per degree difference in latitude
   */
  private static int latitudeDegreeToMetersRatio() {
    // Changes a small amount because earth is not a perfect sphere. Disregarded here
    return 110574;
  }


}
