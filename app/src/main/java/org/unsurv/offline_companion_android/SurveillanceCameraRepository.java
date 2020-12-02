package org.unsurv.offline_companion_android;

import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class SurveillanceCameraRepository {

  private SurveillanceCameraDao cameraDao;

  SurveillanceCameraRepository(Application application) {
    SurveillanceCameraDatabase db = SurveillanceCameraDatabase.getDatabase(application);
    cameraDao = db.surveillanceCameraDao();

  }

  List<SurveillanceCamera> getSynchronizedCamerasInArea(double latMin, double latMax, double lonMin, double lonMax) {

    try {

      return new getCamerasInAreaAsync(cameraDao).execute(latMin, latMax, lonMin, lonMax).get();

    } catch (ExecutionException ee) {

      Log.i("ExecutionException:: " , ee.toString());

    } catch (InterruptedException ie) {

      Log.i("InterruptedException: " , ie.toString());
    }


    return null;
  }

  List<SurveillanceCamera> getAllCameras() {

  try {

    return new getAllCamerasAsync(cameraDao).execute().get();

  } catch (ExecutionException ee) {

    Log.i("ExecutionException:: " , ee.toString());

  } catch (InterruptedException ie) {

    Log.i("InterruptedException: " , ie.toString());
  }
    return null;
  }

  SurveillanceCamera findById(long osmId) {

    try {

      return new findByIdAsync(cameraDao).execute(osmId).get();


    } catch (ExecutionException ee) {

      Log.i("ExecutionException:: " , ee.toString());

    } catch (InterruptedException ie) {

      Log.i("InterruptedException: " , ie.toString());
    }

    return null;

  }


  void insertCamera(SurveillanceCamera camera) {

    new insertAsync(cameraDao).execute(camera);

  }




  private static class getCamerasInAreaAsync extends AsyncTask<Double, Void, List<SurveillanceCamera>> {

    private SurveillanceCameraDao mAsyncTaskDao;

    getCamerasInAreaAsync(SurveillanceCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Double... params) {

      // latMin, latMax, lonMin, lonMax
      return mAsyncTaskDao.getCamerasInArea(params[0], params[1], params[2], params[3]);

    }
  }


  private static class getAllCamerasAsync extends AsyncTask<Double, Void, List<SurveillanceCamera>> {

    private SurveillanceCameraDao mAsyncTaskDao;

    getAllCamerasAsync(SurveillanceCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected List<SurveillanceCamera> doInBackground(Double... params) {

      return mAsyncTaskDao.getAllCameras();

    }
  }


  private static class findByIdAsync extends AsyncTask<Long, Void, SurveillanceCamera> {

    private SurveillanceCameraDao mAsyncTaskDao;
    String TAG = "SynchronizedCameraRepository InsertAsyncTask";

    findByIdAsync(SurveillanceCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected SurveillanceCamera doInBackground(Long... osmId) {


      SurveillanceCamera camera = mAsyncTaskDao.findById(osmId[0]);

      return camera;
    }

  }


  private static class insertAsync extends AsyncTask<SurveillanceCamera, Void, Void> {

    private SurveillanceCameraDao mAsyncTaskDao;
    String TAG = "SynchronizedCameraRepository InsertAsyncTask";

    insertAsync(SurveillanceCameraDao dao) {
      mAsyncTaskDao = dao;
    }

    @Override
    protected Void doInBackground(SurveillanceCamera... cam) {


        if (mAsyncTaskDao.findById(cam[0].getExternalId()) != null) {
          Log.i(TAG, "id " + cam[0].getExternalId() + " already in db" );
        } else {
          mAsyncTaskDao.insert(cam[0]);
        }


      
      return null;
    }

  }

}
