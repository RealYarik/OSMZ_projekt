package com.vsb.kru13.osmzhttpserver;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WCamera extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    public byte[] previewData;
    public Size size;

    public WCamera(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mCamera.setDisplayOrientation(90);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

        } catch (IOException e) {
            Log.d("CameraPreview", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.release();
        mCamera = null;
    }



    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null){
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);

            mCamera.setPreviewCallback(new Camera.PreviewCallback(){
                @Override
                public void onPreviewFrame(byte [] data, Camera camera){
                    //Image v Previewu je ve formátu Yav
                    //potřeba převést do JPEG byte arraye
                    previewData = convertYuvToBytearray(data, camera);
                }
            });

            mCamera.startPreview();

        } catch (Exception e){
            Log.d("CameraPreview", "Error starting camera preview: " + e.getMessage());
        }
    }

    public static byte[] convertYuvToBytearray(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        YuvImage image = new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, out);
        byte[] imageBytes = out.toByteArray();
        return imageBytes;
    }

}
