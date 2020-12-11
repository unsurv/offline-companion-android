package org.unsurv.offline_companion_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NoCache;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConnectionUtils {

  final static String TAG = "ConnectionUtils";


  static void downloadCamerasFromServer(double latMin, double latMax, double lonMin, double lonMax, boolean insertIntoDb, SurveillanceCameraRepository cameraRepository, Context context){


    // baseURL = "https://overpass-api.de/api/interpreter?data=[out:json];node[man_made=surveillance](52.5082248,13.3780064,52.515041,13.3834472);out meta;";
    String baseUrl = "https://overpass-api.de/api/interpreter?";

    // https://overpass-api.de/api/interpreter?

    String homeZone = "52.5082248,52.515041,13.3780064,13.3834472";

    List<SurveillanceCamera> camerasToSync = new ArrayList<>();

    final SurveillanceCameraRepository crep = cameraRepository;

    String completeURL = String.format(baseUrl + "data=[out:json];node[man_made=surveillance](%s,%s,%s,%s);out meta;", latMin, lonMin, latMax, lonMax);

    RequestQueue mRequestQueue;

    // Set up the network to use HttpURLConnection as the HTTP client.
    Network network = new BasicNetwork(new HurlStack());

    // Instantiate the RequestQueue with the cache and network.
    mRequestQueue = new RequestQueue(new NoCache(), network);

    // Start the queue
    mRequestQueue.start();


    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            completeURL,
            null,
            new Response.Listener<JSONObject>() {
              @Override
              public void onResponse(JSONObject response) {



                JSONObject cameraJSON;

                try {

                  String osm_db_timestamp = response.getJSONObject("osm3s").getString("timestamp_osm_base");

                  for (int i = 0; i < response.getJSONArray("elements").length(); i++) {

                    cameraJSON = new JSONObject(String.valueOf(response.getJSONArray("elements").get(i)));
                    String timestamp = cameraJSON.getString("timestamp");

                    JSONObject tags = cameraJSON.getJSONObject("tags");

                    int type = 0; // default for fixed camera
                    int area = 0; // outdoor
                    int direction = -1; // unknown
                    int mount = 0; // unknown
                    int height = -1; // unknown
                    int angle = -1; // unknown

                    List<String> tagsAvailable = new ArrayList<>();

                    // loop through tag keys for more information: area, angle, height etc.
                    Iterator<String> iterTags = tags.keys();
                    while (iterTags.hasNext()) {
                      String key = iterTags.next();
                      tagsAvailable.add(key);
                    }

                    for (String tag : tagsAvailable){

                      try {

                        switch (tag) {

                          case "surveillance":
                            area = StorageUtils.areaList.indexOf(tags.getString(tag));
                            break;

                          case "camera:type":
                            type = StorageUtils.typeList.indexOf(tags.getString(tag));
                            break;

                          case "camera:mount":
                            mount = StorageUtils.mountList.indexOf(tags.getString(tag));
                            break;

                          case "camera:direction":
                            direction = tags.getInt("camera:direction");
                            break;

                          case "height":
                            height = tags.getInt("height");
                            break;
                        }

                      } catch (Exception ex) {
                        Log.i(TAG, "Error creating value from overpass api response: " + ex.toString());
                        continue;
                      }



                    }

                    SurveillanceCamera cameraToAdd = new SurveillanceCamera(
                            type,
                            area,
                            direction,
                            mount,
                            height,
                            angle,
                            null,
                            cameraJSON.getLong("id"),
                            cameraJSON.getDouble("lat"),
                            cameraJSON.getDouble("lon"),
                            "",
                            timestamp


                    );

                    camerasToSync.add(cameraToAdd);

                  }



                } catch (Exception e) {
                  Log.i(TAG, "onResponse: " + e.toString());

                }

                if (insertIntoDb) {
                  crep.insertAll(camerasToSync);
                }

              }
            }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        // TODO: Handle Errors
        Log.i(TAG, "Error in connection " + error.toString());
      }
    }) {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {

        Map<String, String> headers = new HashMap<>();
        // headers.put("Content-Type", "application/json");

        return headers;
      }
    };

    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
            5000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));



    mRequestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<Object>() {

      @Override
      public void onRequestFinished(Request<Object> request) {
        if (request.hasHadResponseDelivered()){

          long currentTime = System.currentTimeMillis();
          SimpleDateFormat timestampIso8601 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

          // sharedPreferences.edit().putString("lastUpdated", timestampIso8601.format(new Date(currentTime))).apply();

          Toast.makeText(context, String.format("Downloaded %s cameras from OpenStreetMap", camerasToSync.size()) , Toast.LENGTH_LONG).show();

        }


      }
    });


    mRequestQueue.add(jsonObjectRequest);

  }





}
