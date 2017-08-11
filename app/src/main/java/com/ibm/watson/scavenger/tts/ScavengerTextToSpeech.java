package com.ibm.watson.scavenger.tts;

import android.os.AsyncTask;

import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;

/**
 * Created by arpitrastogi on 03/08/17.
 */

public class ScavengerTextToSpeech {
    com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech tts_svc = null;

    public ScavengerTextToSpeech(String tts_uname,String tts_pass){
        tts_svc = new com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech();
        tts_svc.setUsernameAndPassword(tts_uname,tts_pass);

    }

    public void playText(String text, Voice voice){
        new AnnounceMessage(text,voice).execute();
    }

private class AnnounceMessage extends AsyncTask<Void, Void, Void>{

    private String msg = null;
    Voice voice = null;
    public AnnounceMessage(String msg, Voice voice){
        this.msg = msg;
        this.voice = voice;
    }

    @Override
    protected Void doInBackground(Void... params) {
        StreamPlayer streamPlayer = new StreamPlayer();
        streamPlayer.playStream(tts_svc.synthesize(msg, voice).execute());
        return null;
    }

}

}
