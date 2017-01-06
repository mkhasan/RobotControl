package com.railbot.usrc.mediaplayer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;

import java.util.Map;

/**
 * Created by usrc on 16. 12. 12.
 */

public class VideoPlayer {

    static {
        System.loadLibrary("yuv");
        System.loadLibrary("ffmpeg");
        System.loadLibrary("native-lib");
    }


    private static final String TAG 	 = "VideoPlayer";



    private static class StopTask extends AsyncTask<Void, Void, Void> {

        private final VideoPlayer player;

        public StopTask(VideoPlayer player) {
            this.player = player;

        }


        @Override
        protected Void doInBackground(Void... params) {

            player.stopNative();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (player.mpegListener != null)
                player.mpegListener.onFFStop();
        }


    }

    private static class CommErrorTask extends AsyncTask<Void, Void, Void> {

        private final VideoPlayer player;

        public CommErrorTask(VideoPlayer player) {
            this.player = player;

        }


        @Override
        protected Void doInBackground(Void... params) {


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (player.mpegListener != null)
                player.mpegListener.onFFError();
        }


    }
    private static class SetDataSourceTaskResult {
        FFError error;
        StreamInfo[] streams;
    }

    private static class SetDataSourceTask extends
            AsyncTask<Object, Void, SetDataSourceTaskResult> {

        private final VideoPlayer player;

        public SetDataSourceTask(VideoPlayer player) {
            this.player = player;
        }

        @Override
        protected SetDataSourceTaskResult doInBackground(Object... params) {
            String url = (String) params[0];
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) params[1];
            Integer videoStream = (Integer) params[2];
            Integer audioStream = (Integer) params[3];
            Integer subtitleStream = (Integer) params[4];

            int videoStreamNo = videoStream == null ? -1 : videoStream.intValue();
            int audioStreamNo = audioStream == null ? -1 : audioStream.intValue();
            int subtitleStreamNo = subtitleStream == null ? -1 : subtitleStream.intValue();

            int err = player.setDataSourceNative(url);//, map, videoStreamNo, audioStreamNo, subtitleStreamNo);
            SetDataSourceTaskResult result = new SetDataSourceTaskResult();
            if (err < 0) {
                result.error = new FFError(err);
                result.streams = null;
            } else {
                result.error = null;
                result.streams = player.getStreamsInfo();
            }
            return result;
        }

        @Override
        protected void onPostExecute(SetDataSourceTaskResult result) {
            if (player.mpegListener != null)
                player.mpegListener.onFFDataSourceLoaded(result.error,
                       result.streams);
        }

    }


    public static final int UNKNOWN_STREAM = -1;
    public static final int NO_STREAM = -2;
    private FFListener mpegListener = null;
    private final RenderedFrame mRenderedFrame = new RenderedFrame();

    private int mNativePlayer;
    private final Activity activity;

    private Runnable updateTimeRunnable = new Runnable() {

        @Override
        public void run() {

            if (mpegListener != null) {
                mpegListener.onFFUpdateTime(mCurrentTimeUs,
                        mVideoDurationUs, mIsFinished);
            }

        }

    };

    private long mCurrentTimeUs;
    private long mVideoDurationUs;
    //private FFmpegStreamInfo[] mStreamsInfos = null;
    private boolean mIsFinished = false;

    static class RenderedFrame {
        public Bitmap bitmap;
        public int height;
        public int width;
    }

    public VideoPlayer(VideoDisplay videoView, Activity activity) {


        this.activity = activity;
        mNativePlayer = -1;
        Log.e("Init", "Initiating");
        int error = initNative();
        if (error != 0)
            throw new RuntimeException(String.format(
                    "Could not initialize player: %d", error));
        videoView.setMpegPlayer(this);

    }

    public void Show() {
        Test();
    }

    public void stop() {
        Log.e(TAG, "going to stop");
        new StopTask(this).execute();
    }


    @Override
    protected void finalize() throws Throwable {
        Log.e("dealloc", "deallocating");
        deallocNative();
        super.finalize();
    }

    public int NativePlayer() {
        return mNativePlayer;
    }


    public native int initNative();
    public native int deallocNative();
    public native void render(Surface surface);
    public native int setDataSourceNative(String url);
    private native void stopNative();
    public native void renderFrameStart();
    public native void renderFrameStop();
    private native void Test();

    public void setDataSource(String url, Map<String, String> dictionary,
                              int videoStream, int audioStream, int subtitlesStream) {
        new SetDataSourceTask(this).execute(url, dictionary,
                Integer.valueOf(videoStream), Integer.valueOf(audioStream),
                Integer.valueOf(subtitlesStream));
    }

    public void setListener(FFListener mpegListener) {
        this.mpegListener = mpegListener;
    }
    private StreamInfo[] mStreamsInfos = null;
    protected StreamInfo[] getStreamsInfo() {
        return mStreamsInfos;
    }

    private void callback() {


        //mpegListener.onFFError();
        new CommErrorTask(this).execute();
        Log.e(TAG, "From callback");
    }

}

