package com.dharmawan.fp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by dharmawan on 11/24/17.
 */

public class NXT {

    static final int STATE_NONE = 0;
    static final int STATE_CONNECTING = 1;
    static final int STATE_CONNECTED = 2;
    
    private int mState;
    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    private BluetoothSocket cv_socket;
    
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private InputStream cv_is = null;
    private OutputStream cv_os = null;
    
    NXT(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        setState(STATE_NONE);
        cv_socket = null;
    }

    private synchronized void setState(int state) {
        mState = state;
        if (mHandler != null) {
            mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        }
    }
    
    public synchronized int getState() {
        return mState;
    }
    
    public synchronized void setHandler(Handler handler) {
        mHandler = handler;
    }
    
    private void toast(String text) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.TOAST, text);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    synchronized void connect(BluetoothDevice device) {
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        cv_socket = socket;
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        setState(STATE_CONNECTED);
    }
    
    synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        try {
            cv_is.close();
            cv_os.close();
        }
        catch(Exception ignored) {

        }
        setState(STATE_NONE);
    }
    
    private void connectionFailed() {
        setState(STATE_NONE);
    }
    
    private void connectionLost() {
        setState(STATE_NONE);
    }

    public void motors2(byte l, byte r, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };

        data[5] = l;
        data[19] = r;
        if (speedReg) {
            data[7] |= 0x01;
            data[21] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
            data[21] |= 0x02;
        }
        write(data);
    }
    
    private void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        r.write(out);
    }
    
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        
        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }
        
        public void run() {
            setName("ConnectThread");
            mAdapter.cancelDiscovery();
            
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    Method method = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
                    mmSocket = (BluetoothSocket) method.invoke(mmDevice, Integer.valueOf(1));
                    mmSocket.connect();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    connectionFailed();
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    return;
                }
            }
            
            synchronized (NXT.this) {
                mConnectThread = null;
            }
            
            connected(mmSocket, mmDevice);
        }
        
        void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        
        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            
            try {
                cv_is = socket.getInputStream();
                cv_os = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (true) {
                /*try {
                    bytes = cv_is.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }*/
            }
        }
        
        void write(byte[] buffer) {
            try {
                cv_os.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cf_setInputMode(int sensorType, int inputPort) {
        try {
            byte[] buffer = new byte[7];
            buffer[0] = 0x05;	            // length lsb
            buffer[1] = 0x00;			    // length msb
            buffer[2] = (byte) 0x80;	    // direct command (without response)
            buffer[3] = 0x05;		        // set input mode
            buffer[4] = (byte)inputPort;    // input port 0x00-0x03
            buffer[5] = (byte)sensorType;   // sensor type(enumerated)
            if(sensorType == Sensor_nxt.touch)
                buffer[6] = Sensor_nxt.bool_mode;
            else
                buffer[6] = Sensor_nxt.percent_mode;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch(Exception e) {
            Log.d("cf_setSensor", e.getStackTrace().toString());
        }
    }

    public int cf_getInputValues(int inputPort) {
        try {
            byte[] buffer = new byte[5];

            buffer[0] = 0x03;	            // length lsb
            buffer[1] = 0x00;			    // length msb
            buffer[2] = 0x00;			    // direct command (with response)
            buffer[3] = 0x07;			    // get output state
            buffer[4] = (byte)inputPort;   // input port

            cv_os.write(buffer);
            cv_os.flush();

            int[] inBuffer = new int[18];

            for (int i = 0; i < inBuffer.length; i++) {
                inBuffer[i] = cv_is.read();
            }

            //index 14 should contain the value we want
            return inBuffer[14];
        }
        catch(Exception e) {
            Log.d("cf_getSensorState", e.getStackTrace().toString());
            connectionLost();
        }
        return 0;
    }
}
