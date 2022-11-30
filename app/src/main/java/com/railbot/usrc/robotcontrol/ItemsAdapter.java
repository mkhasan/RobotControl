package com.railbot.usrc.robotcontrol;

import android.app.LauncherActivity;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by usrc on 16. 12. 22.
 */

public class ItemsAdapter extends BaseAdapter {
    //@Nonnull
    private final LayoutInflater inflater;
    private List<ListItem> items = new ArrayList<ListItem>();

    public static class ViewHolder {

        //  @Nonnull
        private final View view;
        //  @Nonnull
        private final TextView textView;

        public static ViewHolder fromConvertView(View convertView) {
            return (ViewHolder) convertView.getTag();
        }

        public ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            view = inflater.inflate(R.layout.main_list_item, parent, false);
            textView = (TextView) view.findViewById(R.id.main_list_item_text);
            view.setTag(this);
        }

        //  @Nonnull
        public View getView() {
            return view;
        }

        public void bind(ListItem item) {
            textView.setText(item.text());
        }
    }

    public ItemsAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ListItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder(inflater, parent);
            convertView = holder.getView();
        } else {
            holder = ViewHolder.fromConvertView(convertView);
        }
        holder.bind(items.get(position));
        return convertView;
    }

    public void swapItems(List<ListItem> _items) {
        items = _items;
        notifyDataSetChanged();
    }
}
