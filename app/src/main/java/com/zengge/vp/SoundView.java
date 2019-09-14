package com.zengge.vp;

import com.zengge.nbmanager.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

//音量控制界面
public class SoundView extends View {
    public final static String TAG = "SoundView";
    // 上下文
    private Context mContext;
    private Bitmap bm, bm1;
    private int bitmapWidth, bitmapHeight;
    // 音量大小
    private int index;
    // 音量改变监听器
    private OnVolumeChangedListener mOnVolumeChangedListener;
    private final static int HEIGHT = 11;
    public final static int MY_HEIGHT = 163;
    public final static int MY_WIDTH = 44;

    // 音量改变监听器
    public interface OnVolumeChangedListener {
        public void setYourVolume(int index);
    }

    // 设置音量改变监听器
    public void setOnVolumeChangeListener(OnVolumeChangedListener l) {
        mOnVolumeChangedListener = l;
    }

    // 构造函数
    public SoundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        // 带属性、样式初始化界面
        init();
    }

    // 构造函数
    public SoundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        // 带属性初始化界面
        init();
    }

    // 构造函数
    public SoundView(Context context) {
        super(context);
        mContext = context;
        // 不带属性、样式初始化界面
        init();
    }

    // 初始化
    private void init() {
        bm = BitmapFactory.decodeResource(mContext.getResources(),
                                          R.drawable.sound_line);
        bm1 = BitmapFactory.decodeResource(mContext.getResources(),
                                           R.drawable.sound_line1);
        // 取得图片的高宽
        bitmapWidth = bm.getWidth();
        bitmapHeight = bm.getHeight();
        AudioManager am = (AudioManager) mContext
                          .getSystemService(Context.AUDIO_SERVICE);
        // 设置当前音量
        setIndex(am.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    // 触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 根据触摸位置决定音量大小
        int y = (int) event.getY();
        int n = y * 15 / MY_HEIGHT;
        setIndex(15 - n);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int reverseIndex = 15 - index;
        // 绘制彩色音量条
        for(int i = 0; i != reverseIndex; ++i) {
            canvas.drawBitmap(bm1, new Rect(0, 0, bitmapWidth, bitmapHeight),
                              new Rect(0, i * HEIGHT, bitmapWidth, i * HEIGHT
                                       + bitmapHeight), null);
        }
        // 绘制灰色音量条
        for(int i = reverseIndex; i != 15; ++i) {
            canvas.drawBitmap(bm, new Rect(0, 0, bitmapWidth, bitmapHeight),
                              new Rect(0, i * HEIGHT, bitmapWidth, i * HEIGHT
                                       + bitmapHeight), null);
        }
        super.onDraw(canvas);
    }

    // 改变音量大小
    private void setIndex(int n) {
        if(n > 15)
            n = 15;
        else if(n < 0)
            n = 0;
        if(index != n) {
            index = n;
            if(mOnVolumeChangedListener != null)
                mOnVolumeChangedListener.setYourVolume(n);
        }
        // 重绘界面
        invalidate();
    }
}