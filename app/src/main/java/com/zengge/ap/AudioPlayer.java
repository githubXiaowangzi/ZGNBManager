package com.zengge.ap;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import com.zengge.nbmanager.R;
import android.support.v7.app.AppCompatActivity;

public class AudioPlayer extends AppCompatActivity {
    MediaPlayer mPlayer = null;
    Button mPlayBtn;
    TextView mPosition;
    TextView mDuration;
    SeekBar mProgress;
    boolean wasPlaying = false;
    public String content_url_ = "";
    private Handler mHandler;
    private static final int UPDATE_INTERVAL = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);
        content_url_ = getIntent().getStringExtra("AUDIOPATH");
        setTitle(content_url_);
        mPlayBtn = (Button)findViewById(R.id.playbtn);
        mPosition = (TextView)findViewById(R.id.position);
        mDuration = (TextView)findViewById(R.id.duration);
        mProgress = (SeekBar)findViewById(R.id.seeker);
        initPlayer();
        loadMedia(content_url_);
        startMedia();
        mPlayBtn.setText(getString(R.string.audio_pause));
    }

    private void initPlayer() {
        mPlayer = new MediaPlayer();
        mHandler = new Handler();
        mPlayer.setOnSeekCompleteListener(mOnSeekComplete);
        mPlayer.setOnErrorListener(mOnError);
        mProgress.setOnSeekBarChangeListener(mOnSeek);
        mProgressHandler.sendEmptyMessageDelayed(0, 200);
    }

    private void loadMedia(String addr) {
        Uri uri = Uri.fromFile(new File(addr));
        try {
            mPlayer.setDataSource(this, uri);
        } catch(IOException e) {
            Toast.makeText(this, "Load failed:" + e.getMessage(),
                           Toast.LENGTH_SHORT).show();
        }
        if(!prepareMedia())
            Log.d("ZGAP", "PrepareMedia Failed");
        int msDuration_ = mPlayer.getDuration();
        mDuration.setText("/" + formatMusicTime(msDuration_));
        mProgress.setMax(msDuration_);
    }

    private boolean prepareMedia() {
        try {
            mPlayer.prepare();
        } catch(IllegalStateException e) {
            Toast.makeText(this, "Preprare failed:" + e.getMessage(),
                           Toast.LENGTH_SHORT).show();
            return false;
        } catch(IOException e) {
            Toast.makeText(this, "Prepare failed:" + e.getMessage(),
                           Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void onDestroy() {
        super.onDestroy();
        if(mPlayer != null) {
            if(isPlaying())
                mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        if(mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
            mHandler = null;
        }
    }

    public void mOnClick(View v) {
        switch(v.getId()) {
        case R.id.playbtn:
            if(isPlaying()) {
                pauseMedia();
                mPlayBtn.setText(getString(R.string.audio_play));
            } else {
                startMedia();
                mPlayBtn.setText(getString(R.string.audio_pause));
            }
            break;
        case R.id.stopbtn:
            mPlayer.seekTo(0);
            mProgress.setProgress(0);
            pauseMedia();
            mPlayBtn.setText(getString(R.string.audio_play));
            break;
        default:
            Log.d("ZGAP", "Invalid resources's id");
            break;
        }
    }

    private boolean isPlaying() {
        return (mPlayer != null) && (mPlayer.isPlaying());
    }

    private void pauseMedia() {
        if(mPlayer == null)
            return;
        mPlayer.pause();
        if(mHandler == null)
            return;
        mHandler.removeCallbacks(mUpdateCounters);
    }

    private void startMedia() {
        if(mPlayer == null)
            return;
        mPlayer.start();
        if(mHandler == null)
            mHandler = new Handler();
        mHandler.post(mUpdateCounters);
    }

    MediaPlayer.OnErrorListener mOnError =
    new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            String err = "OnError occured. What = " + i + " , extra" + i1;
            Toast.makeText(AudioPlayer.this, err, Toast.LENGTH_LONG).show();
            return false;
        }
    };

    MediaPlayer.OnSeekCompleteListener mOnSeekComplete =
    new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mediaPlayer) {
            if(wasPlaying)
                startMedia();
        }
    };

    Handler mProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(mPlayer == null)
                return;
            if(isPlaying())
                mProgress.setProgress(mPlayer.getCurrentPosition());
            mProgressHandler.sendEmptyMessageDelayed(0, 200);
        }
    };

    SeekBar.OnSeekBarChangeListener mOnSeek =
    new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar,
                                      int progress, boolean fromUser) {
            if(fromUser)
                mPlayer.seekTo(progress);
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            wasPlaying = isPlaying();
            if(wasPlaying)
                pauseMedia();
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    public String formatMusicTime(int p) {
        StringBuffer sb = new StringBuffer();
        sb.append((p / 1000) / 60);
        sb.append(':');
        sb.append((p / 1000) % 60);
        return sb.toString();
    }

    private final Runnable mUpdateCounters = new Runnable() {
        @Override
        public void run() {
            if(mHandler == null || mPlayer == null)
                return;
            int pos = mPlayer.getCurrentPosition();
            if(formatMusicTime(pos).equals(formatMusicTime(mPlayer.getDuration())))
                mPlayBtn.setText(getString(R.string.audio_play));
            mPosition.setText(formatMusicTime(pos));
            if(mHandler != null)
                mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };


}
