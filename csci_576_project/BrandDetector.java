import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.features2d.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;

public class BrandDetector {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        String videoName = "img/data_test2.rgb";
        String brandName = "img/nfl_logo.rgb";

        BrandDetector bd = new BrandDetector(videoName, brandName);
        bd.run();
    }

    private final int WIDTH = 480;
    private final int HEIGHT = 270;
    private final int FPS = 30;
    private final int bytesPerFrame = WIDTH * HEIGHT * 3;

    private final double SCENE_SCALE = 2.0;
    private final int FEATURE_DETECTOR_TYPE = FeatureDetector.PYRAMID_SIFT;
    private final int DESCRIPTOR_EXTRACTOR_TYPE = DescriptorExtractor.SIFT;
    private final int DESCRIPTOR_MATCHER_TYPE = DescriptorMatcher.FLANNBASED;

    private final float nndrRatio = 0.9f;
    private final int minGoodMatchSize = 15;
    private final int minTrainPointSize = 15;

    private boolean brandFound=false;
    private int counter = 0;

    private final int futherSearchCount=3;

    private List<Integer> matchedSeconds = new ArrayList<>();
    private String videoPath;
    private int totalFrames;
    private String imagePath;
    private InputStream inputStream;
    private BufferedImage brandImg;

    public int run(){
        int bestSec=-1;
        try {
            bestSec = this.detect();
            if (bestSec >= 0) {
                System.out.println("Best Matched Second Found:" + bestSec);
                BufferedImage result=ImageIO.read(new File("result.png"));
                displayImage(result);
            } else {
                System.out.println("Cannot Find Brand!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bestSec;
    }
    public BrandDetector(String videoPath, String imagePath) {
        this.videoPath = videoPath;
        this.imagePath = imagePath;
        try {
            File videoFile = new File(this.videoPath);
            inputStream = new FileInputStream(videoFile);
            totalFrames = (int) (videoFile.length() / bytesPerFrame);

            File brandFile = new File(this.imagePath);
            InputStream brandStream = new FileInputStream(brandFile);
            byte[] bytes = new byte[(int) (brandFile.length())];
            int offset = 0;
            int numRead;
            while (offset < bytes.length && (numRead = brandStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            brandStream.close();
            brandImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            int ind = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    byte r = bytes[ind];
                    byte g = bytes[ind + HEIGHT * WIDTH];
                    byte b = bytes[ind + HEIGHT * WIDTH * 2];
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    brandImg.setRGB(x, y, pix);
                    ind++;
                }
            }
            ImageIO.write(brandImg, "jpg", new File("tmp_brandImg.jpg"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int detect() {
        int bestMatchFrame = -1;
        try {
            byte[] buffer = new byte[bytesPerFrame];
            int maxMatchSize = Integer.MIN_VALUE;

            Mat brandMat = Highgui.imread("tmp_brandImg.jpg", Highgui.CV_LOAD_IMAGE_COLOR);

            Path path = FileSystems.getDefault().getPath("tmp_brandImg.jpg");
            Files.deleteIfExists(path);

            MatOfKeyPoint brandKeyPoints = new MatOfKeyPoint();
            FeatureDetector featureDetector = FeatureDetector.create(FEATURE_DETECTOR_TYPE);
            featureDetector.detect(brandMat, brandKeyPoints);

            MatOfKeyPoint brandDescriptors = new MatOfKeyPoint();
            DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DESCRIPTOR_EXTRACTOR_TYPE);

            descriptorExtractor.compute(brandMat, brandKeyPoints, brandDescriptors);

            DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DESCRIPTOR_MATCHER_TYPE);

            for (int i = 0; i < totalFrames/FPS ; i++) {
                int offset = 0;
                int numRead;
                while (offset < buffer.length && (numRead = inputStream.read(buffer, offset, buffer.length - offset)) >= 0) {
                    offset += numRead;
                }
                inputStream.skip((FPS - 1) * bytesPerFrame);

                BufferedImage tmpImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
                int ind = 0;
                for (int y = 0; y < HEIGHT; y++) {
                    for (int x = 0; x < WIDTH; x++) {
                        byte r = buffer[ind];
                        byte g = buffer[ind + HEIGHT * WIDTH];
                        byte b = buffer[ind + HEIGHT * WIDTH * 2];

                        int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                        tmpImg.setRGB(x, y, pix);
                        ind++;
                    }
                }

                BufferedImage sceneMat = new BufferedImage((int) (tmpImg.getWidth() * SCENE_SCALE), (int) (tmpImg.getHeight() * SCENE_SCALE), BufferedImage.TYPE_INT_RGB);

                Graphics g = sceneMat.createGraphics();
                g.drawImage(tmpImg, 0, 0, sceneMat.getWidth(), sceneMat.getHeight(), null);
                g.dispose();

                //displayImage(sceneMat);

                int matchSize = match(brandMat, sceneMat, brandKeyPoints, brandDescriptors, featureDetector, descriptorExtractor, descriptorMatcher,maxMatchSize);
                if (matchSize < 0) {
                    System.out.println("not match, second " + i);
                    if(brandFound){
                        counter++;
                        if(counter >= futherSearchCount){
                            break;
                        }
                    }
                    continue;
                } else {
                    System.out.println("match size " + matchSize + ", second " + i);
                    matchedSeconds.add(i);
                    if (maxMatchSize < matchSize) {
                        maxMatchSize = matchSize;
                        bestMatchFrame = i;
                    }
                    BufferedImage result=ImageIO.read(new File("result.png"));
                    displayImage(result);

                    brandFound = true;
                    counter = 0;
                }
            }
            //path = FileSystems.getDefault().getPath("tmpScene.png");
            //Files.deleteIfExists(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bestMatchFrame;
    }

    public int match(Mat brandMat,BufferedImage scene, MatOfKeyPoint brandKeyPoints, MatOfKeyPoint brandDescriptors, FeatureDetector featureDetector, DescriptorExtractor descriptorExtractor, DescriptorMatcher descriptorMatcher,int maxMatchSize) {
        try {
            ImageIO.write(scene, "png", new File("tmpScene.png"));
            Mat sceneMat = Highgui.imread("tmpScene.png", Highgui.CV_LOAD_IMAGE_COLOR);

            //Path path = FileSystems.getDefault().getPath("tmpScene.png");
            //Files.deleteIfExists(path);

            MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
            MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
            featureDetector.detect(sceneMat, sceneKeyPoints);
            descriptorExtractor.compute(sceneMat, sceneKeyPoints, sceneDescriptors);

            List<MatOfDMatch> matches = new LinkedList<>();

            descriptorMatcher.knnMatch(brandDescriptors, sceneDescriptors, matches, 2);

            LinkedList<DMatch> goodMatchesList = new LinkedList<>();

            for (int i = 0; i < matches.size(); i++) {
                MatOfDMatch matofDMatch = matches.get(i);
                DMatch[] dmatcharray = matofDMatch.toArray();
                DMatch m1 = dmatcharray[0];
                DMatch m2 = dmatcharray[1];

                if (m1.distance <= m2.distance * nndrRatio) {
                    goodMatchesList.addLast(m1);
                }
            }

            if (goodMatchesList.size() < minGoodMatchSize) {
                return -1;
            }
            List<Point> pts1 = new ArrayList<>();
            List<Point> pts2 = new ArrayList<>();

            for (int i = 0; i < goodMatchesList.size(); i++) {
                pts1.add(brandKeyPoints.toList().get(goodMatchesList.get(i).queryIdx).pt);
                pts2.add(sceneKeyPoints.toList().get(goodMatchesList.get(i).trainIdx).pt);
            }

            Mat outputMask = new Mat();
            MatOfPoint2f pts1Mat = new MatOfPoint2f();
            pts1Mat.fromList(pts1);
            MatOfPoint2f pts2Mat = new MatOfPoint2f();
            pts2Mat.fromList(pts2);

            Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 3, outputMask);

            LinkedList<DMatch> betterMatchesList = new LinkedList<DMatch>();
            for (int i = 0; i < goodMatchesList.size(); i++) {
                if (outputMask.get(i, 0)[0] != 0.0) {
                    betterMatchesList.add(goodMatchesList.get(i));
                }
            }

            Set<Integer> betterTrainPoints = new HashSet<>();
            for (int i = 0; i < betterMatchesList.size(); i++) {
                betterTrainPoints.add(betterMatchesList.get(i).trainIdx);
            }

            if (betterTrainPoints.size() < minTrainPointSize) {
                return -1;
            } else {
                if(betterTrainPoints.size() > maxMatchSize) {
                    MatOfDMatch betterMatches = new MatOfDMatch();
                    betterMatches.fromList(betterMatchesList);
                    Mat matchOutput = new Mat(sceneMat.rows() * 2, sceneMat.cols() * 2, Highgui.CV_LOAD_IMAGE_COLOR);
                    Scalar matchColor = new Scalar(0, 255, 0);
                    Scalar newKeyPointColor = new Scalar(255, 0, 0);
                    Features2d.drawMatches(brandMat, brandKeyPoints, sceneMat, sceneKeyPoints, betterMatches, matchOutput, matchColor, newKeyPointColor, new MatOfByte(), 2);
                    Highgui.imwrite("matchOutput.jpg", matchOutput);

                    pts1.clear();pts2.clear();
                    for (int i = 0; i < betterMatchesList.size(); i++) {
                        pts1.add(brandKeyPoints.toList().get(betterMatchesList.get(i).queryIdx).pt);
                        pts2.add(sceneKeyPoints.toList().get(betterMatchesList.get(i).trainIdx).pt);
                    }

                    pts1Mat = new MatOfPoint2f();
                    pts1Mat.fromList(pts1);
                    pts2Mat = new MatOfPoint2f();
                    pts2Mat.fromList(pts2);

                    Mat homograph=Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, 3, outputMask);

                    Mat brandCorners = new Mat(4, 1, CvType.CV_32FC2);
                    Mat sceneCorners = new Mat(4, 1, CvType.CV_32FC2);

                    brandCorners.put(0, 0, new double[]{0, 0});
                    brandCorners.put(1, 0, new double[]{brandMat.cols(), 0});
                    brandCorners.put(2, 0, new double[]{brandMat.cols(), brandMat.rows()});
                    brandCorners.put(3, 0, new double[]{0, brandMat.rows()});

                    Core.perspectiveTransform(brandCorners, sceneCorners, homograph);

                    Mat resultMat = new Mat();
                    sceneMat.copyTo(resultMat);

                    Core.line(resultMat, new Point(sceneCorners.get(0, 0)), new Point(sceneCorners.get(1, 0)), new Scalar(0, 255, 0), 4);
                    Core.line(resultMat, new Point(sceneCorners.get(1, 0)), new Point(sceneCorners.get(2, 0)), new Scalar(0, 255, 0), 4);
                    Core.line(resultMat, new Point(sceneCorners.get(2, 0)), new Point(sceneCorners.get(3, 0)), new Scalar(0, 255, 0), 4);
                    Core.line(resultMat, new Point(sceneCorners.get(3, 0)), new Point(sceneCorners.get(0, 0)), new Scalar(0, 255, 0), 4);

                    Highgui.imwrite("result.png", resultMat);

                }
                return betterTrainPoints.size();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    //Methods for debugging
    public void displayImage(Image img2) {
        ImageIcon icon = new ImageIcon(img2);
        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(img2.getWidth(null) + 50, img2.getHeight(null) + 50);
        JLabel lbl = new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
    }

    public void displayImage(int sec) {
        try {
            InputStream is = new FileInputStream(new File(videoPath));
            is.skip(sec * FPS * bytesPerFrame);
            byte[] bytes = new byte[bytesPerFrame];
            int offset = 0;
            int numRead;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            is.close();
            BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            int ind = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    byte r = bytes[ind];
                    byte g = bytes[ind + HEIGHT * WIDTH];
                    byte b = bytes[ind + HEIGHT * WIDTH * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }
            ImageIcon icon = new ImageIcon(img);
            JFrame frame = new JFrame();
            frame.setLayout(new FlowLayout());
            frame.setSize(img.getWidth(null) + 50, img.getHeight(null) + 50);
            JLabel lbl = new JLabel();
            lbl.setIcon(icon);
            frame.add(lbl);
            frame.setVisible(true);

            //File outputFile = new File("img/" + sec + "_scene.png");
            //ImageIO.write(img, "png", outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


