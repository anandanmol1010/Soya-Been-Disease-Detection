package com.example.soyabeendiseasedetection;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 5000; // 4 seconds
    private static final int PROGRESS_INCREMENT = 5;
    private static final long PROGRESS_DELAY = 100; // 100ms between progress updates

    private ProgressBar progressBar;
    private TextView loadingText;
    private int currentProgress = 0;
    private Handler progressHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find views
        ImageView logoImageView = findViewById(R.id.logoImageView);
        TextView titleTextView = findViewById(R.id.splashTitle);
        TextView taglineTextView = findViewById(R.id.splashTagline);
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);
        
        // Create fade-in animation for logo and title
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(800);
        
        // Apply animation to logo and title
        logoImageView.startAnimation(fadeIn);
        titleTextView.startAnimation(fadeIn);
        
        // Delayed animation for tagline
        new Handler().postDelayed(() -> {
            Animation taglineFadeIn = new AlphaAnimation(0.0f, 1.0f);
            taglineFadeIn.setDuration(800);
            taglineTextView.setVisibility(View.VISIBLE);
            taglineTextView.startAnimation(taglineFadeIn);
            
            // Start progress bar after tagline appears
            startProgressAnimation();
        }, 1000);
    }
    
    private void startProgressAnimation() {
        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentProgress < 100) {
                    currentProgress += PROGRESS_INCREMENT;
                    progressBar.setProgress(currentProgress);
                    
                    // Update loading text with percentage
                    loadingText.setText("Loading... " + currentProgress + "%");
                    
                    // Continue updating progress
                    progressHandler.postDelayed(this, PROGRESS_DELAY);
                } else {
                    // Progress complete, navigate to MainActivity
                    navigateToMainActivity();
                }
            }
        }, PROGRESS_DELAY);
    }
    
    private void navigateToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to prevent memory leaks
        progressHandler.removeCallbacksAndMessages(null);
    }
}
