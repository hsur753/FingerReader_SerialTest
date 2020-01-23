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

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.Arrays;
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
    public static final int MESSAGE_READ=0;

    String message = "";
    String m = "";
    private boolean shouldRun = true;
    boolean isDollar = false;

    UsbSerialPort port;

    private String command = "0";

    @SuppressLint("HandlerLeak")
    Handler mHandler=new Handler()
    {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);

            switch (msg_type.what){
                case MESSAGE_READ:

                    byte[] readbuf=(byte[])msg_type.obj;
                    int i;
                    try {

                        String string_recieved=new String(readbuf);

                        if ((char) readbuf[0] == '$' && (char) readbuf[11] == '&') {
                            serialMessageHandler(readbuf.clone());
                        }
                    }
                    catch (Exception e){Log.e("", "Could't read incoming message");}
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
        /*

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
        }); */

        //TODO: Better to implement displaying text to UI this way
        /*runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (!message.equals("")) {
                    display.setText(message);
                }
            }

        });*/

        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return;
        }

        try{
        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        port.open(connection);
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        ConnectThread connectThread = new ConnectThread();
        connectThread.start();
        }
        catch (Exception e){
            Toast toast = Toast.makeText(context, "XXXXXXXXXXX", Toast.LENGTH_SHORT);
            toast.show();
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                    try {
                        byte[] outBuf = data.getBytes();
                        port.write(outBuf, 0);

                        //Toast toast = Toast.makeText(context, "Sent to Arduino", Toast.LENGTH_SHORT);
                        //toast.show();

                    } catch (Exception e) {Log.e("","Couldn't Write"); e.printStackTrace();}

                }
            }
        });

    }

    private class ConnectThread extends Thread{
        public void run(){
            byte[] response;
            int len;
            while (true) {
                try{
                    response = new byte[12];
                    len = port.read(response, 0);
                    if (len > 0){
                        mHandler.obtainMessage(MESSAGE_READ, len, -1, response).sendToTarget();
                    }
                }
                catch (IOException e){
                    Toast toast = Toast.makeText(context, "XXXXXXXXXXX", Toast.LENGTH_SHORT);
                    toast.show();
                    break;}

            }
        }

    }

    private void startThread() {
        readSerialDataThread = new Thread(new Runnable() {
            public void run() {
                byte[] response = new byte[1024];
                int len;
                while (true) {
                    try{
                        len = port.read(response, 100);
                        if (len > 0){}
                    }
                    catch (IOException e){break;}

                }
            }
        });
        readSerialDataThread.start();
    }

    private void serialMessageHandler(byte[] buffer){
        switch ((char) buffer[1]){
            case 'D':
                Toast.makeText(getApplicationContext(), "DEBUG", Toast.LENGTH_SHORT).show();
                break;

            case 'L':
                Toast.makeText(getApplicationContext(), "LED ON", Toast.LENGTH_SHORT).show();
                break;

            case 'O':
                int i = (short) ((buffer[3] << 8) | buffer[2]);

                /*ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);

                byteBuffer.order(ByteOrder.BIG_ENDIAN);

                byteBuffer.put((byte)0x00);
                byteBuffer.put((byte)0x00);
                byteBuffer.put((byte)buffer[3]);
                byteBuffer.put((byte)buffer[2]);
                byteBuffer.flip();
                int result = byteBuffer.getInt(); */

                Toast.makeText(getApplicationContext(), "LED OFF " + i, Toast.LENGTH_SHORT).show();
                break;

            case '1':
                String s = new String (buffer);
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                break;
        }
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