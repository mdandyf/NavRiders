package navriders.stuttgart.uni.com.example.mdand.navriders;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.R.id.list;

public class BLEConnectionManager extends MainActivity {

    private BluetoothAdapter BA;
    private Set<BluetoothDevice> scanDevices;
    private BluetoothLeScanner BLE;
    private BluetoothGatt BGATT; // Device Left
    private BluetoothGatt BGATT2; // Device Right
    private ListView listBluetooth;
    private ScanResult result_view;
    private ArrayList<String> StringArray;
    private float deviceCounter;
    private boolean deviceScanned1;
    private boolean deviceScanned2;

    private Button buttonBluetoothOn;
    private Button buttonBluetoothConnect;
    private HashSet<String> triedDevices;
    private TextView mTextMessage;
    private TextView textViewConnected1;
    private TextView textViewConnected2;

    private static final UUID SERVICE_VIBROTACTILE = UUID.fromString("00001802-0000-1000-8000-00805F9B34FB");
    private static final UUID SERVICE_BATTERY = UUID.fromString("0000FEE0-0000-1000-8000-00805F9B34FB");

    private static final UUID SERVICE_VIBRATE_ON = UUID.fromString("00002A06-0000-1000-8000-00805F9B34FB");
    private static final UUID SERVICE_VIBRATE_OFF = UUID.fromString("00002A06-0000-1000-8000-00805F9B34FB");
    private static final UUID SERVICE_BATTERY_READING = UUID.fromString("0000FF0C-0000-1000-8000-00805F9B34FB");
    private static final String MAC_ADDRESS_1 = "C8:0F:10:91:3D:36";
    private static final String MAC_ADDRESS_2 = "C8:0F:10:91:3D:37";

    private static final int PERMISSION_REQUEST_ACCESS_COARSE_LOCATION = 1;

