package com.example.myapplication;
import static com.google.android.gms.wearable.DataMap.TAG;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


import com.example.myapplication.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MeasureService extends Service implements SensorEventListener {
    private static final String TAG = "MeasureService";
    private long startTime;
    private SensorManager manager;
    private Sensor[] mSensors;


    private static HashMap<Integer, Integer> sensorType;
    Timer sensorTimer;

    public static boolean isMeasuring;

    long before = System.currentTimeMillis();
    long after;

    ArrayList<String> accelerometerData = new ArrayList<>(Arrays.asList("0.0", "0.0", "0.0"));;
    static public  ArrayList<ArrayList<String>> accelerometerBuffer = new ArrayList<>();
    ArrayList<String> heartRateBuffer = new ArrayList<>();

    String x ="0.0";
    String y ="0.0";
    String z = "0.0";
    String hr = "0.0";


    @Override
    public void onCreate() {
        super.onCreate();
        isMeasuring = true;
        startTime = System.currentTimeMillis();
        foregroundNotification();
        initSensors();
        sensorTimer = new Timer();
        startTimer();

        // accelerometerBuffer 리스트 초기화
        accelerometerBuffer.clear();
        accelerometerBuffer.add(new ArrayList<>(accelerometerData)); // 초기 데이터 추가

        // heartRateBuffer 리스트 초기화
        heartRateBuffer.clear();
    }


    @Override
    public void onDestroy() {
        isMeasuring = false;
        sensorTimer.cancel();
        unregister();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //실질적으로 측정을 시작하는 함수
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        //여러 센서 값 가져오기
        measureSensor(sensor, event);


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    void foregroundNotification() { // foreground 실행 후 신호 전달 (안하면 앱 강제종료 됨)
        NotificationCompat.Builder builder;
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "measuring_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Measuring Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        builder.setContentTitle("측정시작")
                .setContentIntent(pendingIntent);

        startForeground(1, builder.build());
    }

    // 사용할 센서 세팅
    private void initSensors() {
        sensorType = new HashMap<Integer, Integer>();
        sensorType.put(Sensor.TYPE_HEART_RATE, 1); // Heart_rate
        sensorType.put(Sensor.TYPE_LIGHT, 1); // Light
        sensorType.put(Sensor.TYPE_ACCELEROMETER, 3); // Accelerometer
        sensorType.put(Sensor.TYPE_PRESSURE, 1); // Barometer
        sensorType.put(Sensor.TYPE_SIGNIFICANT_MOTION, 1); // Offbody
        sensorType.put(Sensor.TYPE_GYROSCOPE, 3); // Gyroscope
        manager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mSensors = new Sensor[sensorType.size()];

        int i = 0;
        Iterator<Map.Entry<Integer, Integer>> entries = sensorType.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, Integer> entry = entries.next();

            mSensors[i] = manager.getDefaultSensor(entry.getKey());
            manager.registerListener(this, mSensors[i], SensorManager.SENSOR_DELAY_NORMAL);
            i++;
        }

    }


    public void unregister() { // unregister listener
        manager.unregisterListener(this);
    }

    ArrayList<String> accel = new ArrayList<>();
    ArrayList<String> gyro = new ArrayList<>();

    public void measureSensor(Sensor sensor, SensorEvent event) {
        after = System.currentTimeMillis();
        long now = (after - before) / 1000;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = Float.toString(event.values[0]);
            y = Float.toString(event.values[1]);
            z = Float.toString(event.values[2]);

            // 가속도 데이터를 저장
            ArrayList<String> accelData = new ArrayList<>();
            accelData.add(x);
            accelData.add(y);
            accelData.add(z);
            accelerometerBuffer.add(accelData);
        } else if (sensor.getType() == Sensor.TYPE_HEART_RATE) {
            hr = Float.toString(event.values[0]);

            // 심박수 데이터를 최신 값 하나만 저장
            heartRateBuffer.clear(); // 이전 값들을 지우고
            heartRateBuffer.add(hr); // 새로운 값 하나만 저장
        }

        // 데이터를 MainActivity로 전달하는 Intent 생성
        Intent dataIntent = new Intent("ACTION_DATA_RECEIVED");
        dataIntent.putExtra("ACCELEROMETER_DATA", accelerometerBuffer);
        dataIntent.putExtra("HEART_RATE_DATA", heartRateBuffer);

        // MainActivity로 데이터 전달
        sendBroadcast(dataIntent);

        count += now;
        before = after;
        }

    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACTION_DATA_RECEIVED")) {
                ArrayList<ArrayList<String>> accelerometerData = (ArrayList<ArrayList<String>>)
                        intent.getSerializableExtra("ACCELEROMETER_DATA");
                ArrayList<String> heartRateData = intent.getStringArrayListExtra("HEART_RATE_DATA");

                // Logcat에 데이터 출력
                Log.d("Data", "Accelerometer Data: " + accelerometerData.toString());
                Log.d("Data", "Heart Rate Data: " + heartRateData.toString());

                // 데이터를 직접 처리하는 로직 추가
            }
        }
    };

    float count = 0;

    private void startTimer() {

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // 타이머 동작 시 데이터를 저장
                // ...

                // 데이터를 MainActivity로 전달하는 Intent 생성
                Intent dataIntent = new Intent("ACTION_DATA_RECEIVED");
                dataIntent.putExtra("ACCELEROMETER_DATA", accelerometerBuffer);
                dataIntent.putExtra("HEART_RATE_DATA", heartRateBuffer);

                // MainActivity로 데이터 전달
                sendBroadcast(dataIntent);
            }
        };
        Timer timer = new Timer();
        // 타이머 주기를 심박수 데이터 수집 주기로 설정 (예: 500ms)
        timer.schedule(task, 0, 5000); // 500ms마다 실행하도록 설정

    }


}