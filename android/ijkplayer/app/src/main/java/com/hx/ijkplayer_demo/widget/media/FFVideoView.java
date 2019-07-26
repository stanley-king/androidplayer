package com.hx.ijkplayer_demo.widget.media;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.hx.ijkplayer_demo.common.IMediaPlayback;

public class FFVideoView extends FrameLayout
{
    public FFVideoView(Context context) {
        super(context);
    }

    public FFVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FFVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    private IMediaPlayback mMediaPlayer;
    private MediaController mMediaController;

    public void setMediaPlayer(IMediaPlayback mediaPlayer)
    {
        mMediaPlayer = mediaPlayer;
    }

    public void setMediaController(MediaController controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null)
        {
            mMediaController.setMediaPlayer(mMediaPlayer);
            View anchorView = this.getParent() instanceof View ? (View)this.getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(mMediaPlayer.isInPlaybackState());
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && mMediaPlayer.isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return super.onTouchEvent(ev);
    }

    public boolean onTrackballEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && mMediaPlayer.isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return super.onTrackballEvent(ev);
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }
}
