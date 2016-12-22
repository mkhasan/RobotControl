package com.railbot.usrc.robotcontrol;

import android.support.annotation.Nullable;

/**
 * Created by usrc on 16. 12. 22.
 */

public class ListItem {
    private final long id;
    @Nullable
    private final String text;
    @Nullable
    private String key;

    private boolean enabled;

    public ListItem(long id,
                    @Nullable String text,
                    //@Nonnull
                    @Nullable String key,
                    boolean endabled ) {
        this.id = id;
        this.text = text;
        this.key = key;
        this.enabled = enabled;
    }

    public long id() {
        return id;
    }

    @Nullable
    public String text() {
        return text;
    }


    @Nullable
    public String key() {
        return key;
    }

    public boolean IsEnabled() {return enabled; }
}