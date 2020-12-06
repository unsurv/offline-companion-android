package org.unsurv.offline_companion_android;

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
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.osmdroid.util.BoundingBox;
import org.unsurv.offline_companion_android.R;

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
  private boolean infoIsShowing;

  // stupid values so they get changed in first loop, this is stupid
  double latMin = 1000;
  double latMax = -1000;
  double lonMin = 1000;
  double lonMax = -1000;

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

    infoIsShowing = false;

    // info button
    infoButton = findViewById(R.id.map_info_button);

    infoButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        if (!infoIsShowing) {
          for (SurveillanceCamera cam: camerasOnMap) {

            drawCameraArea(new GeoPoint(cam.getLatitude(), cam.getLongitude()),
                    cam.getDirection(),
                    cam.getHeight(),
                    cam.getAngle(),
                    cam.getCameraType());

          }

          drawConnectionLines(surveillanceContacts);
          infoIsShowing = true;

          mapView.invalidate();
        }
        else {
          mapView.getOverlayManager().removeAll(polygonsOnMap);
          mapView.getOverlayManager().removeAll(polylinesOnMap);
          infoIsShowing = false;
          mapView.invalidate();
        }



      }
    });

    try {

      testCameras = new JSONArray("[{'loc': {'lat': 50.0023733, 'lon': 8.2584097, 'SIV': 8, 't': '13:54:45'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a444', 'e1ee48d1']},{'loc': {'lat': 50.0022286, 'lon': 8.258535, 'SIV': 9, 't': '13:54:49'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', '169c0cc32']},{'loc': {'lat': 50.0019124, 'lon': 8.259349, 'SIV': 5, 't': '13:55:14'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '169c0cc32']},{'loc': {'lat': 50.0018535, 'lon': 8.2593633, 'SIV': 4, 't': '13:55:18'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1d52530', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '169c0cc32']},{'loc': {'lat': 50.0018284, 'lon': 8.2594955, 'SIV': 6, 't': '13:55:22'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1d52530', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '169c0cc32']},{'loc': {'lat': 50.0018316, 'lon': 8.2595912, 'SIV': 6, 't': '13:55:26'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1d52530', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff', '169c0cc32']},{'loc': {'lat': 50.0018459, 'lon': 8.2595606, 'SIV': 5, 't': '13:55:30'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1d52530', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '169c0cc32']},{'loc': {'lat': 50.0018186, 'lon': 8.2597161, 'SIV': 5, 't': '13:55:34'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1d52530', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff', '169c0cc32']},{'loc': {'lat': 50.0018961, 'lon': 8.2596485, 'SIV': 5, 't': '13:55:39'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff', '169c0cc32']},{'loc': {'lat': 50.0019342, 'lon': 8.2596933, 'SIV': 7, 't': '13:55:44'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '169c0cc32']},{'loc': {'lat': 50.0019292, 'lon': 8.2597676, 'SIV': 4, 't': '13:55:48'}, 'ids': ['c099a43e', 'c099a43f', 'c099a440', 'c099a441', 'c099a442', 'c099a444', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff', '169c0cc32']},{'loc': {'lat': 50.0019012, 'lon': 8.2598427, 'SIV': 4, 't': '13:55:52'}, 'ids': ['c099a43e', 'c099a43f', 'c099a442', 'c099a444', 'e1ee48cf', 'e1ee48d1', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff', '169c0cc32']},{'loc': {'lat': 50.001675, 'lon': 8.2601779, 'SIV': 5, 't': '13:55:56'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0015558, 'lon': 8.2602749, 'SIV': 5, 't': '13:56:0'}, 'ids': ['c099a442', 'e1d5252e', 'e1d5252f', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0013623, 'lon': 8.2603339, 'SIV': 5, 't': '13:56:4'}, 'ids': ['c099a442', 'e1d5252e', 'e1d5252f', 'e1d52530', 'e1d52531', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.001384, 'lon': 8.2604102, 'SIV': 6, 't': '13:56:8'}, 'ids': ['c099a442', 'e1d5252e', 'e1d5252f', 'e1d52530', 'e1d52531', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0013397, 'lon': 8.2605158, 'SIV': 6, 't': '13:56:12'}, 'ids': ['e1d5252e', 'e1d5252f', 'e1d52530', 'e1d52531', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0013689, 'lon': 8.2605226, 'SIV': 5, 't': '13:56:16'}, 'ids': ['e1d5252e', 'e1d5252f', 'e1d52530', 'e1d52531', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0014552, 'lon': 8.2604472, 'SIV': 6, 't': '13:56:20'}, 'ids': ['c099a442', 'e1d5252e', 'e1d5252f', 'e1d52530', 'e1d52531', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0014558, 'lon': 8.260383, 'SIV': 5, 't': '13:56:24'}, 'ids': ['c099a442', 'e1d5252e', 'e1d5252f', 'e1d52530', 'e1d52531', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0015022, 'lon': 8.260322, 'SIV': 5, 't': '13:56:28'}, 'ids': ['c099a442', 'e1d5252e', 'e1d5252f', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0015708, 'lon': 8.260367, 'SIV': 5, 't': '13:56:32'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0015942, 'lon': 8.2604458, 'SIV': 5, 't': '13:56:36'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0016223, 'lon': 8.2605225, 'SIV': 7, 't': '13:56:40'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0016619, 'lon': 8.2605468, 'SIV': 6, 't': '13:56:44'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.001665, 'lon': 8.2605594, 'SIV': 6, 't': '13:56:48'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0016671, 'lon': 8.2605462, 'SIV': 7, 't': '13:56:52'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0016573, 'lon': 8.2604921, 'SIV': 5, 't': '13:56:56'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0016399, 'lon': 8.2604721, 'SIV': 5, 't': '13:57:0'}, 'ids': ['c099a442', 'e1d52530', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0017212, 'lon': 8.2604583, 'SIV': 5, 't': '13:57:4'}, 'ids': ['c099a442', 'e1ee48cf', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0018372, 'lon': 8.260613, 'SIV': 8, 't': '13:57:8'}, 'ids': ['c099a442', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', 'e1ee48d4', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0018236, 'lon': 8.2608628, 'SIV': 6, 't': '13:57:12'}, 'ids': ['c099a442', 'e1ee48d0', 'e1ee48d2', 'e1ee48d3', 'e1ee48d4', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0015685, 'lon': 8.2611225, 'SIV': 6, 't': '13:57:16'}, 'ids': ['e1d52531', 'e1ee48d0', 'e1ee48d3', 'e1ee48d4', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0013841, 'lon': 8.2612745, 'SIV': 8, 't': '13:57:20'}, 'ids': ['e1d52531', 'e1ee48d0', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0013164, 'lon': 8.2614612, 'SIV': 5, 't': '13:57:24'}, 'ids': ['e1d52531', 'e1ee48d0', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0011652, 'lon': 8.2619231, 'SIV': 7, 't': '13:57:28'}, 'ids': ['e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e1d52531', 'e1ee48d0', 'e74b9677', '1093196fe', '1093196ff']},{'loc': {'lat': 50.0009311, 'lon': 8.2622165, 'SIV': 7, 't': '13:57:32'}, 'ids': ['e19ec9f9', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e1d52531', 'e74b9677']},{'loc': {'lat': 50.0007789, 'lon': 8.2624691, 'SIV': 7, 't': '13:57:36'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e1d52531', 'e74b9677']},{'loc': {'lat': 50.0006665, 'lon': 8.2626968, 'SIV': 7, 't': '13:57:40'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e1d52531', 'e74b9677']},{'loc': {'lat': 50.0006187, 'lon': 8.2628864, 'SIV': 7, 't': '13:57:44'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.0006006, 'lon': 8.2629936, 'SIV': 7, 't': '13:57:48'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.0005993, 'lon': 8.2630412, 'SIV': 7, 't': '13:57:52'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.0006518, 'lon': 8.2631441, 'SIV': 7, 't': '13:57:56'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.0006837, 'lon': 8.2631712, 'SIV': 7, 't': '13:58:0'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.0006586, 'lon': 8.2631324, 'SIV': 7, 't': '13:58:4'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.0005913, 'lon': 8.2630855, 'SIV': 7, 't': '13:58:8'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.0005615, 'lon': 8.2630807, 'SIV': 6, 't': '13:58:12'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d', 'e74b9677']},{'loc': {'lat': 50.000516, 'lon': 8.263075, 'SIV': 7, 't': '13:58:16'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d']},{'loc': {'lat': 50.0004936, 'lon': 8.2632664, 'SIV': 7, 't': '13:58:20'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d']},{'loc': {'lat': 50.0003592, 'lon': 8.2634935, 'SIV': 7, 't': '13:58:24'}, 'ids': ['e19ec9f9', 'e1d52527', 'e1d5252a', 'e1d5252b', 'e1d5252c', 'e1d5252d']},{'loc': {'lat': 50.0003091, 'lon': 8.2639134, 'SIV': 7, 't': '13:58:28'}, 'ids': ['e19ec9f9', 'e1d52527']},{'loc': {'lat': 50.0001867, 'lon': 8.264306, 'SIV': 7, 't': '13:58:32'}, 'ids': ['e1d52527']},{'loc': {'lat': 49.9998376, 'lon': 8.265026, 'SIV': 8, 't': '13:58:40'}, 'ids': ['e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88f2', 'e74c88f3']},{'loc': {'lat': 49.9996736, 'lon': 8.2652947, 'SIV': 6, 't': '13:58:44'}, 'ids': ['e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88f2', 'e74c88f3']},{'loc': {'lat': 49.9995618, 'lon': 8.2655887, 'SIV': 7, 't': '13:58:48'}, 'ids': ['e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88f2', 'e74c88f3']},{'loc': {'lat': 49.9993242, 'lon': 8.2659943, 'SIV': 8, 't': '13:58:52'}, 'ids': ['e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88ef', 'e74c88f2', 'e74c88f3']},{'loc': {'lat': 49.999148, 'lon': 8.2663406, 'SIV': 6, 't': '13:58:56'}, 'ids': ['e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88ef', 'e74c88f2', 'e74c88f3', '108e99c64']},{'loc': {'lat': 49.9990461, 'lon': 8.266457, 'SIV': 9, 't': '13:59:0'}, 'ids': ['e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88ef', 'e74c88f2', '108e99c62', '108e99c64']},{'loc': {'lat': 49.9989766, 'lon': 8.2665068, 'SIV': 6, 't': '13:59:4'}, 'ids': ['e74c88eb', 'e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88ef', '108e99c62', '108e99c64']},{'loc': {'lat': 49.9988039, 'lon': 8.2666096, 'SIV': 7, 't': '13:59:13'}, 'ids': ['e74c88eb', 'e74c88ec', 'e74c88ed', 'e74c88ee', 'e74c88ef', '108e99c62', '108e99c63', '108e99c64']},{'loc': {'lat': 49.9986672, 'lon': 8.2665983, 'SIV': 6, 't': '13:59:17'}, 'ids': ['e74c88eb', 'e74c88ec', '108e99c62', '108e99c63', '108e99c64']},{'loc': {'lat': 49.9986268, 'lon': 8.2666453, 'SIV': 6, 't': '13:59:21'}, 'ids': ['e74c88e8', 'e74c88eb', '108e99c62', '108e99c63', '108e99c64']},{'loc': {'lat': 49.9986631, 'lon': 8.2665812, 'SIV': 9, 't': '13:59:25'}, 'ids': ['e74c88eb', 'e74c88ec', '108e99c62', '108e99c63', '108e99c64']},{'loc': {'lat': 49.9986923, 'lon': 8.2665792, 'SIV': 6, 't': '13:59:29'}, 'ids': ['e74c88eb', 'e74c88ec', '108e99c62', '108e99c63', '108e99c64']},{'loc': {'lat': 49.9986341, 'lon': 8.2665927, 'SIV': 5, 't': '13:59:33'}, 'ids': ['e74c88e8', 'e74c88eb', '108e99c62', '108e99c63', '108e99c64']},{'loc': {'lat': 49.9986136, 'lon': 8.2666593, 'SIV': 6, 't': '13:59:37'}, 'ids': ['e74c88e8', 'e74c88eb', '108e99c62', '108e99c63', '108e99c64']},{'loc': {'lat': 49.998514, 'lon': 8.2668969, 'SIV': 8, 't': '13:59:41'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88eb', '108e99c62', '108e99c64']},{'loc': {'lat': 49.998367, 'lon': 8.2671482, 'SIV': 8, 't': '13:59:45'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb', '108e99c64', '108ff4ab8', '108ff4ab9']},{'loc': {'lat': 49.9982188, 'lon': 8.2673857, 'SIV': 7, 't': '13:59:49'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb', '108e99c64', '108ff4ab8', '108ff4ab9']},{'loc': {'lat': 49.9980445, 'lon': 8.2675879, 'SIV': 7, 't': '13:59:54'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb', '108e99c64']},{'loc': {'lat': 49.9979628, 'lon': 8.2677017, 'SIV': 8, 't': '13:59:58'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb', '108e99c64']},{'loc': {'lat': 49.99791, 'lon': 8.2679228, 'SIV': 7, 't': '14:0:2'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb']},{'loc': {'lat': 49.9979296, 'lon': 8.2681109, 'SIV': 5, 't': '14:0:6'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb']},{'loc': {'lat': 49.9980539, 'lon': 8.2683474, 'SIV': 9, 't': '14:0:10'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb', '108ff4ab8', '108ff4ab9']},{'loc': {'lat': 49.9981447, 'lon': 8.2686651, 'SIV': 10, 't': '14:0:14'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', 'e74c88eb', '108ff4ab8', '108ff4ab9']},{'loc': {'lat': 49.9982976, 'lon': 8.2690492, 'SIV': 9, 't': '14:0:19'}, 'ids': ['e74c88e6', 'e74c88e7', 'e74c88e8', 'e74c88e9', 'e74c88ea', '108ff4ab8', '108ff4ab9']},{'loc': {'lat': 49.9983758, 'lon': 8.2694115, 'SIV': 10, 't': '14:0:24'}, 'ids': ['e74c88e7', 'e74c88e9', 'e74c88ea', '108ff4ab8']},{'loc': {'lat': 49.9984284, 'lon': 8.2697233, 'SIV': 10, 't': '14:0:28'}, 'ids': ['e74c88e9', 'e74c88ea', '108e99c65', '10b74b4dd']},{'loc': {'lat': 49.9985258, 'lon': 8.2700125, 'SIV': 11, 't': '14:0:32'}, 'ids': ['a93369d4', 'a93369d5', '108e99c65', '109315011', '10b74b4dd']},{'loc': {'lat': 49.9985812, 'lon': 8.2702833, 'SIV': 11, 't': '14:0:36'}, 'ids': ['a93369d4', 'a93369d5', '108e99c65', '108f47cea', '109315011', '10b74b4dd']},{'loc': {'lat': 49.9986601, 'lon': 8.2705595, 'SIV': 11, 't': '14:0:40'}, 'ids': ['a93369d4', 'a93369d5', '108f47cea', '109315011', '10b74b4dd']},{'loc': {'lat': 49.9987887, 'lon': 8.2708175, 'SIV': 6, 't': '14:0:44'}, 'ids': ['a93369d4', 'a93369d5', '109315011', '10b74b4dd']},{'loc': {'lat': 49.9988954, 'lon': 8.2710843, 'SIV': 7, 't': '14:0:48'}, 'ids': ['a93369d4', 'a93369d5', '109315011', '10b74b4dd']},{'loc': {'lat': 49.9989681, 'lon': 8.2713834, 'SIV': 7, 't': '14:0:52'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd']},{'loc': {'lat': 49.9990334, 'lon': 8.2716001, 'SIV': 7, 't': '14:0:56'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991113, 'lon': 8.2718067, 'SIV': 7, 't': '14:1:0'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991164, 'lon': 8.2720649, 'SIV': 7, 't': '14:1:4'}, 'ids': ['109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991356, 'lon': 8.2721485, 'SIV': 7, 't': '14:1:8'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992845, 'lon': 8.2721534, 'SIV': 6, 't': '14:1:12'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993799, 'lon': 8.2721381, 'SIV': 7, 't': '14:1:16'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993888, 'lon': 8.2721458, 'SIV': 8, 't': '14:1:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993835, 'lon': 8.2721375, 'SIV': 5, 't': '14:1:24'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993797, 'lon': 8.271964, 'SIV': 7, 't': '14:1:28'}, 'ids': ['a93369d5', '108f4271c', '109323bc6', '10b931852']},{'loc': {'lat': 49.9993487, 'lon': 8.2719934, 'SIV': 5, 't': '14:1:32'}, 'ids': ['a93369d5', '109323bc6', '10b931852']},{'loc': {'lat': 49.9993525, 'lon': 8.2719927, 'SIV': 8, 't': '14:1:36'}, 'ids': ['a93369d5', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994371, 'lon': 8.2721001, 'SIV': 6, 't': '14:1:40'}, 'ids': ['108f4271c', '109323bc6', '10b931852']},{'loc': {'lat': 49.9995053, 'lon': 8.272063, 'SIV': 8, 't': '14:1:44'}, 'ids': ['108f4271c', '109323bc6']},{'loc': {'lat': 49.9995031, 'lon': 8.271961, 'SIV': 6, 't': '14:1:48'}, 'ids': ['a93369d5', '108f4271c', '109323bc6']},{'loc': {'lat': 49.9994818, 'lon': 8.2718988, 'SIV': 6, 't': '14:1:52'}, 'ids': ['a93369d5', '108f4271c', '109323bc6']},{'loc': {'lat': 49.9994479, 'lon': 8.2718974, 'SIV': 6, 't': '14:1:56'}, 'ids': ['a93369d5', '108f4271c', '109323bc6']},{'loc': {'lat': 49.9993621, 'lon': 8.2718559, 'SIV': 6, 't': '14:2:0'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b931852']},{'loc': {'lat': 49.9993548, 'lon': 8.271883, 'SIV': 6, 't': '14:2:4'}, 'ids': ['a93369d5', '109323bc6', '10b931852']},{'loc': {'lat': 49.999336, 'lon': 8.2718743, 'SIV': 6, 't': '14:2:8'}, 'ids': ['a93369d5', '109323bc6', '10b931852']},{'loc': {'lat': 49.9992657, 'lon': 8.2718691, 'SIV': 6, 't': '14:2:12'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9992588, 'lon': 8.2718204, 'SIV': 9, 't': '14:2:16'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9992223, 'lon': 8.2717402, 'SIV': 6, 't': '14:2:20'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9992328, 'lon': 8.2718236, 'SIV': 8, 't': '14:2:24'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991043, 'lon': 8.2719798, 'SIV': 8, 't': '14:2:28'}, 'ids': ['a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9990526, 'lon': 8.2719179, 'SIV': 7, 't': '14:2:32'}, 'ids': ['a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9990787, 'lon': 8.2716643, 'SIV': 6, 't': '14:2:37'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9990058, 'lon': 8.2716848, 'SIV': 8, 't': '14:2:41'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991466, 'lon': 8.2719706, 'SIV': 7, 't': '14:2:45'}, 'ids': ['a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991678, 'lon': 8.27216, 'SIV': 6, 't': '14:2:49'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999181, 'lon': 8.272208, 'SIV': 7, 't': '14:2:53'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991631, 'lon': 8.2722423, 'SIV': 7, 't': '14:2:57'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991957, 'lon': 8.2723537, 'SIV': 7, 't': '14:3:1'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992288, 'lon': 8.2723387, 'SIV': 8, 't': '14:3:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992316, 'lon': 8.2723155, 'SIV': 8, 't': '14:3:9'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992909, 'lon': 8.2722967, 'SIV': 8, 't': '14:3:13'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992706, 'lon': 8.2723092, 'SIV': 7, 't': '14:3:17'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992972, 'lon': 8.272363, 'SIV': 7, 't': '14:3:21'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999229, 'lon': 8.2723413, 'SIV': 6, 't': '14:3:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991937, 'lon': 8.2723233, 'SIV': 7, 't': '14:3:29'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992059, 'lon': 8.272233, 'SIV': 7, 't': '14:3:33'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992174, 'lon': 8.2722078, 'SIV': 6, 't': '14:3:37'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992194, 'lon': 8.2721665, 'SIV': 7, 't': '14:3:41'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991839, 'lon': 8.2721204, 'SIV': 7, 't': '14:3:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992258, 'lon': 8.2721521, 'SIV': 8, 't': '14:3:49'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992596, 'lon': 8.2721833, 'SIV': 7, 't': '14:3:53'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992121, 'lon': 8.2721333, 'SIV': 7, 't': '14:3:57'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992164, 'lon': 8.2721556, 'SIV': 8, 't': '14:4:1'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991939, 'lon': 8.2721479, 'SIV': 7, 't': '14:4:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992403, 'lon': 8.2721871, 'SIV': 6, 't': '14:4:9'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993024, 'lon': 8.2722057, 'SIV': 7, 't': '14:4:13'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999332, 'lon': 8.2722106, 'SIV': 7, 't': '14:4:17'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993307, 'lon': 8.272244, 'SIV': 7, 't': '14:4:21'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993227, 'lon': 8.272304, 'SIV': 8, 't': '14:4:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993687, 'lon': 8.272324, 'SIV': 7, 't': '14:4:29'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9994772, 'lon': 8.2724019, 'SIV': 7, 't': '14:4:33'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994538, 'lon': 8.2723709, 'SIV': 7, 't': '14:4:38'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994089, 'lon': 8.2723913, 'SIV': 7, 't': '14:4:43'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994042, 'lon': 8.2723429, 'SIV': 8, 't': '14:4:48'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9994305, 'lon': 8.2723963, 'SIV': 6, 't': '14:4:52'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994261, 'lon': 8.2723411, 'SIV': 7, 't': '14:4:57'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994453, 'lon': 8.2723179, 'SIV': 7, 't': '14:5:2'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994395, 'lon': 8.2723151, 'SIV': 7, 't': '14:5:7'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9993805, 'lon': 8.2722719, 'SIV': 7, 't': '14:5:12'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993964, 'lon': 8.2721451, 'SIV': 7, 't': '14:5:16'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9994393, 'lon': 8.2721642, 'SIV': 7, 't': '14:5:21'}, 'ids': ['108f4271c', '109323bc6', '10b931852']},{'loc': {'lat': 49.9993775, 'lon': 8.2721375, 'SIV': 6, 't': '14:5:26'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9994038, 'lon': 8.2722219, 'SIV': 7, 't': '14:5:31'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993656, 'lon': 8.272243, 'SIV': 7, 't': '14:5:36'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993651, 'lon': 8.2722883, 'SIV': 8, 't': '14:5:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993264, 'lon': 8.2722673, 'SIV': 7, 't': '14:5:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992982, 'lon': 8.2723444, 'SIV': 8, 't': '14:5:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992966, 'lon': 8.2723641, 'SIV': 7, 't': '14:5:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992628, 'lon': 8.2723257, 'SIV': 7, 't': '14:6:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992244, 'lon': 8.2722842, 'SIV': 7, 't': '14:6:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992002, 'lon': 8.2722828, 'SIV': 8, 't': '14:6:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991754, 'lon': 8.2723023, 'SIV': 8, 't': '14:6:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999166, 'lon': 8.2722877, 'SIV': 7, 't': '14:6:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991494, 'lon': 8.2722894, 'SIV': 8, 't': '14:6:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9990978, 'lon': 8.27227, 'SIV': 7, 't': '14:6:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991794, 'lon': 8.2723112, 'SIV': 7, 't': '14:6:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992226, 'lon': 8.2723107, 'SIV': 7, 't': '14:6:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992649, 'lon': 8.2722929, 'SIV': 7, 't': '14:6:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993073, 'lon': 8.272307, 'SIV': 7, 't': '14:6:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993119, 'lon': 8.2723191, 'SIV': 7, 't': '14:6:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993108, 'lon': 8.2723177, 'SIV': 7, 't': '14:7:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993393, 'lon': 8.2722875, 'SIV': 7, 't': '14:7:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993651, 'lon': 8.2722766, 'SIV': 6, 't': '14:7:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993418, 'lon': 8.272327, 'SIV': 7, 't': '14:7:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993548, 'lon': 8.2723442, 'SIV': 7, 't': '14:7:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993226, 'lon': 8.2723569, 'SIV': 7, 't': '14:7:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992949, 'lon': 8.2723605, 'SIV': 6, 't': '14:7:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993291, 'lon': 8.2723166, 'SIV': 7, 't': '14:7:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993188, 'lon': 8.2722544, 'SIV': 6, 't': '14:7:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992423, 'lon': 8.2722058, 'SIV': 7, 't': '14:7:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992708, 'lon': 8.2722118, 'SIV': 7, 't': '14:7:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992754, 'lon': 8.2722145, 'SIV': 7, 't': '14:7:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991919, 'lon': 8.2721628, 'SIV': 7, 't': '14:8:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992275, 'lon': 8.2722225, 'SIV': 7, 't': '14:8:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992462, 'lon': 8.2723149, 'SIV': 7, 't': '14:8:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992084, 'lon': 8.2724209, 'SIV': 7, 't': '14:8:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992391, 'lon': 8.272466, 'SIV': 8, 't': '14:8:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992452, 'lon': 8.2724625, 'SIV': 7, 't': '14:8:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993165, 'lon': 8.2724591, 'SIV': 7, 't': '14:8:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992695, 'lon': 8.2724663, 'SIV': 5, 't': '14:8:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991934, 'lon': 8.2723879, 'SIV': 8, 't': '14:8:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992273, 'lon': 8.2723358, 'SIV': 7, 't': '14:8:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992593, 'lon': 8.2723526, 'SIV': 7, 't': '14:8:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992677, 'lon': 8.2723678, 'SIV': 7, 't': '14:8:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992971, 'lon': 8.272381, 'SIV': 7, 't': '14:9:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993088, 'lon': 8.2723921, 'SIV': 7, 't': '14:9:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992655, 'lon': 8.2724351, 'SIV': 8, 't': '14:9:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993185, 'lon': 8.2723804, 'SIV': 7, 't': '14:9:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992756, 'lon': 8.2723886, 'SIV': 7, 't': '14:9:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999273, 'lon': 8.2724019, 'SIV': 7, 't': '14:9:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992221, 'lon': 8.2724591, 'SIV': 6, 't': '14:9:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991843, 'lon': 8.2724149, 'SIV': 8, 't': '14:9:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992445, 'lon': 8.2724261, 'SIV': 7, 't': '14:9:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991824, 'lon': 8.2724094, 'SIV': 7, 't': '14:9:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999215, 'lon': 8.2724027, 'SIV': 7, 't': '14:9:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992488, 'lon': 8.2724142, 'SIV': 7, 't': '14:9:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992369, 'lon': 8.272432, 'SIV': 8, 't': '14:10:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991652, 'lon': 8.2724584, 'SIV': 7, 't': '14:10:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991715, 'lon': 8.2724387, 'SIV': 8, 't': '14:10:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992068, 'lon': 8.2724079, 'SIV': 9, 't': '14:10:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992678, 'lon': 8.2724457, 'SIV': 7, 't': '14:10:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992488, 'lon': 8.2724279, 'SIV': 7, 't': '14:10:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992271, 'lon': 8.2723814, 'SIV': 7, 't': '14:10:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992878, 'lon': 8.272339, 'SIV': 7, 't': '14:10:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993298, 'lon': 8.2723308, 'SIV': 7, 't': '14:10:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993552, 'lon': 8.2722906, 'SIV': 7, 't': '14:10:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9994072, 'lon': 8.2722756, 'SIV': 7, 't': '14:10:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9994522, 'lon': 8.272267, 'SIV': 7, 't': '14:10:55'}, 'ids': ['108f4271c', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994068, 'lon': 8.272255, 'SIV': 6, 't': '14:11:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993478, 'lon': 8.2722769, 'SIV': 7, 't': '14:11:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993405, 'lon': 8.2722734, 'SIV': 7, 't': '14:11:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993374, 'lon': 8.2722706, 'SIV': 8, 't': '14:11:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999287, 'lon': 8.2722695, 'SIV': 7, 't': '14:11:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992835, 'lon': 8.2722856, 'SIV': 6, 't': '14:11:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992645, 'lon': 8.2723081, 'SIV': 8, 't': '14:11:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993201, 'lon': 8.2724141, 'SIV': 7, 't': '14:11:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993447, 'lon': 8.2724456, 'SIV': 6, 't': '14:11:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993283, 'lon': 8.2724447, 'SIV': 7, 't': '14:11:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992698, 'lon': 8.2724515, 'SIV': 5, 't': '14:11:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992649, 'lon': 8.272433, 'SIV': 7, 't': '14:11:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992541, 'lon': 8.2724598, 'SIV': 7, 't': '14:12:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991762, 'lon': 8.2724205, 'SIV': 8, 't': '14:12:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999183, 'lon': 8.2723933, 'SIV': 8, 't': '14:12:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991461, 'lon': 8.2724046, 'SIV': 8, 't': '14:12:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999145, 'lon': 8.2723882, 'SIV': 7, 't': '14:12:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992159, 'lon': 8.2723732, 'SIV': 8, 't': '14:12:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992068, 'lon': 8.2723851, 'SIV': 8, 't': '14:12:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992265, 'lon': 8.2723556, 'SIV': 8, 't': '14:12:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992178, 'lon': 8.2722988, 'SIV': 7, 't': '14:12:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992683, 'lon': 8.2723095, 'SIV': 7, 't': '14:12:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993484, 'lon': 8.2723347, 'SIV': 7, 't': '14:12:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993302, 'lon': 8.2723195, 'SIV': 7, 't': '14:12:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999311, 'lon': 8.2722877, 'SIV': 7, 't': '14:13:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993166, 'lon': 8.2722333, 'SIV': 7, 't': '14:13:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993321, 'lon': 8.2722323, 'SIV': 7, 't': '14:13:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993248, 'lon': 8.2721931, 'SIV': 7, 't': '14:13:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992314, 'lon': 8.2721316, 'SIV': 7, 't': '14:13:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991836, 'lon': 8.2720428, 'SIV': 7, 't': '14:13:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991697, 'lon': 8.2720309, 'SIV': 7, 't': '14:13:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999183, 'lon': 8.2720072, 'SIV': 7, 't': '14:13:35'}, 'ids': ['a93369d5', '109323bc6', '10b931852']},{'loc': {'lat': 49.9991469, 'lon': 8.2720542, 'SIV': 7, 't': '14:13:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992256, 'lon': 8.2721319, 'SIV': 7, 't': '14:13:45'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993052, 'lon': 8.2721146, 'SIV': 7, 't': '14:13:50'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992804, 'lon': 8.2721248, 'SIV': 8, 't': '14:13:55'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992975, 'lon': 8.2721473, 'SIV': 7, 't': '14:14:0'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999304, 'lon': 8.2721962, 'SIV': 7, 't': '14:14:5'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9993167, 'lon': 8.2722059, 'SIV': 7, 't': '14:14:10'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992827, 'lon': 8.2722442, 'SIV': 7, 't': '14:14:15'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992164, 'lon': 8.2722169, 'SIV': 7, 't': '14:14:20'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992518, 'lon': 8.2721458, 'SIV': 7, 't': '14:14:25'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9992326, 'lon': 8.2721223, 'SIV': 7, 't': '14:14:30'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.999223, 'lon': 8.2720762, 'SIV': 7, 't': '14:14:35'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991762, 'lon': 8.272044, 'SIV': 7, 't': '14:14:40'}, 'ids': ['109323bc6', '10b931852']},{'loc': {'lat': 49.9991309, 'lon': 8.2720345, 'SIV': 7, 't': '14:14:45'}, 'ids': ['109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991349, 'lon': 8.2719886, 'SIV': 7, 't': '14:14:50'}, 'ids': ['a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991715, 'lon': 8.2719433, 'SIV': 8, 't': '14:14:55'}, 'ids': ['a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991361, 'lon': 8.2719217, 'SIV': 8, 't': '14:15:0'}, 'ids': ['a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991362, 'lon': 8.2718317, 'SIV': 8, 't': '14:15:5'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991742, 'lon': 8.2718043, 'SIV': 7, 't': '14:15:10'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.999171, 'lon': 8.2718043, 'SIV': 8, 't': '14:15:15'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.999155, 'lon': 8.2717331, 'SIV': 7, 't': '14:15:20'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9992083, 'lon': 8.2716494, 'SIV': 7, 't': '14:15:25'}, 'ids': ['a93369d4', 'a93369d5', '109323bc6', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991889, 'lon': 8.271648, 'SIV': 6, 't': '14:15:30'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.999197, 'lon': 8.2716243, 'SIV': 7, 't': '14:15:35'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9990784, 'lon': 8.2716202, 'SIV': 7, 't': '14:15:40'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9991109, 'lon': 8.2717017, 'SIV': 7, 't': '14:15:45'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.999057, 'lon': 8.2717461, 'SIV': 7, 't': '14:15:50'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9990417, 'lon': 8.2717183, 'SIV': 7, 't': '14:15:55'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9989948, 'lon': 8.2716901, 'SIV': 7, 't': '14:16:0'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9989663, 'lon': 8.2716575, 'SIV': 8, 't': '14:16:5'}, 'ids': ['a93369d4', 'a93369d5', '108f75f05', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.99905, 'lon': 8.2715412, 'SIV': 7, 't': '14:16:10'}, 'ids': ['a93369d4', 'a93369d5', '10b74b4dd', '10b931852']},{'loc': {'lat': 49.9993585, 'lon': 8.2728719, 'SIV': 6, 't': '14:36:58'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9995193, 'lon': 8.2729561, 'SIV': 7, 't': '14:37:28'}, 'ids': ['e184456d', 'e184456e', '109317ade', '109323bc6', '10b931852']},{'loc': {'lat': 49.9995793, 'lon': 8.2729463, 'SIV': 7, 't': '14:37:38'}, 'ids': ['e184456d', 'e184456e', '109317ade', '109323bc6', '10b931852']},{'loc': {'lat': 49.999457, 'lon': 8.2726557, 'SIV': 6, 't': '14:37:43'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9992964, 'lon': 8.2727175, 'SIV': 4, 't': '14:37:48'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9995304, 'lon': 8.2728675, 'SIV': 4, 't': '14:37:53'}, 'ids': ['e184456d', '109317ade', '109323bc6', '10b931852']},{'loc': {'lat': 49.9994456, 'lon': 8.2728496, 'SIV': 4, 't': '14:37:58'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9993892, 'lon': 8.2727829, 'SIV': 5, 't': '14:38:3'}, 'ids': ['e184456d', '109323bc6', '10b931852']},{'loc': {'lat': 49.9993417, 'lon': 8.2727799, 'SIV': 6, 't': '14:38:8'}, 'ids': ['e184456d', '109323bc6', '10b931852']}]");

      // populateMapFromNfc(new JSONArray("[{\"loc\":{\"lat\":49.99631,\"lon\":8.28206,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[1A5DE6F8EC]},{\"loc\":{\"lat\":49.99584,\"lon\":8.28214,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[1A5DE6F8ED]},{\"loc\":{\"lat\":49.99611,\"lon\":8.28208,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[33FA69C4EC]},{\"loc\":{\"lat\":49.99581,\"lon\":8.28222,\"SIV\":4,\"t\":\"20:23:40\"},\"ids\":[33FA69C4ED]}]"));
      populateMapFromNfc(testCameras);


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

        double deviceLat = deviceLocation.getDouble("lat");
        double deviceLon = deviceLocation.getDouble("lon");


        if (deviceLat < latMin) {
          latMin = deviceLat;
        }

        if (deviceLat > latMax) {
          latMax = deviceLat;
        }


        if (deviceLon < lonMin) {
          lonMin = deviceLon;
        }

        if (deviceLon > lonMax) {
          lonMax = deviceLon;
        }




        SurveillanceContact surveillanceContact = new SurveillanceContact(
                new GeoPoint(deviceLat, deviceLon)
                , new ArrayList<SurveillanceCamera>());

        deviceLocationItems.add(
                new OverlayItem(
                        "your location at time of contact",
                        "",
                        "Accuracy (SIV): " + deviceLocation.getString("SIV"),
                        new GeoPoint(deviceLat, deviceLon)
                )
        );


        JSONArray ids = (JSONArray) contact.get("ids");

        // fill camera itemOverlay here

        for (int k = 0; k < ids.length(); k++) {

          long contactId = Long.valueOf((String) ids.get(k), 16);

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


            double cameraLat = camera.getLatitude();
            double cameraLon = camera.getLongitude();

            if (cameraLat < latMin) {
              latMin = cameraLat;
            }

            if (cameraLat > latMax) {
              latMax = cameraLat;
            }


            if (cameraLon < lonMin) {
              lonMin = cameraLon;
            }

            if (cameraLon > lonMax) {
              lonMax = cameraLon;
            }



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

    mapView.getOverlays().remove(deviceLocationOverlay);
    mapView.getOverlays().remove(cameraOverlay);

    mapView.getOverlays().add(deviceLocationOverlay);
    mapView.getOverlays().add(cameraOverlay);


    // add 10 % between latmin and latmax to better show all markers in Boundingbox
    double latInterval = Math.abs(latMax - latMin);
    // 5 % each
    latMin -= latInterval / 20;
    latMax += latInterval / 20;

    double lonInterval = Math.abs(lonMax - lonMin);

    lonMin -= lonInterval / 20;
    lonMax += lonInterval / 20;

    mapView.post(
            new Runnable() {
              @Override
              public void run() {
                mapView.zoomToBoundingBox(new BoundingBox(latMax, lonMax, latMin, lonMin), true);
              }
            }
    );
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

    polygonsOnMap.add(polygon);

    mapView.getOverlayManager().add(polygon);
    mapView.invalidate();

  }

}