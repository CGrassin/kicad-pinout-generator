package fr.charleslabs.tinwhistletabs.music.synth;

public class TinWhistleSynth {
    //private final static float ENV_ATTACK = 0.15f, ENV_DECAY = 0.2f, ENV_RELEASE = 0.1f, ENV_LEVEL_SUSTAIN = 0.3f;
    private final static float OSCI_AMP = 0.3f;
    private final static float ENV_ATTACK = 0.05f, ENV_DECAY = 0.2f, ENV_RELEASE = 0.2f, ENV_LEVEL_SUSTAIN =0.4f;
    private final static float VIBRATO_FREQ = 4f, VIBRATO_AMP=0.18f;
    private final static float NOISE_AMP = 0.015f;
    //private final static PinkNoise pink = new PinkNoise();

    public static void genNote(float frequency, int numSamples, float[] music, int offset, int sampleRate) {
        oscillator(frequency, numSamples, music, offset, sampleRate, OSCI_AMP);
        //normalizeNote(numSamples, music, offset);
        //pinkNoise(numSamples, music, offset,NOISE_AMP/3);
        whiteNoise(numSamples, music, offset,NOISE_AMP);
        envelope(numSamples,music,offset,(int)(ENV_ATTACK*numSamples),(int)(ENV_DECAY*numSamples),
                (int)(ENV_RELEASE*numSamples),ENV_LEVEL_SUSTAIN);
        vibrato(numSamples,music,offset,sampleRate,VIBRATO_AMP,VIBRATO_FREQ);
        //amplification(numSamples,music,offset,0.3f);
    }

    /**
     * Main oscillator.
     */
    private static void oscillator(float frequency, int numSamples, float[] music, int offset, int sampleRate, float amp) {
        for (int i = 0; i < numSamples && i+offset < music.length; ++i)
            music[i+offset] = amp*((float)(Math.sin(2 * Math.PI * i / (sampleRate / frequency)) +
                    0.30 * Math.sin(2 * Math.PI * i / (sampleRate / 2d / frequency)) +
                    0.20 * Math.sin(2 * Math.PI * i / (sampleRate / 3d / frequency)) +
                    0.10 * Math.sin(2 * Math.PI * i / (sampleRate / 4d / frequency))));
    }

    private static void amplification(int numSamples, float[] music, int offset, float amp) {
        for (int i = 0; i < numSamples; ++i)
            music[i+offset] *= amp;
    }

    private static void whiteNoise(int numSamples, float[] music, int offset, float amp) {
        for (int i = 0; i < numSamples && i+offset < music.length; ++i)
            music[i+offset] += amp * Math.random(); // White
    }

    /*private static void pinkNoise(int numSamples, float[] music, int offset, float amp) {
        for (int i = 0; i < numSamples && i+offset < music.length; ++i)
            music[i+offset] += (float)pink.nextValue()*amp;
    }*/

    /**
     * Envelope generator (attack, decay, sustain and release).
     */
    private static void envelope(int numSamples, float[] music, int offset,
                                 int attack, int decay, int release, float sustain_level) {
        int sustain = numSamples - attack - decay - release;

        for (int i = 0; i < attack; ++i)
            music[i+offset] *= (float)i/attack;
        for (int i = 0; i < decay; ++i)
            music[i+offset+attack] *= 1f + (float)i*(sustain_level - 1f)/decay;
        for (int i = 0; i < sustain; ++i)
            music[i+offset+attack+decay] *= sustain_level;
        for (int i = 0; i < release; ++i)
            music[i+offset+numSamples-release] *= sustain_level - (float)i* sustain_level/(float)release;
    }

    // TODO Filter

    /**
     * Vibrato generator.
     */
    private static void vibrato(int numSamples, float[] music, int offset, int sampleRate,
                                float vibrato_amp, float vibrato_freq) {
        for (int i = 0; i < numSamples; ++i)
            music[i+offset] *= (1-vibrato_amp) + vibrato_amp*Math.cos(2 * Math.PI * i / (sampleRate / vibrato_freq));
    }
    /**
     * Normalize to [0;1].
     */
    private static void normalizeNote(int numSamples, float[] music, int offset) {
        // Find max
        double max = music[offset];
        for (int i = 0; i < numSamples; ++i) {
            if(music[i+offset]>max)
                max = music[i+offset];
        }
        // If required, normalize
        if (max > 1)
            for (int i = 0; i < numSamples; ++i)
                music[i+offset] /= max;
    }

    public static void reverb(float[] music, int delay, float amp) {
        for (int i = delay; i < music.length; ++i) {
            music[i] += amp*music[i-delay];
        }

    }

}