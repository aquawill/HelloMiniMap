package com.fencynavi.minimap;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.GestureType;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapCameraObserver;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.mapview.WatermarkPlacement;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private PermissionsRequestor permissionsRequestor;
    private MapView primaryMapView;
    private MapView secondaryMapView;
    private MapCamera primaryMapCamera;
    private MapCamera secondaryMapCamera;
    private MapPolyline visibleAreaBorderMapPolyline;
    private short colorRed = 32767;
    private short colorGreen = 0;
    private short colorBlue = 0;
    //    private short colorAlpha = 14400;
    private Color VisibleAreaBorderColor = new Color(colorRed, colorGreen, colorBlue);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a MapView instance from the layout.
        primaryMapView = findViewById(R.id.map_view);
        secondaryMapView = findViewById(R.id.secondary_map_view);
        primaryMapView.onCreate(savedInstanceState, "primaryMapView");
        primaryMapView.setWatermarkPosition(WatermarkPlacement.BOTTOM_LEFT, 0);
        secondaryMapView.onCreate(savedInstanceState, "secondaryMapView");
        primaryMapCamera = primaryMapView.getCamera();
        secondaryMapCamera = secondaryMapView.getCamera();
        for (GestureType gestureType : GestureType.values()) {
            secondaryMapView.getGestures().disableDefaultAction(gestureType);
        }
        primaryMapView.setOnReadyListener(new MapView.OnReadyListener() {
            @Override
            public void onMapViewReady() {
                // This will be called each time after this activity is resumed.
                // It will not be called before the first map scene was loaded.
                // Any code that requires map data may not work as expected.
                Log.d(TAG, "HERE Rendering Engine attached.");
            }
        });

        handleAndroidPermissions();
    }

    private void handleAndroidPermissions() {
        permissionsRequestor = new PermissionsRequestor(this);
        permissionsRequestor.request(new PermissionsRequestor.ResultListener() {

            @Override
            public void permissionsGranted() {
                loadMapScene();
            }

            @Override
            public void permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults);
    }

    GeoPolyline getVisibleBorder(MapCamera mapCamera) {
        if (mapCamera.getBoundingBox() != null) {
            double visibleAreaLatitudeNorth = mapCamera.getBoundingBox().northEastCorner.latitude;
            double visibleAreaLongitudeEast = mapCamera.getBoundingBox().northEastCorner.longitude;
            double visibleAreaLatitudeSouth = mapCamera.getBoundingBox().southWestCorner.latitude;
            double visibleAreaLongitudeWest = mapCamera.getBoundingBox().southWestCorner.longitude;
            GeoCoordinates visibleAreaNorthWestGeoCoordinate = new GeoCoordinates(visibleAreaLatitudeNorth, visibleAreaLongitudeWest);
            GeoCoordinates visibleAreaSouthEastGeoCoordinate = new GeoCoordinates(visibleAreaLatitudeSouth, visibleAreaLongitudeEast);
            List<GeoCoordinates> geoCoordinatesList = new ArrayList<>();
            geoCoordinatesList.add(visibleAreaNorthWestGeoCoordinate);
            geoCoordinatesList.add(mapCamera.getBoundingBox().southWestCorner);
            Log.d(TAG, "distance 1: " + visibleAreaNorthWestGeoCoordinate.distanceTo(mapCamera.getBoundingBox().southWestCorner));
            geoCoordinatesList.add(visibleAreaSouthEastGeoCoordinate);
            Log.d(TAG, "distance 2: " + mapCamera.getBoundingBox().southWestCorner.distanceTo(visibleAreaSouthEastGeoCoordinate));
            geoCoordinatesList.add(mapCamera.getBoundingBox().northEastCorner);
            Log.d(TAG, "distance 3: " + visibleAreaSouthEastGeoCoordinate.distanceTo(mapCamera.getBoundingBox().northEastCorner));
            geoCoordinatesList.add(visibleAreaNorthWestGeoCoordinate);
            Log.d(TAG, "distance 4: " + mapCamera.getBoundingBox().northEastCorner.distanceTo(visibleAreaNorthWestGeoCoordinate));
            try {
                return new GeoPolyline(geoCoordinatesList);
            } catch (InstantiationErrorException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private void loadMapScene() {

        MapScheme mapScheme = MapScheme.NORMAL_DAY;
        // Load a scene from the HERE SDK to render the map with a map scheme.
        primaryMapView.getMapScene().loadScene(mapScheme, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapError mapError) {
                if (mapError == null) {
                    double distanceInMeters = 1000 * 10;
                    primaryMapCamera.lookAt(new GeoCoordinates(25.039883, 121.513043), distanceInMeters);
                    secondaryMapView.getMapScene().loadScene(mapScheme, new MapScene.LoadSceneCallback() {
                        @Override
                        public void onLoadScene(@Nullable MapError mapError) {

                        }
                    });
                } else {
                    Log.d(TAG, "Loading map failed: mapError: " + mapError.name());
                }
            }
        });

        primaryMapCamera.addObserver(new MapCameraObserver() {
            @Override
            public void onCameraUpdated(MapCamera.State state) {
                MapCamera.OrientationUpdate orientationUpdate = new MapCamera.OrientationUpdate(state.targetOrientation.bearing, 0.0);
                secondaryMapCamera.lookAt(primaryMapCamera.getState().targetCoordinates, orientationUpdate, primaryMapCamera.getState().distanceToTargetInMeters * 4);
//                GeoPolyline geoPolyline = getVisibleBorder(primaryMapCamera);
//                if (visibleAreaBorderMapPolyline == null) {
//                    if (geoPolyline != null) {
//                        visibleAreaBorderMapPolyline = new MapPolyline(geoPolyline, 4, VisibleAreaBorderColor);
//                        secondaryMapView.getMapScene().addMapPolyline(visibleAreaBorderMapPolyline);
//                    }
//                } else {
//                    if (geoPolyline != null) {
//                        visibleAreaBorderMapPolyline.updateGeometry(geoPolyline);
//                    } else {
//                        visibleAreaBorderMapPolyline = null;
//                        primaryMapView.getMapScene().removeMapPolyline(visibleAreaBorderMapPolyline);
//                    }
//                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        primaryMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        primaryMapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        primaryMapView.onDestroy();
    }
}
