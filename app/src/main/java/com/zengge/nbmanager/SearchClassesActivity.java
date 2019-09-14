package com.zengge.nbmanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SearchClassesActivity extends AppCompatActivity {

    private ClassItemAdapter mAdapter;
    public static List<String> classList;
    public ListView lv;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listact);
        lv = findViewById(R.id.zglist);
        if(classList == null)
            classList = new ArrayList<String>();
        mAdapter = new ClassItemAdapter(getApplication());
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> list, View view, int position, long id) {
                ClassListActivity.setCurrnetClass(classList.get(position));
                Intent intent = new Intent(SearchClassesActivity.this, ClassItemActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearAll();
    }

    private void clearAll() {
        classList = null;
        mAdapter = null;
    }

    public static void initClassList(List<String> list) {
        classList = list;
    }

    private class ClassItemAdapter extends BaseAdapter {

        protected final Context mContext;
        protected final LayoutInflater mInflater;

        public ClassItemAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return classList.size();
        }

        public Object getItem(int position) {
            return classList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout container;
            if(convertView == null) {
                container = (LinearLayout) mInflater.inflate(
                                R.layout.list_item, null);
            } else
                container = (LinearLayout) convertView;
            ImageView icon = (ImageView) container
                             .findViewById(R.id.list_item_icon);
            icon.setImageResource(R.drawable.clazz);
            TextView text = (TextView) container
                            .findViewById(R.id.list_item_title);
            text.setText(classList.get(position));
            return container;
        }
    }
}
