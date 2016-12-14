package net.kazav.gabi.ble_finder;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_ENABLE_BT = 111;
    private final int REQUEST_ENABLE_LOC = 112;
    private BluetoothAdapter bta;
    private BluetoothLeScanner bts;
    private ScanSettings settings;
    private String TAG = "Scanner";
    private final String MY_ADDR = "F5:B9:30:90:73:D3";
    private CustomGauge gauge;
    private TextView txtRssi;
    private TextView txtStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bta = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        bts = bta.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        gauge = (CustomGauge) findViewById(R.id.gauge);
        txtRssi = (TextView) findViewById(R.id.rssi);
        txtStr = (TextView) findViewById(R.id.str);

        check_ble_support();
        enable_bluetooth();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scan_for_ble();
    }

    private void toast_and_quit() {
        Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
        finish();
    }

    private void check_ble_support() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast_and_quit();
        }
    }

    private void enable_bluetooth() {
        if (bta == null || !bta.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_LOC);
        }
    }

    private void scan_for_ble() {
        ArrayList<ScanFilter> filters = new ArrayList<>();
        bts.startScan(filters, settings, scb);
    }

    private void stopScan() {
        bts.stopScan(scb);
    }

    private ScanCallback scb = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult");
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: "+results.size()+" results");
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "LE Scan Failed: "+errorCode);
        }

        private void processResult(ScanResult result) {
            String cur_addr = result.getDevice().toString();
            int rssi = result.getRssi();
            Log.i(TAG, "New LE Device: " + result.getDevice().getName() + " @ " + rssi + " : " + cur_addr);
            if (Objects.equals(cur_addr, MY_ADDR)) {
                gauge.setValue(150 + rssi);
                txtRssi.setText(Integer.toString(rssi));
                String str = "Very Near!";
                if (rssi < -90) str = "Very Far..";
                else if (rssi < -80) str = "Far.";
                else if (rssi < -70) str = "Medium";
                else if (rssi < -60) str = "Near.";
                txtStr.setText(str);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode ==REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Canceled!");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_LOC: {
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    toast_and_quit();
                }
            }
        }
    }

}
