package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 17. 1. 16.
 */

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.transition.Slide;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.Toast;

/**
 * Created by usrc on 17. 1. 10.
 */

public class IR_Viewer {

    private static final String TAG 	 = "IR_Viewer";
    private IR_SurfaceView surfaceView;
    private long mNativeViewer;
    private boolean connected;
    private String hostname;

    private IR_ViewerListener listener = null;
    static {
        System.loadLibrary("native-irViewer-lib");
    }

    private class ConnectTask extends AsyncTask<Void, Void, Integer> {
        //private static class StopTask extends AsyncTask<Void, Void, Void> {




        IR_Viewer viewer;
        public ConnectTask(IR_Viewer _viewer) {

            viewer = _viewer;

        }

        @Override
        protected Integer doInBackground(Void... params) {

            int ret = viewer.connectNativeViewer();

            Log.e(TAG, "connection done");

            return ret;

            /*
            try {
                Thread.sleep(5000);

            }
            catch (Exception e) {

                return -1;
            }

            return 0;
            */
            //return viewer.connectNativeViewer();

        }


        @Override
        protected void onPostExecute(Integer result){
            String str = "Connect result is " + result;
            Log.e(TAG, str);

            if (viewer.listener != null)
                viewer.listener.onDataSourceLoaded(result);

            //viewer.surfaceView.onGetTemperature((float) 30.0);
        }
    }




    private static class StopTask extends AsyncTask<Void, Void, Void> {

        private final IR_Viewer viewer;

        public StopTask(IR_Viewer _viewer) {
            this.viewer = _viewer;

        }


        @Override
        protected Void doInBackground(Void... params) {



            Log.e(TAG, "renderFrameStop begins");

            viewer.renderFrameStop();
            Log.e(TAG, "renderFrameStop done");
            return null;

        }

        @Override
        protected void onPostExecute(Void reuslt) {



            viewer.Deallocate();
        }


    }

    private static class GetTemperatureTask extends AsyncTask<Integer, Void, Float> {

        private final IR_Viewer viewer;
        public GetTemperatureTask(IR_Viewer _viewer) {
            viewer = _viewer;
        }

        @Override
        protected Float doInBackground(Integer... params) {

            int x = params[0];
            int y = params[1];
            int maxX = params[2];
            int maxY = params[3];





            float ret = viewer.getTemperature(x, y, maxX, maxY);

            Log.e(TAG, "retrun is " + ret);
            return ret;


        }

        @Override
        protected void onPostExecute(Float result) {
            Log.e(TAG, Float.toString(result));

            viewer.surfaceView.onGetTemperature(result);
        }
    }


    IR_Viewer(IR_SurfaceView _surfaceView, String _hostname) {
        surfaceView = _surfaceView;
        hostname = _hostname;

        connected = false;

        //Log.e(TAG, "Ggoing to init native ...");
        int error = initNative(hostname);
        if (error != 0) {
            Log.e(TAG, "Error " + error);
            throw new RuntimeException(String.format(
                    "Could not initialize player: %d", error));
        }

        surfaceView.SetViewer(this);




    }



    @Override
    protected void finalize() throws Throwable {
        //Deallocate();
        super.finalize();
    }

    public void Deallocate() {
        Log.e(TAG, "Viewer is "+ Long.toHexString(mNativeViewer));
        deallocNative();
    }


    private native int initNative(String hostname);
    public native int connectNativeViewer();
    public native void renderViewer(Surface surface);
    public native void renderFrameStop();
    public native void deallocNative();
    private native float getTemperature(int x, int y, int maxX, int maxY);



    public void Connect() {
        new ConnectTask(this).execute();

    }

    public void Finalize() {
        Log.e(TAG, "Finalizing");
        new StopTask(this).execute();
    }

    public void GetTemperature(int x, int y, int maxX, int maxy){
        new GetTemperatureTask(this).execute(x, y, maxX, maxy);
    }

    public void setListener(IR_ViewerListener _listener) {
        listener = _listener;
    }
    /*
    public native void render(Surface surface);

    private native void stopNative();
    public native void renderFrameStart();
    public native void renderFrameStop();
    */
}
