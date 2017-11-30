package com.railbot.usrc.robotcontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    // Used to load the 'native-lib' library on application startup.

    static {
        System.loadLibrary("yuv");
    }


    String TAG = "RobotControl";

    TextView tv;

    //static public String serverAddr = "1"

    static public String url = "rtsp://admin:admin@192.168.0.101:554/stream1";
    //static public String url = "rtsp://admin:admin@192.168.1.100:554/12";

    private ItemsAdapter adapter;
    TextView textView = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        final ListView listView = (ListView) findViewById(R.id.main_list);
        textView = (TextView) findViewById(R.id.select);


        adapter = new ItemsAdapter(LayoutInflater.from(this));
        adapter.swapItems(getItems());

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);


        //VideoActivity.ShowAllertTest(this);

        //String str = stringFromJNI();

        //Log.e(TAG, stringFromJNI());





    }

    @NonNull
    private List<ListItem> getItems() {
        final List<ListItem> items = new ArrayList<ListItem>();
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.motion_only).toString(),
                null,
                true));

        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.image_camera).toString(),
                null,
                true));
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.thermal_camera).toString(),
                null,
                true));
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.image_and_thermal_cam).toString(),
                null,
                true));
        items.add(new ListItem(
                items.size(),
                getResources().getText(R.string.exit).toString(),
                null,
                true));
        return items;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
        final ListItem item = adapter.getItem(position);
    /*
    final Intent intent = new Intent(AppConstants.VIDEO_PLAY_ACTION)
            .putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_URL, videoItem.video())
            .putExtra(AppConstants.VIDEO_PLAY_ACTION_EXTRA_ENCRYPTION_KEY, videoItem.video());
    startActivity(intent);
    */


        String str = item.text();

        String exitStr = getResources().getText(R.string.exit).toString();
        Log.e(TAG, "Str is "+str.length());
        if (str.equals(exitStr)) {

            Log.e(TAG, "Str is "+str);
            finish();
        }

        else
        {
            Intent intent = new Intent(this, VideoActivity.class);
            intent.putExtra("choice", str);
            this.startActivityForResult(intent, 0);

        }
        // text_view.setText(str);
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == 0) {
            if (resultCode == 0) {
                // A contact was picked.  Here we will just display it
                // to the user.
                //Log.e(TAG, "10");
            }
            //else
              //  Log.e(TAG, "100");
        }

        Log.e(TAG, Integer.toString(requestCode)+ " " + (resultCode == 0 ? "OK" : "ERROR"));
    }


 //   public native String stringFromJNI();
}
