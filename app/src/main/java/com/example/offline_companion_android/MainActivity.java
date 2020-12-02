package com.example.offline_companion_android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED;


/**
 * modified from Ralf Wondratschek  NFC tutorial
 *
 */

public class MainActivity extends AppCompatActivity {

  public static final String TAG = "mainActivity";

  private Context ctx;
  private TextView debugText;
  private NfcAdapter nfcAdapter;

  private MapView mapView;

  private JSONObject nfcData;
  private JSONArray testCameras;

  private ItemizedOverlay<OverlayItem> cameraOverlay;
  private ItemizedOverlay<OverlayItem> deviceLocationOverlay;

  private SurveillanceCameraRepository cameraRepository;

  private List<SurveillanceCamera> camerasOnMap;
  private List<SurveillanceContact> surveillanceContacts;

  private List<Polygon> polygonsOnMap;
  private List<Polyline> polylinesOnMap;

  private ImageButton infoButton;

  // private List<OverlayItem> cameraItemsToDisplay = new ArrayList<>();
  // private List<OverlayItem> locationItemsToDisplay = new ArrayList<>();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ctx = this;

    cameraRepository = new SurveillanceCameraRepository(getApplication());

    debugText = findViewById(R.id.debugTextView);

    nfcAdapter = NfcAdapter.getDefaultAdapter(ctx);

    Intent startntent =  getIntent();

    // Intent that launched activity is from discovering a NFC tag.
    if (startntent.getAction().equals(ACTION_NDEF_DISCOVERED)) {

      Tag tag = startntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      new NdefReaderTask().execute(tag);
    }

    camerasOnMap = new ArrayList<>();
    surveillanceContacts = new ArrayList<>();
    polylinesOnMap = new ArrayList<>();
    polygonsOnMap = new ArrayList<>();

    mapView = findViewById(R.id.map);
    CopyrightOverlay copyrightOverlay = new CopyrightOverlay(ctx);
    mapView.getOverlays().add(copyrightOverlay);

    Configuration.getInstance().setUserAgentValue("github-unsurv-oflline-companion");
    mapView.setTileSource(TileSourceFactory.MAPNIK);

    mapView.setTilesScaledToDpi(true);
    mapView.setClickable(true);

    //enable pinch to zoom
    mapView.setMultiTouchControls(true);

    final IMapController mapController = mapView.getController();

