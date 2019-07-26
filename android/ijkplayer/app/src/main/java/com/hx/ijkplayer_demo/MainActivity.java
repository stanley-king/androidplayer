package com.hx.ijkplayer_demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.MediaController;

import com.hx.ijkplayer_demo.common.FFMpegPlayerController;
import com.hx.ijkplayer_demo.common.FFPlayer;

import java.util.Vector;

public class MainActivity extends AppCompatActivity implements FFMpegPlayerController.PlayerStateListener
{
    private FFMpegPlayerController player;
    private Vector<FFPlayer> mPlayers = new Vector<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPlayer();
    }

    private void initPlayer() {
//        player = new FFMpegPlayerController(this,R.id.video_view);
////        player.setFullScreenOnly(true);
////        player.setScaleType(FFMpegPlayerController.SCALETYPE_FILLPARENT);
////        player.playInFullScreen(true);
////        player.setPlayerStateListener(this);
//        player.play("rtmp://live.hkstv.hk.lxdns.com/live/hks2");
//         player = new FFMpegPlayerController(this,R.id.video_view);
//         player.play("rtmp://live.hkstv.hk.lxdns.com/live/hks2");

        Vector<String> urls = new Vector<>();
        urls.add("rtmp://live.hkstv.hk.lxdns.com/live/hks2");
        urls.add("rtmp://live.hkstv.hk.lxdns.com/live/hks2");
        urls.add("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");
        urls.add("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");
        urls.add("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");
        urls.add("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");
        urls.add("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");
        urls.add("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");
        urls.add("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");

        int rid = R.id.video_view_2;
        for (String url : urls) {
            FFPlayer player = new FFPlayer(this,rid++);
            player.play(url);
            MediaController controller = new MediaController(this);
            player.setMediaController(controller);

            mPlayers.add(player);
        }



//        mPlayer = new FFPlayer(this,R.id.video_view_2);
////        mPlayer.play("rtmp://live.hkstv.hk.lxdns.com/live/hks2");
//        mPlayer.play("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_lryork_mf_p");
//        MediaController controller = new MediaController(this);
//        mPlayer.setMediaController(controller);

//        controller.setMediaPlayer(mPlayer);
//        mVideoView.setMediaController(controller);


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        if (player.mGestureDetector.onTouchEvent(event))
//            return true;
        return super.onTouchEvent(event);
    }

    @Override
    public void onComplete() {
    }

    @Override
    public void onError() {
    }

    @Override
    public void onLoading() {
    }

    @Override
    public void onPlay() {
    }
}
