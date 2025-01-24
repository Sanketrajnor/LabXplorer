package com.example.labxplorer;

import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.imgcodecs.Imgcodecs;

public class ImageProcessor {
    public boolean matchImage(String capturedImagePath, String referenceImagePath) {
        // Load images
        Mat img1 = Imgcodecs.imread(capturedImagePath, Imgcodecs.IMREAD_GRAYSCALE);
        Mat img2 = Imgcodecs.imread(referenceImagePath, Imgcodecs.IMREAD_GRAYSCALE);

        // Check if images are loaded
        if (img1.empty() || img2.empty()) {
            return false;
        }

        // Detect keypoints and descriptors
        ORB orb = ORB.create();
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();

        orb.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
        orb.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);

        // Match features using BFMatcher
        BFMatcher matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, false);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptors1, descriptors2, matches);

        // Filter matches
        DMatch[] matchArray = matches.toArray();
        double threshold = 50.0;
        int goodMatches = 0;
        for (DMatch match : matchArray) {
            if (match.distance < threshold) {
                goodMatches++;
            }
        }

        // Return true if enough good matches are found
        return goodMatches > 10;
    }
}
