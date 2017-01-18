import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class VideoAnalyzer implements Runnable {

    private final int WIDTH = 480;
    private final int HEIGHT = 270;
    private final int BLOCK_WIDTH = 16;
    private final int BLOCK_HEIGHT = 9;
    private final int NORMALIZER = 35000;
    private final double THRESHOLD = 2.5;

    private final int Y = 0, R = 1, G = 2, B = 3;
    private String fileName;
    private InputStream inputStream;
    private byte[] buffer;
    private int frames = 0;
    private long totalFrames;

    private double[][][] currentFrame = new double[HEIGHT / BLOCK_HEIGHT][WIDTH / BLOCK_WIDTH][4], prevFrame = new double[HEIGHT / BLOCK_HEIGHT][WIDTH / BLOCK_WIDTH][4];
    private double[] yErrors, rErrors, gErrors, bErrors;
    private double[] coloWeight;
    private double[] finalWeight;

    private List<Integer> breakPoints;


    /**
     * Constructor for video analyzer with no output files
     *
     * @param fileName The file name of video to analyze
     */
    public VideoAnalyzer(String fileName) {
        this.fileName = fileName;
    }

    public static void main(String[] args) {
        String starBucks = "Starbucks_Ad_15s.rgb";
        String testData = "data_test2.rgb";
        String subway = "Subway_Ad_15s.rgb";
        VideoAnalyzer analyzer = new VideoAnalyzer(testData);
        analyzer.analyze();
        boolean skipArray[] = new boolean[9000];
        for (int i = 0; i < skipArray.length; i++) {
            if ((i >= 2400 && i <= 2850) || (i >= 5550 && i <= 6000))
                skipArray[i] = true;
            else {
                skipArray[i] = false;
            }
        }
        //analyzer.cut(skipArray, "output.rgb");
    }

    /**
     * Run the analyzer
     */
    public void run() {
        analyze();
    }

    /**
     * Analyze the video by comparing block color and luma differences between frames
     */
    public void analyze() {

        try {
            File inputFile = new File(fileName);
            inputStream = new FileInputStream(inputFile);

            long bytesPerFrame = WIDTH * HEIGHT * 3;
            totalFrames = inputFile.length() / bytesPerFrame;

            breakPoints = new ArrayList<>();

            yErrors = new double[(int) totalFrames];
            rErrors = new double[(int) totalFrames];
            gErrors = new double[(int) totalFrames];
            bErrors = new double[(int) totalFrames];
            coloWeight = new double[(int) totalFrames];

            buffer = new byte[(int) bytesPerFrame];

            finalWeight = new double[(int) totalFrames];


            readFrameBlock();
            frames++;
            int nextFrame = 1;
            while (frames < totalFrames) {
                if (frames % 30 == 0) {
                    System.out.println("Processing " + frames / 30 + "s");
                }
                readFrameBlock();
                calculateError();

                frames += nextFrame;
                skipFrames(nextFrame - 1);
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setBreakPoints();
        assignWeight();
        writeToFile();
    }

    /**
     * Set up break points based on normalized weight for each frame
     */
    private void setBreakPoints() {
        int breakCount = 10;
        breakPoints.add(0);
        for (int i = 1; i < yErrors.length - 2; i++) {
            if (coloWeight[i] >= THRESHOLD && breakCount <= 0) {
                breakCount = 10;
                breakPoints.add(i);
            }
            breakCount--;
        }
        breakPoints.add(yErrors.length - 1);
    }

    /**
     * Assign weights to frames based on break points and shot lengths
     */
    private void assignWeight() {
        double weight = 0;
        int averageShotLength = (int)totalFrames / (breakPoints.size()-1);
        for (int i = 0; i < breakPoints.size() - 1; i++) {
            int start = breakPoints.get(i);
            int end = breakPoints.get(i + 1);
            if (end - start <= averageShotLength * 0.7) { // If the duration between two scene changes are too short (Less than average shot length)
                weight += 0.3;
            } else {
                weight = 0.0;
            }
            for (int k = start; k < end; k++) {
                finalWeight[k] += weight;
            }
        }
        weight = 0.0;
        for (int i = breakPoints.size() - 1; i > 0; i--) {
            int start = breakPoints.get(i);
            int end = breakPoints.get(i - 1);
            if (start - end <= averageShotLength * 0.7) { // If the duration between two scene changes are too short (Less than average shot length)
                weight += 0.3;
            } else {
                weight = 0.0;
            }
            for (int k = start-1; k >= end; k--) {
                finalWeight[k] += weight;
            }
        }
    }

    /**
     * Skip the next WIDTH*HEIGHT*3*toSkip bytes in the inputStream
     *
     * @param toSkip the number of frames to skip
     */
    private void skipFrames(int toSkip) {
        if (toSkip == 0)
            return;
        int temp = WIDTH * HEIGHT * 3 * toSkip;
        try {
            inputStream.skip(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Read Frame
    private void readFrameBlock() {

        prevFrame = currentFrame;
        currentFrame = new double[HEIGHT / BLOCK_HEIGHT][WIDTH / BLOCK_WIDTH][4];

        try {
            int offset = 0;
            int numRead;
            while (offset < buffer.length && (numRead = inputStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            int ind = 0;
            for (int y = 0; y < HEIGHT; y += BLOCK_HEIGHT) {
                for (int x = 0; x < WIDTH; x += BLOCK_WIDTH) {
                    double luma_mean = 0.0, r_mean = 0.0, g_mean = 0.0, b_mean = 0.0;

                    for (int j = 0; j < BLOCK_HEIGHT; j++) {
                        for (int i = 0; i < BLOCK_WIDTH; i++) {
                            int r = buffer[ind] & 0xff;
                            int g = buffer[ind + HEIGHT * WIDTH] & 0xff;
                            int b = buffer[ind + HEIGHT * WIDTH * 2] & 0xff;

                            double luma = 0.299 * r + 0.587 * g + 0.114 * b;
                            luma_mean += luma;
                            r_mean += r;
                            g_mean += g;
                            b_mean += b;

                            ind++;
                        }
                    }
                    int j = y / BLOCK_HEIGHT, i = x / BLOCK_WIDTH;

                    luma_mean = luma_mean / BLOCK_HEIGHT / BLOCK_WIDTH;
                    r_mean = r_mean / BLOCK_HEIGHT / BLOCK_WIDTH;
                    g_mean = g_mean / BLOCK_HEIGHT / BLOCK_WIDTH;
                    b_mean = b_mean / BLOCK_HEIGHT / BLOCK_WIDTH;
                    currentFrame[j][i][Y] = luma_mean;
                    currentFrame[j][i][R] = r_mean;
                    currentFrame[j][i][G] = g_mean;
                    currentFrame[j][i][B] = b_mean;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Calculate R, G, B, and luma difference of blocks between frames
     */
    private void calculateError() {
        double errorY = 0.0, errorR = 0.0, errorG = 0.0, errorB = 0.0;
        for (int j = 0; j < currentFrame.length; j++) {
            for (int i = 0; i < currentFrame[0].length; i++) {
                errorY += Math.abs(currentFrame[j][i][Y] - prevFrame[j][i][Y]);
                errorR += Math.abs(currentFrame[j][i][R] - prevFrame[j][i][R]);
                errorG += Math.abs(currentFrame[j][i][G] - prevFrame[j][i][G]);
                errorB += Math.abs(currentFrame[j][i][B] - prevFrame[j][i][B]);
            }
        }
        yErrors[frames] = Math.round(errorY);
        rErrors[frames] = Math.round(errorR);
        gErrors[frames] = Math.round(errorG);
        bErrors[frames] = Math.round(errorB);
        coloWeight[frames] = Math.round((errorY / NORMALIZER + errorR / NORMALIZER + errorG / NORMALIZER + errorB / NORMALIZER) * 10) / 10.0;
    }

    private void writeToFile() {
        File file;
        FileWriter writer;

        try {
            file = new File("breaks2.txt");
            file.createNewFile();
            writer = new FileWriter(file);
            writer.write("");
            /*for (int i = 0; i < isAds.length; i++) {
                //writer.write("Frame " + (i+1) + ": " + weight[i+1] + " Y: " + yErrors[i+1] + " | R: " + rErrors[i+1] + " | G: " + gErrors[i+1] + " | B: " + bErrors[i+1] + "\n");
                if (isAds[i])
                    writer.write("Frame " + (i) + ": " + weight[i] + " Y: " + isAds[i] + "\n");
            }*/

            for (Integer i: breakPoints) {
                writer.write("Frame " + (i) + ": " + coloWeight[i] + " Weight: " + (Math.round(finalWeight[i] * 10) / 10.0) + "\n");
            }

            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cut(boolean[] skipArray, String outputFileName) {
        try {
            File inputFile = new File(fileName);
            inputStream = new FileInputStream(inputFile);
            FileOutputStream outputStream = new FileOutputStream(outputFileName);

            long bytesPerFrame = WIDTH * HEIGHT * 3;

            for (int i = 0; i < totalFrames; i++) {
                if (i % 30 == 0) {
                    System.out.println("Processing " + i / 30 + "s");
                }
                if (skipArray[i]) {
                    inputStream.skip(bytesPerFrame);
                } else {
                    int offset = 0;
                    int numRead;
                    while (offset < buffer.length && (numRead = inputStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                        offset += numRead;
                    }
                    outputStream.write(buffer);
                }
            }
            outputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double[] getVideoWeight() {
        return finalWeight;
    }

    public List<Integer> getBreakPoints() {
        return breakPoints;
    }

    public long getArrayLength() {
        return totalFrames;
    }
}