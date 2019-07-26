package com.hx.ijkplayer_demo.common;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.hx.ijkplayer_demo.widget.media.FFVideoView;
import com.hx.ijkplayer_demo.widget.media.IRenderView;
import com.hx.ijkplayer_demo.widget.media.SurfaceRenderView;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class FFPlayer implements IMediaPlayback
{
    private String TAG = "FFPlayer";

    // all possible internal states
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_PLAYBACK_COMPLETED = 5;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;
    public int getCurrentState() {
        return mCurrentState;
    }

    private Uri  mUri;
    private Map<String, String> mHeaders;

    private final FFVideoView mVideoView;
    private IMediaPlayer mMediaPlayer = null;
    private MediaController mMediaController;

    private Context mAppContext;
    private boolean mPlayerSupport;

    private final AudioManager  mAudioManager;
    private IRenderView mRenderView;
    private IRenderView.ISurfaceHolder mSurfaceHolder = null;

    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int mVideoRotationDegree;

    private long mSeekWhenPrepared;  // recording the seek position while preparing
    private int  mCurrentAspectRatio = IRenderView.AR_ASPECT_FIT_PARENT;

    private int mCurrentBufferPercentage;
    private boolean mCanPause = true;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;

    private final int mMaxVolume;
    public int getMaxVolume() {
        return mMaxVolume;
    }

    private static final int MUTE_NONE = 0;
    private static final int MUTE_PREPARING = 1;
    private static final int MUTE_COMPLETE  = 2;

    private int mMuteState = MUTE_NONE;
    private int mCurrentVolume = 0;

    //OnPreparedListener
    private IMediaPlayer.OnPreparedListener mOnPreparedListener = null;
    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }
    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener()
    {
        public void onPrepared(IMediaPlayer mp)
        {
            mCurrentState = STATE_PREPARED;
//            mp.getMediaInfo() 此处来判断是否能够seek back，seek forward

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }

            mVideoWidth  = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            int seekToPosition = (int)mSeekWhenPrepared;
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }

            if (mVideoWidth != 0 && mVideoHeight != 0)
            {
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

                    if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight)
                    {
                        if (mTargetState == STATE_PLAYING) {
                            start();
                            if (mMediaController != null) {
                                mMediaController.show();
                            }
                        } else if (!isPlaying() &&
                                (seekToPosition != 0 || getCurrentPosition() > 0)) {
                            if (mMediaController != null) {
                                mMediaController.show(0);
                            }
                        }
                    }
                }
            } else {
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new IMediaPlayer.OnVideoSizeChangedListener()
    {
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            mVideoSarNum = mp.getVideoSarNum();
            mVideoSarDen = mp.getVideoSarDen();
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                }
                if (mVideoView != null) {
                    mVideoView.requestLayout();
                }
            }
        }
    };
    private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnCompletionListener mCompletionListener = new IMediaPlayer.OnCompletionListener() {
        public void onCompletion(IMediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mMediaController != null) {
                mMediaController.hide();
            }
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
        }
    };

    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    private IMediaPlayer.OnErrorListener mOnErrorListener;
    private IMediaPlayer.OnErrorListener mErrorListener = new IMediaPlayer.OnErrorListener()
    {
        public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
            Log.d(TAG, "Error: " + framework_err + "," + impl_err);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            if (mMediaController != null) {
                mMediaController.hide();
            }

            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                    return true;
                }
            }

            if (mVideoView != null && mVideoView.getWindowToken() != null) {
                String message="Unknown error";
                if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                    message="Invalid progressive playback";
                }
                new android.app.AlertDialog.Builder(mVideoView.getContext())
                        .setMessage(message)
                        .setPositiveButton("error", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mOnCompletionListener != null) {
                                    mOnCompletionListener.onCompletion(mMediaPlayer);
                                }
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
            return true;
        }
    };

    public void setOnErrorListener(IMediaPlayer.OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    private IMediaPlayer.OnInfoListener mOnInfoListener;
    private IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener() {
        public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
            if (mOnInfoListener != null) {
                mOnInfoListener.onInfo(mp, arg1, arg2);
            }
            switch (arg1) {
                case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                    mVideoRotationDegree = arg2;
                    Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
                    if (mRenderView != null)
                        mRenderView.setVideoRotation(arg2);
                    break;
            }
            return true;
        }
    };

    public void setOnInfoListener(IMediaPlayer.OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder)
    {
        if (mp == null) return;

        if (holder == null) {
            mp.setDisplay(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }

    //SufferView 回调，
    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
                return;
            }

            mSurfaceWidth = w;
            mSurfaceHeight = h;

            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo((int) mSeekWhenPrepared);
                }
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }

            mSurfaceHolder = holder;
            if (mMediaPlayer != null)
                bindSurfaceHolder(mMediaPlayer, holder);
            else
                openVideo();
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = null;
            if (mMediaController != null) mMediaController.hide();
            releaseWithoutStop();
        }
    };

    public void releaseWithoutStop() {
        if (mMediaPlayer != null)
            mMediaPlayer.setDisplay(null);
    }


    public FFPlayer(final Activity activity, int viewid)
    {
        mAppContext = activity.getApplicationContext();
        if(viewid > 0) {
            mVideoView = activity.findViewById(viewid);
        } else {
            mVideoView = null;
        }

        if (mVideoView != null) {
            mVideoView.setFocusable(true);
            mVideoView.setFocusableInTouchMode(true);
            mVideoView.requestFocus();
            mVideoView.setMediaPlayer(this);
        }

        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            mPlayerSupport =true;
        } catch (Throwable e) {
            Log.e("GiraffePlayer", "loadLibraries error", e);
        }

        if (!mPlayerSupport) {
            DebugLog.e("播放器不支持此设备");
        }

        mAudioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        SurfaceRenderView renderView = new SurfaceRenderView(mAppContext);
        setRenderView(renderView);

        mVideoWidth = 0;
        mVideoHeight = 0;
        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
    }

    public void play(String url) {
        if (mPlayerSupport) {
            setVideoPath(url);
            start();
        }
    }
    public void setMediaController(MediaController controller) {
        if (mVideoView != null) {
            mVideoView.setMediaController(controller);
            mMediaController = controller;
        }
    }

    @Override
    public boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getAudioSessionId();
        }
        return 0;
    }

    @Override
    public boolean isMute()
    {
        return (mMuteState == MUTE_COMPLETE || mMuteState == MUTE_PREPARING);
    }
    @Override
    public void mute()
    {
        if(mMuteState == MUTE_PREPARING) {
            return;
        }
        else {
            mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if(mMediaPlayer != null) {
                mMediaPlayer.setVolume(0.0f,0.0f);
                mMuteState = MUTE_COMPLETE;
            }
            else {
                mMuteState = MUTE_PREPARING;
            }
        }
    }
    @Override
    public void unmute()
    {
        if(mMuteState == MUTE_COMPLETE) {
            if(mMediaPlayer != null) {
                mMediaPlayer.setVolume(0.0f,0.0f);
                mMuteState = MUTE_COMPLETE;
            }
        } else {
            mMuteState = MUTE_PREPARING;
        }
    }

    private void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    private void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    private void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        if (mVideoView != null) {
            mVideoView.requestLayout();
            mVideoView.invalidate();
        }
    }

    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            return;
        }
        release(false);
        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        try
        {
            mMediaPlayer = createPlayer();

            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
            bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();

            if(mMuteState == MUTE_PREPARING) {
                mMediaPlayer.setVolume(0.0f,0.0f);
                mMuteState = MUTE_COMPLETE;
            }

            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            mCurrentState = STATE_ERROR;
            mTargetState  = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            Log.w(TAG, "Unable to open content: " + mUri, ex);
        } catch (IllegalArgumentException ex) {
            mCurrentState = STATE_ERROR;
            mTargetState  = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            Log.w(TAG, "Unable to open content: " + mUri, ex);
        } finally {
            Log.w(TAG, "Unable to open content: " + mUri);
        }
    }

    public void setRenderView(IRenderView renderView)
    {
        if (mRenderView != null) {
            if (mMediaPlayer != null)
                mMediaPlayer.setDisplay(null);

            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            if (mVideoView != null) {
                mVideoView.removeView(renderUIView);
            }
        }

        if (renderView == null)
            return;

        mRenderView = renderView;
        renderView.setAspectRatio(mCurrentAspectRatio);
        if (mVideoWidth > 0 && mVideoHeight > 0)
            renderView.setVideoSize(mVideoWidth, mVideoHeight);
        if (mVideoSarNum > 0 && mVideoSarDen > 0)
            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

        View renderUIView = mRenderView.getView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        if (mVideoView != null) {
            mVideoView.addView(renderUIView);
        }

        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);
    }

    private IMediaPlayer createPlayer() {
        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
        ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 10000000);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC,  "skip_loop_filter", 48);

        return ijkMediaPlayer;
    }
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }
}