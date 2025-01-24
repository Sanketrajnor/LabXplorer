package com.example.labxplorer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;

public class CaptureActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 201;

    private ImageCapture imageCapture;
    private File capturedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            setupCamera(); // Set up the camera if permissions are granted
        }

        findViewById(R.id.capture).setOnClickListener(v -> captureImage());
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera(); // Set up the camera if permission is granted
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // Set up the camera for capturing images
    private void setupCamera() {
        // Initialize the camera provider asynchronously
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Get the camera provider and bind the lifecycle to it
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // PreviewView to show the camera feed
                PreviewView previewView = findViewById(R.id.previewView);

                // Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Initialize ImageCapture
                imageCapture = new ImageCapture.Builder().build();

                // Create CameraSelector for the back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Bind the camera to the lifecycle (this Activity)
                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                Toast.makeText(this, "Camera setup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Capture the image when the button is clicked
    private void captureImage() {
        capturedImageFile = new File(getExternalFilesDir(null), "capturedImage.jpg");

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(capturedImageFile).build();
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                // Image captured successfully
                Toast.makeText(CaptureActivity.this, "Image Captured", Toast.LENGTH_SHORT).show();

                // Process the captured image (you can replace with your own processing logic)
                if (processImage(capturedImageFile)) {
                    // If matching is successful, navigate to the AR activity
                    Intent intent = new Intent(CaptureActivity.this, ArActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(CaptureActivity.this, "Diagram not matched!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(ImageCaptureException exception) {
                // Error occurred during image capture
                Toast.makeText(CaptureActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Dummy image processing method for illustration
    private boolean processImage(File imageFile) {
        // Add your image processing logic here
        // Return true if matching is successful, false otherwise
        return true;  // Replace with actual logic
    }
}
