package com.example.labxplorer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAPTURE_IMAGE = 1;
    private static final int REQUEST_UPLOAD_IMAGE = 2;
    private static final int CAMERA_PERMISSION_CODE = 101;

    private ImageView imageArea;
    private Uri photoUri;
    private File photoFile;

    private Mat originalImage; // Reference for the original experimental image
    private Mat currentImage;  // Stores the captured or uploaded image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "Failed to load OpenCV", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize UI components
        imageArea = findViewById(R.id.imageArea);
        ImageView btnCapture = findViewById(R.id.iconCapture);
        ImageView btnUpload = findViewById(R.id.iconUpload);
        Button btnSimulate = findViewById(R.id.btnSimulate);

        // Load the original experimental image as a Mat (stored in the app's drawable folder)
        try {
            InputStream is = getResources().openRawResource(R.drawable.orig_diagram); // Replace with your image
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            originalImage = new Mat();
            Mat temp = new Mat();
            Utils.bitmapToMat(bitmap, temp);
            Imgproc.cvtColor(temp, originalImage, Imgproc.COLOR_BGR2GRAY); // Convert to grayscale
        } catch (Exception e) {
            Log.e("ImageLoadError", "Failed to load original experimental image.");
        }

        // Capture button functionality
        btnCapture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            }
        });

        // Upload button functionality
        btnUpload.setOnClickListener(v -> {
            Intent uploadIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (uploadIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(uploadIntent, REQUEST_UPLOAD_IMAGE);
            } else {
                Toast.makeText(this, "Gallery not available", Toast.LENGTH_SHORT).show();
            }
        });

        // Simulate button functionality
        btnSimulate.setOnClickListener(v -> {
            if (currentImage == null) {
                Toast.makeText(this, "Please capture or upload an image first.", Toast.LENGTH_SHORT).show();
            } else if (validateImage(currentImage)) {
                Toast.makeText(this, "Image matched successfully!", Toast.LENGTH_SHORT).show();
                Intent simulateIntent = new Intent(MainActivity.this, SimulateActivity.class);
                startActivity(simulateIntent);
            } else {
                Toast.makeText(this, "Image does not match. Please upload the correct image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this,
                            getApplicationContext().getPackageName() + ".provider",
                            photoFile);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(cameraIntent, REQUEST_CAPTURE_IMAGE);
                }
            } catch (IOException e) {
                Log.e("CameraError", "Error creating file: " + e.getMessage());
                Toast.makeText(this, "Error creating image file.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String fileName = "IMG_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir("Pictures");
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAPTURE_IMAGE) {
                if (photoFile != null && photoFile.exists()) {
                    imageArea.setImageURI(photoUri); // Display the captured image
                    currentImage = loadImageFromUri(photoUri); // Store the captured image
                }
            } else if (requestCode == REQUEST_UPLOAD_IMAGE) {
                if (data != null && data.getData() != null) {
                    Uri selectedImageUri = data.getData();
                    imageArea.setImageURI(selectedImageUri); // Display the uploaded image
                    currentImage = loadImageFromUri(selectedImageUri); // Store the uploaded image
                }
            }
        } else {
            Toast.makeText(this, "Action canceled.", Toast.LENGTH_SHORT).show();
        }
    }

    private Mat loadImageFromUri(Uri imageUri) {
        try {
            InputStream is = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            Mat imageMat = new Mat();
            Utils.bitmapToMat(bitmap, imageMat);
            Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY); // Convert to grayscale
            return imageMat;
        } catch (Exception e) {
            Log.e("ImageLoadError", "Failed to load image from URI.");
            return null;
        }
    }

    private boolean validateImage(Mat inputImage) {
        if (inputImage == null || originalImage == null) {
            return false;
        }

        // Use ORB feature detection and matching
        ORB orb = ORB.create();

        MatOfKeyPoint keypointsOriginal = new MatOfKeyPoint();
        Mat descriptorsOriginal = new Mat();
        orb.detectAndCompute(originalImage, new Mat(), keypointsOriginal, descriptorsOriginal);

        MatOfKeyPoint keypointsInput = new MatOfKeyPoint();
        Mat descriptorsInput = new Mat();
        orb.detectAndCompute(inputImage, new Mat(), keypointsInput, descriptorsInput);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(descriptorsOriginal, descriptorsInput, matches);

        // Filter good matches based on distance
        List<DMatch> matchList = matches.toList();
        List<DMatch> goodMatches = new ArrayList<>();
        double maxDist = 0;
        for (DMatch match : matchList) {
            if (match.distance < 50) {
                goodMatches.add(match);
            }
            maxDist = Math.max(maxDist, match.distance);
        }

        Log.d("ImageComparison", "Good Matches: " + goodMatches.size() + ", Max Distance: " + maxDist);

        // Check if the number of good matches is sufficient
        return goodMatches.size() > 10; // Adjust threshold based on your requirements
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
