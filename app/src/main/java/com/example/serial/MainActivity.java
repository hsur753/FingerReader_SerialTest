package com.example.serial;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android_serialport_api.SerialPort;

public class MainActivity extends Activity {

    private TextView display;
    private EditText editText;
    private Button sendButton;
    private Context context;

    private SerialPort  mSerialPort;
    private InputStream inStream;
    private OutputStream mOutputStream;
    private Thread readSerialDataThread;
    String message = "";
    private boolean shouldRun = true;

    final int MESSAGE_READ = 0;

    private String command = "0";
    private Handler handler;
   int i = 0;

    @SuppressLint("HandlerLeak")
    Handler mHandler=new Handler()
    {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);

            switch (msg_type.what) {
                case MESSAGE_READ:

                    byte[] readbuf = (byte[]) msg_type.obj;
                    String string_recieved=new String(readbuf);

                    try {
                        Log.w("", string_recieved);
                        if (!string_recieved.trim().equals("")) {
                        display.setText(string_recieved);
                        Log.w ("received ", string_recieved);
                        }

                    } catch (Exception e) {
                        Log.e("error", "Could't read incoming message");
                        e.printStackTrace();
                    }
                    //String string_recieved=new String(readbuf);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = findViewById(R.id.received);
        editText = findViewById(R.id.editText);
        sendButton = findViewById(R.id.button);

        context = getApplicationContext();
        handler = new Handler();

        // Find all available drivers from attached devices.

        File dir2 = new File("/dev");
        File[] files = dir2.listFiles();
        for (int i = 0; i < files.length; i++)
        {
            Log.e("Files", "FileName:" + files[i].getName());
        }


        File serport = new File("/dev/ttyHS4"); //Serial port of Qualcomm 410
        try {
            mSerialPort = new SerialPort(serport, 115200, 0);
            inStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();

        } catch (Exception e) {Log.e("","Connection Failed"); e.printStackTrace();}

        //startThread();
        ConnectedThread connectedThread = new ConnectedThread();
        connectedThread.start();
        SendThread();
       //s.start();


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                    try {
                        byte[] outBuf = data.getBytes();
                        mOutputStream.write(outBuf);
                        mOutputStream.flush();
                        Toast toast = Toast.makeText(context, "Sent to Arduino", Toast.LENGTH_SHORT);
                        toast.show();

                    } catch (Exception e) {Log.e("","Couldn't Write"); e.printStackTrace();}

                }
            }
        });

        //TODO: Better to implement displaying text to UI this way
       /*runOnUiThread(new Runnable() {

           @Override
           public void run() {
               if (!message.equals("")) {
                   display.setText(message);
               }
           }

       });*/

    }

    private void startThread() {
        readSerialDataThread = new Thread(new Runnable() {
            public void run() {
                while (shouldRun) {
                    int dataSize = 0;
                    try {
                        //mOutputStream.write(("DATA_" + System.currentTimeMillis()).getBytes());
                        Log.e("","1111111111");
                        dataSize = inStream.available();
                        if (inStream != null) {
                            Log.e("", "2222222222");
                            byte[] data = new byte[dataSize];
                            inStream.read(data);
                            message = (new String(data));
                            Log.e("", message);
                            if (!message.equals("")) {
                                display.setText(message);
                            }
                        }
                        //LED1 of the board will blink if app deploys successfully
                        Process p = Runtime.getRuntime().exec("su");
                        DataOutputStream dos = new DataOutputStream(p.getOutputStream());
                        dos.writeBytes("cd /sys/class/leds/led1\n");

                        if (command.equals("0")) {
                            command = "1";
                            dos.writeBytes("echo 1 > brightness\n");
                        }
                        else {
                            command = "0";
                            dos.writeBytes("echo 0 > brightness\n");}
                        dos.flush();
                        dos.close();
                        p.waitFor();
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        readSerialDataThread.start();
    }

    private class ConnectedThread extends Thread{
        public void run(){
            byte[] message;
            int len;
            while (true){
                try{
                    message = new byte[128];
                    if(inStream.available() > 0){
                        // byte[] inputData = new byte[10];
                        //int readCount = readInputStreamWithTimeout(inStream, inputData, 100);

                        len = inStream.read(message);
                        Log.e("debug", "---------- " + new String(message));
                        mHandler.obtainMessage(MESSAGE_READ, len, -1, message).sendToTarget();

                    }
                   // else {
                     //   SystemClock.sleep(100);
                   // }
                }
                catch (Exception e){
                    Log.e("debug", "?????????????????????");
                    break;
                }
            }
        }
    }

    private void SendThread()

    {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                    try {
                        byte[] outBuf = (" ").getBytes();
                        mOutputStream.write(outBuf);
                        mOutputStream.flush();
                        i++;

                        Log.i("", "Sending... " + i);
                    } catch (Exception e) {
                }
                handler.postDelayed(this, 100);
            }
        }, 100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.shouldRun = false;
        try{
            readSerialDataThread.join();
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes("cd /sys/class/leds/led1\n");
            dos.writeBytes("echo 0 > brightness\n");
        }
        catch (Exception e){e.printStackTrace();}
        mSerialPort.close();
    }

    public static int readInputStreamWithTimeout(InputStream is, byte[] b, int timeoutMillis)
            throws IOException {
        int bufferOffset = 0;
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
            int readLength = java.lang.Math.min(is.available(),b.length-bufferOffset);
            // can alternatively use bufferedReader, guarded by isReady():
            int readResult = is.read(b, bufferOffset, readLength);
            if (readResult == -1) break;
            bufferOffset += readResult;
        }
        return bufferOffset;
    }

}
