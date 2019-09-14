package com.zengge.vp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import java.io.IOException;

//视频播放界面
public class VideoView extends SurfaceView implements MediaPlayerControl {

    public int getAudioSessionId() {
        // TODO: Implement this method
        return 0;
    }

    private String TAG = "VideoView";
    // 上下文
    private Context mContext;
    // 视频路径和持续时间
    private Uri mUri;
    private int mDuration;

    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private boolean mIsPrepared;
    // 视频的高宽
    private int mVideoWidth;
    private int mVideoHeight;
    // 播放界面的高宽
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    // 媒体控制器
    private MediaController mMediaController;
    // 播放完毕监听器
    private OnCompletionListener mOnCompletionListener;
    // 播放准备监听器
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private int mCurrentBufferPercentage;
    // 出错监听器
    private OnErrorListener mOnErrorListener;
    private boolean mStartWhenPrepared;
    private int mSeekWhenPrepared;
    // 尺寸改变监听器
    private MySizeChangeLinstener mMyChangeLinstener;

    // 取得视频的宽
    public int getVideoWidth() {
        return mVideoWidth;
    }

    // 取得视频的高
    public int getVideoHeight() {
        return mVideoHeight;
    }

    // 设置视频播放窗口的高宽
    public void setVideoScale(int width, int height) {
        LayoutParams lp = getLayoutParams();
        lp.height = height;
        lp.width = width;
        setLayoutParams(lp);
    }

    // 构造函数
    public VideoView(Context context) {
        super(context);
        mContext = context;
        // 初始化视频界面
        initVideoView();
    }

