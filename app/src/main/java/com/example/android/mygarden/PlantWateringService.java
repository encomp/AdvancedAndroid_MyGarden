package com.example.android.mygarden;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in a service on a
 * separate handler thread.
 */
public class PlantWateringService extends IntentService {

  public static final String ACTION_WATER_PLANTS =
      "com.example.android.mygarden.action.water_plants";
  public static final String ACTION_UPDATE_PLANT_WIDGETS =
      "com.example.android.mygarden.action.update_plant_widgets";

  public PlantWateringService() {
    super("PlantWateringService");
  }

  /**
   * Starts this service to perform WaterPlants action with the given parameters. If the service is
   * already performing a task this action will be queued.
   *
   * @see IntentService
   */
  public static void startActionWaterPlants(Context context) {
    Intent intent = new Intent(context, PlantWateringService.class);
    intent.setAction(ACTION_WATER_PLANTS);
    context.startService(intent);
  }

  public static void startActionUpdateWidgets(Context context) {
    Intent intent = new Intent(context, PlantWateringService.class);
    intent.setAction(ACTION_UPDATE_PLANT_WIDGETS);
    context.startService(intent);
  }

  /** @param intent */
  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent != null) {
      final String action = intent.getAction();
      if (ACTION_WATER_PLANTS.equals(action)) {
        handleActionWaterPlants();
      } else if (ACTION_UPDATE_PLANT_WIDGETS.equals(action)) {
        handleActionUpdatePlantWidgets();
      }
    }
  }

  /** Handle action WaterPlant in the provided background thread with the provided parameters. */
  private void handleActionWaterPlants() {
    Uri PLANTS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();
    ContentValues contentValues = new ContentValues();
    long timeNow = System.currentTimeMillis();
    contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
    // Update only plants that are still alive
    getContentResolver()
        .update(
            PLANTS_URI,
            contentValues,
            PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME + ">?",
            new String[] {String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});
  }

  private void handleActionUpdatePlantWidgets() {
    Uri PLANTS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();
    Cursor cursor =
        getContentResolver()
            .query(PLANTS_URI, null, null, null, PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);

    // Update only plants that are still alive
    int imgRes = R.drawable.grass;
    if (cursor != null && cursor.getCount() > 0) {
      cursor.moveToFirst();
      int createTime = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
      int waterTime = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
      int plantType = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);
      long timeNow = System.currentTimeMillis();
      long createdAt = cursor.getLong(createTime);
      long waterAt = cursor.getLong(waterTime);
      int plant = cursor.getInt(plantType);
      cursor.close();
      imgRes = PlantUtils.getPlantImageRes(this, timeNow - createdAt, timeNow - waterAt, plant);
    }

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
    int[] appWidgetIds =
        appWidgetManager.getAppWidgetIds(new ComponentName(this, PlantWidgetProvider.class));
    PlantWidgetProvider.updatePlantWidgets(this, appWidgetManager, imgRes, appWidgetIds);
  }
}
