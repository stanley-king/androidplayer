package com.hx.ijkplayer_demo.common;

import android.widget.MediaController;

import tv.danmaku.ijk.media.player.IMediaPlayer;

public interface IMediaPlayback extends MediaController.MediaPlayerControl
{
    int MEDIA_ERROR_UNKNOWN = 1;
    int MEDIA_ERROR_SERVER_DIED = 100;
    int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    int MEDIA_ERROR_IO = -1004;
    int MEDIA_ERROR_MALFORMED = -1007;
    int MEDIA_ERROR_UNSUPPORTED = -1010;
    int MEDIA_ERROR_TIMED_OUT = -110;

    void setOnPreparedListener(IMediaPlayer.OnPreparedListener listener);
    void setOnCompletionListener(IMediaPlayer.OnCompletionListener listener);
    void setOnErrorListener(IMediaPlayer.OnErrorListener listener);
    void setOnInfoListener(IMediaPlayer.OnInfoListener listener);
    boolean isInPlaybackState();

    boolean isMute();
    void mute();
    void unmute();
}
