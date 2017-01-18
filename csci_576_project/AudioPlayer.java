import javax.sound.sampled.*;
import java.io.File;

public class AudioPlayer implements Runnable {


    private File file;
    private AudioFormat audioFormat;
    private AudioInputStream waveStream;
    private Clip waveClip;
    private DataLine.Info waveInfo;

    /**
     * The constructor for PlaySound
     *
     * @param fileName The audio file input stream
     */
    public AudioPlayer(String fileName) {
        try {
            this.file = new File(fileName);
            waveStream = AudioSystem.getAudioInputStream(this.file);
            audioFormat = waveStream.getFormat();
            waveInfo = new DataLine.Info(Clip.class, audioFormat);
            waveClip = (Clip) AudioSystem.getLine(waveInfo);
            waveClip.open(waveStream);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void run() {
        this.play();
    }


    public void play() {
        try {
            waveClip.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getPosition() {
        return waveClip.getLongFramePosition();
    }

    public float getSampleRate() {
        return audioFormat.getFrameRate();
    }

    public int getFramesCount() {
        return waveClip.getFrameLength();
    }

    public void pause() {
        waveClip.stop();
    }

    public void resume() {
        waveClip.start();
    }

/*    public void reset() {
        waveClip.setMicrosecondPosition(0);
        waveClip.stop();
    }*/
}