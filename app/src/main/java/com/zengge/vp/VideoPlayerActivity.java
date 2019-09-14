package com.zengge.vp;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;

import com.zengge.nbmanager.R;
import com.zengge.vp.SoundView.OnVolumeChangedListener;
import com.zengge.vp.VideoView.MySizeChangeLinstener;
import android.support.v7.app.AppCompatActivity;

//播放视频主界面
public class VideoPlayerActivity extends AppCompatActivity {
    // 播放列表
    public static LinkedList<MovieInfo> playList = new LinkedList<MovieInfo>();

    // 视频信息类
    public class MovieInfo {
        // 电影名称
        String displayName;
        // 文件路径
        String path;
    }

    // 媒体文件数据库查询地址
    private Uri videoListUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    // 播放进度条位置
    private static int position;
    // 播放时间
    private int playedTime;
    // 视频视图界面
    private VideoView vv = null;
    // 进度条
    private SeekBar seekBar = null;
    // 视频总时间
    private TextView durationTextView = null;
    // 已播放时间
    private TextView playedTextView = null;
    // 手势检测器
    private GestureDetector mGestureDetector = null;
    // 声音管理
    private AudioManager mAudioManager = null;
    // 最大音量、当前音量
    private int maxVolume = 0;
    private int currentVolume = 0;
    // 播放/暂停
    private ImageButton bn3 = null;
    // 音量控制
    private ImageButton bn5 = null;
    // 控制视图
    private View controlView = null;
    // 弹出窗口
    private PopupWindow controler = null;
    // 声音控制视图
    private SoundView mSoundView = null;
    private PopupWindow mSoundWindow = null;
    // 屏幕高宽
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private static int controlHeight = 0;