    // 带属性构造函数
    public VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
        // 初始化视频界面
        initVideoView();
    }

    // 带属性、样式构造函数
    public VideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        // 初始化视频界面
        initVideoView();
    }

    // 初始化界面
    private void initVideoView() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        // 请求焦点
        requestFocus();
    }

    // surfaceview回调函数
    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w,
                                   int h) {
            // 取得播放界面的尺寸
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            if(mMediaPlayer != null && mIsPrepared && mVideoWidth == w
                    && mVideoHeight == h) {
                if(mSeekWhenPrepared != 0) {
                    mMediaPlayer.seekTo(mSeekWhenPrepared);
                    mSeekWhenPrepared = 0;
                }
                // 开始播放视频
                mMediaPlayer.start();
                // 并显示控制器界面
                if(mMediaController != null)
                    mMediaController.show();
            }
        }
        // 打开视频
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceHolder = holder;
            openVideo();
        }
        // 界面销毁
        public void surfaceDestroyed(SurfaceHolder holder) {
            // 释放媒体播放器资源
            mSurfaceHolder = null;
            if(mMediaController != null)
                mMediaController.hide();
            if(mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }
    };

    // 打开视频
    private void openVideo() {
        if(mUri == null || mSurfaceHolder == null) {
            // 当前未准备就绪
            return;
        }
        // 暂停
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);
        // 释放资源
        if(mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        try {
            // 新建meidiaplayer
            mMediaPlayer = new MediaPlayer();
            // 设置准备监听器
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            // 设置尺寸改变监听器
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mIsPrepared = false;
            // 重置视频播放时间
            mDuration = -1;
            // 设置播放完成监听器
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            // 设置错误监听器
            mMediaPlayer.setOnErrorListener(mErrorListener);
            // 设置缓冲更新监听器
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            // 设置视频文件路径
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            // 设置音频流类型
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置播放时屏幕一直点亮
            mMediaPlayer.setScreenOnWhilePlaying(true);
            // 异步准备
            mMediaPlayer.prepareAsync();
            // 绑定媒体控制器
            attachMediaController();
            // 异常处理
        } catch(IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            return;
        } catch(IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            return;
        }
    }

    // 设置媒体播放器控制器
    public void setMediaController(MediaController controller) {
        if(mMediaController != null)
            mMediaController.hide();
        mMediaController = controller;
        // 绑定媒体控制器
        attachMediaController();
    }

    // 绑定媒体控制器
    private void attachMediaController() {
        if(mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            View anchorView = this.getParent() instanceof View ? (View) this
                              .getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(mIsPrepared);
        }
    }

    // 自定义回调函数接口
    public interface MySizeChangeLinstener {
        public void doMyThings();
    }

    // 取得尺寸改变监听器
    public void setMySizeChangeLinstener(MySizeChangeLinstener l) {
        mMyChangeLinstener = l;
    }

    // 尺寸改变监听器
    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            // 取得当前的尺寸
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if(mMyChangeLinstener != null)
                mMyChangeLinstener.doMyThings();
            // 设置尺寸
            if(mVideoWidth != 0 && mVideoHeight != 0)
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
        }
    };
    // 媒体播放器准备监听器
    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            if(mOnPreparedListener != null)
                mOnPreparedListener.onPrepared(mMediaPlayer);
            // 设置该视图的可用状态
            if(mMediaController != null)
                mMediaController.setEnabled(true);
            // 取得视频文件的宽高信息
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if(mVideoWidth != 0 && mVideoHeight != 0) {
                // 为界面设置高宽信息
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if(mSurfaceWidth == mVideoWidth
                        && mSurfaceHeight == mVideoHeight) {
                    // 设置播放初始位置
                    if(mSeekWhenPrepared != 0) {
                        mMediaPlayer.seekTo(mSeekWhenPrepared);
                        mSeekWhenPrepared = 0;
                    }
                    // 当准备完毕，播放视频
                    if(mStartWhenPrepared) {
                        mMediaPlayer.start();
                        mStartWhenPrepared = false;
                        // 显示控制器
                        if(mMediaController != null)
                            mMediaController.show();
                    } else if(!isPlaying()
                              && (mSeekWhenPrepared != 0 || getCurrentPosition() > 0)) {
                        if(mMediaController != null) {
                            // 当暂停时显示控制器
                            mMediaController.show(0);
                        }
                    }
                }
            } else {
                // 未知视频尺寸时仍然播放该视频
                if(mSeekWhenPrepared != 0) {
                    mMediaPlayer.seekTo(mSeekWhenPrepared);
                    mSeekWhenPrepared = 0;
                }
                // 开始播放
                if(mStartWhenPrepared) {
                    mMediaPlayer.start();
                    mStartWhenPrepared = false;
                }
            }
        }
    };
    // 播放完毕时调用
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            // 隐藏控制器
            if(mMediaController != null)
                mMediaController.hide();
            if(mOnCompletionListener != null)
                mOnCompletionListener.onCompletion(mMediaPlayer);
        }
    };
    // 错误监听器
    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            // 隐藏控制器
            if(mMediaController != null)
                mMediaController.hide();
            if(mOnErrorListener != null) {
                if(mOnErrorListener.onError(mMediaPlayer, framework_err,
                                            impl_err))
                    return true;
            }
            return true;
        }
    };
    // 缓冲监听器
    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            // 设置当前缓冲百分比
            mCurrentBufferPercentage = percent;
        }
    };

    // 设置准备监听器
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    // 设置播放完毕监听器
    public void setOnCompletionListener(OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    // 设置出错监听器
    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 传递尺寸信息
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    // 调整界面尺寸适应分辨率
    public int resolveAdjustedSize(int desiredSize, int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch(specMode) {
        // 无限制
        case MeasureSpec.UNSPECIFIED:
            result = desiredSize;
            break;
        // 不能超过限制尺寸
        case MeasureSpec.AT_MOST:
            result = Math.min(desiredSize, specSize);
            break;
        // 精确设置尺寸
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        }
        return result;
    }

    // 设置视频路径
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    // 设置视频uri地址
    public void setVideoURI(Uri uri) {
        mUri = uri;
        mStartWhenPrepared = false;
        mSeekWhenPrepared = 0;
        // 打开视频
        openVideo();
        requestLayout();
        // 更新界面
        invalidate();
    }

    // 停止播放
    public void stopPlayback() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    // 相应触摸事件、暂停/播放切换
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(mIsPrepared && mMediaPlayer != null && mMediaController != null)
            toggleMediaControlsVisiblity();
        return false;
    }

    // 视频播放控制器隐藏、显示切换
    private void toggleMediaControlsVisiblity() {
        if(mMediaController.isShowing())
            mMediaController.hide();
        else
            mMediaController.show();
    }

    // 开始播放
    public void start() {
        if(mMediaPlayer != null && mIsPrepared) {
            mMediaPlayer.start();
            mStartWhenPrepared = false;
        } else
            mStartWhenPrepared = true;
    }

    // 暂停播放
    public void pause() {
        if(mMediaPlayer != null && mIsPrepared) {
            if(mMediaPlayer.isPlaying())
                mMediaPlayer.pause();
        }
        mStartWhenPrepared = false;
    }

    // 取得视频的持续时间
    public int getDuration() {
        if(mMediaPlayer != null && mIsPrepared) {
            if(mDuration > 0)
                return mDuration;
            // 获得播放时间
            mDuration = mMediaPlayer.getDuration();
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    // 取得当前播放的位置
    public int getCurrentPosition() {
        if(mMediaPlayer != null && mIsPrepared)
            return mMediaPlayer.getCurrentPosition();
        return 0;
    }

    // 跳转到指定进度
    public void seekTo(int msec) {
        if(mMediaPlayer != null && mIsPrepared)
            mMediaPlayer.seekTo(msec);
        else
            mSeekWhenPrepared = msec;
    }

    // 返回播放器播放的状态
    public boolean isPlaying() {
        if(mMediaPlayer != null && mIsPrepared)
            return mMediaPlayer.isPlaying();
        return false;
    }

    public int getBufferPercentage() {
        if(mMediaPlayer != null)
            return mCurrentBufferPercentage;
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }
}
