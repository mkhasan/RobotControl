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

public class ServerSettingsActivity extends Activity {

    final static String TAG = ServerSettingsActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_settings);


        Intent intent = getIntent();
        final String ipAddress = intent.getStringExtra(MainActivity.EXTRA_IP);
        final int voidPort = intent.getIntExtra(MainActivity.EXTRA_PORT, 1049);
//        String test = ;

        final EditText ipAddressEdit = (EditText) findViewById(R.id.ip_address);
        final EditText voipPortEdit = (EditText) findViewById(R.id.voip_port);


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

        ipAddressEdit.setText(ipAddress);
        voipPortEdit.setText(Integer.toString(voidPort));




        final Button setBtn = (Button) findViewById(R.id.set);
        setBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                data.putExtra("server_ip", ((EditText) findViewById(R.id.ip_address)).getText().toString());
                data.putExtra("voip_port", voipPortEdit.getText().toString());
                setResult(RESULT_OK, data);

                Log.e(TAG, "In settings " + ((EditText) findViewById(R.id.ip_address)).getText());
                finish();
            }
        });

    }
}