    @Override
    public void onStop() {
        super.onStop();
        disconnect();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        buttonBluetoothOn = (Button) findViewById(R.id.buttonBluetoothOn);
        buttonBluetoothConnect = (Button) findViewById(R.id.buttonBluetoothConnect);

        mTextMessage = (TextView) findViewById(R.id.message);
        textViewConnected1 = (TextView) findViewById(R.id.textViewConnected1);
        textViewConnected2 = (TextView) findViewById(R.id.textViewConnected2);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        BA = BluetoothAdapter.getDefaultAdapter();
        BLE = BA.getBluetoothLeScanner();

    }

    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_ACCESS_COARSE_LOCATION);
                return;
            }
        }

        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        }

        //scanBluetooth();

   }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //scanBluetooth();
                } else {
                    Toast.makeText(this, "Location access is required to scan for Bluetooth devices.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    //mTextMessage.setText(R.string.title_home);
                    startActivity(new Intent(BLEConnectionManager.this, MainActivity.class));
                    return true;
                case R.id.navigation_dashboard:
                    //mTextMessage.setText(R.string.title_dashboard);
                    startActivity(new Intent(BLEConnectionManager.this, BLEConnectionManager.class));
                    return true;
                case R.id.navigation_notifications:
                    startActivity(new Intent(BLEConnectionManager.this, MapsActivity.class));
                    //mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };



    public void bluetoothOn(View v) {

        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }

        scanBluetooth(v);

    }

    public void bluetoothOff(View v) {

        if (!BA.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Already off", Toast.LENGTH_LONG).show();
        } else {
            BA.disable();
            Toast.makeText(getApplicationContext(), "Turned off", Toast.LENGTH_LONG).show();
        }

    }

    public void writeVibrateOn(View v) {

        if(BGATT == null) {
            Toast.makeText(getApplicationContext(), "Device is not yet connected", Toast.LENGTH_LONG).show();
        } else {
            setVibrationDevice("on");
        }
    }

    public void writeVibrateOff(View v) {
        setVibrationDevice("off");
    }

    public void writeVibrateOnContinous(View v) {
        setVibrationDevice("on_continous");
    }

    public void setVibrationDevice(String condition) {

        BluetoothGattCharacteristic characteristic = BGATT.getService(SERVICE_VIBROTACTILE).getCharacteristic(SERVICE_VIBRATE_ON);
        BGATT.setCharacteristicNotification(characteristic, true);
        byte[] vibrationCommand = {0X0};
        switch (condition) {
            case "off":
                characteristic.setValue(vibrationCommand);
                BGATT.writeCharacteristic(characteristic);
                break;
            case "on":
                vibrationCommand = new byte[] {0X4};
                characteristic.setValue(vibrationCommand);
                BGATT.writeCharacteristic(characteristic);
                break;
            case "on_continous":
                vibrationCommand = new byte[]{0X2};
                characteristic.setValue(vibrationCommand);
                BGATT.writeCharacteristic(characteristic);
                break;
        }
    }

    public void setVibrationDevice2(String condition) {

        BluetoothGattCharacteristic characteristic = BGATT2.getService(SERVICE_VIBROTACTILE).getCharacteristic(SERVICE_VIBRATE_ON);
        BGATT2.setCharacteristicNotification(characteristic, true);
        byte[] vibrationCommand = {0X0};
        switch (condition) {
            case "off":
                characteristic.setValue(vibrationCommand);
                BGATT2.writeCharacteristic(characteristic);
                break;
            case "on":
                vibrationCommand = new byte[] {0X4};
                characteristic.setValue(vibrationCommand);
                BGATT2.writeCharacteristic(characteristic);
                break;
            case "on_continous":
                vibrationCommand = new byte[]{0X2};
                characteristic.setValue(vibrationCommand);
                BGATT2.writeCharacteristic(characteristic);
                break;
        }
    }

    private void scanBluetooth(View v) {

        clearBluetoothScreen();
        deviceCounter = 0;
        triedDevices = new HashSet<>();

        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        settingsBuilder.setReportDelay(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //settingsBuilder.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT);
            settingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        }
        ScanSettings scanSettings = settingsBuilder.build();

        Log.d("bt", "start scanning");
        Toast.makeText(BLEConnectionManager.this, "Start Scanning", Toast.LENGTH_SHORT).show();

        BLE.startScan(scanFilters, scanSettings, scanCallback);


    }

    private void clearBluetoothScreen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewConnected1.setText("?");
                textViewConnected2.setText("?");
            }
        });
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            connect(result);

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                connect(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(BLEConnectionManager.this, String.format("Scanning failed (%d)", errorCode), Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    private void connect(ScanResult result) {
        Log.d("bt", result.getDevice().getAddress());
        Log.d("bt", String.valueOf(result.getRssi()));
        //BLE.stopScan(scanCallback);
        if (BGATT == null) {
            synchronized (triedDevices) {
                if (!triedDevices.contains(result.getDevice().getAddress())) {
                    triedDevices.add(result.getDevice().getAddress());
                    result.getDevice().connectGatt(this, false, new MyBluetoothGattCallback());
                }
            }
        }
    }

    private void disconnect() {
        Log.d("bt", "stop");

        if (BLE != null) {
            BLE.stopScan(scanCallback);
        }

        if (BGATT != null) {
            BGATT.disconnect();
        }
        BGATT = null;
    }

    private class MyBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                if (gatt == BGATT) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(BLEConnectionManager.this, "Disconnected.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    finish();
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            boolean containsVibrotactile = false;
            boolean containsVibrotactile2 = false;

            for (BluetoothGattService service : gatt.getServices()) {
                if (service.getUuid().equals(SERVICE_VIBROTACTILE)) {
                    if(gatt.getDevice().getAddress().equals(MAC_ADDRESS_1)) {
                        containsVibrotactile = true;
                    }
                    if(gatt.getDevice().getAddress().equals(MAC_ADDRESS_2)) {
                        containsVibrotactile2 = true;
                    }

                }

            }

            if (containsVibrotactile) {
                deviceCounter++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (BGATT == null) {
                            BGATT = gatt;
                            Toast.makeText(BLEConnectionManager.this, "Connected.", Toast.LENGTH_SHORT).show();
                            textViewConnected1.setText("Connected" + " " + gatt.getDevice().getAddress());
                        }
                    }
                });
            }

            if (containsVibrotactile2) {
                deviceCounter++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (BGATT2 == null) {
                            BGATT2 = gatt;
                            Toast.makeText(BLEConnectionManager.this, "Connected.", Toast.LENGTH_SHORT).show();
                            textViewConnected2.setText("Connected" + " " + gatt.getDevice().getAddress());
                        }
                    }
                });
            }

            //if ((deviceCounter == 2) && containsVibrotactile && containsVibrotactile2) {
            if ((deviceCounter == 1) && containsVibrotactile) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BLEConnectionManager.this, "Stopping Scan", Toast.LENGTH_SHORT).show();
                    }
                });

                BLE.stopScan(scanCallback);
            }


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            parseCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            parseCharacteristic(characteristic);
        }

        public void parseCharacteristic(BluetoothGattCharacteristic characteristic) {
           if(characteristic.getUuid().equals(SERVICE_BATTERY_READING)) {

               int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
               final String batteryString = String.format(" Battery: %d", flags);
               Log.d("Battery:", ""+ batteryString);

               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       textViewConnected1.append(batteryString);
                   }
               });


           }

        }

    }


}
