```java
package com.deroid.unbrickernota;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION = 7;

    private PreviewView preview;
    private ImageCapture capture;
    private Camera camera;

    private SeekBar zoomBar;
    private TextView zoomText;
    private TextView modeText;
    private TextView status;

    private Button torchBtn;
    private Button photoBtn;

    private boolean torch = false;
    private boolean microscope = true;

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button makeButton(String text) {
        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setBackgroundColor(Color.rgb(35, 40, 44));

        return button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            buildUi();

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION
                );

            } else {
                startCamera();
            }

        } catch (Exception e) {
            e.printStackTrace();

            if (status != null) {
                status.setText(
                        "Error aplikasi: " + e.getClass().getSimpleName()
                );
            }
        }
    }

    private void buildUi() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        // =========================
        // TOP BAR
        // =========================

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(3), dp(8), dp(3));
        top.setBackgroundColor(Color.rgb(18, 20, 22));

        modeText = new TextView(this);
        modeText.setText("🔬 MICROSCOPE");
        modeText.setTextColor(Color.WHITE);
        modeText.setTextSize(18);
        modeText.setGravity(Gravity.CENTER_VERTICAL);

        top.addView(
                modeText,
                new LinearLayout.LayoutParams(
                        0,
                        dp(50),
                        1
                )
        );

        Button modeButton = makeButton("MODE");

        top.addView(
                modeButton,
                new LinearLayout.LayoutParams(
                        dp(90),
                        dp(50)
                )
        );

        root.addView(top);

        // =========================
        // CAMERA
        // =========================

        FrameLayout cameraBox = new FrameLayout(this);

        preview = new PreviewView(this);

        preview.setScaleType(
                PreviewView.ScaleType.FILL_CENTER
        );

        cameraBox.addView(
                preview,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        status = new TextView(this);
        status.setText("Menyiapkan kamera...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(12);
        status.setBackgroundColor(Color.argb(150, 0, 0, 0));
        status.setPadding(
                dp(8),
                dp(5),
                dp(8),
                dp(5)
        );

        FrameLayout.LayoutParams statusParams =
                new FrameLayout.LayoutParams(
                        -2,
                        -2,
                        Gravity.TOP | Gravity.START
                );

        statusParams.setMargins(
                dp(8),
                dp(8),
                0,
                0
        );

        cameraBox.addView(status, statusParams);

        root.addView(
                cameraBox,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        // =========================
        // ZOOM TEXT
        // =========================

        zoomText = new TextView(this);

        zoomText.setText("Zoom 1.0×");
        zoomText.setTextColor(Color.WHITE);
        zoomText.setTextSize(15);
        zoomText.setGravity(Gravity.CENTER);

        root.addView(
                zoomText,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(30)
                )
        );

        // =========================
        // ZOOM BAR
        // =========================

        zoomBar = new SeekBar(this);
        zoomBar.setMax(100);
        zoomBar.setProgress(0);

        root.addView(
                zoomBar,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(42)
                )
        );

        // =========================
        // BUTTON CONTROL
        // =========================

        LinearLayout controls = new LinearLayout(this);

        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(
                dp(4),
                dp(4),
                dp(4),
                dp(6)
        );

        controls.setBackgroundColor(
                Color.rgb(10, 12, 14)
        );

        Button minus = makeButton("−");

        torchBtn = makeButton("🔦\nLampu");

        photoBtn = makeButton("📸\nFOTO");

        Button focus = makeButton("🎯\nFokus");

        Button plus = makeButton("+");

        controls.addView(
                minus,
                new LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        0.75f
                )
        );

        controls.addView(
                torchBtn,
                new LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        1.15f
                )
        );

        controls.addView(
                photoBtn,
                new LinearLayout.LayoutParams(
                        0,
                        dp(70),
                        1.55f
                )
        );

        controls.addView(
                focus,
                new LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        1.15f
                )
        );

        controls.addView(
                plus,
                new LinearLayout.LayoutParams(
                        0,
                        dp(62),
                        0.75f
                )
        );

        root.addView(controls);

        setContentView(root);

        // =========================
        // MODE
        // =========================

        modeButton.setOnClickListener(v -> {

            microscope = !microscope;

            if (microscope) {
                modeText.setText("🔬 MICROSCOPE");
            } else {
                modeText.setText("🔍 KACA PEMBESAR");
            }
        });

        // =========================
        // ZOOM SLIDER
        // =========================

        zoomBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (fromUser) {
                            setZoomFromProgress(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar
                    ) {
                    }
                }
        );

        // =========================
        // BUTTONS
        // =========================

        minus.setOnClickListener(v -> changeZoom(-0.5f));

        plus.setOnClickListener(v -> changeZoom(0.5f));

        torchBtn.setOnClickListener(v -> toggleTorch());

        focus.setOnClickListener(v -> {

            if (preview != null) {

                focusAt(
                        preview.getWidth() / 2f,
                        preview.getHeight() / 2f
                );
            }
        });

        photoBtn.setOnClickListener(v -> takePhoto());

        // =========================
        // TAP CAMERA = FOCUS
        // =========================

        preview.setOnTouchListener(
                (v, event) -> {

                    if (event.getAction() ==
                            MotionEvent.ACTION_UP) {

                        focusAt(
                                event.getX(),
                                event.getY()
                        );
                    }

                    return true;
                }
        );
    }

    // =========================================================
    // CAMERA
    // =========================================================

    private void startCamera() {

        status.setText("Menyiapkan kamera...");

        final ProcessCameraProvider cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(
                () -> {

                    try {

                        ProcessCameraProvider provider =
                                cameraProviderFuture.get();

                        Preview previewUseCase =
                                new Preview.Builder()
                                        .build();

                        capture =
                                new ImageCapture.Builder()
                                        .setCaptureMode(
                                                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                                        )
                                        .setJpegQuality(95)
                                        .build();

                        CameraSelector selector =
                                CameraSelector.DEFAULT_BACK_CAMERA;

                        provider.unbindAll();

                        camera = provider.bindToLifecycle(
                                this,
                                selector,
                                previewUseCase,
                                capture
                        );

                        previewUseCase.setSurfaceProvider(
                                preview.getSurfaceProvider()
                        );

                        updateZoomText();

                        status.setText(
                                "Kamera siap • tap untuk fokus"
                        );

                    } catch (Exception e) {

                        e.printStackTrace();

                        status.setText(
                                "Kamera gagal: " +
                                        e.getClass().getSimpleName()
                        );
                    }

                },
                ContextCompat.getMainExecutor(this)
        );
    }

    // =========================================================
    // ZOOM
    // =========================================================

    private void updateZoomText() {

        if (camera == null) return;

        if (camera.getCameraInfo()
                .getZoomState()
                .getValue() == null) {
            return;
        }

        float max =
                camera.getCameraInfo()
                        .getZoomState()
                        .getValue()
                        .getMaxZoomRatio();

        float current =
                camera.getCameraInfo()
                        .getZoomState()
                        .getValue()
                        .getZoomRatio();

        zoomText.setText(
                String.format(
                        "Zoom %.1f×  (maks %.1f×)",
                        current,
                        max
                )
        );
    }

    private void setZoomFromProgress(int progress) {

        if (camera == null) return;

        if (camera.getCameraInfo()
                .getZoomState()
                .getValue() == null) {
            return;
        }

        float max =
                camera.getCameraInfo()
                        .getZoomState()
                        .getValue()
                        .getMaxZoomRatio();

        if (max <= 1f) {
            return;
        }

        float ratio =
                1f +
                        (max - 1f) *
                                progress /
                                100f;

        camera.getCameraControl()
                .setZoomRatio(ratio);

        updateZoomText();
    }

    private void changeZoom(float delta) {

        if (camera == null) return;

        if (camera.getCameraInfo()
                .getZoomState()
                .getValue() == null) {
            return;
        }

        float current =
                camera.getCameraInfo()
                        .getZoomState()
                        .getValue()
                        .getZoomRatio();

        float max =
                camera.getCameraInfo()
                        .getZoomState()
                        .getValue()
                        .getMaxZoomRatio();

        float newZoom =
                Math.max(
                        1f,
                        Math.min(
                                max,
                                current + delta
                        )
                );

        camera.getCameraControl()
                .setZoomRatio(newZoom);

        if (max > 1f) {

            int progress =
                    (int)
                            ((newZoom - 1f) /
                                    (max - 1f) *
                                    100f);

            zoomBar.setProgress(progress);
        }

        updateZoomText();
    }

    // =========================================================
    // TORCH
    // =========================================================

    private void toggleTorch() {

        if (camera == null) {
            status.setText("Kamera belum siap");
            return;
        }

        if (!camera.getCameraInfo()
                .hasFlashUnit()) {

            status.setText(
                    "Flash tidak tersedia"
            );

            return;
        }

        torch = !torch;

        camera.getCameraControl()
                .enableTorch(torch);

        if (torch) {

            torchBtn.setText(
                    "🔦\nLampu ON"
            );

        } else {

            torchBtn.setText(
                    "🔦\nLampu"
            );
        }
    }

    // =========================================================
    // FOCUS
    // =========================================================

    private void focusAt(float x, float y) {

        if (camera == null) {
            status.setText(
                    "Kamera belum siap"
            );
            return;
        }

        if (preview == null) return;

        MeteringPoint point =
                preview.getMeteringPointFactory()
                        .createPoint(x, y);

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF
                ).build();

        camera.getCameraControl()
                .startFocusAndMetering(action);

        status.setText("Fokus...");
    }

    // =========================================================
    // PHOTO
    // =========================================================

    private void takePhoto() {

        if (capture == null) {

            status.setText(
                    "Kamera belum siap"
            );

            return;
        }

        photoBtn.setEnabled(false);

        photoBtn.setText(
                "📸\nMEMOTRET..."
        );

        ContentValues values =
                new ContentValues();

        values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "Magnifier_" +
                        System.currentTimeMillis() +
                        ".jpg"
        );

        values.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
        );

        values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/MagnifierMicroscope"
        );

        ImageCapture.OutputFileOptions output =
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                ).build();

        capture.takePicture(
                output,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults result
                    ) {

                        photoBtn.setEnabled(true);

                        photoBtn.setText(
                                "📸\nFOTO"
                        );

                        status.setText(
                                "Foto tersimpan"
                        );

                        showPhotoOptions(
                                result.getSavedUri()
                        );
                    }

                    @Override
                    public void onError(
                            @NonNull ImageCaptureException error
                    ) {

                        photoBtn.setEnabled(true);

                        photoBtn.setText(
                                "📸\nFOTO"
                        );

                        status.setText(
                                "Foto gagal: " +
                                        error.getMessage()
                        );
                    }
                }
        );
    }

    // =========================================================
    // PHOTO OPTIONS
    // =========================================================

    private void showPhotoOptions(Uri uri) {

        if (uri == null) return;

        new AlertDialog.Builder(this)
                .setTitle(
                        "Foto berhasil disimpan"
                )
                .setMessage(
                        "Foto tersimpan di " +
                                "Pictures/MagnifierMicroscope"
                )
                .setPositiveButton(
                        "🔎 Google Lens",
                        (dialog, which) ->
                                sendToGoogleLens(uri)
                )
                .setNegativeButton(
                        "Tutup",
                        null
                )
                .show();
    }

    private void sendToGoogleLens(Uri uri) {

        Intent intent =
                new Intent(Intent.ACTION_SEND);

        intent.setType("image/jpeg");

        intent.putExtra(
                Intent.EXTRA_STREAM,
                uri
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        intent.setPackage(
                "com.google.android.googlequicksearchbox"
        );

        try {

            startActivity(intent);

        } catch (Exception e) {

            intent.setPackage(null);

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Buka foto dengan"
                    )
            );
        }
    }

    // =========================================================
    // CAMERA PERMISSION
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == CAMERA_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startCamera();

            } else {

                if (status != null) {

                    status.setText(
                            "Izin kamera diperlukan"
                    );
                }
            }
        }
    }

    @Override
    protected void onDestroy() {

        try {

            if (camera != null) {

                camera.getCameraControl()
                        .enableTorch(false);
            }

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }
}
```
