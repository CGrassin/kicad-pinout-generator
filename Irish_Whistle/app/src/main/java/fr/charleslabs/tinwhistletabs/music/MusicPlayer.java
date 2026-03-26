package fr.charleslabs.tinwhistletabs.music;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.PresetReverb;
import android.os.Handler;

import java.util.List;

import fr.charleslabs.tinwhistletabs.music.synth.TinWhistleSynth;

public class MusicPlayer {
    private static final int SAMPLE_RATE = 22050;

    // Internal states
    private AudioTrack audioTrack = null;
    private final Handler handler = new Handler();

    //Singleton
    private static MusicPlayer instance;
    public static MusicPlayer getInstance(){
        if(instance == null){
            instance = new MusicPlayer();
        }
        return instance;
    }
    private MusicPlayer(){}

    public static byte[] genMusic(List<MusicNote> notes, float tempoModifier){
        // Compute length of music
        float lengthInS = 0;
        for(MusicNote note : notes) {
            lengthInS += note.getLengthInS(tempoModifier);
        }

        final float[] music = new float[(int)(lengthInS*SAMPLE_RATE)];
        int index = 0;
        for(MusicNote note : notes) {
            index += genNote(note,tempoModifier,music,index);
        }

        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.1f), 0.2f);
        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.2f), 0.1f);
        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.3f), 0.05f);
        TinWhistleSynth.reverb(music, (int)(SAMPLE_RATE*0.4f), 0.05f);

        return toneToBytePCM(music);
    }

    private static int genNote(MusicNote note, float tempoModifier, float[] music, int offset){
        int numSamples = (int)(note.getLengthInS(tempoModifier)*SAMPLE_RATE);

        if (numSamples+offset >= music.length -1)
            numSamples = music.length - offset -1;

        if (note.isRest())
            for (int i = 0; i < numSamples; ++i)
                music[i+offset] = 0;
        else
            TinWhistleSynth.genNote(note.getFrequency(),numSamples,music,offset,SAMPLE_RATE);

        return numSamples;
    }

    // Media controls: set, play, pause, stop, clear
    public void setAudioTrack(byte[] generatedSnd){
        if(audioTrack != null){
            audioTrack.stop();
            audioTrack.flush();
            audioTrack.release();
        }
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);

        // PRESET
        try {
            final PresetReverb reverb = new PresetReverb(0, audioTrack.getAudioSessionId());
            reverb.setPreset(PresetReverb.PRESET_SMALLROOM);
            reverb.setEnabled(true);
            audioTrack.setAuxEffectSendLevel(1.0f);
        }catch (Exception ignored){}

        audioTrack.write(generatedSnd, 0, generatedSnd.length);
    }

    public void play() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if( audioTrack != null) audioTrack.play();
                    }
                });
            }
        });
        thread.start();
    }

    public void pause() {
        if (audioTrack != null){
            audioTrack.pause();
            audioTrack.flush();
        }
    }

    public void stop() {
        // @TODO Fade volume
        if (audioTrack != null)
            //fadeOutAndStop();
            audioTrack.stop();
    }

    public void move(float time) {
        if (audioTrack != null)
            audioTrack.setPlaybackHeadPosition((int)(time*SAMPLE_RATE));
    }

    private static byte[] toneToBytePCM(double[] tone){
        final byte[] generatedSnd = new byte[tone.length * 2];
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (double dVal : tone) {
            short val = (short) (dVal * 32767);
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        return generatedSnd;
    }

    private static byte[] toneToBytePCM(float[] tone){
        final byte[] generatedSnd = new byte[tone.length * 2];
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (float dVal : tone) {
            short val = (short) (dVal * 32767);
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        return generatedSnd;
    }

    /*float volume;
    final private static int FADE_DURATION = 300; //The duration of the fade
    final private static  int FADE_INTERVAL = 150; //The amount of time between volume changes.
    private void fadeOutAndStop(){
        volume = AudioTrack.getMaxVolume();
        final float deltaVolume = volume / (float)(FADE_DURATION/FADE_INTERVAL);
        final Timer timer = new Timer(true);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                volume -= deltaVolume;
                System.out.println(volume);
                if (volume >= AudioTrack.getMinVolume())
                    audioTrack.setStereoVolume(volume,volume);
                //Cancel and Purge the Timer if the desired volume has been reached
                else{
                    timer.cancel();
                    timer.purge();
                    audioTrack.stop();
                    audioTrack.setStereoVolume(1,1);
                }
            }
        };
        timer.schedule(timerTask,FADE_INTERVAL,FADE_INTERVAL);
    }*/
}