    private final static int TIME = 6868;
    // 标志位
    private boolean isControllerShow = true;
    private boolean isPaused = false;
    private boolean isFullScreen = false;
    private boolean isSilent = false;
    private boolean isSoundShow = false;

    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        // 添加idle处理器
        Looper.myQueue().addIdleHandler(new IdleHandler() {
            // 当当前消息队列中的所体验消息都执行完调用
            @Override
            public boolean queueIdle() {
                // 显示控制条
                if(controler != null && vv.isShown()) {
                    controler.showAtLocation(vv, Gravity.BOTTOM, 0, 0);
                    // 更新控制的位置
                    controler.update(0, 0, screenWidth, controlHeight);
                }
                // 返回false将使该handler调用一次之后被移除
                return false;
            }
        });
        // 膨胀出控制条
        controlView = getLayoutInflater().inflate(R.layout.controler, null);
        controler = new PopupWindow(controlView);
        // 视频持续时间
        durationTextView = (TextView) controlView.findViewById(R.id.duration);
        // 已播放时间
        playedTextView = (TextView) controlView.findViewById(R.id.has_played);
        // 音量控制界面
        mSoundView = new SoundView(this);
        mSoundView.setOnVolumeChangeListener(new OnVolumeChangedListener() {
            // 设置音量
            @Override
            public void setYourVolume(int index) {
                // 移除消息队列的消息
                cancelDelayHide();
                // 更新音量大小
                updateVolume(index);
                // 延迟隐藏控制器
                hideControllerDelay();
            }
        });
        // 获得音量控制界面
        mSoundWindow = new PopupWindow(mSoundView);
        position = -1;
        // 初始化2个按钮
        bn3 = (ImageButton) controlView.findViewById(R.id.button3);
        bn5 = (ImageButton) controlView.findViewById(R.id.button5);
        // 初始化视频播放界面
        vv = (VideoView) findViewById(R.id.vv);
        // 取得视频路径
        Uri uri = getIntent().getData();
        if(uri != null) {
            if(vv.getVideoHeight() == 0)
                vv.setVideoURI(uri);
            bn3.setImageResource(R.drawable.pause);
        } else
            bn3.setImageResource(R.drawable.play);
        // 取得指定路径下的所有视频列表
        getVideoFile(playList, new File("/sdcard/"));
        // 取得媒体库中的视频信息
        Cursor cursor = getContentResolver().query(videoListUri,
                        new String[] { "_display_name", "_data" }, null, null, null);
        int n = cursor.getCount();
        cursor.moveToFirst();
        LinkedList<MovieInfo> playList2 = new LinkedList<MovieInfo>();
        // 遍历得到的数据，将媒体信息保存到playList2中
        for(int i = 0; i != n; ++i) {
            MovieInfo mInfo = new MovieInfo();
            mInfo.displayName = cursor.getString(cursor
                                                 .getColumnIndex("_display_name"));
            mInfo.path = cursor.getString(cursor.getColumnIndex("_data"));
            playList2.add(mInfo);
            cursor.moveToNext();
        }
        // 比较这两种方式获得的视频数目，取较大者
        if(playList2.size() > playList.size())
            playList = playList2;
        // 当播放视频窗口大小改变时
        vv.setMySizeChangeLinstener(new MySizeChangeLinstener() {
            @Override
            public void doMyThings() {
                // 设置视频播放窗口高宽
                setVideoScale(SCREEN_DEFAULT);
            }
        });
        // 设置按键的透明度
        bn3.setAlpha(0xBB);
        // 取得试音管理器
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = mAudioManager
                        .getStreamVolume(AudioManager.STREAM_MUSIC);
        // 根据当前音量大小设置按键的透明度
        bn5.setAlpha(findAlphaFromSound());
        // 播放、暂停切换按钮
        bn3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                cancelDelayHide();
                // 当前暂停，则变为播放
                if(isPaused) {
                    vv.start();
                    bn3.setImageResource(R.drawable.pause);
                    hideControllerDelay();
                } else {
                    vv.pause();
                    bn3.setImageResource(R.drawable.play);
                }
                // 改变暂停播放标志位
                isPaused = !isPaused;
            }
        });
        // 显示音量控制界面
        bn5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                cancelDelayHide();
                // 如果已经显示则隐藏
                if(isSoundShow)
                    mSoundWindow.dismiss();
                else {
                    // 显示音量控制界面
                    if(mSoundWindow.isShowing()) {
                        mSoundWindow.update(15, 0, SoundView.MY_WIDTH,
                                            SoundView.MY_HEIGHT);
                    } else {
                        mSoundWindow.showAtLocation(vv, Gravity.RIGHT
                                                    | Gravity.CENTER_VERTICAL, 15, 0);
                        mSoundWindow.update(15, 0, SoundView.MY_WIDTH,
                                            SoundView.MY_HEIGHT);
                    }
                }
                isSoundShow = !isSoundShow;
                hideControllerDelay();
            }
        });
        // 设置音量键长按监听器
        bn5.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                // 静音、非静音切换
                if(isSilent)
                    bn5.setImageResource(R.drawable.soundenable);
                else
                    bn5.setImageResource(R.drawable.sounddisable);
                isSilent = !isSilent;
                // 更新音量
                updateVolume(currentVolume);
                // 去除隐藏消息
                cancelDelayHide();
                // 发送隐藏消息
                hideControllerDelay();
                return true;
            }
        });
        // 进度条
        seekBar = (SeekBar) controlView.findViewById(R.id.seekbar);
        // 进度条进度改变监听器
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekbar, int progress,
                                          boolean fromUser) {
                // 改变到指定位置
                if(fromUser)
                    vv.seekTo(progress);
            }
            // 按下的时候触发
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                myHandler.removeMessages(HIDE_CONTROLER);
            }
            // 离开进度条的时候触发
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
            }
        });
        // 取得屏幕大小
        getScreenSize();
        // 手势检测器
        mGestureDetector = new GestureDetector(new SimpleOnGestureListener() {
            // 双击
            @Override
            public boolean onDoubleTap(final MotionEvent e) {
                // 进行视频播放窗口大小切换
                if(isFullScreen)
                    setVideoScale(SCREEN_DEFAULT);
                else
                    setVideoScale(SCREEN_FULL);
                // 全屏显示标志位
                isFullScreen = !isFullScreen;
                // 显示控制器界面
                if(isControllerShow)
                    showController();
                return true;
            }
            // 单击
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                // 显示控制器界面
                if(!isControllerShow) {
                    showController();
                    hideControllerDelay();
                } else {
                    // 隐藏控制器界面
                    cancelDelayHide();
                    hideController();
                }
                return true;
            }
            // 长按
            @Override
            public void onLongPress(final MotionEvent e) {
                // 播放、暂停切换
                if(isPaused) {
                    vv.start();
                    bn3.setImageResource(R.drawable.pause);
                    cancelDelayHide();
                    hideControllerDelay();
                } else {
                    vv.pause();
                    bn3.setImageResource(R.drawable.play);
                    cancelDelayHide();
                    showController();
                }
                // 更改暂停标志位
                isPaused = !isPaused;
            }
        });
        // 当媒体文件载入时调用
        vv.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer arg0) {
                // 设置视频播放尺寸
                setVideoScale(SCREEN_DEFAULT);
                // 默认尺寸播放
                isFullScreen = false;
                if(isControllerShow)
                    showController();
                // 获得视频持续时间
                int i = vv.getDuration();
                seekBar.setMax(i);
                i /= 1000;
                int minute = i / 60;
                int hour = minute / 60;
                int second = i % 60;
                minute %= 60;
                // 显示视频持续时间
                durationTextView.setText(String.format("%02d:%02d:%02d", hour,
                                                       minute, second));
                vv.start();
                bn3.setImageResource(R.drawable.pause);
                hideControllerDelay();
                myHandler.sendEmptyMessage(PROGRESS_CHANGED);
            }
        });
        // 视频播放完毕调用
        vv.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer arg0) {
                // 取得播放列表大小
                int n = playList.size();
                // 循环播放
                if(++position < n)
                    vv.setVideoPath(playList.get(position).path);
                else
                    VideoPlayerActivity.this.finish();
            }
        });
    }

    // 改变播放进度条事件ID
    private final static int PROGRESS_CHANGED = 0;
    // 隐藏控制器事件ID
    private final static int HIDE_CONTROLER = 1;
    // 消息处理器
    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch(msg.what) {
            // 进度条改变
            case PROGRESS_CHANGED:
                // 进度条移动到指定位置
                int i = vv.getCurrentPosition();
                seekBar.setProgress(i);
                // 显示已播放时间
                i /= 1000;
                int minute = i / 60;
                int hour = minute / 60;
                int second = i % 60;
                minute %= 60;
                playedTextView.setText(String.format("%02d:%02d:%02d", hour,
                                                     minute, second));
                sendEmptyMessage(PROGRESS_CHANGED);
                break;
            case HIDE_CONTROLER:
                // 隐藏控制条
                hideController();
                break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        // 触发点击事件
        boolean result = mGestureDetector.onTouchEvent(event);
        return result;
    }

    // 响应android:configChanges中指定的事件
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        // 重新获得屏幕尺寸
        getScreenSize();
        if(isControllerShow) {
            // 取消延迟隐藏
            cancelDelayHide();
            // 隐藏控制器
            hideController();
            // 显示控制器
            showController();
            // 发送延迟隐藏的消息
            hideControllerDelay();
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        // 保存当前的播放时间
        playedTime = vv.getCurrentPosition();
        // 暂停播放
        vv.pause();
        // 改变按钮
        bn3.setImageResource(R.drawable.play);
        super.onPause();
    }

    @Override
    protected void onResume() {
        // 恢复到之前播放的位置开始播放
        vv.seekTo(playedTime);
        vv.start();
        if(vv.getVideoHeight() != 0) {
            bn3.setImageResource(R.drawable.pause);
            hideControllerDelay();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        // 关闭控制器
        if(controler.isShowing())
            controler.dismiss();
        // 关闭音量控制界面
        if(mSoundWindow.isShowing())
            mSoundWindow.dismiss();
        // 移除消息队列中的消息
        myHandler.removeMessages(PROGRESS_CHANGED);
        myHandler.removeMessages(HIDE_CONTROLER);
        // 移除播放列表
        playList.clear();
        super.onDestroy();
    }

    // 取得屏幕的尺寸
    private void getScreenSize() {
        Display display = getWindowManager().getDefaultDisplay();
        screenHeight = display.getHeight();
        screenWidth = display.getWidth();
        controlHeight = screenHeight / 4;
    }

    // 隐藏控制界面
    private void hideController() {
        // 隐藏视频控制条
        if(controler.isShowing()) {
            controler.update(0, 0, 0, 0);
            isControllerShow = false;
        }
        // 隐藏音量控制界面
        if(mSoundWindow.isShowing()) {
            mSoundWindow.dismiss();
            isSoundShow = false;
        }
    }

    // 延迟发送隐藏控制界面的消息
    private void hideControllerDelay() {
        myHandler.sendEmptyMessageDelayed(HIDE_CONTROLER, TIME);
    }

    // 显示控制界面
    private void showController() {
        controler.update(0, 0, screenWidth, controlHeight);
        isControllerShow = true;
    }

    // 移除消息队列中待处理隐藏控制条的消息
    private void cancelDelayHide() {
        myHandler.removeMessages(HIDE_CONTROLER);
    }

    // 屏幕状态标志
    private final static int SCREEN_FULL = 0;
    private final static int SCREEN_DEFAULT = 1;

    // 设置显示比例
    private void setVideoScale(int flag) {
        vv.getLayoutParams();
        switch(flag) {
        // 全屏显示
        case SCREEN_FULL:
            vv.setVideoScale(screenWidth, screenHeight);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            break;
        // 默认显示
        case SCREEN_DEFAULT:
            // 获得当前播放窗口的高宽
            int videoWidth = vv.getVideoWidth();
            int videoHeight = vv.getVideoHeight();
            // 获得当前屏幕的高宽
            int mWidth = screenWidth;
            int mHeight = screenHeight - 25;
            // 适配比例
            if(videoWidth > 0 && videoHeight > 0) {
                if(videoWidth * mHeight > mWidth * videoHeight)
                    mHeight = mWidth * videoHeight / videoWidth;
                else if(videoWidth * mHeight < mWidth * videoHeight)
                    mWidth = mHeight * videoWidth / videoHeight;
            }
            // 设置视频显示的尺寸
            vv.setVideoScale(mWidth, mHeight);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            break;
        }
    }

    // 根据当前的音量值决定按钮显示的透明度
    private int findAlphaFromSound() {
        if(mAudioManager != null) {
            int alpha = currentVolume * (0xCC - 0x55) / maxVolume + 0x55;
            return alpha;
        } else
            return 0xCC;
    }

    // 更新音量
    private void updateVolume(int index) {
        if(mAudioManager != null) {
            if(isSilent) {
                // 静音
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            } else {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index,
                                              0);
            }
            currentVolume = index;
            // 设置音量键的透明度
            bn5.setAlpha(findAlphaFromSound());
        }
    }

    // 获得视频文件列表
    private void getVideoFile(final LinkedList<MovieInfo> list, File file) {
        // 取得满足条件的视频文件
        file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                // TODO Auto-generated method stub
                String name = file.getName();
                int i = name.indexOf('.');
                if(i != -1) {
                    name = name.substring(i);
                    // 若文件的扩展名为mp4或者3gp
                    if(name.equalsIgnoreCase(".mp4")
                            || name.equalsIgnoreCase(".3gp")) {
                        // 该符合条件的视频文件信息保存到list中
                        MovieInfo mi = new MovieInfo();
                        // 获得文件名
                        mi.displayName = file.getName();
                        // 获得视频文件的路径
                        mi.path = file.getAbsolutePath();
                        list.add(mi);
                        return true;
                    }
                } else if(file.isDirectory())
                    getVideoFile(list, file);
                return false;
            }
        });
    }


    /**
     * 按下“返回键”后提示用户是否退出程序
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            stopService(new Intent(VideoPlayerActivity.this,
                                   VideoPlayerActivity.class));
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

}