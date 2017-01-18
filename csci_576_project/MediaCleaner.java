import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Created by hanzh on 12/5/2016.
 */
public class MediaCleaner {

    private String outputVideo, outputAudio;
    private VideoAnalyzer videoAnalyzer;
    private AudioAnalyzer audioAnalyzer;
    private double[] weightArray;
    private boolean[] skipArray;

    public static void main(String[] args) {

        if (args.length != 4) {
            System.out.println("Usage: MediaCleaner inputVideo.rgb inputAudio.wav outputVideo.rgb outputAudio.wav");
            return;
        }
        MediaCleaner cleaner = new MediaCleaner(args[0], args[1], args[2], args[3]);
        cleaner.clean();
        CustomPlayer player = new CustomPlayer(args[3], args[2]);
        player.play();
    }

    public MediaCleaner(String inputVideo, String inputAudio, String videoOut, String audioOut) {
        videoAnalyzer = new VideoAnalyzer(inputVideo);
        audioAnalyzer = new AudioAnalyzer(inputAudio, videoAnalyzer);
        outputVideo = videoOut;
        outputAudio = audioOut;
    }


    public void clean() {

        videoAnalyzer.analyze();
        audioAnalyzer.analyze();
        weightArray = audioAnalyzer.getFinalWeight();
        skipArray = new boolean[weightArray.length];
        DescriptiveStatistics da = new DescriptiveStatistics(weightArray);
        double threshold = da.getMean() + da.getStandardDeviation();

        for (int i = 0; i < skipArray.length; i++) {
            if (weightArray[i] > threshold) {
                skipArray[i] = true;
            }
        }
    }

    public void cut() {
        System.out.println("Generating video file, please wait...");
        videoAnalyzer.cut(skipArray, outputVideo);
        System.out.println("Generating audio file, please wait...");
        audioAnalyzer.cut(skipArray, outputAudio);
        System.out.println("All finished!");
    }

    public boolean[] getSkipArray() {
        return skipArray;
    }
}
