package com.zengge.imageviewer;

import java.io.File;

import android.app.Activity;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;

import com.zengge.imageviewer.PinchImageView;
import com.zengge.nbmanager.R;
import android.support.v7.app.AppCompatActivity;

public class HugeImageViewerActivity extends AppCompatActivity {

    private TileDrawable mTileDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_huge);
        final String str = this.getIntent().getStringExtra("IMAGEPATH");
        final PinchImageView pinchImageView = (PinchImageView) findViewById(R.id.pic);
        pinchImageView.post(new Runnable() {
            @Override
            public void run() {
                mTileDrawable = new TileDrawable();
                mTileDrawable.setInitCallback(new TileDrawable.InitCallback() {
                    @Override
                    public void onInit() {
                        pinchImageView.setImageDrawable(mTileDrawable);
                    }
                });
                mTileDrawable.init(new HugeImageRegionLoader(HugeImageViewerActivity.this, Uri.fromFile(new File(str))), new Point(pinchImageView.getWidth(), pinchImageView.getHeight()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if(mTileDrawable != null)
            mTileDrawable.recycle();
        super.onDestroy();
    }
}