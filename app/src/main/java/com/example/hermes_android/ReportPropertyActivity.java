package com.example.hermes_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.List;

public class ReportPropertyActivity extends AppCompatActivity implements PermissionsListener, LocationListener {

    private EditText report;
    private Button sendReport;
    private EditText displayCoordinates;
    private Button generateCoordinates;
    private Button cancelGenerate;
    private Point newProperty;
    private PermissionsManager permissionsManager;
    private LocationManager locationManager;
    private String provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_property);
        generateCoordinates = (Button) findViewById(R.id.generateButton);
        sendReport = (Button) findViewById(R.id.sendReportButton);
        cancelGenerate = (Button) findViewById(R.id.cancelGenerateBtn);
        displayCoordinates = (EditText) findViewById(R.id.coordinates);
        report = (EditText) findViewById(R.id.editReport);
    }

    public void generatePropertyCoordinates(View view) {
        if(generateCoordinates.getText().toString().equalsIgnoreCase("Save")) {
            //call api to save coordinates

            return;
        }
        //send alert to generate coordinates
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("");
        alertDialog.setMessage("Generate coordinates with current location?");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Confirm",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //get current location and display coordinates
                        getLocation();
                        //update button to save
                        generateCoordinates.setText("Save");
                        cancelGenerate.setVisibility(View.VISIBLE);
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void cancelGenerate(View view) {
        //clear coordinates
        displayCoordinates.setText("");

        //set generate button text
        generateCoordinates.setText(R.string.generate_coordinates_button);

        //hide cancel button
        cancelGenerate.setVisibility(View.GONE);
    }

    public void sendReport(View view) {
        String message = report.getText().toString();
        if(!message.isEmpty()) {
            Log.d("Report", message);
            Toast.makeText(ReportPropertyActivity.this, message, Toast.LENGTH_SHORT).show();
            //call api to send report
            return;
        }
        //toast to say message is required
        Toast.makeText(ReportPropertyActivity.this, "Please enter a complaint", Toast.LENGTH_SHORT).show();
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
           getLocation();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            provider = LocationManager.GPS_PROVIDER;
            Location location = locationManager.getLastKnownLocation(provider);
            newProperty = Point.fromLngLat(location.getLongitude(), location.getLatitude());
            Log.w("Longitude: ", String.valueOf(location.getLongitude()));
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }
}