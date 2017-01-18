import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.SequenceInputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by hanzh on 12/5/2016.
 */
public class MediaFusor {

    private final int WIDTH = 480;
    private final int HEIGHT = 270;
    private final int videoFPS=30;

    private MediaCleaner cleaner;
    private BrandDetector detector;
    private String inputVideo, inputAudio, adVideo, adAudio, outputVideo, outputAudio;


    public static void main(String[] args) {

        if (args.length != 7) {
            System.out.println("Usage: MediaFusor inputVideo.rgb inputAudio.wav adLogo.rgb adVideo.rgb adAudio.rgb outputVideo.rgb outputAudio.wav");
            return;
        }

        try{
            MediaFusor fusor = new MediaFusor(args[0],args[1],args[2],args[3],args[4],args[5],args[6]);
            fusor.run();
            CustomPlayer player = new CustomPlayer("fusedAudio.wav", "fusedVideo.rgb");
            player.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public MediaFusor(String inputVideo, String inputAudio, String brandImg, String adVideo, String adAudio, String outputVideo, String outputAudio) {
        this.cleaner = new MediaCleaner(inputVideo, inputAudio, outputVideo, outputAudio);
        this.detector = new BrandDetector(inputVideo, brandImg);
        this.inputVideo = inputVideo;
        this.inputAudio = inputAudio;
        this.adVideo = adVideo;
        this.adAudio = adAudio;
        this.outputVideo = outputVideo;
        this.outputAudio = outputAudio;
    }

    public void run() {
        try {
            System.out.println("Matching brand in Video...");
            int bestMatchFrame = detector.run() * 30;
            cleaner.clean();
            if(bestMatchFrame < 0){
                cleaner.cut();
                return;
            }
            boolean[] skipArray = cleaner.getSkipArray();
            //int logoFrameNum = bestMatchFrame * 30;
            //int frameNum = findInsertionPos(skipArray, logoFrameNum);
            int frameNum = findInsertionPos(skipArray, bestMatchFrame);

            fuseVideo(skipArray, frameNum);
            fuseAudio(skipArray,frameNum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int findInsertionPos(boolean[] skipArray, int logoFrame) {
        boolean adStarted = false;
        int adPos = 0;
        for (int i = 0; i < skipArray.length; i++) {
            if (skipArray[i]) {
                if (!adStarted) {
                    adStarted = true;
                    adPos = Math.abs(logoFrame - i) < Math.abs(logoFrame - adPos) ? i : adPos;
                }
            } else {
                adStarted = false;
            }
        }
        return adPos;
    }

    public void fuseVideo(boolean[] skipArray, int frameNum) {
        try {
            boolean fused = false;
            File mainFile = new File(inputVideo);
            File adFile = new File(adVideo);

            FileInputStream mainStream = new FileInputStream(mainFile);
            FileInputStream adStream = new FileInputStream(adFile);
            FileOutputStream outputStream = new FileOutputStream(outputVideo);

            long bytesPerFrame = WIDTH * HEIGHT * 3;
            byte[] buffer = new byte[(int) bytesPerFrame];

            int mainFrames = (int) (mainFile.length() / bytesPerFrame);
            int adFrames = (int) (adFile.length() / bytesPerFrame);

            System.out.println("Fusing video...");
            for (int i = 0; i < mainFrames; i++) {
                if (!fused && i == frameNum) {
                    System.out.println("Adding Advertisements at time: " + frameNum/30);
                    fused = true;
                    for (int j = 0; j < adFrames; j++) {
                        int offset = 0;
                        int numRead;
                        while (offset < buffer.length && (numRead = adStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                            offset += numRead;
                        }
                        outputStream.write(buffer);

                    }
                }

                if (skipArray[i]) {
                    mainStream.skip(bytesPerFrame);
                    continue;
                }


                int offset = 0;
                int numRead;
                while (offset < buffer.length && (numRead = mainStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                    offset += numRead;
                }
                outputStream.write(buffer);

            }
            mainStream.close();
            adStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fuseVideo(int frameNum) {
        try {
            boolean fused = false;
            File mainFile = new File(inputVideo);
            File adFile = new File(adVideo);

            FileInputStream mainStream = new FileInputStream(mainFile);
            FileInputStream adStream = new FileInputStream(adFile);
            FileOutputStream outputStream = new FileOutputStream(outputVideo);

            long bytesPerFrame = WIDTH * HEIGHT * 3;
            byte[] buffer = new byte[(int) bytesPerFrame];

            int mainFrames = (int) (mainFile.length() / bytesPerFrame);
            int adFrames = (int) (adFile.length() / bytesPerFrame);

            System.out.println("Fusing video...");
            for (int i = 0; i < mainFrames; i++) {
                if (!fused && i == frameNum) {
                    System.out.println("Adding Advertisements at time: " + frameNum/30);
                    fused = true;
                    for (int j = 0; j < adFrames; j++) {
                        int offset = 0;
                        int numRead;
                        while (offset < buffer.length && (numRead = adStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                            offset += numRead;
                        }
                        outputStream.write(buffer);

                    }
                }
                int offset = 0;
                int numRead;
                while (offset < buffer.length && (numRead = mainStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                    offset += numRead;
                }
                outputStream.write(buffer);

            }
            mainStream.close();
            adStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fuseAudio(int frameNum) {
        try{
            System.out.println("Fusing audio...");
            File inputAudioFile = new File(inputAudio);
            AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(inputAudioFile);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(inputAudioFile);

            long inputAudioLength = inputStream.getFrameLength();
            AudioFormat audioFormat = inputStream.getFormat();
            int audioFPS = (int) audioFormat.getFrameRate();

            AudioInputStream firstStream = new AudioInputStream(inputStream,audioFormat, frameNum/videoFPS*audioFPS);
            long firstAudioLength = firstStream.getFrameLength();
            File firstPartFile = new File("first_part.wav");
            AudioSystem.write(firstStream, audioFileFormat.getType(), firstPartFile);

            AudioInputStream lastStream = new AudioInputStream(inputStream, audioFormat, inputAudioLength-firstAudioLength);
            File lastPartFile = new File("last_part.wav");
            AudioSystem.write(lastStream, audioFileFormat.getType(), lastPartFile);

            firstStream.close();lastStream.close();

            firstStream = AudioSystem.getAudioInputStream(firstPartFile);
            AudioInputStream adStream = AudioSystem.getAudioInputStream(new File(adAudio));
            AudioInputStream combinedStream = new AudioInputStream(new SequenceInputStream(firstStream, adStream), audioFormat, firstStream.getFrameLength() + adStream.getFrameLength());

            lastStream = AudioSystem.getAudioInputStream(lastPartFile);
            combinedStream = new AudioInputStream(new SequenceInputStream(combinedStream, lastStream), audioFormat, combinedStream.getFrameLength() + lastStream.getFrameLength());
            AudioSystem.write(combinedStream, audioFileFormat.getType(), new File(outputAudio));

            firstStream.close();lastStream.close();
            firstPartFile.deleteOnExit(); lastPartFile.deleteOnExit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fuseAudio(boolean skipArray[], int frameNum) {
        try{
            System.out.println("Fusing audio...");
            File inputAudioFile = new File(inputAudio);
            AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(inputAudioFile);
            AudioInputStream waveStream = AudioSystem.getAudioInputStream(inputAudioFile);

            AudioFormat audioFormat = waveStream.getFormat();
            int audioFPS = (int) audioFormat.getFrameRate();
            frameNum = frameNum * audioFPS / videoFPS;

            List<int[]> segPointList = new ArrayList<>();
            int iter = 0;
            while (iter < skipArray.length) {
                if (!skipArray[iter]) {
                    iter++;
                    continue;
                }
                int[] framePair = new int[2];
                framePair[0] = iter * audioFPS / videoFPS;
                while (iter < skipArray.length && skipArray[iter]) {
                    iter++;
                }
                if (iter < skipArray.length) {
                    framePair[1] = iter * audioFPS / videoFPS;
                } else {
                    framePair[1] = (int) waveStream.getFrameLength();
                }
                segPointList.add(framePair);
            }
            waveStream.close();

            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(inputAudio));
            int frameIter = 0, i = 0;
            Iterator<int[]> iterator = segPointList.iterator();
            for (i = 0; i < segPointList.size()+1; i++) {
                int[] segPoint = iterator.next();
                int startFrame = segPoint[0];
                int endFrame = segPoint[1];
                AudioInputStream segStream = new AudioInputStream(inputStream, audioFormat, startFrame - frameIter);
                File segFile = new File("part"+i + "_" + outputAudio);
                AudioSystem.write(segStream, audioFileFormat.getType(), segFile);
                inputStream.skip((endFrame-startFrame)*audioFormat.getFrameSize());
                frameIter = endFrame;

                if(startFrame == frameNum){
//                    AudioInputStream tempStream = AudioSystem.getAudioInputStream(segFile);
                    File adFile = new File("part" + ++i + "_" + outputAudio);
                    AudioInputStream adStream = AudioSystem.getAudioInputStream(new File(adAudio));
//                    AudioInputStream adAdded = new AudioInputStream(new SequenceInputStream(tempStream, adStream), audioFormat, tempStream.getFrameLength() + adStream.getFrameLength());
                    AudioSystem.write(adStream, audioFileFormat.getType(), adFile);
                }
            }
            AudioInputStream segStream = new AudioInputStream(inputStream, audioFormat, waveStream.getFrameLength() - frameIter);
            File segFile = new File("part"+i + "_" + outputAudio);
            AudioSystem.write(segStream, audioFileFormat.getType(), segFile);

            int subFileNum = i + 1;
            File firstFile = new File("part0_" + outputAudio);
            File secondFile = new File("part1_" + outputAudio);
            AudioInputStream firstStream = AudioSystem.getAudioInputStream(firstFile);
            AudioInputStream secondStream = AudioSystem.getAudioInputStream(secondFile);
            AudioInputStream combinedStream = new AudioInputStream(new SequenceInputStream(firstStream, secondStream), audioFormat, firstStream.getFrameLength() + secondStream.getFrameLength());

            for (i = 2; i < subFileNum; i++) {
                File followingFile = new File("part"+i + "_" + outputAudio);
                AudioInputStream followingStream = AudioSystem.getAudioInputStream(followingFile);
                combinedStream = new AudioInputStream(new SequenceInputStream(combinedStream, followingStream), audioFormat, combinedStream.getFrameLength() + followingStream.getFrameLength());
            }

            File combinedFile = new File(outputAudio);
            AudioSystem.write(combinedStream, audioFileFormat.getType(), combinedFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
