package com.example.hermes_android;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.turf.TurfAssertions;
import com.mapbox.turf.TurfConversion;
import com.mapbox.turf.TurfMeasurement;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.geojson.Point.fromLngLat;
import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.switchCase;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.turf.TurfMeasurement.distance;

public class OfflineNavigatorActivity extends AppCompatActivity implements MapboxMap.OnMapClickListener, PermissionsListener, MapboxMap.OnMapLongClickListener {

    private static final String PURPLE_MARKER_ID = "PURPLE-MARKER-ID";
    private static final String BLUE_MARKER_ID = "BLUE-MARKER-ID";
    private static final String YELLOW_MARKER_ID = "YELLOW-MARKER-ID";
    private static final String GEOJSON_SOURCE_ID = "PROPERTIES";
    private static final String MARKER_LAYER_ID = "PROPERTY-LAYER";
    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Point origin;
    private Point destination;
    private Button button;
    private Point currentLocation;
    private LocationEngine locationEngine;
    private OfflineNavigatorActivityLocationCallback callback =
            new OfflineNavigatorActivityLocationCallback(this);
    private FeatureCollection routeCollection;
    private int currentLeg = 0;
    private boolean markerSelected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        provider = LocationManager.GPS_PROVIDER;
//         Location location = locationManager.getLastKnownLocation(provider);
//
//        onLocationChanged(location);


// Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

// This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_offline_navigator);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap map) {
                mapboxMap = map;

                map.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
// Map is set up and the style has loaded. Now you can add data or make other map adjustments.
                        setUpMarkerLayer(style);
                        setUpImages(style);
                        mapboxMap.addOnMapClickListener(OfflineNavigatorActivity.this);
                        mapboxMap.addOnMapLongClickListener(OfflineNavigatorActivity.this);
                        button = findViewById(R.id.startButton);
                        getCachedRoute();
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startNavigation();
                            }
                        });
                        Toast.makeText(OfflineNavigatorActivity.this, R.string.tap_on_marker_instruction,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public String readJSON(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null){
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            return stringBuilder.toString();
        } catch (Exception e) {
            Timber.e("Error while reading file: %s", e.getMessage());
            Log.e("Error: OfflineNavigator", e.getMessage());
        }
        return "";
    }


    //read route object from sqlite
    private void getCachedRoute() {
        String json = readJSON( "example-route.json");
        List<Feature> routeFeatures = new ArrayList<>();
        Route[] cachedRoute = new GsonBuilder().create().fromJson(json, Route[].class);
        Route.Legs[] legs = cachedRoute[0].legs;
        for(Route.Legs leg : legs) {
            for(Route.Legs.Steps step : leg.steps) {
                List <Point> routePoints = new ArrayList<>();
                for(double[] coords : step.geometry.coordinates) {
                    routePoints.add(Point.fromLngLat(coords[0], coords[1]));
                }
                routeFeatures.add(Feature.fromGeometry(LineString.fromLngLats(routePoints)));
            }
        }
        routeCollection = FeatureCollection.fromFeatures(routeFeatures);
        drawRoute();
    }

    //read markers from sqlite
    private void getPropertyMarkers() {

    }

    @Override
    public boolean onMapLongClick(@NonNull LatLng point) {
        handleLongClick(point);
        return true;
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

// Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS);

// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    private static class OfflineNavigatorActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<OfflineNavigatorActivity> activityWeakReference;

        OfflineNavigatorActivityLocationCallback(OfflineNavigatorActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            OfflineNavigatorActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }
                activity.drawProgressLine();

// Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                    activity.currentLocation = Point.fromLngLat(result.getLastLocation().getLongitude(), result.getLastLocation().getLatitude());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            OfflineNavigatorActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setUpImages(@NonNull Style loadedStyle) {
        loadedStyle.addImage(YELLOW_MARKER_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.yellow_marker));
        loadedStyle.addImage(BLUE_MARKER_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.blue_marker_view));
        loadedStyle.addImage(PURPLE_MARKER_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.purple_marker));
    }


    private void setUpMarkerLayer(@NonNull Style loadedStyle) {
        try{
            loadedStyle.addSource(new GeoJsonSource(GEOJSON_SOURCE_ID,
                    new URI("asset://cached-data.json")));
            loadedStyle.addSource(new GeoJsonSource("selected-marker"));
            loadedStyle.addLayer(new SymbolLayer(MARKER_LAYER_ID, GEOJSON_SOURCE_ID)
                    .withProperties(
                            iconImage(switchCase(
                                    has("complete"), literal(PURPLE_MARKER_ID),
                                    literal(YELLOW_MARKER_ID))),
                            iconAllowOverlap(true),
                            iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                            iconSize(literal(0.4))
                    ).withFilter(has("id")));
            loadedStyle.addLayer(new SymbolLayer("selected-marker-layer", "selected-marker")
                    .withProperties(iconImage(BLUE_MARKER_ID),
                            iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                            iconAllowOverlap(true),
                            iconSize(literal(0.5))
                            ));
        } catch (Exception e) {
            Timber.e("Error: %s", e.getMessage());
        }
    }

    //draw navigation route
    private void drawRoute() {
        try {
            Style style = mapboxMap.getStyle();
            if(style != null) {

                style.addSource(new GeoJsonSource("route-source", routeCollection));
            style.addLayer(new LineLayer("route-layer", "route-source")
            .withProperties(lineColor(Color.parseColor("#e55e5e")),lineWidth(5f),lineCap(Property.LINE_CAP_ROUND),
                    lineJoin(Property.LINE_JOIN_ROUND)));
            }
        button.setBackgroundResource(R.color.mapboxBlue);
        button.setEnabled(true);
        }catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }

    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        setCurrentMarker(point);
        return true;
