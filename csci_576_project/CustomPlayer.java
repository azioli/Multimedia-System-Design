import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CustomPlayer {

    private AudioPlayer audioPlayer;
    private VideoPlayer videoPlayer;

    public CustomPlayer(String audioIn, String videoIn) {
        audioPlayer = new AudioPlayer(audioIn);
        videoPlayer = new VideoPlayer(videoIn, audioPlayer);

    }

    public void play() {
        videoPlayer.setLayout(new BorderLayout());
        videoPlayer.setPreferredSize(new Dimension(480, 270));
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Custom Player");
        JButton pauseButton = new JButton();
        JButton playButton = new JButton();
        pauseButton.setSize(50, 50);
        pauseButton.setVisible(true);
        pauseButton.setText("Pause");
        playButton.setSize(50, 50);
        playButton.setVisible(true);
        playButton.setText("Play");


        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                audioPlayer.pause();
                videoPlayer.pause();
            }
        });

        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                audioPlayer.resume();
                videoPlayer.resume();
            }
        });

        JPanel panel = new JPanel();
        panel.add(playButton);
        panel.add(pauseButton);
        frame.getContentPane().add(videoPlayer, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);

        Thread t1 = new Thread(audioPlayer);
        Thread t2 = new Thread(videoPlayer);

        t1.start();
        t2.start();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: CustomPlayer video.rgb audio.wav");
            return;
        }
        CustomPlayer player = new CustomPlayer(args[1], args[0]);
        player.play();
    }
}