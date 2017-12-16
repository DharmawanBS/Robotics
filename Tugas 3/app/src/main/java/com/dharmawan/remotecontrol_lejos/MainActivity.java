package com.dharmawan.remotecontrol_lejos;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by dharmawan on 11/24/17.
 */

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

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

    private int mState = NXT.STATE_NONE;
    private int mSavedState = NXT.STATE_NONE;
    private boolean mNewLaunch = true;
    private String mDeviceAddress = null;
    private TextView mStateDisplay;
    private Button mConnectButton;
    private Button mDisconnectButton;

    private int mPower = 30;

    private boolean mReverse;
    private boolean mReverseLR;
    private boolean mRegulateSpeed;
    private boolean mSynchronizeMotors;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

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

        init();

        mNXTTalker = new NXT(mHandler);
    }

    private class DirectionButtonOnTouchListener implements View.OnTouchListener {

        private double left_motor;
        private double right_motor;
        private double hand_motor;

        DirectionButtonOnTouchListener(double l, double r, double hand) {
            left_motor = l;
            right_motor = r;
            hand_motor = hand;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                byte power = (byte) mPower;
                if (mReverse) {
                    power *= -1;
                }
                byte l = (byte) (power* left_motor);
                byte r = (byte) (power* right_motor);
                byte h = (byte) (10* hand_motor);
                if (!mReverseLR) {
                    mNXTTalker.motors3(l, r, h, mRegulateSpeed, mSynchronizeMotors);
                } else {
                    mNXTTalker.motors3(r, l, h, mRegulateSpeed, mSynchronizeMotors);
                }
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
                mNXTTalker.motors3((byte) 0, (byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            }
            return true;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        setContentView(R.layout.activity_main);

        ImageButton buttonUp = (ImageButton) findViewById(R.id.button_up);
        buttonUp.setOnTouchListener(new DirectionButtonOnTouchListener(1, 1,0));
        ImageButton buttonLeft = (ImageButton) findViewById(R.id.button_left);
        buttonLeft.setOnTouchListener(new DirectionButtonOnTouchListener(-0.6, 0.6,0));
        ImageButton buttonDown = (ImageButton) findViewById(R.id.button_down);
        buttonDown.setOnTouchListener(new DirectionButtonOnTouchListener(-1, -1,0));
        ImageButton buttonRight = (ImageButton) findViewById(R.id.button_right);
        buttonRight.setOnTouchListener(new DirectionButtonOnTouchListener(0.6, -0.6,0));

        ImageButton tanganUp = (ImageButton) findViewById(R.id.button_tangan_up);
        tanganUp.setOnTouchListener(new DirectionButtonOnTouchListener(0, 0,-0.5));
        ImageButton tanganDown = (ImageButton) findViewById(R.id.button_tangan_down);
        tanganDown.setOnTouchListener(new DirectionButtonOnTouchListener(0, 0,0.5));

        SeekBar powerSeekBar = (SeekBar) findViewById(R.id.power_seekbar);
        powerSeekBar.setProgress(mPower);
        powerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mPower = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mStateDisplay = (TextView) findViewById(R.id.state_display);

        mConnectButton = (Button) findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NO_BT) {
                    findBrick();
                } else {
                    mState = NXT.STATE_CONNECTED;
                    displayState();
                }
            }
        });

        mDisconnectButton = (Button) findViewById(R.id.disconnect_button);
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNXTTalker.stop();
            }
        });

        displayState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i("NXT", "MainActivity.onStart()");
        if (!NO_BT) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                if (mSavedState == NXT.STATE_CONNECTED) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    mNXTTalker.connect(device);
                } else {
                    if (mNewLaunch) {
                        mNewLaunch = false;
                        findBrick();
                    }
                }
            }
        }
    }

    private void findBrick() {
        Intent intent = new Intent(this, SelectDeviceActivity.class);
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
                    String address = data.getExtras().getString(SelectDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    //Toast.makeText(this, address, Toast.LENGTH_LONG).show();
                    mDeviceAddress = address;
                    mNXTTalker.connect(device);
                }
                break;
            case REQUEST_SETTINGS:
                //XXX?
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Log.i("NXT", "MainActivity.onSaveInstanceState()");
        if (mState == NXT.STATE_CONNECTED) {
            outState.putString("device_address", mDeviceAddress);
        }
        //outState.putBoolean("reverse", mReverse);
        outState.putInt("power", mPower);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Log.i("NXT", "MainActivity.onConfigurationChanged()");
        init();
    }

    private void displayState() {
        String stateText = null;
        int color = 0;
        switch (mState){
            case NXT.STATE_NONE:
                stateText = "Not connected";
                color = 0xffff0000;
                mConnectButton.setVisibility(View.VISIBLE);
                mDisconnectButton.setVisibility(View.GONE);
                setProgressBarIndeterminateVisibility(false);
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                break;
            case NXT.STATE_CONNECTING:
                stateText = "Connecting...";
                color = 0xffffff00;
                mConnectButton.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.GONE);
                setProgressBarIndeterminateVisibility(true);
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
                break;
            case NXT.STATE_CONNECTED:
                stateText = "Connected";
                color = 0xff00ff00;
                mConnectButton.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.VISIBLE);
                setProgressBarIndeterminateVisibility(false);
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
                break;
        }
        mStateDisplay.setText(stateText);
        mStateDisplay.setTextColor(color);
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
        //Log.i("NXT", "MainActivity.onStop()");
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
