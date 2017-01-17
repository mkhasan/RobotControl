package com.railbot.usrc.robotcontrol;

/**
 * Created by usrc on 17. 1. 16.
 */

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.animation.Interpolator;

/**
 * Created by usrc on 17. 1. 10.
 */

public class IR_Viewer {

    private static final String TAG 	 = "IR_Viewer";
    private IR_SurfaceView surfaceView;
    private long mNativeViewer;
    static {
        System.loadLibrary("native-irViewer-lib");
    }

    private static class ConnectTask extends AsyncTask<Void, Void, Integer> {
        //private static class StopTask extends AsyncTask<Void, Void, Void> {




        IR_Viewer viewer;
        public ConnectTask(IR_Viewer _viewer) {

            viewer = _viewer;

        }

        @Override
        protected Integer doInBackground(Void... params) {
            //ret = initIR_Native();

            //ret = irViewer.Init();

            return viewer.connectNativeViewer();


            //ret = viewer.p

        }

        @Override
        protected void onPostExecute(Integer result){
            String str = "Connect result is " + result;
            Log.e(TAG, str);
        }
    }


    private static class StopTask extends AsyncTask<Void, Void, Void> {

        private final IR_Viewer viewer;

        public StopTask(IR_Viewer _viewer) {
            this.viewer = _viewer;

        }


        @Override
        protected Void doInBackground(Void... params) {

            viewer.renderFrameStop();
            return null;

        }

        @Override
        protected void onPostExecute(Void reust) {

            viewer.Deallocate();
        }


    }



    IR_Viewer(IR_SurfaceView _surfaceView) {
        surfaceView = _surfaceView;


        //Log.e(TAG, "Ggoing to init native ...");
        int error = initNative();
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


    private native int initNative();
    public native int connectNativeViewer();
    public native void renderViewer(Surface surface);
    public native void renderFrameStop();
    public native void deallocNative();


    public void Connect() {
        new ConnectTask(this).execute();
    }

    public void Finalize() {
        new StopTask(this).execute();
    }
    /*
    public native void render(Surface surface);

    private native void stopNative();
    public native void renderFrameStart();
    public native void renderFrameStop();
    */
}
