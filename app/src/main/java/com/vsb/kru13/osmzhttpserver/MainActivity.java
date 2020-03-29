package com.vsb.kru13.osmzhttpserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SocketServer s;
    private static final int READ_EXTERNAL_STORAGE = 1;
    private String setText;
    TextView threadCounter;
    String count;
    TextView textView;
    private EditText editText;
    private static final String MSG_KEY_DATE = "date";
    private static final String MSG_KEY_HTTP= "GET";
    private static final String MSG_KEY_FILENAME= "/";
    private static final String MSG_KEY_FILESIZE= "0";
    private static final String COUNT= "0";
    private int threadCount;
    private FrameLayout previewLayout;
    private Intent service;
    private ServiceActivity serviceActivity ;
    boolean mBound = false;


    public static final Camera mCamera = getCameraInstance();

    public static WCamera preview;
    private CameraActivity cameraActivity;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn1 = (Button)findViewById(R.id.button1);
        Button btn2 = (Button)findViewById(R.id.button2);
        threadCounter = (TextView) findViewById(R.id.threadCounter);
        textView = (TextView) findViewById(R.id.textView);
        editText = (EditText)findViewById((R.id.editText));
        threadCount = Integer.parseInt(editText.getText().toString());
        Button shutterBtn = (Button)findViewById(R.id.capturebtn);
        count = "5";
        setText = "";
        textView.setText("Threads");
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);

        preview = new WCamera(this,mCamera);
        previewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        previewLayout.addView(preview);
        shutterBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("CAMERA", "Taking picture...");

                        // get an image from the camera
                        mCamera.takePicture(null, null, cameraActivity);
                    }
                }
        );



    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.button1) {

            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
            } else {
                setText +="Start Service\n";
                threadCounter.setText(setText);
                service = new Intent(this,ServiceActivity.class);
                startService(service);
                bindService(service, mConnection, Context.BIND_AUTO_CREATE);
            }
        }
        //Stop
        if (v.getId() == R.id.button2) {
            if (mBound) {
                unbindService(mConnection);
                mBound = false;
            }
            stopService(service);
            setText +="Stop Service\n";
            threadCounter.setText(setText);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder iBinder) {
            serviceActivity = ((ServiceActivity.LocalBinder)iBinder).getService();
            serviceActivity.setSettings(messageHandler,threadCount);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case READ_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    s = new SocketServer(messageHandler,threadCount);
                    s.start();
                }
                break;

            default:
                break;
        }
    }
    @SuppressLint("HandlerLeak")
    private Handler messageHandler = new Handler() {

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            setText += bundle.getString(MSG_KEY_DATE) +" "+bundle.getString(MSG_KEY_HTTP) +" "+bundle.getString(MSG_KEY_FILENAME)+" "+bundle.getString(MSG_KEY_FILESIZE)+" byte"+"\n";
            threadCounter.setText(setText);


        }
    };

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
        }
        return c;
    }




}
