package com.railbot.usrc.robotcontrol;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

/**
 * Created by usrc on 18. 1. 14.
 */

public class LockEditText extends EditText {
    /* Must use this constructor in order for the layout files to instantiate the class properly */
    final static String TAG = LockEditText.class.getName();
    public LockEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean onKeyPreIme (int keyCode, KeyEvent event)
    {
        // Return true if I handle the event:
        // In my case i want the keyboard to not be dismissible so i simply return true
        // Other people might want to handle the event differently
        Log.e(TAG, "GOT IT");
        setText("123");
        return false;
    }

}