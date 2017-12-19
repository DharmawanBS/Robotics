package com.dharmawan.fp;
import android.content.BroadcastReceiver;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static java.lang.Math.floor;

/**
 * Created by dharmawan on 11/24/17.
 */

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener,SensorEventListener  {

    private boolean NO_BT = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_SETTINGS = 3;

    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_STATE_CHANGE = 2;

    public static final String TOAST = "toast";
    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager.WakeLock mWakeLock;
    private NXT mNXTTalker;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;
    private int mState = NXT.STATE_NONE;
    private int mSavedState = NXT.STATE_NONE;
    private boolean mNewLaunch = true;
    private String mDeviceAddress = null;
    private Button mConnectButton;
    private Button mDisconnectButton;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private int mPower;

    private boolean mReverse;
    private boolean mReverseLR;
    private boolean mRegulateSpeed;
    private boolean mSynchronizeMotors;
    private double left=0, right=0;
    private ArrayList<SensorData> cv_pollData;
    private RobotThread robotThread;
    final byte[] sensor = {Sensor_nxt.touch, Sensor_nxt.sound_db, Sensor_nxt.light_active, Sensor_nxt.touch};
    private Sensor_value value;

    private String show;
    private TextView status;
    private TextView tr_cahaya;
    private TextView cahaya;
    private TextView tr_suara;
    private TextView suara;
    private TextView orientation;
    private EditText tr_cahaya_min;
    private EditText tr_cahaya_max;
    private EditText tr_suara_val;
    private SeekBar power_seek;
    private LinearLayout background;
    private boolean vibrate;
    private int color;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        readPreferences(prefs, null);
        prefs.registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState != null) {
            mNewLaunch = false;
            mDeviceAddress = savedInstanceState.getString("device_address");
            if (mDeviceAddress != null) {
                mSavedState = NXT.STATE_CONNECTED;
            }

            if (savedInstanceState.containsKey("power")) {
                mPower = savedInstanceState.getInt("power");
            }
        }

        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "NXT Remote Control");

        if (!NO_BT) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        mNXTTalker = new NXT(mHandler);

        set_action();

        displayState();

        value = new Sensor_value();
        mPower = Sensor_nxt.power_jalan;
        vibrate = false;
        color = Color.WHITE;
        show = Sensor_nxt.start_string;
        status.setText(show);
        background.setBackgroundColor(color);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
        double now = floor(last_y);

        if(now >= -1 && now <= 1){ //lurus
            left=1;
            right=1;
            orientation.setText("Orientasi : Lurus");
        }else if(now > 1){  // belok kanan
            left = 1;
            right = 0.5;
            orientation.setText("Orientasi : Kanan");
        }else if(now < -1){  // belok kiri
            right =  1;
            left = 0.5;
            orientation.setText("Orientasi : Kiri");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void set_action() {
        status = findViewById(R.id.tv_status);
        tr_cahaya = findViewById(R.id.tv_cahaya_tr);
        tr_cahaya.setText("Value threshold cahaya :");
        cahaya = findViewById(R.id.tv_cahaya);
        cahaya.setVisibility(View.GONE);
        tr_suara = findViewById(R.id.tv_suara_tr);
        tr_suara.setText("Value threshold suara :");
        suara = findViewById(R.id.tv_suara);
        suara.setVisibility(View.GONE);
        orientation = findViewById(R.id.tv_orientation);
        tr_cahaya_min = findViewById(R.id.et_cahaya_tr_min);
        tr_cahaya_min.setText(String.valueOf(Sensor_nxt.min_red));
        tr_cahaya_max = findViewById(R.id.et_cahaya_tr_max);
        tr_cahaya_max.setText(String.valueOf(Sensor_nxt.max_red));
        tr_suara_val = findViewById(R.id.et_suara_tr);
        tr_suara_val.setText(String.valueOf(Sensor_nxt.min_suara));

        background = findViewById(R.id.ll_background);

        power_seek = findViewById(R.id.seekBar);
        power_seek.setProgress(mPower);
        power_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPower = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        ImageButton buttonUp = findViewById(R.id.btn_forward);
        buttonUp.setOnTouchListener(new DirectionButtonOnTouchListener(1));
        ImageButton buttonDown = findViewById(R.id.btn_backward);
        buttonDown.setOnTouchListener(new DirectionButtonOnTouchListener(-1));
        mConnectButton = findViewById(R.id.btn_connect);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NO_BT) {
                    findBrick();
                } else {
                    mState = NXT.STATE_CONNECTED;
                    suara.setVisibility(View.VISIBLE);
                    cahaya.setVisibility(View.VISIBLE);
                    displayState();
                }
            }
        });

        mDisconnectButton = findViewById(R.id.btn_dc);
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                suara.setVisibility(View.GONE);
                cahaya.setVisibility(View.GONE);
                robotThread.terminate();
                mNXTTalker.stop();
                status.setText(Sensor_nxt.start_string);
            }
        });
    }

    class RobotThread implements Runnable {
        private Thread t;
        private String threadName;
        private volatile boolean running;

        RobotThread(String threadName) {
            this.threadName = threadName;
            running = true;
        }

        void terminate() {
            Log.e("state","terminated");
            mNXTTalker.cf_setInputMode(0x06, 0x02);
            running = false;
        }

        public void run() {
            try {
                while(running) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mState == NXT.STATE_CONNECTED) {
                                for(int i=0; i<4; i++) {
                                    SensorData lv_mpd = cv_pollData.get(i);
                                    if(lv_mpd.isActive()) {
                                        switch (lv_mpd.getPort()) {
                                            case 1:
                                                int val = mNXTTalker.cf_getInputValues(Sensor_nxt.port1);
                                                lv_mpd.setValue(val);
                                                value.setTouch1(val);
                                                if (val == 1) {
                                                    show = Sensor_nxt.depan_nabrak;
                                                    if (color != Color.RED) color = Color.RED;
                                                    vibrate = true;
                                                }
                                                else show = "";
                                                Log.d("value touch1",String.valueOf(val));
                                                break;
                                            case 2:
                                                int val1 = mNXTTalker.cf_getInputValues(Sensor_nxt.port2);
                                                lv_mpd.setValue(val1);
                                                value.setSound(val1);
                                                if (val1 > Sensor_nxt.min_suara) {
                                                    show = show + Sensor_nxt.diklakson;
                                                    if (color != Color.RED) color = Color.YELLOW;
                                                    vibrate = true;
                                                }
                                                else show = show + "";
                                                suara.setText("Value suara : "+val1);
                                                Log.d("value sound",String.valueOf(val1));
                                                break;
                                            case 3:
                                                int val2 = mNXTTalker.cf_getInputValues(Sensor_nxt.port3);
                                                lv_mpd.setValue(val2);
                                                value.setLight(val2);
                                                if (is_red()) {
                                                    show = show + Sensor_nxt.lampu_merah;
                                                    if (color != Color.RED) color = Color.RED;
                                                }
                                                else show = show + "";
                                                cahaya.setText("Value cahaya : "+val2);
                                                Log.d("value light",String.valueOf(val2));
                                                break;
                                            case 4:
                                                int val3 = mNXTTalker.cf_getInputValues(Sensor_nxt.port4);
                                                lv_mpd.setValue(val3);
                                                value.setTouch2(val3);
                                                if (val3 == 1) {
                                                    show = show + Sensor_nxt.belakang_nabrak;
                                                    if (color != Color.RED) color = Color.RED;
                                                    vibrate = true;
                                                }
                                                else show = show + "";
                                                status.setText(show);
                                                Log.d("value touch2",String.valueOf(val3));
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                    else {
                                        cv_pollData.get(i).setValue(0);
                                    }
                                }
                                if (vibrate) {
                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    if (v != null) {
                                        v.vibrate(500);
                                    }
                                }
                                background.setBackgroundColor(color);
                                color = Color.WHITE;
                                vibrate = false;
                                if (!(tr_suara_val.getText().toString()).equals("")) {
                                    Sensor_nxt.min_suara = Integer.parseInt(tr_suara_val.getText().toString());
                                }
                                if (!(tr_cahaya_min.getText().toString()).equals("")) {
                                    Sensor_nxt.min_red = Integer.parseInt(tr_cahaya_min.getText().toString());
                                }
                                if (!(tr_cahaya_max.getText().toString()).equals("")) {
                                    Sensor_nxt.max_red = Integer.parseInt(tr_cahaya_max.getText().toString());
                                }
                            }
                            else {
                                terminate();
                            }
                        }
                    });
                    Thread.sleep(1000);
                }
            }
            catch(InterruptedException e) {
                System.out.println(e.toString());
                running = false;
            }
        }

        void start() {
            if(t == null) {
                t = new Thread(this, threadName);
                t.start();
                suara.setVisibility(View.VISIBLE);
                cahaya.setVisibility(View.VISIBLE);
            }
        }
    }

    private void listSensor() {
        mNXTTalker.cf_setInputMode(Sensor_nxt.touch, Sensor_nxt.port1);
        mNXTTalker.cf_setInputMode(Sensor_nxt.sound_db, Sensor_nxt.port2);
        mNXTTalker.cf_setInputMode(Sensor_nxt.light_active, Sensor_nxt.port3);
        mNXTTalker.cf_setInputMode(Sensor_nxt.touch, Sensor_nxt.port4);

        cv_pollData = new ArrayList<>();
        for(int i=0; i<4; i++) {
            cv_pollData.add(new SensorData(true,sensor[i],i+1));
        }
    }

    private boolean is_red() {
        return value.getLight() >= Sensor_nxt.min_red && value.getLight() <= Sensor_nxt.max_red;
    }

    private class DirectionButtonOnTouchListener implements View.OnTouchListener {
        int pengali;
        int temp;

        DirectionButtonOnTouchListener(int reverse) {
            pengali = reverse;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                if ((value.getTouch1() == 1 || is_red()) && pengali == 1) temp = 0;
                else if ((value.getTouch2() == 1 || is_red()) && pengali == -1) temp = 0;
                else temp = 1;
                byte power = (byte) (temp*pengali*mPower);
                byte l = (byte) (power* left);
                byte r = (byte) (power* right);
                if (!mReverseLR) {
                    mNXTTalker.motors2(l, r, mRegulateSpeed, mSynchronizeMotors);
                } else {
                    mNXTTalker.motors2(r, l, mRegulateSpeed, mSynchronizeMotors);
                }
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
                mNXTTalker.motors2((byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            }
            return true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!NO_BT) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
            else {
                if (mSavedState == NXT.STATE_CONNECTED) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    mNXTTalker.connect(device);
                }
                else {
                    if (mNewLaunch) {
                        mNewLaunch = false;
                        findBrick();
                    }
                }
            }
        }
    }

    private void findBrick() {
        Intent intent = new Intent(this, ChooseActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    findBrick();
                } else {
                    Toast.makeText(this, "Bluetooth not enabled, exiting.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(ChooseActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mDeviceAddress = address;
                    mNXTTalker.connect(device);
                }
                break;
            case REQUEST_SETTINGS:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mState == NXT.STATE_CONNECTED) {
            outState.putString("device_address", mDeviceAddress);
        }
        outState.putInt("power", mPower);
    }

    private void displayState() {
        switch (mState){
            case NXT.STATE_NONE:
                mConnectButton.setVisibility(View.VISIBLE);
                mDisconnectButton.setVisibility(View.GONE);
                setProgressBarIndeterminateVisibility(false);
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                break;
            case NXT.STATE_CONNECTING:
                mConnectButton.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.GONE);
                setProgressBarIndeterminateVisibility(true);
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
                break;
            case NXT.STATE_CONNECTED:
                mConnectButton.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.VISIBLE);
                setProgressBarIndeterminateVisibility(false);
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
                listSensor();
                robotThread = new RobotThread("robotThread");
                robotThread.start();
                break;
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_STATE_CHANGE:
                    mState = msg.arg1;
                    displayState();
                    break;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        mSavedState = mState;
        mNXTTalker.stop();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        readPreferences(sharedPreferences, key);
    }

    private void readPreferences(SharedPreferences prefs, String key) {
        if (key == null) {
            mReverse = prefs.getBoolean("PREF_SWAP_FWDREV", false);
            mReverseLR = prefs.getBoolean("PREF_SWAP_LEFTRIGHT", false);
            mRegulateSpeed = prefs.getBoolean("PREF_REG_SPEED", false);
            mSynchronizeMotors = prefs.getBoolean("PREF_REG_SYNC", false);
            if (!mRegulateSpeed) {
                mSynchronizeMotors = false;
            }
        } else if (key.equals("PREF_SWAP_FWDREV")) {
            mReverse = prefs.getBoolean("PREF_SWAP_FWDREV", false);
        } else if (key.equals("PREF_SWAP_LEFTRIGHT")) {
            mReverseLR = prefs.getBoolean("PREF_SWAP_LEFTRIGHT", false);
        } else if (key.equals("PREF_REG_SPEED")) {
            mRegulateSpeed = prefs.getBoolean("PREF_REG_SPEED", false);
            if (!mRegulateSpeed) {
                mSynchronizeMotors = false;
            }
        } else if (key.equals("PREF_REG_SYNC")) {
            mSynchronizeMotors = prefs.getBoolean("PREF_REG_SYNC", false);
        }
    }
}
