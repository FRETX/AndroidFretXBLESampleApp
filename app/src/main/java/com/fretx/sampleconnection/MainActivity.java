package com.fretx.sampleconnection;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.fretx.fretxcommunication.BluetoothInterface;
import com.fretx.fretxcommunication.BluetoothLEService;
import com.fretx.fretxcommunication.BluetoothListener;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements BluetoothListener {
    private static final String TAG = "KJKP6_MAIMACTIVITY";
    private static final int NB_FRET = 5;
    private static final int NB_STRING = 6;
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 1;

    private static final byte clearBytes[] = new byte[] {0};
    private static final byte lightBytes[] = new byte[] {46,35,24,12,01,41,32,23,15,06};

    private TextView status;
    private Button action;
    private Button send;
    private boolean lightOn;

    private BluetoothInterface com;
    private BluetoothListener listener = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.status_textview);
        action = (Button) findViewById(R.id.action_button);
        send = (Button) findViewById(R.id.send_button);
        send.setVisibility(View.INVISIBLE);

        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (com != null) {
                    if (com.isConnected()) {
                        com.disconnect();
                        send.setVisibility(View.INVISIBLE);
                    } else {
                        status.setText(R.string.scanning_status);
                        com.connect();
                    }
                }
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (com != null)
                    if (lightOn) {
                        com.send(clearBytes);
                    } else {
                        com.send(lightBytes);
                    }
            }
        });

        requestRuntimePermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    launchCommunicationService();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void requestRuntimePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_COARSE_LOCATION is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            launchCommunicationService();
        }
    }

    private void launchCommunicationService() {
        final Intent intent = new Intent(this, BluetoothLEService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
                com = (BluetoothInterface) serviceBinder;
                com.registerBluetoothListener(listener);
                status.setText(R.string.scanning_status);
                com.connect();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //com.unregisterBluetoothListener(listener);
                com = null;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    public void onScanFailure(){
        Log.d(TAG, "onScanFailure");
        this.runOnUiThread(disconnectedRunnable);
        com.send(new byte[] {0});
    }

    public void onConnect(){
        Log.d(TAG, "onConnect");
        this.runOnUiThread(connectedRunnable);
    }

    public void onDisconnect(){
        Log.d(TAG, "onDisconnect");
        this.runOnUiThread(disconnectedRunnable);
    }

    public void onFailure(){
        Log.d(TAG, "onFailure");
        this.runOnUiThread(disconnectedRunnable);
    }

    private Runnable connectedRunnable = new Runnable() {
        @Override
        public void run() {
            status.setText(R.string.connected_status);
            action.setText(R.string.disconnect_action);
            send.setVisibility(View.VISIBLE);
        }
    };

    private Runnable disconnectedRunnable = new Runnable() {
        @Override
        public void run() {
            lightOn = false;
            status.setText(R.string.disconnected_status);
            action.setText(R.string.connect_action);
            send.setVisibility(View.INVISIBLE);
        }
    };
}
