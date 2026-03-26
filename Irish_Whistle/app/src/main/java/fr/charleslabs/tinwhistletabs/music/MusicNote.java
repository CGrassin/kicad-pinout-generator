package fr.charleslabs.tinwhistletabs.music;

public class MusicNote {
    private final int duration; //ms
    private int pitch = 0; //keynumber, 0=rest

    /**
     * Create a musical note.
     */
    MusicNote(int pitch, int duration){
        this.duration = duration;
        this.pitch = pitch;
    }

    public float getLengthInMS(float tempoModifier) {
        return duration / tempoModifier;
    }

    float getLengthInS(float tempoModifier) {
        return getLengthInMS(tempoModifier)/1000f;
    }

    void transpose(int shiftPitch){
        if(!this.isRest())
            this.pitch += shiftPitch;
    }

    float getFrequency() {
        return  440f * (float)Math.pow(2f, (float)(pitch- 49)/12f);
    }

    public boolean isRest(){
        return pitch == 0;
    }

    public String toTab(){
        if(this.isRest())
            return "";

        // D Tin whistle tabs
        switch (this.pitch){
            case 54: return "d";
            case 55: return "i";
            case 56: return "e";
            case 57: return "j";
            case 58: return "f";
            case 59: return "g";
            case 60: return "h";
            case 61: return "a";
            case 62: return "n";
            case 63: return "b";
            case 64: return "m";
            case 65: return "c";
            case 66: return "D";
            case 67: return "I";
            case 68: return "E";
            case 69: return "J";
            case 70: return "F";
            case 71: return "G";
            case 72: return "H";
            case 73: return "A";
            case 74: return "N";
            case 75: return "B";
            case 76: return "M";
            case 77: return "C";
            case 78: return "\u00CE";
            default: return "?";
        }
    }

    int getPitch() {
        return pitch;
    }
}
