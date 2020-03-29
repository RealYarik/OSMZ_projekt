package com.vsb.kru13.osmzhttpserver;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ServiceActivity extends Service {
    private SocketServer s;
    public Intent intent;
    private final IBinder mIBinder = new LocalBinder();


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SERVICE", "created");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mIBinder;
    }
    public class LocalBinder extends Binder {
        ServiceActivity getService() {
            // Return this instance of LocalService so clients can call public methods
            return ServiceActivity.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;
        Log.d("SERVICE", "Service Started");
        return super.onStartCommand(intent, flags, startId);
    }
    public void setSettings(Handler handler,int thread){
        s = new SocketServer(handler,thread);
        s.start();

    }

    @Override
    public void onDestroy() {
            s.close();
            try {
                s.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        Log.d("SERVICE", "stopped"); }



}
