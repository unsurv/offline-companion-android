package org.unsurv.offline_companion_android;


import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;


@Database(entities = {SurveillanceCamera.class}, version = 1, exportSchema = false)
public abstract class SurveillanceCameraDatabase extends RoomDatabase {

  public abstract SurveillanceCameraDao surveillanceCameraDao();

  private static volatile SurveillanceCameraDatabase INSTANCE;

  static SurveillanceCameraDatabase getDatabase(final Context context) {
    if (INSTANCE == null) {
      synchronized (SurveillanceCameraDatabase.class) {
        if (INSTANCE == null) {
          // create db here
          INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                  SurveillanceCameraDatabase.class, "local_surveillance_cameras")
                  .addCallback(roomDatabaseCallback)
                  .build();

        }

      }
    }
    return INSTANCE;
  }


  private static SurveillanceCameraDatabase.Callback roomDatabaseCallback =
          new SurveillanceCameraDatabase.Callback(){

            @Override
            public void onOpen (@NonNull SupportSQLiteDatabase db){
              super.onOpen(db);
              new PopulateDbAsync(INSTANCE).execute();
            }
          };


  private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

    private final SurveillanceCameraDao mDao;

    PopulateDbAsync(SurveillanceCameraDatabase db) {
      mDao = db.surveillanceCameraDao();
    }

    @Override
    protected Void doInBackground(final Void... params) {
      mDao.deleteAll();



      // use sync here

      for (int i = 0; i < 10; i++) {

        SurveillanceCamera testCam = new SurveillanceCamera(
                StorageUtils.FIXED_CAMERA,
                StorageUtils.AREA_UNKNOWN,
                150,
                StorageUtils.MOUNT_UNKNOWN,
                5,
                15,
                null,
                113244567788L + i,
                49.99646 + i / 1000f,
                8.27983 + i / 1000f,
                null,
                null);

        SurveillanceCamera testCam2 = new SurveillanceCamera(
                StorageUtils.DOME_CAMERA,
                StorageUtils.AREA_UNKNOWN,
                150,
                StorageUtils.MOUNT_UNKNOWN,
                5,
                15,
                null,
                223244567788L + i,
                49.99542 + i / 1000f,
                8.27885 + i / 1000f,
                null,
                null);



        // mDao.insert(testCam);
        // mDao.insert(testCam2);
      }



      return null;
    }
  }

}