//        return handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
    }

    //select start and end.
    private boolean handleClickIcon(PointF screenPoint) {
        boolean success = false;

        return success;
    }

    private boolean markPropertyComplete(Feature selectedFeature) {
        boolean success = false;
        Style style = mapboxMap.getStyle();
        if(style != null) {
            GeoJsonSource source = style.getSourceAs(GEOJSON_SOURCE_ID);
            if (source != null) {
                List<Feature> featureList = source.querySourceFeatures(has("id"));
                selectedFeature.addBooleanProperty("complete", true);
                for(int i = 0; i < featureList.size(); i++) {
                    if(featureList.get(i).id().equalsIgnoreCase(selectedFeature.id())) {
                        featureList.set(i, selectedFeature);
                    }
                }
                source.setGeoJson(FeatureCollection.fromFeatures(featureList));
                success = true;
            }
        }
        return success;

    }

    private class Route{
        private Legs[] legs;

        private class Legs {
            private Steps[] steps;

            private class Steps {
                private Geometry geometry;

                private class Geometry {
                    private String type;
                    private double[][] coordinates;
                }
            }
        }
    }

    //start navigation with progress line and camera following user
    private void startNavigation() {
        try {
            button.setBackgroundColor(getResources().getColor(R.color.mapboxRed));
            button.setText("End Navigation");
            Feature start = routeCollection.features().get(0);
            //if user location is far from start display alert
            Route.Legs.Steps.Geometry geom = new GsonBuilder().create().fromJson(start.geometry().toJson(), Route.Legs.Steps.Geometry.class);

            double distance = distance(currentLocation, Point.fromLngLat(geom.coordinates[0][0], geom.coordinates[0][1]));
            if(distance > 3) {
                AlertDialog alertDialog = new AlertDialog.Builder(OfflineNavigatorActivity.this).create();
                alertDialog.setTitle("Warning");
                alertDialog.setMessage("Your current location is far from the start point. Start anyway?");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

                //if user start navigation animate camera to start
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(geom.coordinates[0][1], geom.coordinates[0][0])) // Sets the new camera position
                        .zoom(17) // Sets the zoom
                        .tilt(30)
                        .build(); // Creates a CameraPosition from the builder

                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 4000);
            }
        } catch (Exception e) {
            Log.d("Error: ", e.getMessage());
        } }

        private void drawProgressLine() {
            Style style = mapboxMap.getStyle();
            if(style == null || style.getLayer("route-layer") == null) {
                return;
            }
            Feature start = routeCollection.features().get(currentLeg);
            Route.Legs.Steps.Geometry geom = new GsonBuilder().create().fromJson(start.geometry().toJson(), Route.Legs.Steps.Geometry.class);

            double distanceFromStart = distance(currentLocation, Point.fromLngLat(geom.coordinates[0][0], geom.coordinates[0][1]));
            double currentDistance = distance(currentLocation, Point.fromLngLat(geom.coordinates[geom.coordinates.length-1][0], geom.coordinates[geom.coordinates.length-1][1]));

            if(distanceFromStart > 3) {
//                Toast.makeText(this, "Please move closer to the starting point",
//                        Toast.LENGTH_SHORT).show();
                return;
            }
                try {
                        List <double[]>progressCoords = new ArrayList<>();
                        List <Point> routePoints = new ArrayList<>();
                        List <Feature> routeFeatures = new ArrayList<>();

                        final int nearestIndex = getNearestIndex(currentDistance, start);

                    for (int i = 0; i < geom.coordinates.length; i ++) {
                             if (i > nearestIndex) progressCoords.add(geom.coordinates[i]);
                         }

                        if(progressCoords.size() < 2) {
                            manageProperty(progressCoords.get(progressCoords.size() -1));
                            if(routeCollection.features().size() - 1 > currentLeg) {
                                currentLeg += 1;
                            }
                             return;
                         }

                        for(double[] coords : progressCoords) {
                            routePoints.add(Point.fromLngLat(coords[0], coords[1]));
                        }
                        routeFeatures.add(Feature.fromGeometry(LineString.fromLngLats(routePoints)));

                        style.addSource(new GeoJsonSource("progress-source", FeatureCollection.fromFeatures(routeFeatures)));
                        style.addLayer(new LineLayer("progress-layer", "progress-source")
                            .withProperties(lineColor(Color.parseColor("blue")),lineWidth(5f),lineCap(Property.LINE_CAP_ROUND),
                                    lineJoin(Property.LINE_JOIN_ROUND)));
                }catch (Exception e) {
                    Timber.e(e.getMessage());
                }
            }

        private int getNearestIndex(Double currentDistance, Feature leg) {
            double runningDistance = 0;
            Route.Legs.Steps.Geometry geom = new GsonBuilder().create().fromJson(leg.geometry().toJson(), Route.Legs.Steps.Geometry.class);

            for (int i = 1; i < geom.coordinates.length; i++) {
                runningDistance += distance(Point.fromLngLat(geom.coordinates[i][0], geom.coordinates[i - 1][1]), Point.fromLngLat(geom.coordinates[i - 1][0], geom.coordinates[0][i]));

                if (runningDistance >= currentDistance) {
                                  return i - 1;
                }
            }
            return -1;
        }

        private void setCurrentMarker(@NonNull LatLng point) {

            final PointF pixel = mapboxMap.getProjection().toScreenLocation(point);
            Style style = mapboxMap.getStyle();
            if(style != null) {
                List<Feature> features = mapboxMap.queryRenderedFeatures(pixel, MARKER_LAYER_ID);
                List<Feature> selectedFeature = mapboxMap.queryRenderedFeatures(
                        pixel, "selected-marker-layer");

                if (selectedFeature.size() > 0 && markerSelected) {
                    return;
                }
                if (features.isEmpty()) {
                    if (markerSelected) {
                        deselectMarker(style);
                    }
                    return;
                }
                selectMarker(style, features.get(0));
                markerSelected= true;
            }
        }

        private void selectMarker(Style style, Feature feature) {
            if (markerSelected) {
                deselectMarker(style);
            }
            GeoJsonSource source = style.getSourceAs("selected-marker");
            if (source != null) {
                source.setGeoJson(FeatureCollection.fromFeatures(
                        new Feature[]{feature}));
            }
        }

        private void deselectMarker(Style style) {
            FeatureCollection featureCollection = null;
            GeoJsonSource source = style.getSourceAs("selected-marker");
            if (source != null) {
                source.setGeoJson(featureCollection);
                markerSelected = false;
            }
        }

        private void manageProperty(@NonNull double[] point) {
            Toast.makeText(this, "Property reached",
                    Toast.LENGTH_SHORT).show();
//            setCurrentMarker(new LatLng(point[1], point[0]));
            //add listener for long click
            //show options on long click (mark as complete, not found, other)
            //call functions. If other, redirect to complaints page or generate property coordinates
        }

        private void handleLongClick(@NonNull LatLng point) {
            Style style = mapboxMap.getStyle();
            if(style == null || !markerSelected) return;
            try {
                GeoJsonSource source = style.getSourceAs("selected-marker");
                Feature selectedFeature = source.querySourceFeatures(has("id")).get(0);
                JSONObject jsonObject = new JSONObject(selectedFeature.geometry().toJson());
                JSONArray geom = jsonObject.getJSONArray("coordinates");
                if (geom.getDouble(0) == point.getLongitude() && geom.getDouble(1) == point.getLatitude()) {
                    return;
                }
                //check if the current prop has been selected else return
                PopupMenu p = new PopupMenu(OfflineNavigatorActivity.this, button);
                p.getMenuInflater().inflate(R.menu.property_options_menu, p.getMenu());
                p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        Toast.makeText(OfflineNavigatorActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                        switch (item.getItemId()) {
                            case R.id.complete:
                                //mark as complete

                                return true;
                            case R.id.missing:
                                //call api to mark address as not found
                                return true;
                            case R.id.report:
                                Intent intent = new Intent(OfflineNavigatorActivity.this, ReportPropertyActivity.class);
                                intent.putExtra("featureId", selectedFeature.id());
                                intent.putExtra("geometry", selectedFeature.geometry().toJson());
                                startActivity(intent);
                                //redirect to report page
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                p.show();
            } catch (Exception e) {
                Timber.e(e.getMessage());
            }
        }

    private void handleArrival() {
        //end navigation (show start button)
        //manage property
        //update cache (update property status, additional comments etc)
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}