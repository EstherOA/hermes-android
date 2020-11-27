package com.example.hermes_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.layers.TransitionOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.division;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toNumber;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

/**
 * Use GeoJSON and circle layers to visualize point data as circle clusters.
 */
public class MainActivity extends AppCompatActivity implements MapboxMap.OnMapClickListener{

    private static final String MARKER_IMAGE_ID = "MARKER-IMAGE-ID";
    private static final String GEOJSON_SOURCE_ID = "PROPERTIES";
    private static final String MARKER_LAYER_ID = "UNCLUSTERED";
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FeatureCollection featureCollection;
    private String properties;

    private class Address {
        private Properties properties;
        private Geometry geometry;

        private class Properties {
            private String id;
            private String name;
            private String street;
            private String house_no;
            private String validated;
        }
        private class Geometry {
            private String type;
            private double[] coordinates;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_home);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap map) {

                mapboxMap = map;

                map.setStyle(Style.LIGHT, new Style.OnStyleLoaded() {
                    @SuppressLint("UseCompatLoadingForDrawables")
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        // Disable any type of fading transition when icons collide on the map. This enhances the visual
                        // look of the data clustering together and breaking apart.
                        style.setTransition(new TransitionOptions(0, 0, false));

                        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                                5.68890741, -0.18367902), 8));

                        initFeatureCollection();
                        mapboxMap.addOnMapClickListener(MainActivity.this);
                        addClusteredGeoJsonSource(style);
                        setUpImage(style);
                        setUpMarkerLayer(style);
                        Toast.makeText(MainActivity.this, R.string.zoom_map_in_and_out_instruction,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        return handleClickIcon(mapboxMap.getProjection().toScreenLocation(point));
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
            Log.e("Error: MainActivity", e.getMessage());
        }
        return "";
    }

    //check connectivity status
    //check cache
    //update server and refetch route
    //update cache

    private boolean handleClickIcon(PointF screenPoint) {
        boolean success = false;
        try {
            List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, MARKER_LAYER_ID);
            Timber.d("Features: %s", features);
            if (!features.isEmpty()) {
                Feature feature = features.get(0);

                // Ensure the feature has properties defined
                if (feature.properties() != null) {
                    Timber.d("Geometry: %s", feature.geometry());
                    JSONObject geom = new JSONObject(feature.geometry().toJson());
                    double longitude = geom.getJSONArray("coordinates").getDouble(0);
                    double latitude = geom.getJSONArray("coordinates").getDouble(1);
                    Intent intent = new Intent(this, OfflineNavigatorActivity.class);
          intent.putExtra("longitude", longitude);
          intent.putExtra("latitude", latitude);
          startActivity(intent);
                }
                success = true;
            }
        } catch (Exception e) {
            Timber.e("Error: %s", e.getMessage());
        }
        return success;
    }

    /**
     * Create sample data to use for both the {@link CircleLayer} and
     * {@link SymbolLayer}.
     */
    private void initFeatureCollection() {
        this.properties = readJSON("data.json");
        if(this.properties.isEmpty()) return;
        try {
            Random rand = new Random();
            Address[] propArray = new GsonBuilder().create().fromJson(this.properties, Address[].class);
            Point[] waypoints = new Point[100];
            for(int i = 0; i < propArray.length; i++) {
                waypoints[i] = Point.fromLngLat(propArray[i].geometry.coordinates[0], propArray[i].geometry.coordinates[1]);
            }

            fetchNextCachedRoute(waypoints);
//            List<Feature> markerCoordinates = new ArrayList<>();
//
//            for(int i = 0; i < propArray.length; i++) {
//                Feature feature = Feature.fromGeometry(
//                        Point.fromLngLat(propArray[i].geometry.coordinates[0], propArray[i].geometry.coordinates[1]));
//                feature.addStringProperty("ID", "1");
//                feature.addStringProperty("VALIDATED", propArray[i].properties.validated);
//                feature.addStringProperty("STREET", propArray[i].properties.street);
//                feature.addStringProperty("HOUSE_NO", propArray[i].properties.house_no);
//            }
//            featureCollection = FeatureCollection.fromFeatures(markerCoordinates);
        } catch (Exception e) {
            Timber.e("Error while creating clusters: %s", e.getMessage());
        }
    }

    private void setUpImage(@NonNull Style loadedStyle) {
        loadedStyle.addImage(MARKER_IMAGE_ID, BitmapFactory.decodeResource(
                this.getResources(), R.drawable.red_marker));
    }

    private void setUpMarkerLayer(@NonNull Style loadedStyle) {
        loadedStyle.addLayer(new SymbolLayer(MARKER_LAYER_ID, GEOJSON_SOURCE_ID)
                .withProperties(
                        iconImage(MARKER_IMAGE_ID),
                        iconAllowOverlap(true),
                        iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                        iconSize(literal(0.4))
                ).withFilter(has("id")));
    }

    private void fetchNextCachedRoute(Point[] waypoints) {

        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(waypoints[0])
                .destination(waypoints[waypoints.length-1])
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .addWaypoint(waypoints[1])
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d("MainActivity", "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e("MainActivity", "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e("MainActivity", "No routes found");
                            return;
                        }

                        final DirectionsRoute cachedRoute = response.body().routes().get(0);
                        Timber.d("Route: %s", cachedRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e("MainActivity", "Error: " + throwable.getMessage());
                    }
                });

    }

    private void addClusteredGeoJsonSource(@NonNull Style loadedMapStyle) {

// Add a new source from the GeoJSON data and set the 'cluster' option to true.
        try {
            loadedMapStyle.addSource(
// Point to GeoJSON data. This example visualizes all M1.0+ earthquakes from
// 12/22/15 to 1/21/16 as logged by USGS' Earthquake hazards program.
                    new GeoJsonSource(GEOJSON_SOURCE_ID,
                            new URI("asset://data.json"),
                            new GeoJsonOptions()
                                    .withCluster(true)
                                    .withClusterMaxZoom(14)
                                    .withClusterRadius(50)
                    )
            );
        } catch (Exception e) {
            Timber.e("Check the URL %s", e.getMessage());
        }

        int[][] layers = new int[][] {
                new int[] {150, ContextCompat.getColor(this, R.color.mapboxRed)},
                new int[] {20, ContextCompat.getColor(this, R.color.mapboxGreen)},
                new int[] {0, ContextCompat.getColor(this, R.color.mapbox_blue)}
        };

        for (int i = 0; i < layers.length; i++) {
//Add clusters' circles
            CircleLayer circles = new CircleLayer("cluster-" + i, GEOJSON_SOURCE_ID);
            circles.setProperties(
                    circleColor(layers[i][1]),
                    circleRadius(18f)
            );

            Expression pointCount = toNumber(get("point_count"));

// Add a filter to the cluster layer that hides the circles based on "point_count"
            circles.setFilter(
                    i == 0
                            ? all(has("point_count"),
                            gte(pointCount, literal(layers[i][0]))
                    ) : all(has("point_count"),
                            gte(pointCount, literal(layers[i][0])),
                            lt(pointCount, literal(layers[i - 1][0]))
                    )
            );
            loadedMapStyle.addLayer(circles);
        }

//Add the count labels
        SymbolLayer count = new SymbolLayer("count", GEOJSON_SOURCE_ID);
        count.setProperties(
                textField(Expression.toString(get("point_count"))),
                textSize(12f),
                textColor(Color.WHITE),
                textIgnorePlacement(true),
                textAllowOverlap(true)
        );
        loadedMapStyle.addLayer(count);
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
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

