package com.example.offline_companion_android;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
@Dao
public interface SurveillanceCameraDao {

  @Query("DELETE FROM local_surveillance_cameras")
  void deleteAll();

  @Insert
  void insert(SurveillanceCamera surveillanceCamera);

  @Query("SELECT * FROM local_surveillance_cameras")
  List<SurveillanceCamera> getAllCameras();


  @Update
  void updateArea(SurveillanceCamera surveillanceCamera);

  @Query("SELECT * FROM local_surveillance_cameras " +
          "WHERE latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lonMin AND :lonMax")
  List<SurveillanceCamera> getCamerasInArea(double latMin, double latMax, double lonMin, double lonMax);

  @Query("SELECT * FROM local_surveillance_cameras WHERE :osmId = externalId")
  SurveillanceCamera findById(long osmId);
}
