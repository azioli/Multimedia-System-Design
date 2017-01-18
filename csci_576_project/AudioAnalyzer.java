import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileWriter;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioAnalyzer {

    public byte[] byteArray;
    public int[] sampleArray;
    public double[] levelArray;

    public List<String> resultPathList = new ArrayList<>();

    private double[] finalWeight;

    private String fileName;
    private File file;
    private AudioFileFormat audioFileFormat;
    private AudioFormat audioFormat;
    private AudioInputStream waveStream;
    private int videoFPS = 30;
    private int audioFPS;
    private int audioSec;

    private VideoAnalyzer videoAnalyzer;

    public AudioAnalyzer(String fileName, VideoAnalyzer video) {
        try {
            this.videoAnalyzer = video;
            this.fileName = fileName;
            this.file = new File(this.fileName);
            waveStream = AudioSystem.getAudioInputStream(this.file);
            audioFileFormat = AudioSystem.getAudioFileFormat(file);
            audioFormat = waveStream.getFormat();
            audioFPS = (int) audioFormat.getFrameRate();
            audioSec = (int) (waveStream.getFrameLength() / audioFPS);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void analyze() {
        int frameSize = audioFormat.getFrameSize();
        int numBytes = (int) (frameSize * waveStream.getFrameLength());
        byteArray = new byte[numBytes];
        int totalFramesRead = 0;

        try {
            int numBytesRead, numFramesRead;

            while ((numBytesRead = waveStream.read(byteArray)) != -1) {
                numFramesRead = numBytesRead / frameSize;
                totalFramesRead += numFramesRead;
            }

            sampleArray = new int[totalFramesRead];
            int minValue = Integer.MAX_VALUE, maxValue = Integer.MIN_VALUE;
            for (int i = 0; i < totalFramesRead; i++) {
                sampleArray[i] = (int) ((byteArray[i * 2] & 0xff) + (byteArray[i * 2 + 1] << 8));
                if (sampleArray[i] < minValue) {
                    minValue = sampleArray[i];
                }
                if (sampleArray[i] > maxValue) {
                    maxValue = sampleArray[i];
                }
            }

            System.out.println(minValue + "," + maxValue);

            //Use root-mean-square average to represent the audio level corresponding to every video frame
            int levelArrayLength = totalFramesRead / audioFPS * videoFPS;
            levelArray = new double[(int) videoAnalyzer.getArrayLength()];
            for (int i = 0; i < levelArrayLength; i++) {
                double sum = 0;
                for (int j = 0; j < audioFPS / videoFPS; j++) {
                    sum += Math.pow(sampleArray[i * audioFPS / videoFPS + j], 2.0);
                }
                levelArray[i] = Math.round(Math.sqrt(sum / (audioFPS / videoFPS)));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        writeToFile();
        determineBreakPoints();
    }

    private void determineBreakPoints() {
        finalWeight = videoAnalyzer.getVideoWeight();
        List<Integer> breaks = videoAnalyzer.getBreakPoints();
        DescriptiveStatistics da = new DescriptiveStatistics(levelArray);
        double mean = da.getMean();
        double std = da.getStandardDeviation();
        // Assign weight from start to end
        for (int i = 0; i < breaks.size()-1; i++) {
            da = new DescriptiveStatistics(Arrays.copyOfRange(levelArray, breaks.get(i), breaks.get(i + 1)));
            int start = breaks.get(i);
            int end = breaks.get(i+1);
            double tempMean = Math.round(Math.abs(da.getMean()/mean - 1.0) * 10 ) / 10.0;
            double tempSTD = Math.round(da.getStandardDeviation()/std * 10 ) / 10.0;
            for (int j = start; j < end; j++) {
                finalWeight[j] += (tempMean + tempSTD);
            }
            if (end - start >= (levelArray.length / (breaks.size() - 1))) { // If shot length greater than average shot length, consider it main content
                System.out.println("----Frames " + breaks.get(i) + ": " + Math.round(finalWeight[start]) + "----");
            } else {    // If shot length is less than average, and there were main contents before, compare it against main contents, add weight
                System.out.println("||||Frames " + breaks.get(i) + ": " + Math.round(finalWeight[start]) +  "||||");
            }
        }
        da = new DescriptiveStatistics(finalWeight);
    }

    //cut video segment from startSec to endSec
    //resultPathList={ outputPath, part0_outputPath, part1_outputPath }
    public void cut(int startSec, int endSec, String outputPath) {
        int bytePerSec = audioFPS * audioFormat.getFrameSize();
        resultPathList.clear();
        try {
            resultPathList.add(outputPath);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(fileName));
            AudioInputStream firstStream = new AudioInputStream(inputStream, audioFormat, startSec * audioFPS);

            File firstFile = new File("part0" + outputPath);
            AudioSystem.write(firstStream, audioFileFormat.getType(), firstFile);
            resultPathList.add("part0_" + outputPath);
            inputStream.skip((endSec - startSec) * bytePerSec);
            AudioInputStream secondStream = new AudioInputStream(inputStream, audioFormat, (audioSec - endSec) * audioFPS);
            File secondFile = new File("part1_" + outputPath);
            AudioSystem.write(secondStream, audioFileFormat.getType(), secondFile);
            resultPathList.add("part1_" + outputPath);

            firstStream.close();
            secondStream.close();
            inputStream.close();

            firstStream = AudioSystem.getAudioInputStream(firstFile);
            secondStream = AudioSystem.getAudioInputStream(secondFile);
            AudioInputStream combinedStream = new AudioInputStream(new SequenceInputStream(firstStream, secondStream), audioFormat, firstStream.getFrameLength() + secondStream.getFrameLength());
            File combinedFile = new File(outputPath);
            AudioSystem.write(combinedStream, audioFileFormat.getType(), combinedFile);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    //skipArray stores video frames to skip
    //resultPathList={outputPath, part0_outputPath, part1_outputPath, ...}
    public void cut(boolean skipArray[], String outputPath) {
        List<int[]> segPointList = new ArrayList<>();
        resultPathList.clear();
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
        try {
            resultPathList.add(outputPath);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(fileName));
            int frameIter = 0, i = 0;
            for (i = 0; i < segPointList.size(); i++) {
                int[] segPoint = segPointList.get(i);
                int startFrame = segPoint[0];
                int endFrame = segPoint[1];
                AudioInputStream segStream = new AudioInputStream(inputStream, audioFormat, startFrame - frameIter);
                File segFile = new File("part"+i + "_" + outputPath);
                AudioSystem.write(segStream, audioFileFormat.getType(), segFile);
                resultPathList.add("part"+i + "_" + outputPath);
                inputStream.skip((endFrame-startFrame)*audioFormat.getFrameSize());
                frameIter = endFrame;
            }
            AudioInputStream segStream = new AudioInputStream(inputStream, audioFormat, waveStream.getFrameLength() - frameIter);
            File segFile = new File("part"+i + "_" + outputPath);
            AudioSystem.write(segStream, audioFileFormat.getType(), segFile);

            int subFileNum = i + 1;
            File firstFile = new File("part0_" + outputPath);
            File secondFile = new File("part1_" + outputPath);
            AudioInputStream firstStream = AudioSystem.getAudioInputStream(firstFile);
            AudioInputStream secondStream = AudioSystem.getAudioInputStream(secondFile);
            AudioInputStream combinedStream = new AudioInputStream(new SequenceInputStream(firstStream, secondStream), audioFormat, firstStream.getFrameLength() + secondStream.getFrameLength());

            for (i = 2; i < subFileNum; i++) {
                File followingFile = new File("part"+i + "_" + outputPath);
                AudioInputStream followingStream = AudioSystem.getAudioInputStream(followingFile);
                combinedStream = new AudioInputStream(new SequenceInputStream(combinedStream, followingStream), audioFormat, combinedStream.getFrameLength() + followingStream.getFrameLength());
            }

            File combinedFile = new File(outputPath);
            AudioSystem.write(combinedStream, audioFileFormat.getType(), combinedFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToFile() {
        File file;
        FileWriter writer;

        try {
            file = new File("audio.txt");
            file.createNewFile();
            writer = new FileWriter(file);
            writer.write("");
            for (int i = 0; i < levelArray.length; i++) {
                writer.write("Frame " + i + ": " + levelArray[i] + "\n");
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getResultPathList(){
        return resultPathList;
    }

    public double[] getFinalWeight() {
        return finalWeight;
    }
}
