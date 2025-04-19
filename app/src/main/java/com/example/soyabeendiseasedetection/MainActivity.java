package com.example.soyabeendiseasedetection;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int GALLERY_REQUEST = 200;
    private static final int PERMISSION_REQUEST_CODE = 123;

    private enum PendingAction { NONE, CAMERA, GALLERY }

    private PendingAction pendingAction = PendingAction.NONE;

    private ImageView imagePreview;
    private TextView predictionText;
    private TextView noImageText;
    private MaterialCardView resultCard;
    private TFLiteClassifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up toolbar - REMOVED setSupportActionBar to avoid conflicts
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Soybean Disease Detector");
        // We're not setting it as support action bar to avoid conflicts
        // setSupportActionBar(toolbar);

        // Find views
        MaterialButton captureButton = findViewById(R.id.captureFromCameraButton);
        MaterialButton galleryButton = findViewById(R.id.selectFromGalleryButton);
        imagePreview = findViewById(R.id.imagePreview);
        predictionText = findViewById(R.id.predictionText);
        noImageText = findViewById(R.id.noImageText);
        resultCard = findViewById(R.id.resultCard);

        // Initially hide the result card until we have a prediction
        resultCard.setVisibility(View.GONE);

        try {
            classifier = new TFLiteClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Model failed to load", Toast.LENGTH_LONG).show();
        }

        // Apply button animations
        captureButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            pendingAction = PendingAction.CAMERA;
            if (checkPermissions()) openCamera();
        });

        galleryButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            pendingAction = PendingAction.GALLERY;
            if (checkPermissions()) openGallery();
        });
    }

    private boolean checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 1);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            Toast.makeText(this, "Permissions granted. Try again.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please grant all permissions to proceed.", Toast.LENGTH_LONG).show();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Bitmap bitmap = null;

            if (requestCode == CAMERA_REQUEST) {
                bitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == GALLERY_REQUEST) {
                Uri imageUri = data.getData();
                try {
                    if (Build.VERSION.SDK_INT >= 29) {
                        ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                        bitmap = ImageDecoder.decodeBitmap(source);

                        // Ensure the bitmap is mutable or in ARGB_8888 format
                        if (bitmap != null && bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        }
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        // Ensure the bitmap is mutable or in ARGB_8888 format
                        if (bitmap != null && bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Proceed with classification if bitmap is valid
            if (bitmap != null) {
                // Hide the "No image selected" text
                noImageText.setVisibility(View.GONE);
                
                // Apply fade-in animation to the image
                imagePreview.setAlpha(0f);
                imagePreview.setImageBitmap(bitmap);
                imagePreview.animate().alpha(1f).setDuration(500).start();
                
                // Show loading state
                predictionText.setText("Analyzing leaf...");
                resultCard.setVisibility(View.VISIBLE);
                
                // Perform classification
                String prediction = classifier.classify(bitmap);
                
                // Show result with animation
                resultCard.setAlpha(0.7f);
                resultCard.animate().alpha(1f).setDuration(300).start();
                predictionText.setText("Prediction: " + prediction);
            } else {
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}