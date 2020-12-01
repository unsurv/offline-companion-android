package com.example.offline_companion_android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

  // private List<OverlayItem> cameraItemsToDisplay = new ArrayList<>();
  // private List<OverlayItem> locationItemsToDisplay = new ArrayList<>();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ctx = this;

    cameraRepository = new SurveillanceCameraRepository(getApplication());


    List<SurveillanceCamera> allCameras = cameraRepository.getAllCameras();

    debugText = findViewById(R.id.debugTextView);

    nfcAdapter = NfcAdapter.getDefaultAdapter(ctx);

    Intent startntent =  getIntent();

    // Intent that launched activity is from discovering a NFC tag.
    if (startntent.getAction().equals(ACTION_NDEF_DISCOVERED)) {

      Tag tag = startntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      new NdefReaderTask().execute(tag);
    }

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

    // Setting starting position and zoom level. Use center of homezone for now
    GeoPoint startPoint = new GeoPoint(50.0035,8.2743);
    mapController.setZoom(11.8);
    mapController.setCenter(startPoint);

    try {

      testCameras = new JSONArray("[{\"id\":113244567788,\"location\":{\"lat\":49.9958,\"lon\":8.28196}},{\"id\":1132445677223,\"location\":{\"lat\":49.99557,\"lon\":8.28153}}]\n");

      populateMapFromNfc(new JSONArray("[{\"loc\":{\"lat\":50.022714,\"lon\":8.2239192,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[113244567788]},{\"loc\":{\"lat\":50.022724,\"lon\":8.2239392,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[1132445677223]}]"));


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

    ArrayList<OverlayItem> cameraItems = new ArrayList<OverlayItem>();

    ArrayList<OverlayItem> deviceLocationItems = new ArrayList<OverlayItem>();

    try {


      for (int i = 0; i < nfcJSON.length(); i++) {

        JSONObject contact = (JSONObject) nfcJSON.get(i);

        JSONObject deviceLocation = (JSONObject) contact.getJSONObject("loc");

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
          for (int j = 0; j < testCameras.length(); j++) {
            JSONObject camera = (JSONObject) testCameras.get(j);
            JSONObject cameraLocation = (JSONObject) camera.getJSONObject("location");

            if (contactId == camera.getLong("id") ) {

              cameraItems.add(
                      new OverlayItem(
                              String.valueOf(contactId),
                              "surveillance camera",
                              "You were in range of this camera on XX:XX XX.XX.XXXX",
                              new GeoPoint(cameraLocation.getDouble("lat"),
                                      cameraLocation.getDouble("lon"))
                      )
              );


            }
          }
        }

        
      }





    } catch (JSONException jsonException) {

      Log.i(TAG, jsonException.toString());

    }
    Drawable cameraMarkerIcon = ContextCompat.getDrawable(ctx, R.drawable.simple_marker_5dpi);

    // Drawable deviceLocationMarker = getResources().getDrawable(R.drawable.ic_baseline_location_on_24_green);
    Drawable deviceLocationMarker = ContextCompat.getDrawable(ctx, R.drawable.simple_green_5dpi);

    cameraOverlay = new ItemizedIconOverlay<>(cameraItems, cameraMarkerIcon, null, ctx);

    deviceLocationOverlay = new ItemizedIconOverlay<>(deviceLocationItems, deviceLocationMarker,  null, ctx);

    mapView.getOverlays().remove(deviceLocationOverlay);
    mapView.getOverlays().remove(cameraOverlay);

    mapView.getOverlays().add(deviceLocationOverlay);
    mapView.getOverlays().add(cameraOverlay);

    mapView.invalidate();


  }

}