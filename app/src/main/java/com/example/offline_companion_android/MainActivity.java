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

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED;


/**
 * modified from Ralf Wondratschek  NFC tutorial
 *
 */

public class MainActivity extends AppCompatActivity {

  public static final String TAG = "mainActivity";

  Context ctx;
  TextView debugText;
  NfcAdapter nfcAdapter;

  private MapView mapView;



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
        debugText.setText("Read content: " + result);
      }
    }
  }



}