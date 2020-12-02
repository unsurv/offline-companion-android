package org.unsurv.offline_companion_android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageUtils {

  // used for height and angle and type
  final static int UNKNOWN = -1;

  // openstreetmap camera types
  final static int FIXED_CAMERA = 0;
  final static int DOME_CAMERA = 1;
  final static int PANNING_CAMERA = 2;

  // strings in order of types for easy access
  final static List<String> typeList = new ArrayList<>(Arrays.asList("fixed", "dome", "panning"));

  // osm "surveillance" tag
  final static int AREA_UNKNOWN = 0;
  final static int AREA_OUTDOOR = 1;
  final static int AREA_PUBLIC = 2;
  final static int AREA_INDOOR = 3;
  final static int AREA_TRAFFIC = 4;

  final static List<String> areaList = new ArrayList<>(Arrays.asList(
          "unknown",
          "outdoor",
          "public",
          "indoor",
          "traffic"));


  // osm mount types
  final static int MOUNT_UNKNOWN = 0;
  final static int MOUNT_POLE = 1;
  final static int MOUNT_WALL = 2;
  final static int MOUNT_CEILING = 3;
  final static int MOUNT_STREET_LAMP = 4;

  final static List<String> mountList = new ArrayList<>(Arrays.asList(
          "unknown",
          "pole",
          "wall",
          "ceiling",
          "street_lamp"));


}
