package com.example.myapplication;

import static com.example.myapplication.MeasureService.accelerometerBuffer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BODY_SENSORS = 1;
    private Button startButton;
    private Button stopButton;
    private ArrayList<String> heartRateBuffer = new ArrayList<>();
    private boolean isMeasuring = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic dataCharacteristic;

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("Bluetooth", "Connected");

                // 서비스 검색
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("Bluetooth", "Disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = bluetoothGatt.getService(UUID.fromString("0000180a-0000-1000-8888-00805f9b34fb"));
                if (service != null) {
                    dataCharacteristic = service.getCharacteristic(UUID.fromString("00002a26-0000-1000-8888-00805f9b34fb"));
                }
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.start);
        stopButton = findViewById(R.id.stop);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestBodySensorsPermission();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMeasurement();
            }
        });

        // BluetoothAdapter 초기화
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        // BroadcastReceiver 등록
        IntentFilter filter = new IntentFilter("ACTION_DATA_RECEIVED");
        registerReceiver(dataReceiver, filter);
    }

    private void requestBodySensorsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    REQUEST_BODY_SENSORS);
        } else {
            startMeasurementService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BODY_SENSORS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMeasurementService();
            } else {
                // 권한이 거부된 경우 처리
                // 필요한 권한이 거부되었을 때 사용자에게 알리거나 대체 로직을 수행할 수 있습니다.
            }
        }
    }

    private void startMeasurementService() {
        if (!isMeasuring) {
            Intent serviceIntent = new Intent(this, MeasureService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            // 측정 시작 상태로 변경
            isMeasuring = true;
        }
    }

    private void stopMeasurement() {
        // 측정 중지 상태로 변경
        isMeasuring = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // BroadcastReceiver 해제
        unregisterReceiver(dataReceiver);
    }

    private void startDataProcessing(ArrayList<ArrayList<String>> accelerometerData, ArrayList<String> heartRateData) {
        // 데이터 처리 작업 수행
        // 가속도 데이터와 심박수 데이터를 활용하여 처리 로직을 작성합니다.
        // 예시로 데이터를 로그로 출력하는 부분을 작성했습니다.
        for (ArrayList<String> accelData : accelerometerData) {
            Log.d("DataProcessing", "Accelerometer Data: " + accelData.toString());
        }

        Log.d("DataProcessing", "Heart Rate Data: " + heartRateData.toString());

        // 처리 작업이 완료된 후에 필요한 처리를 수행합니다.
        // 예시로 작업 완료 후에 알림을 표시하는 부분을 작성했습니다.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showNotification("작업 완료", "데이터 처리가 완료되었습니다.");
            }
        });

        // 필요한 경우 데이터 초기화 등의 작업을 수행합니다.
        accelerometerBuffer.clear();
        heartRateBuffer.clear();
    }

    private void showNotification(String title, String message) {
        // 알림을 표시하는 로직을 구현합니다.
        // 필요한 경우 NotificationCompat.Builder를 사용하여 알림을 생성합니다.
    }

    private void sendDataToPhone(String data) {
        if (dataCharacteristic != null) {
            dataCharacteristic.setValue(data);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothGatt.writeCharacteristic(dataCharacteristic);
        }
    }

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACTION_DATA_RECEIVED")) {
                if (isMeasuring) {
                    ArrayList<ArrayList<String>> accelerometerData = (ArrayList<ArrayList<String>>)
                            intent.getSerializableExtra("ACCELEROMETER_DATA");
                    ArrayList<String> heartRateData = intent.getStringArrayListExtra("HEART_RATE_DATA");

                    // 가속도 데이터 출력
                    for (ArrayList<String> accelData : accelerometerData) {
                        Log.d("Data", "Accelerometer Data: " + accelData.toString());
                    }

                    // 심박수 데이터 출력
                    Log.d("Data", "Heart Rate Data: " + heartRateData.toString());

                    // 데이터를 처리하는 로직 추가
                    startDataProcessing(accelerometerData, heartRateData);

                    // 데이터를 폰으로 전송
                    sendDataToPhone("Your Data");
                }
            }
        }
    };
}