    // remove big + and - buttons at the bottom of the map
    final CustomZoomButtonsController zoomController = mapView.getZoomController();
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER);

    // Setting starting position and zoom level.
    GeoPoint startPoint = new GeoPoint(50.0035,8.2743);
    mapController.setZoom(11.8);
    mapController.setCenter(startPoint);

    // info button
    infoButton = findViewById(R.id.map_info_button);

    try {

      // testCameras = new JSONArray("[{\"id\":113244567788,\"location\":{\"lat\":49.9958,\"lon\":8.28196}},{\"id\":113244567789,\"location\":{\"lat\":49.99557,\"lon\":8.28153}}]\n");

      populateMapFromNfc(new JSONArray("[{\"loc\":{\"lat\":49.99631,\"lon\":8.28206,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[113244567788]},{\"loc\":{\"lat\":49.99584,\"lon\":8.28214,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[113244567789]},{\"loc\":{\"lat\":49.99611,\"lon\":8.28208,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[223244567788]},{\"loc\":{\"lat\":49.99581,\"lon\":8.28222,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[223244567789]}]"));

    } catch (JSONException jsonException) {

      Log.i(TAG, jsonException.toString());
    }


  }

  @Override
  protected void onResume() {
    super.onResume();


  }


  private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

    @Override
    protected String doInBackground(Tag... params) {
      Tag tag = params[0];

      Ndef ndef = Ndef.get(tag);
      if (ndef == null) {
        // NDEF is not supported by this Tag.
        return null;
      }

      NdefMessage ndefMessage = ndef.getCachedNdefMessage();

      NdefRecord[] records = ndefMessage.getRecords();
      for (NdefRecord ndefRecord : records) {
        if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
          try {
            return readText(ndefRecord);
          } catch (UnsupportedEncodingException e) {
            Log.i(TAG, "Unsupported Encoding", e);
          }
        }
      }

      return null;
    }

    private String readText(NdefRecord record) throws UnsupportedEncodingException {
      /*
       * See NFC forum specification for "Text Record Type Definition" at 3.2.1
       *
       * http://www.nfc-forum.org/specs/
       *
       * bit_7 defines encoding
       * bit_6 reserved for future use, must be 0
       * bit_5..0 length of IANA language code
       */

      byte[] payload = record.getPayload();

      // Get the Text Encoding
      String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

      // Get the Language Code
      int languageCodeLength = payload[0] & 0063;

      // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
      // e.g. "en"

      // Get the Text
      return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    @Override
    protected void onPostExecute(String result) {
      if (result != null) {
        debugText.setText(result);
        try {
          nfcData = new JSONObject(result);
          debugText.setText(result);



        } catch (JSONException jsonException) {
          Log.i(TAG, jsonException.toString());
          debugText.setText("EROOR CONVERTING TO JSON");
        }
      }
    }
  }



  void populateMapFromNfc(JSONArray nfcJSON) {

    camerasOnMap.clear();

    ArrayList<OverlayItem> cameraItems = new ArrayList<OverlayItem>();

    ArrayList<OverlayItem> deviceLocationItems = new ArrayList<OverlayItem>();

    try {


      for (int i = 0; i < nfcJSON.length(); i++) {

        JSONObject contact = (JSONObject) nfcJSON.get(i);

        JSONObject deviceLocation = (JSONObject) contact.getJSONObject("loc");

        SurveillanceContact surveillanceContact = new SurveillanceContact(
                new GeoPoint(deviceLocation.getDouble("lat"),
                        deviceLocation.getDouble("lon"))
                , new ArrayList<SurveillanceCamera>());

        deviceLocationItems.add(
                new OverlayItem(
                        "your location at time of contact",
                        "",
                        "Accuracy (SIV): " + deviceLocation.getString("SIV"),
                        new GeoPoint(deviceLocation.getDouble("lat"), deviceLocation.getDouble("lon"))
                )
        );


        JSONArray ids = (JSONArray) contact.get("ids");

        // fill camera itemOverlay here

        for (int k = 0; k < ids.length(); k++) {

          long contactId =  (long) ids.get(k);

          // match id from nfc with data from local db

          SurveillanceCamera camera = cameraRepository.findById(contactId);

          surveillanceContact.addCamera(camera);

          camerasOnMap.add(camera);

          if (camera != null) {
            cameraItems.add(
                    new OverlayItem(
                            String.valueOf(contactId),
                            "surveillance camera",
                            "You were in range of this camera on XX:XX XX.XX.XXXX",
                            new GeoPoint(camera.getLatitude(), camera.getLongitude())
                    )
            );

          }

        }


        surveillanceContacts.add(surveillanceContact);
      }


    } catch (JSONException jsonException) {

      Log.i(TAG, jsonException.toString());

    }

    Drawable cameraMarkerIcon = ContextCompat.getDrawable(ctx, R.drawable.simple_marker_5dpi);

    // Drawable deviceLocationMarker = getResources().getDrawable(R.drawable.ic_baseline_location_on_24_green);
    Drawable deviceLocationMarker = ContextCompat.getDrawable(ctx, R.drawable.simple_green_5dpi);

    cameraOverlay = new ItemizedIconOverlay<>(cameraItems, cameraMarkerIcon, null, ctx);

    deviceLocationOverlay = new ItemizedIconOverlay<>(deviceLocationItems, deviceLocationMarker,  null, ctx);

    for (SurveillanceCamera cam: camerasOnMap) {

      drawCameraArea(new GeoPoint(cam.getLatitude(), cam.getLongitude()),
              cam.getDirection(),
              cam.getHeight(),
              cam.getAngle(),
              cam.getCameraType());

    }

    drawConnectionLines(surveillanceContacts);


    mapView.getOverlays().remove(deviceLocationOverlay);
    mapView.getOverlays().remove(cameraOverlay);

    mapView.getOverlays().add(deviceLocationOverlay);
    mapView.getOverlays().add(cameraOverlay);

    mapView.invalidate();


  }

  void drawConnectionLines(List<SurveillanceContact> contacts) {


    for (SurveillanceContact contact: contacts) {
      Polyline line = new Polyline();

      int hotPink = Color.argb(127, 255, 0, 255);
      line.setColor(hotPink);

      for (SurveillanceCamera camera: contact.getAllCameras()) {
        line.setPoints(Arrays.asList(contact.getDeviceLocation(), new GeoPoint(camera.getLatitude(), camera.getLongitude())));
      }
      polylinesOnMap.add(line);
    }

    mapView.getOverlayManager().addAll(polylinesOnMap);
  }

  void drawCameraArea(GeoPoint currentPos, int direction, int height, int horizontalAngle, int cameraType) {

    if (cameraType == StorageUtils.UNKNOWN) {
      return;
    }

    Polygon polygon = new Polygon();

    int hotPink = Color.argb(127, 255, 0, 255);
    polygon.setFillColor(hotPink);
    polygon.setStrokeColor(hotPink);



    int baseViewDistance = 15; // in m

    // if height entered by user
    if (height >= 0) {
      // TODO use formula from surveillance under surveillance https://sunders.uber.space
      // add 30% viewdistance per meter of height

      double heightFactor = 1 + (0.3 * height);
      baseViewDistance *= heightFactor;
    }

    if (horizontalAngle != -1) {
      // TODO use formula from surveillance under surveillance https://sunders.uber.space

      // about the same as SurveillanceUnderSurveillance https://sunders.uber.space
      double angleFactor = Math.pow(25f / horizontalAngle, 2) * 0.4;
      baseViewDistance *= angleFactor;
    }

    // remove old drawings
    mapView.getOverlayManager().remove(polygon);

    List<GeoPoint> geoPoints;

    if (cameraType == StorageUtils.FIXED_CAMERA || cameraType == StorageUtils.PANNING_CAMERA) {

      int viewAngle;

      // calculate geopoints for triangle

      double startLat = currentPos.getLatitude();
      double startLon = currentPos.getLongitude();

      geoPoints = new ArrayList<>();


      if (cameraType == StorageUtils.FIXED_CAMERA) {
        viewAngle = 60; // fixed camera

        // triangle sides compass direction
        int direction1 = direction - viewAngle / 2;
        int direction2 = direction + viewAngle / 2;

        // in meters, simulate a 2d coordinate system, known values are: hyp length, and inside angles
        double xDiff1 = Math.cos(Math.toRadians(90 - direction1)) * baseViewDistance;
        double yDiff1 = Math.sin(Math.toRadians(90 - direction1)) * baseViewDistance;

        double xDiff2 = Math.cos(Math.toRadians(90 - direction2)) * baseViewDistance;
        double yDiff2 = Math.sin(Math.toRadians(90 - direction2)) * baseViewDistance;


        Location endpoint1 = LocationUtils.getNewLocation(startLat, startLon, yDiff1, xDiff1);
        Location endpoint2 = LocationUtils.getNewLocation(startLat, startLon, yDiff2, xDiff2);


        geoPoints.add(new GeoPoint(startLat, startLon));
        geoPoints.add(new GeoPoint(endpoint1.getLatitude(), endpoint1.getLongitude()));
        geoPoints.add(new GeoPoint(endpoint2.getLatitude(), endpoint2.getLongitude()));


      } else {
        viewAngle = 120; // panning camera

        // using two 60 degree cones instead of one 120 cone

        // triangle sides compass direction
        int direction1 = direction - viewAngle / 2;
        int direction2 = direction;
        int direction3 = direction + viewAngle / 2;


        // in meters, simulate a 2d coordinate system, known values are: hyp length, and inside angles
        double xDiff1 = Math.cos(Math.toRadians(90 - direction1)) * baseViewDistance;
        double yDiff1 = Math.sin(Math.toRadians(90 - direction1)) * baseViewDistance;

        double xDiff2 = Math.cos(Math.toRadians(90 - direction2)) * baseViewDistance;
        double yDiff2 = Math.sin(Math.toRadians(90 - direction2)) * baseViewDistance;

        double xDiff3 = Math.cos(Math.toRadians(90 - direction3)) * baseViewDistance;
        double yDiff3 = Math.sin(Math.toRadians(90 - direction3)) * baseViewDistance;


        Location endpoint1 = LocationUtils.getNewLocation(startLat, startLon, yDiff1, xDiff1);
        Location endpoint2 = LocationUtils.getNewLocation(startLat, startLon, yDiff2, xDiff2);
        Location endpoint3 = LocationUtils.getNewLocation(startLat, startLon, yDiff3, xDiff3);

        geoPoints.add(new GeoPoint(startLat, startLon));
        geoPoints.add(new GeoPoint(endpoint1.getLatitude(), endpoint1.getLongitude()));
        geoPoints.add(new GeoPoint(endpoint2.getLatitude(), endpoint2.getLongitude()));
        geoPoints.add(new GeoPoint(endpoint3.getLatitude(), endpoint3.getLongitude()));


      }

    } else {

      // circle for dome cameras
      geoPoints = Polygon.pointsAsCircle(currentPos, height * 7);

    }

    polygon.setPoints(geoPoints);
    mapView.getOverlayManager().add(polygon);
    mapView.invalidate();

  }

}