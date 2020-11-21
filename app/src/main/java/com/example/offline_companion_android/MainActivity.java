package com.example.offline_companion_android;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
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

  // private List<OverlayItem> cameraItemsToDisplay = new ArrayList<>();
  // private List<OverlayItem> locationItemsToDisplay = new ArrayList<>();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ctx = this;


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

    mapView.setTileSource(TileSourceFactory.OpenTopo);

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

      testCameras = new JSONArray("[{\"id\":1475,\"location\":{\"lat\":49.9958,\"lon\":8.28196}},{\"id\":2298,\"location\":{\"lat\":49.99557,\"lon\":8.28153}}]\n");

      populateMapFromNfc(new JSONObject("{\"battery\":109,\"time\":\"2020-11-21 15:29:8\",\"location\":{\"lat\":49.99592,\"lon\":8.28185,\"alt\":123890,\"SIV\":4,\"time\":\"2020-11-21 15:29:9\"},\"contacts\":[{\"id\":1475,\"distance\":74.867378234863281},{\"id\":2298,\"distance\":99.140815734863281}]}\n"));




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



  void populateMapFromNfc(JSONObject nfcJSON) {

    ArrayList<OverlayItem> cameraItems = new ArrayList<OverlayItem>();

    ArrayList<OverlayItem> deviceLocationItems = new ArrayList<OverlayItem>();

    try {

      JSONArray contacts = (JSONArray) nfcJSON.get("contacts");

      for (int i = 0; i < contacts.length(); i++) {

        // fill camera itemOverlay here

        JSONObject contact = contacts.getJSONObject(i);

        long contactId = contact.getLong("id");

        // match id from nfc with data from local db

        for (int j = 0; j < testCameras.length(); j++) {
          JSONObject camera = (JSONObject) testCameras.get(j);
          JSONObject cameraLocation = (JSONObject) camera.getJSONObject("location");

          if (contactId == camera.getLong("id") ) {

            cameraItems.add(
                    new OverlayItem(
                            contact.getString("id"),
                            "surveillance camera",
                            "You were in range of this camera on XX:XX XX.XX.XXXX",
                            new GeoPoint(cameraLocation.getDouble("lat"),
                                    cameraLocation.getDouble("lon"))
                    )
            );


          }
        }

      }

      JSONObject deviceLocation = nfcJSON.getJSONObject("location");

      deviceLocationItems.add(
              new OverlayItem(
                      "your location at time of contact",
                      "",
                      "Accuracy (SIV): " + deviceLocation.getString("SIV"),
                      new GeoPoint(deviceLocation.getDouble("lat"), deviceLocation.getDouble("lon"))
              )
      );



    } catch (JSONException jsonException) {

      Log.i(TAG, jsonException.toString());

    }

    cameraOverlay = new ItemizedIconOverlay<>(cameraItems, null, ctx);

    deviceLocationOverlay = new ItemizedIconOverlay<>(deviceLocationItems, null, ctx);

    mapView.getOverlays().remove(cameraOverlay);
    mapView.getOverlays().remove(deviceLocationOverlay);

    mapView.getOverlays().add(cameraOverlay);
    mapView.getOverlays().add(deviceLocationOverlay);

    mapView.invalidate();




  }

}