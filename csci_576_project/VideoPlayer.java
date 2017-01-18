import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Video player class
 */
public class VideoPlayer extends JComponent implements Runnable {

    private final int WIDTH = 480;
    private final int HEIGHT = 270;
    private final int FPS = 30; // Frames Per Second
    private AudioPlayer audio;
    private String fileName;
    private InputStream inputStream;
    private BufferedImage img;
    private Graphics2D g2d;
    private byte[] buffer;
    private boolean pausing = true;


    /**
     * @param fileName Name of video file
     * @param audio The audio player
     */
    public VideoPlayer(String fileName, AudioPlayer audio) {
        this.fileName = fileName;
        this.audio = audio;
    }


    public void run() {
        play();
    }


    private void play() {

        // Used to output frame number for debugging

        img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_3BYTE_BGR);

        try {
            File file = new File(fileName);
            inputStream = new FileInputStream(file);

            long bytesPerFrame = WIDTH * HEIGHT * 3;
            long numFrames = file.length() / bytesPerFrame;

            buffer = new byte[(int) bytesPerFrame];


            System.out.println("Sample rate is " + audio.getSampleRate());
            System.out.println("Frame count is " + audio.getFramesCount());
            double samplePerFrame = audio.getSampleRate() / FPS;


            for (int i = 0; i < numFrames; ) {
                while (pausing) {
                    Thread.sleep(30);
                }
                // Video ahead of audio, wait
                while (i > Math.round(audio.getPosition() / samplePerFrame)) {
                }

                // Audio ahead of video, fast forward
                while (i < Math.round(audio.getPosition() / samplePerFrame)) {
                    inputStream.skip(bytesPerFrame);
                    i++;
                }

                this.readFrame();
                this.repaint();
                i++;
                if (i % 30 == 0) {
                    System.out.println(i / 30 + "s");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read Image
    private void readFrame() {
        try {
            int offset = 0;
            int numRead;
            while (offset < buffer.length && (numRead = inputStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            int ind = 0;

            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    byte r = buffer[ind];
                    byte g = buffer[ind + HEIGHT * WIDTH];
                    byte b = buffer[ind + HEIGHT * WIDTH * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, pix);

                    ind++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        g2d = (Graphics2D) g;
        g2d.drawImage(img, 0, 0, this);
    }

    public void pause() {
        this.pausing = true;
    }

    public void resume() {
        this.pausing = false;
    }

}