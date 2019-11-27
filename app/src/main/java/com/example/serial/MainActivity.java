package com.example.serial;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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

    private String command = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = findViewById(R.id.received);
        editText = findViewById(R.id.editText);
        sendButton = findViewById(R.id.button);

        context = getApplicationContext();

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

        startThread();


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                    try {
                        byte[] outBuf = data.getBytes();
                        mOutputStream.write(outBuf);

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

}

