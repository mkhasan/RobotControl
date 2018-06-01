package com.railbot.usrc.robotcontrol;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.nio.charset.MalformedInputException;

public class ServerSettingsActivity extends Activity {

    final static String TAG = ServerSettingsActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_settings);


        Intent intent = getIntent();
        final String ipAddress = intent.getStringExtra(MainActivity.EXTRA_IP);
        final int voidPort = intent.getIntExtra(MainActivity.EXTRA_PORT, 1049);
        final int railRobotPort = intent.getIntExtra(MainActivity.EXTRA_RAIL_ROBOT_PORT, 8081);
        final String imageIP = intent.getStringExtra(MainActivity.EXTRA_IMAGE_IP);
        final String thermalIP = intent.getStringExtra(MainActivity.EXTRA_THERMAL_IP);
        final String railRobotIP = intent.getStringExtra(MainActivity.EXTRA_RAIL_ROBOT_IP);
//        String test = ;

        final EditText ipAddressEdit = (EditText) findViewById(R.id.ip_address);
        final EditText voipPortEdit = (EditText) findViewById(R.id.voip_port);
        final EditText railRobotPortEdit = (EditText) findViewById(R.id.rail_robot_port);

        final EditText imageIPEdit = (EditText) findViewById(R.id.image_ip);
        final EditText thermalIPEdit = (EditText) findViewById(R.id.thermal_ip);
        final EditText railRobotIPEdit = (EditText) findViewById(R.id.railbot_ip);



        final InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                                       int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\." +
                            "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        ipAddressEdit.setFilters(filters);
        imageIPEdit.setFilters(filters);
        thermalIPEdit.setFilters(filters);
        railRobotIPEdit.setFilters(filters);

        ipAddressEdit.setText(ipAddress);
        voipPortEdit.setText(Integer.toString(voidPort));
        railRobotPortEdit.setText(Integer.toString(railRobotPort));
        imageIPEdit.setText(imageIP);
        thermalIPEdit.setText(thermalIP);
        railRobotIPEdit.setText(railRobotIP);




        final Button setBtn = (Button) findViewById(R.id.set);
        setBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                data.putExtra(MainActivity.SERVER_ADDR, ((EditText) findViewById(R.id.ip_address)).getText().toString());
                data.putExtra(MainActivity.IMAGE_IP, ((EditText) findViewById(R.id.image_ip)).getText().toString());
                data.putExtra(MainActivity.THERMAL_IP, ((EditText) findViewById(R.id.thermal_ip)).getText().toString());
                data.putExtra(MainActivity.RAIL_ROBOT_IP, ((EditText) findViewById(R.id.railbot_ip)).getText().toString());
                data.putExtra(MainActivity.VOIP_PORT, voipPortEdit.getText().toString());
                data.putExtra(MainActivity.RAIL_ROBOT_PORT, railRobotPortEdit.getText().toString());

                setResult(RESULT_OK, data);

                Log.e(TAG, "In settings " + ((EditText) findViewById(R.id.ip_address)).getText());
                finish();
            }
        });

    }
}
