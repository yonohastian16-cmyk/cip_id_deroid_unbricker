package com.deroid.unbrickernota;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ScaleGestureDetector;
import android.graphics.Matrix;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.Toast;
import java.util.Locale;
import android.content.Context;
import java.util.ArrayList;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import androidx.annotation.NonNull;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.camera2.interop.Camera2Interop;
import android.hardware.camera2.CaptureRequest;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION = 7;
    private static final int GALLERY_PICK = 101;
    private AlertDialog emmcDialog;

    private PreviewView preview;
    private ImageCapture capture;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private SeekBar zoomBar;
    private TextView zoomText;
    private TextView status;
    private TextView exposureText;
    private SeekBar exposureBar;
    private Button torchBtn;
    private Button photoBtn;
    private Button freezeBtn;
    private ImageView freezeView;
    private boolean frozen = false;
    private Bitmap frozenBitmap;
    private float frozenZoom = 1f;
    private float frozenPanX = 0f;
    private float frozenPanY = 0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean movingFrozen = false;
    private ScaleGestureDetector frozenScaleDetector;
    private ScaleGestureDetector liveScaleDetector;
    private boolean livePinching = false;
    // Quick double-tap zoom for microscope inspection.
    private long lastLiveTapTime = 0L;
    private long lastFrozenTapTime = 0L;
    private float liveLastScale = 1f;
    private float liveDownX = 0f;
    private float liveDownY = 0f;
    private final Matrix frozenMatrix = new Matrix();
    private Button detailBtn;
    private Button galleryBtn;
    private Button autoExposureBtn;
    private SeekBar detailBar;
    private TextView detailText;
    private boolean detailOn = true;
    private LinearLayout topBar;
    private LinearLayout controlsBar;
    private LinearLayout rootLayout;
    private LinearLayout infoRow;
    private LinearLayout exposureRow;
    private LinearLayout detailRow;
    private Button cameraMinusBtn;
    private Button cameraFocusBtn;
    private Button cameraPlusBtn;
    private Button overlayBtn;
    private boolean torch = false;
    private int navigationBarBottomInset = 0;
    private int exposureLower = -4;
    private int exposureUpper = 4;
    // V3.7: preserve Live camera tuning across preview restarts
    private float lastLiveZoom = 1f;
    private int lastLiveExposure = 0;
    // LIVE camera control panel auto-hide
    private final Handler livePanelHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideLivePanelRunnable = () -> setLivePanelVisible(false);
    private boolean livePanelVisible = true;

    // LIVE bottom sheet: draggable camera controls with snap positions.
    private FrameLayout bottomSheetHost;
    private TextView bottomSheetHandle;
    private float bottomSheetDownY;
    private int bottomSheetStartHeight;
    private boolean bottomSheetMoved = false;
    private static final int BOTTOM_SHEET_MIN_DP = 72;
    private static final int BOTTOM_SHEET_START_DP = 80;
    private static final int BOTTOM_SHEET_MID_DP = 108;
    private static final int BOTTOM_SHEET_MAX_DP = 142;

    // V3.0 Crosshair + Grid overlay
    private OverlayView overlayView;
    private boolean crosshairOn = false;
    private boolean gridOn = false;
    private int gridDivisions = 10;
    private int overlayColor = Color.GREEN;
    private int crosshairColor = Color.RED;
    private int overlayAlpha = 150;

    // V2.5 PCB inspection / annotation
    private FrameLayout cameraBox;
    private AnnotationView annotationView;
    private LinearLayout annotationBar;
    private enum AnnotationMode { NONE, SELECT, PEN, MARKER, ARROW, LINE, CIRCLE, RECT, HIGHLIGHT, TEXT, OCR, CROP, JUMPER }
    private AnnotationMode annotationMode = AnnotationMode.NONE;

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackgroundColor(Color.rgb(35, 40, 44));
        return b;
    }

    private TextView makeInfoText(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // LIVE microscope: keep the display awake so the screen does not dim/sleep
        // while the technician is inspecting a PCB.
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Edge-to-edge: keep the camera immersive, but reserve system-bar space
        // for the header and bottom controls so buttons (including Lampu/Flash)
        // are never covered by the Android navigation bar.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        buildUi();
        applySystemBarInsets();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION
            );
        } else {
            startCamera();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        rootLayout = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout top = new LinearLayout(this);
        topBar = top;
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(3), dp(8), dp(3));
        top.setBackgroundColor(Color.rgb(18, 20, 22));

        ImageView logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.deroid_unbricker_nota_logo);
        logoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logoView.setContentDescription("Logo Deroid Unbricker Nota");

        top.addView(
                logoView,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(44)
                )
        );

        // Header dibuat ringkas: logo tetap ada, tulisan JEJAK TEKNISI di atas kamera dihilangkan
        // agar area preview lebih luas.
        top.addView(new View(this), new LinearLayout.LayoutParams(0, dp(44), 1));

        // Nota servis (daftar nota, buat nota, PDF, bagikan, cetak)
        Button notaBtn = makeButton("🧾\nNota");
        notaBtn.setTextSize(11);
        notaBtn.setOnClickListener(v -> startActivity(new Intent(this, NotaActivity.class)));
        top.addView(notaBtn, new LinearLayout.LayoutParams(dp(68), dp(48)));

        galleryBtn = makeButton("🖼️\nGaleri");
        galleryBtn.setTextSize(11);
        top.addView(galleryBtn, new LinearLayout.LayoutParams(dp(68), dp(48)));

        detailBtn = makeButton("DETAIL\nON");
        detailBtn.setTextSize(11);
        top.addView(detailBtn, new LinearLayout.LayoutParams(dp(68), dp(48)));

        root.addView(top);

        cameraBox = new FrameLayout(this);

        preview = new PreviewView(this);
        preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        cameraBox.addView(
                preview,
                new FrameLayout.LayoutParams(-1, -1)
        );

        status = new TextView(this);
        status.setText("Menyiapkan kamera...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(12);
        status.setBackgroundColor(Color.argb(150, 0, 0, 0));
        status.setPadding(dp(8), dp(5), dp(8), dp(5));

        FrameLayout.LayoutParams statusParams =
                new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        statusParams.setMargins(dp(8), dp(8), 0, 0);
        cameraBox.addView(status, statusParams);

        overlayView = new OverlayView(this);
        overlayView.setVisibility(View.VISIBLE);
        cameraBox.addView(overlayView, new FrameLayout.LayoutParams(-1, -1));

        root.addView(
                cameraBox,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        infoRow.setPadding(dp(8), 0, dp(8), 0);
        infoRow.setBackgroundColor(Color.BLACK);

        zoomText = makeInfoText("Zoom 1.0×");
        exposureText = makeInfoText("Exposure 0 • NORMAL");
        infoRow.addView(zoomText, new LinearLayout.LayoutParams(0, dp(30), 1));
        infoRow.addView(exposureText, new LinearLayout.LayoutParams(0, dp(30), 1));
        root.addView(infoRow, new LinearLayout.LayoutParams(-1, dp(30)));

        zoomBar = new SeekBar(this);
        zoomBar.setMax(100);
        zoomBar.setProgress(0);
        root.addView(zoomBar, new LinearLayout.LayoutParams(-1, dp(38)));

        exposureRow = new LinearLayout(this);
        exposureRow.setOrientation(LinearLayout.HORIZONTAL);
        exposureRow.setGravity(Gravity.CENTER_VERTICAL);
        exposureRow.setPadding(dp(8), 0, dp(8), 0);
        exposureBar = new SeekBar(this);
        exposureBar.setMax(8);
        exposureBar.setProgress(4);
        exposureRow.addView(exposureBar, new LinearLayout.LayoutParams(0, dp(40), 1));
        autoExposureBtn = makeButton("AUTO");
        autoExposureBtn.setTextSize(12);
        exposureRow.addView(autoExposureBtn, new LinearLayout.LayoutParams(dp(68), dp(40)));
        root.addView(exposureRow, new LinearLayout.LayoutParams(-1, dp(44)));

        detailRow = new LinearLayout(this);
        detailRow.setOrientation(LinearLayout.HORIZONTAL);
        detailRow.setGravity(Gravity.CENTER_VERTICAL);
        detailRow.setPadding(dp(8), 0, dp(8), 0);
        detailText = makeInfoText(detailOn ? "Ultra Detail" : "Detail normal");
        detailRow.addView(detailText, new LinearLayout.LayoutParams(dp(88), dp(38)));
        detailBar = new SeekBar(this);
        detailBar.setMax(100);
        detailBar.setProgress(detailOn ? 65 : 35);
        detailRow.addView(detailBar, new LinearLayout.LayoutParams(0, dp(38), 1));
        root.addView(detailRow, new LinearLayout.LayoutParams(-1, dp(40)));

        LinearLayout controls = new LinearLayout(this);
        controlsBar = controls;
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(4), dp(4), dp(4), dp(6));
        controls.setBackgroundColor(Color.rgb(10, 12, 14));

        Button minus = makeButton("−");
        cameraMinusBtn = minus;
        torchBtn = makeButton("🔦\nLampu");
        freezeBtn = makeButton("❄️\nBeku");
        photoBtn = makeButton("📸\nFOTO");
        Button focus = makeButton("🎯\nFokus");
        cameraFocusBtn = focus;
        overlayBtn = makeButton("🎯\nGrid");
        overlayBtn.setTextSize(11);
        Button plus = makeButton("+");
        cameraPlusBtn = plus;

        controls.addView(minus, new LinearLayout.LayoutParams(0, dp(56), .55f));
        controls.addView(torchBtn, new LinearLayout.LayoutParams(0, dp(56), 1.0f));
        controls.addView(freezeBtn, new LinearLayout.LayoutParams(0, dp(56), 1.0f));
        controls.addView(photoBtn, new LinearLayout.LayoutParams(0, dp(62), 1.25f));
        controls.addView(focus, new LinearLayout.LayoutParams(0, dp(56), 1.0f));
        controls.addView(overlayBtn, new LinearLayout.LayoutParams(0, dp(56), 1.0f));
        controls.addView(plus, new LinearLayout.LayoutParams(0, dp(56), .55f));

        // Bottom sheet host: tinggi berubah mengikuti drag sehingga preview kamera
        // ikut membesar/mengecil. Ada 3 posisi snap: rendah, tengah, tinggi.
        bottomSheetHost = new FrameLayout(this);
        bottomSheetHost.setBackgroundColor(Color.rgb(10, 12, 14));
        bottomSheetHost.setClipChildren(true);

        bottomSheetHandle = new TextView(this);
        bottomSheetHandle.setText("━");
        bottomSheetHandle.setTextColor(Color.rgb(145, 155, 165));
        bottomSheetHandle.setTextSize(18);
        bottomSheetHandle.setGravity(Gravity.CENTER);
        bottomSheetHost.addView(
                bottomSheetHandle,
                new FrameLayout.LayoutParams(-1, dp(24), Gravity.TOP)
        );

        FrameLayout.LayoutParams controlsParams =
                new FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM);
        bottomSheetHost.addView(controls, controlsParams);

        root.addView(
                bottomSheetHost,
                new LinearLayout.LayoutParams(-1, dp(BOTTOM_SHEET_START_DP))
        );

        bottomSheetHandle.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    bottomSheetDownY = event.getRawY();
                    bottomSheetStartHeight = bottomSheetHost.getLayoutParams().height;
                    bottomSheetMoved = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dy = event.getRawY() - bottomSheetDownY;
                    if (Math.abs(dy) > dp(3)) bottomSheetMoved = true;
                    int minH = dp(BOTTOM_SHEET_MIN_DP);
                    int maxH = dp(BOTTOM_SHEET_MAX_DP);
                    int newH = bottomSheetStartHeight - (int) dy;
                    newH = Math.max(minH, Math.min(maxH, newH));
                    ViewGroup.LayoutParams lp = bottomSheetHost.getLayoutParams();
                    lp.height = newH;
                    bottomSheetHost.setLayoutParams(lp);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    snapBottomSheet();
                    return true;
            }
            return true;
        });

        setContentView(root);
        // Keep the LIVE preview clean; controls reappear with a tap.
        scheduleLivePanelHide();

        frozenScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                if (!frozen) return false;
                frozenZoom = Math.max(1f, Math.min(8f, frozenZoom * detector.getScaleFactor()));
                if (zoomBar != null) {
                    zoomBar.setProgress((int)(((frozenZoom - 1f) / 7f) * 100f));
                }
                updateFrozenImage();
                return true;
            }
        });

        liveScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                livePinching = true;
                return true;
            }
            @Override public boolean onScale(ScaleGestureDetector detector) {
                if (frozen || camera == null) return false;
                if (camera.getCameraInfo().getZoomState().getValue() == null) return false;
                float factor = detector.getScaleFactor();
                if (!Float.isFinite(factor) || factor <= 0f) return false;
                liveLastScale = factor;
                float current = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                float max = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
                float newZoom = Math.max(1f, Math.min(max, current * factor));
                camera.getCameraControl().setZoomRatio(newZoom);
                lastLiveZoom = newZoom;
                if (zoomBar != null && max > 1f) {
                    zoomBar.setProgress((int)(((newZoom - 1f) / (max - 1f)) * 100f));
                }
                showLivePanelTemporarily();
                updateZoomText();
                return true;
            }
            @Override public void onScaleEnd(ScaleGestureDetector detector) {
                livePinching = false;
                liveLastScale = 1f;
            }
        });

        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (frozen) setFrozenZoomFromProgress(progress);
                    else setZoomFromProgress(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { showLivePanelTemporarily(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { scheduleLivePanelHide(); }
        });

        exposureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) setExposure(exposureLower + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { showLivePanelTemporarily(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { scheduleLivePanelHide(); }
        });

        detailBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                detailText.setText(progress >= 50 ? "Ultra Detail" : "Detail normal");
                if (fromUser) {
                    boolean requested = progress >= 50;
                    if (requested != detailOn) {
                        detailOn = requested;
                        detailBtn.setText(detailOn ? "ULTRA\nDETAIL" : "DETAIL\nNORMAL");
                        restartLivePreview();
                        status.setText(detailOn ? "LIVE • Ultra Detail ON" : "LIVE • Detail normal");
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { showLivePanelTemporarily(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { scheduleLivePanelHide(); }
        });

        autoExposureBtn.setOnClickListener(v -> setExposure(0));
        galleryBtn.setOnClickListener(v -> openGallery());

        detailBtn.setOnClickListener(v -> {
            detailOn = !detailOn;
            detailBtn.setText(detailOn ? "ULTRA\nDETAIL" : "DETAIL\nNORMAL");
            if (camera != null) {
                restartLivePreview();
            }
            status.setText(detailOn ? "LIVE • Ultra Detail ON" : "LIVE • Detail normal");
        });

        minus.setOnClickListener(v -> {
            if (frozen) changeFrozenZoom(-0.5f); else changeZoom(-0.5f);
        });
        plus.setOnClickListener(v -> {
            if (frozen) changeFrozenZoom(0.5f); else changeZoom(0.5f);
        });
        torchBtn.setOnClickListener(v -> toggleTorch());
        freezeBtn.setOnClickListener(v -> toggleFreeze());
        focus.setOnClickListener(v -> {
            if (preview != null) {
                focusAt(preview.getWidth() / 2f, preview.getHeight() / 2f);
            }
        });
        photoBtn.setOnClickListener(v -> takePhoto());
        overlayBtn.setOnClickListener(v -> showOverlaySettings());

        preview.setOnTouchListener((v, event) -> {
            if (frozen) {
                frozenScaleDetector.onTouchEvent(event);
                if (event.getPointerCount() == 1) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            lastTouchX = event.getX();
                            lastTouchY = event.getY();
                            movingFrozen = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getX() - lastTouchX;
                            float dy = event.getY() - lastTouchY;
                            if (Math.abs(dx) + Math.abs(dy) > dp(2)) movingFrozen = true;
                            frozenPanX += dx;
                            frozenPanY += dy;
                            lastTouchX = event.getX();
                            lastTouchY = event.getY();
                            updateFrozenImage();
                            return true;
                        case MotionEvent.ACTION_UP:
                            long nowFrozen = System.currentTimeMillis();
                            if (!movingFrozen && nowFrozen - lastFrozenTapTime <= 280L) {
                                float target = frozenZoom > 1.01f ? 1f : 2f;
                                frozenZoom = Math.max(1f, Math.min(8f, target));
                                frozenPanX = 0f;
                                frozenPanY = 0f;
                                if (zoomBar != null) {
                                    zoomBar.setProgress((int)(((frozenZoom - 1f) / 7f) * 100f));
                                }
                                updateFrozenImage();
                                status.setText(frozenZoom > 1.01f ? "Freeze Zoom 2×" : "Freeze Zoom 1×");
                                lastFrozenTapTime = 0L;
                            } else if (!movingFrozen) {
                                lastFrozenTapTime = nowFrozen;
                            }
                            return true;
                    }
                }
                return true;
            }

            // LIVE: pinch with two fingers for smooth optical/digital zoom.
            liveScaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    showLivePanelTemporarily();
                    liveDownX = event.getX();
                    liveDownY = event.getY();
                    livePinching = false;
                    return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                    livePinching = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() > 1) livePinching = true;
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!livePinching && Math.hypot(event.getX() - liveDownX, event.getY() - liveDownY) <= dp(18)) {
                        long nowLive = System.currentTimeMillis();
                        if (nowLive - lastLiveTapTime <= 280L) {
                            if (camera != null && camera.getCameraInfo().getZoomState().getValue() != null) {
                                float max = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
                                float target = lastLiveZoom > 1.01f ? 1f : Math.min(2f, max);
                                camera.getCameraControl().setZoomRatio(target);
                                lastLiveZoom = target;
                                if (zoomBar != null && max > 1f) {
                                    zoomBar.setProgress((int)(((target - 1f) / (max - 1f)) * 100f));
                                }
                                updateZoomText();
                                status.setText(target > 1.01f ? "LIVE Zoom 2×" : "LIVE Zoom 1×");
                            }
                            lastLiveTapTime = 0L;
                        } else {
                            focusAt(event.getX(), event.getY());
                            lastLiveTapTime = nowLive;
                        }
                    }
                    livePinching = false;
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    livePinching = false;
                    return true;
            }
            return true;
        });
    }


    private void openGallery() {
        if (frozen) {
            status.setText("Kembali ke LIVE sebelum membuka Galeri");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != GALLERY_PICK || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        try {
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap;
            try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bitmap == null) { status.setText("Foto Galeri tidak dapat dibuka"); return; }
            frozenBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            bitmap.recycle();
            frozen = true;
            frozenZoom = 1f;
            frozenPanX = 0f;
            frozenPanY = 0f;
            freezeView = new ImageView(this);
            freezeView.setImageBitmap(frozenBitmap);
            freezeView.setScaleType(ImageView.ScaleType.MATRIX);
            freezeView.setBackgroundColor(Color.BLACK);
            freezeView.setOnTouchListener((v, event) -> {
                if (!frozen || annotationMode != AnnotationMode.NONE) return false;
                frozenScaleDetector.onTouchEvent(event);
                if (event.getPointerCount() == 1) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: lastTouchX=event.getX(); lastTouchY=event.getY(); return true;
                        case MotionEvent.ACTION_MOVE: frozenPanX += event.getX()-lastTouchX; frozenPanY += event.getY()-lastTouchY; lastTouchX=event.getX(); lastTouchY=event.getY(); updateFrozenImage(); return true;
                    }
                }
                return true;
            });
            FrameLayout parent = cameraBox;
            parent.addView(freezeView, new FrameLayout.LayoutParams(-1, -1));
            zoomBar.setProgress(0);
            showAnnotationTools();
            setFreezeFullscreen(true);
            status.setText("✏️ EDITOR PRO V3.6 • foto Galeri siap diedit");
        } catch (Exception e) {
            status.setText("Gagal membuka foto Galeri");
        }
    }

    private void showAnnotationTools() {
        if (annotationBar != null) return;

        annotationBar = new LinearLayout(this);
        annotationBar.setOrientation(LinearLayout.VERTICAL);
        annotationBar.setGravity(Gravity.CENTER);
        annotationBar.setPadding(dp(4), dp(3), dp(4), dp(3));
        annotationBar.setBackgroundColor(Color.rgb(22, 24, 27));

        // V3.8: each editor row scrolls horizontally. This prevents the toolbar
        // from becoming too dense on smaller phones while keeping every tool reachable.
        HorizontalScrollView rowScroll1 = new HorizontalScrollView(this);
        rowScroll1.setHorizontalScrollBarEnabled(false);
        HorizontalScrollView rowScroll2 = new HorizontalScrollView(this);
        rowScroll2.setHorizontalScrollBarEnabled(false);
        HorizontalScrollView rowScroll3 = new HorizontalScrollView(this);
        rowScroll3.setHorizontalScrollBarEnabled(false);

        LinearLayout toolRow1 = new LinearLayout(this);
        toolRow1.setOrientation(LinearLayout.HORIZONTAL);
        toolRow1.setGravity(Gravity.CENTER);
        LinearLayout toolRow2 = new LinearLayout(this);
        toolRow2.setOrientation(LinearLayout.HORIZONTAL);
        toolRow2.setGravity(Gravity.CENTER);
        LinearLayout toolRow3 = new LinearLayout(this);
        toolRow3.setOrientation(LinearLayout.HORIZONTAL);
        toolRow3.setGravity(Gravity.CENTER);
        LinearLayout optionRow = new LinearLayout(this);
        optionRow.setOrientation(LinearLayout.HORIZONTAL);
        optionRow.setGravity(Gravity.CENTER);

        Button pan = makeButton("✋\nGeser");
        Button select = makeButton("☝\nPilih");
        Button pen = makeButton("✎\nPen");
        Button marker = makeButton("●\nTitik");
        Button arrow = makeButton("➜\nPanah");
        Button circle = makeButton("○\nLingkar");
        Button text = makeButton("T\nTeks");
        Button ocr = makeButton("🔎\nOCR");
        Button undo = makeButton("↩\nUndo");
        Button redo = makeButton("↪\nRedo");
        Button duplicate = makeButton("⧉\nDuplikat");
        Button edit = makeButton("✎\nEdit");
        Button clear = makeButton("✕\nHapus");
        Button layer = makeButton("☷\nLayer");
        Button save = makeButton("💾\nSimpan");
        Button share = makeButton("↗\nShare");

        Button[] row1 = {pan, select, pen, marker, arrow, circle, text};
        for (Button b : row1) {
            b.setTextSize(10);
            toolRow1.addView(b, new LinearLayout.LayoutParams(dp(72), dp(38)));
        }
        Button[] row2 = {ocr, undo, redo, duplicate, edit, clear, layer};
        for (Button b : row2) {
            b.setTextSize(10);
            toolRow2.addView(b, new LinearLayout.LayoutParams(dp(72), dp(30)));
        }

        // Simpan dan Share dibuat fixed (tidak ikut horizontal scroll)
        // agar selalu terlihat di layar saat editor digunakan.
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER);
        actionRow.setPadding(dp(2), 0, dp(2), 0);
        save.setTextSize(10);
        share.setTextSize(10);
        actionRow.addView(save, new LinearLayout.LayoutParams(0, dp(32), 1));
        actionRow.addView(share, new LinearLayout.LayoutParams(0, dp(32), 1));

        Button line = makeButton("╱\nGaris");
        Button rect = makeButton("□\nKotak");
        Button highlight = makeButton("▰\nHighlight");
        Button crop = makeButton("✂\nCrop");
        Button rotate = makeButton("⟳\nPutar");
        Button adjust = makeButton("☀\nAtur");
        Button resetView = makeButton("⌖\nReset");
        Button jumper = makeButton("⚡\nJumper");
        Button[] row3 = {line, rect, highlight, crop, rotate, adjust, resetView, jumper};
        for (Button b : row3) {
            b.setTextSize(9);
            toolRow3.addView(b, new LinearLayout.LayoutParams(dp(72), dp(30)));
        }

        TextView legend = makeInfoText("OBJEK • Warna / Ukuran / Rotasi");
        legend.setTextSize(9);
        Button red = makeButton("●R"); Button yellow = makeButton("●K");
        Button green = makeButton("●H"); Button blue = makeButton("●B");
        Button small = makeButton("S"); Button medium = makeButton("M"); Button large = makeButton("L");
        Button rotateLeft = makeButton("↶15°"); Button rotateRight = makeButton("↷15°");
        Button duplicateObj = makeButton("⧉"); Button deleteObj = makeButton("✕");
        Button[] opts = {red, yellow, green, blue, small, medium, large, rotateLeft, rotateRight, duplicateObj, deleteObj};
        optionRow.addView(legend, new LinearLayout.LayoutParams(dp(155), dp(28)));
        for (Button b : opts) { b.setTextSize(9); optionRow.addView(b, new LinearLayout.LayoutParams(dp(42), dp(28))); }
        // Keep the editor compact; color/size options appear only when needed.
        optionRow.setVisibility(View.GONE);

        rowScroll1.addView(toolRow1, new HorizontalScrollView.LayoutParams(dp(504), dp(38)));
        rowScroll2.addView(toolRow2, new HorizontalScrollView.LayoutParams(dp(648), dp(30)));
        rowScroll3.addView(toolRow3, new HorizontalScrollView.LayoutParams(dp(576), dp(30)));

        annotationBar.addView(rowScroll1, new LinearLayout.LayoutParams(-1, dp(38)));
        annotationBar.addView(rowScroll2, new LinearLayout.LayoutParams(-1, dp(30)));
        annotationBar.addView(rowScroll3, new LinearLayout.LayoutParams(-1, dp(30)));
        annotationBar.addView(actionRow, new LinearLayout.LayoutParams(-1, dp(32)));
        annotationBar.addView(optionRow, new LinearLayout.LayoutParams(-1, dp(28)));

        pan.setOnClickListener(v -> setAnnotationMode(AnnotationMode.NONE, "Geser aktif • gunakan 1 jari untuk pan / 2 jari untuk zoom"));
        pen.setOnClickListener(v -> setAnnotationMode(AnnotationMode.PEN, "Pen aktif • gambar bebas pada PCB"));
        marker.setOnClickListener(v -> setAnnotationMode(AnnotationMode.MARKER, "Marker aktif • tap titik komponen"));
        arrow.setOnClickListener(v -> setAnnotationMode(AnnotationMode.ARROW, "Panah aktif • tarik dari awal ke akhir"));
        circle.setOnClickListener(v -> setAnnotationMode(AnnotationMode.CIRCLE, "Lingkaran aktif • tarik mengelilingi komponen"));
        line.setOnClickListener(v -> setAnnotationMode(AnnotationMode.LINE, "Garis aktif • tarik dari titik awal ke titik akhir"));
        rect.setOnClickListener(v -> setAnnotationMode(AnnotationMode.RECT, "Kotak aktif • tarik mengelilingi area"));
        highlight.setOnClickListener(v -> setAnnotationMode(AnnotationMode.HIGHLIGHT, "Highlight aktif • tarik garis untuk menyorot jalur/komponen"));
        jumper.setOnClickListener(v -> setAnnotationMode(AnnotationMode.JUMPER, "Jumper aktif • tarik A → B • warna & ukuran mengikuti pilihan"));
        crop.setOnClickListener(v -> {
            if (annotationMode == AnnotationMode.CROP && annotationView != null && annotationView.cropSelecting) {
                applyCropSelection(annotationView.cropStartX, annotationView.cropStartY, annotationView.cropEndX, annotationView.cropEndY);
                crop.setText("✂\nCrop");
            } else if (annotationView != null) {
                annotationView.beginCrop();
                setAnnotationMode(AnnotationMode.CROP, "Crop aktif • geser kotak atau tarik sudut, lalu tekan ✓ Crop untuk menerapkan");
                crop.setText("✓\nTerapkan");
            }
        });
        rotate.setOnClickListener(v -> rotateFrozenImage());
        adjust.setOnClickListener(v -> showImageAdjustDialog());
        resetView.setOnClickListener(v -> { frozenZoom=1f; frozenPanX=0f; frozenPanY=0f; if(zoomBar!=null) zoomBar.setProgress(0); updateFrozenImage(); status.setText("Tampilan gambar di-reset"); });
        text.setOnClickListener(v -> setAnnotationMode(AnnotationMode.TEXT, "Teks aktif • tap lokasi untuk menulis catatan"));
        select.setOnClickListener(v -> setAnnotationMode(AnnotationMode.SELECT, "Pilih aktif • geser untuk pindah • 2 jari untuk putar"));
        ocr.setOnClickListener(v -> detectOcrOnFrozenImage());
        undo.setOnClickListener(v -> { annotationView.undo(); status.setText("Undo anotasi"); });
        redo.setOnClickListener(v -> { annotationView.redo(); status.setText("Redo anotasi"); });
        duplicate.setOnClickListener(v -> { if (annotationView.duplicateSelected()) status.setText("Objek diduplikat"); else status.setText("Pilih objek dulu"); });
        edit.setOnClickListener(v -> {
            if (annotationView != null && annotationView.hasSelectedText()) {
                editSelectedText();
            } else {
                setAnnotationMode(AnnotationMode.SELECT, "Edit aktif • tap teks yang ingin diubah");
            }
        });
        clear.setOnClickListener(v -> { if (annotationView.deleteSelected()) status.setText("Objek terpilih dihapus"); else { annotationView.clearAll(); status.setText("Semua anotasi dihapus"); } });
        layer.setOnClickListener(v -> showLayerDialog());
        save.setOnClickListener(v -> saveAnnotatedFreeze());
        share.setOnClickListener(v -> shareAnnotatedFreeze());

        red.setTextColor(Color.RED); yellow.setTextColor(Color.YELLOW); green.setTextColor(Color.GREEN); blue.setTextColor(Color.CYAN);
        red.setOnClickListener(v -> { annotationView.setColor(Color.RED); status.setText("Warna merah"); });
        yellow.setOnClickListener(v -> { annotationView.setColor(Color.YELLOW); status.setText("Warna kuning"); });
        green.setOnClickListener(v -> { annotationView.setColor(Color.GREEN); status.setText("Warna hijau"); });
        blue.setOnClickListener(v -> { annotationView.setColor(Color.CYAN); status.setText("Warna biru"); });
        small.setOnClickListener(v -> { annotationView.setSize(3); status.setText("Ukuran kecil"); });
        medium.setOnClickListener(v -> { annotationView.setSize(5); status.setText("Ukuran sedang"); });
        large.setOnClickListener(v -> { annotationView.setSize(8); status.setText("Ukuran besar"); });
        rotateLeft.setOnClickListener(v -> { if (annotationView.rotateSelectedBy(-15f)) status.setText("Rotasi -15°"); else status.setText("Pilih objek dulu"); });
        rotateRight.setOnClickListener(v -> { if (annotationView.rotateSelectedBy(15f)) status.setText("Rotasi +15°"); else status.setText("Pilih objek dulu"); });
        // Long-press gives precision rotation without adding more buttons to the compact row.
        rotateLeft.setOnLongClickListener(v -> { if (annotationView.rotateSelectedBy(-1f)) status.setText("Rotasi -1°"); else status.setText("Pilih objek dulu"); return true; });
        rotateRight.setOnLongClickListener(v -> { if (annotationView.rotateSelectedBy(1f)) status.setText("Rotasi +1°"); else status.setText("Pilih objek dulu"); return true; });
        duplicateObj.setOnClickListener(v -> { if (annotationView.duplicateSelected()) status.setText("Objek diduplikat"); else status.setText("Pilih objek dulu"); });
        deleteObj.setOnClickListener(v -> { if (annotationView.deleteSelected()) status.setText("Objek terpilih dihapus"); else status.setText("Pilih objek dulu"); });

        // Color/size/rotation controls are contextual so the PCB preview stays large.
        pen.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.PEN, "Pen aktif • gambar bebas pada PCB"); });
        arrow.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.ARROW, "Panah aktif • tarik dari awal ke akhir"); });
        circle.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.CIRCLE, "Lingkaran aktif • tarik mengelilingi komponen"); });
        line.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.LINE, "Garis aktif • tarik dari titik awal ke titik akhir"); });
        rect.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.RECT, "Kotak aktif • tarik mengelilingi area"); });
        highlight.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.HIGHLIGHT, "Highlight aktif • tarik garis untuk menyorot jalur/komponen"); });
        jumper.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.JUMPER, "Jumper aktif • tarik A → B • warna & ukuran mengikuti pilihan"); });
        text.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.TEXT, "Teks aktif • tap lokasi untuk menulis catatan"); });
        select.setOnClickListener(v -> { optionRow.setVisibility(View.VISIBLE); setAnnotationMode(AnnotationMode.SELECT, "Pilih aktif • tap objek • geser, tarik handle, atau putar dengan 2 jari"); });
        pan.setOnClickListener(v -> { optionRow.setVisibility(View.GONE); setAnnotationMode(AnnotationMode.NONE, "Geser aktif • gunakan 1 jari untuk pan / 2 jari untuk zoom"); });
        ocr.setOnClickListener(v -> { optionRow.setVisibility(View.GONE); detectOcrOnFrozenImage(); });

        // In Freeze mode the editing toolbar moves into the former Exposure area.
        // Zoom and Detail controls are hidden so the microscope preview becomes taller.
        annotationView = new AnnotationView(this);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(-1, -1);
        cameraBox.addView(annotationView, overlayParams);
        annotationView.setMode(annotationMode);

        int exposureIndex = rootLayout.indexOfChild(exposureRow);
        // Freeze layout: maximize the PCB image by hiding all live-only status/adjustment rows.
        infoRow.setVisibility(View.GONE);
        exposureRow.setVisibility(View.GONE);
        zoomBar.setVisibility(View.GONE);
        detailRow.setVisibility(View.GONE);

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootLayout.addView(annotationBar, exposureIndex, barParams);
    }

    private void detectOcrOnFrozenImage() {
        if (!frozen || frozenBitmap == null || annotationView == null) {
            status.setText("Bekukan gambar dulu");
            return;
        }
        Bitmap cropBitmap = getFrozenZoomedBitmap();
        CropInfo crop = getCurrentCropInfo();
        if (cropBitmap == null || crop == null) {
            status.setText("Area gambar belum siap");
            return;
        }

        status.setText("🔎 Mendeteksi tulisan...");
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage input = InputImage.fromBitmap(cropBitmap, 0);
        recognizer.process(input)
                .addOnSuccessListener(result -> {
                    java.util.ArrayList<String> found = new java.util.ArrayList<>();
                    int count = 0;
                    boolean historyPushed = false;
                    for (Text.TextBlock block : result.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            String value = line.getText() == null ? "" : line.getText().trim();
                            if (value.isEmpty() || line.getBoundingBox() == null) continue;
                            android.graphics.Rect r = line.getBoundingBox();
                            float sx = (float) crop.width / Math.max(1, cropBitmap.getWidth());
                            float sy = (float) crop.height / Math.max(1, cropBitmap.getHeight());
                            float x1 = crop.left + r.left * sx;
                            float y1 = crop.top + r.top * sy;
                            float x2 = crop.left + r.right * sx;
                            float y2 = crop.top + r.bottom * sy;
                            if (!historyPushed) { annotationView.prepareOcrHistory(); historyPushed=true; }
                            annotationView.addOcr(x1, y1, x2, y2, value);
                            found.add(value);
                            count++;
                        }
                    }
                    annotationView.invalidate();
                    if (count == 0) {
                        status.setText("OCR tidak menemukan tulisan");
                        showOcrResultDialog(new java.util.ArrayList<String>());
                    } else {
                        status.setText("OCR menemukan " + count + " tulisan");
                        showOcrResultDialog(found);
                    }
                    recognizer.close();
                })
                .addOnFailureListener(e -> {
                    status.setText("OCR gagal • coba foto lebih terang/dekat");
                    recognizer.close();
                });
    }

    private void showOcrResultDialog(java.util.ArrayList<String> found) {
        if (found.isEmpty()) return;
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < found.size(); i++) {
            message.append(i + 1).append(". ").append(found.get(i));
            if (i < found.size() - 1) message.append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Hasil Deteksi OCR")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Salin", (d, which) -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("OCR PCB", message.toString()));
                        status.setText("Hasil OCR disalin");
                    }
                })
                .show();
    }

    private void showLayerDialog() {
        if (annotationView == null) return;
        final String[] labels = annotationView.getLayerLabels();
        if (labels.length == 0) { status.setText("Belum ada objek anotasi"); return; }
        new AlertDialog.Builder(this)
                .setTitle("Layer Objek • V3.10.6")
                .setSingleChoiceItems(labels, annotationView.getSelectedIndex(), (dialog, which) -> {
                    annotationView.selectIndex(which);
                    status.setText("Layer dipilih: " + labels[which]);
                })
                .setPositiveButton("↑ Ke Depan", (dialog, which) -> {
                    if (annotationView.bringSelectedToFront()) status.setText("Objek dibawa ke depan");
                })
                .setNegativeButton("↓ Ke Belakang", (dialog, which) -> {
                    if (annotationView.sendSelectedToBack()) status.setText("Objek dikirim ke belakang");
                })
                .setNeutralButton("Tutup", null)
                .show();
    }

    private void setAnnotationMode(AnnotationMode mode, String message) {
        annotationMode = mode;
        if (annotationView != null) annotationView.setMode(mode);
        status.setText(message);
    }

    private void hideAnnotationTools() {
        annotationMode = AnnotationMode.NONE;
        if (annotationBar != null) {
            rootLayout.removeView(annotationBar);
            annotationBar = null;
        }
        if (annotationView != null) {
            cameraBox.removeView(annotationView);
            annotationView = null;
        }
        if (infoRow != null) infoRow.setVisibility(View.VISIBLE);
        if (zoomBar != null) zoomBar.setVisibility(View.VISIBLE);
        if (exposureRow != null) exposureRow.setVisibility(View.VISIBLE);
        if (detailRow != null) detailRow.setVisibility(View.VISIBLE);
    }

    private void cropToCurrentView() {
        if (annotationView != null) {
            annotationView.beginCrop();
            setAnnotationMode(AnnotationMode.CROP, "Crop aktif • geser kotak atau tarik sudut, lalu terapkan");
        }
    }

    private void applyCropSelection(float x1, float y1, float x2, float y2) {
        if (!frozen || frozenBitmap == null) return;
        float[] a = annotationView.viewToSourcePublic(x1, y1);
        float[] b = annotationView.viewToSourcePublic(x2, y2);
        int left = Math.max(0, Math.min(frozenBitmap.getWidth()-1, Math.round(Math.min(a[0], b[0]))));
        int top = Math.max(0, Math.min(frozenBitmap.getHeight()-1, Math.round(Math.min(a[1], b[1]))));
        int right = Math.max(left+1, Math.min(frozenBitmap.getWidth(), Math.round(Math.max(a[0], b[0]))));
        int bottom = Math.max(top+1, Math.min(frozenBitmap.getHeight(), Math.round(Math.max(a[1], b[1]))));
        int w = right-left, h = bottom-top;
        if (w < 20 || h < 20) { status.setText("Area crop terlalu kecil"); return; }
        Bitmap old=frozenBitmap;
        try {
            Bitmap cropped=Bitmap.createBitmap(old,left,top,w,h);
            // Keep annotations that were already placed on the image. Crop changes
            // the source coordinate origin, so translate every object into the new
            // bitmap coordinate system instead of deleting the technician's work.
            if(annotationView!=null && !annotationView.items.isEmpty()) {
                annotationView.pushUndo();
                annotationView.transformAnnotationsForCrop(left, top);
            }
            frozenBitmap=cropped; frozenZoom=1f; frozenPanX=0f; frozenPanY=0f;
            if(zoomBar!=null) zoomBar.setProgress(0);
            if(annotationView!=null) annotationView.setMode(AnnotationMode.SELECT);
            updateFrozenImage();
            if(freezeView!=null){ freezeView.setImageBitmap(frozenBitmap); freezeView.invalidate(); }
            status.setText("Crop diterapkan • area baru siap diedit");
        } catch(Throwable e) { frozenBitmap=old; status.setText("Crop gagal • coba area lebih kecil"); }
    }

    private void rotateFrozenImage() {
        if (!frozen || frozenBitmap == null) return;
        Bitmap old = frozenBitmap;
        try {
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.postRotate(90f);
            // Jangan membuat copy kedua: ini mengurangi penggunaan RAM dan
            // mencegah aplikasi keluar saat memutar foto microscope beresolusi besar.
            Bitmap rotated = Bitmap.createBitmap(old, 0, 0, old.getWidth(), old.getHeight(), m, true);
            if (rotated == null) throw new IllegalStateException("Bitmap rotate null");
            // Keep existing annotations when the base image is rotated.
            // Bitmap.createBitmap(..., 90°) changes the source coordinate system,
            // so every annotation must be transformed to the new width/height.
            if (annotationView != null && !annotationView.items.isEmpty()) {
                annotationView.pushUndo();
                annotationView.transformAnnotationsForRotate(old.getWidth(), old.getHeight());
            }
            frozenBitmap = rotated;
            frozenZoom=1f; frozenPanX=0f; frozenPanY=0f;
            if (zoomBar != null) zoomBar.setProgress(0);
            updateFrozenImage();
            if (freezeView != null) {
                freezeView.setImageBitmap(frozenBitmap);
                freezeView.invalidate();
            }
            status.setText("Gambar diputar 90°");
            // old sengaja tidak langsung di-recycle karena ImageView mungkin
            // masih memegang frame sebelumnya. Android akan membersihkannya.
        } catch (Throwable e) {
            frozenBitmap = old;
            status.setText("Putar gagal • foto terlalu besar, coba foto yang lebih kecil");
        }
    }

    private void showImageAdjustDialog() {
        if (!frozen || frozenBitmap == null) return;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(8));
        TextView info = makeInfoText("Atur gambar untuk membantu melihat jalur PCB");
        info.setTextSize(12);
        SeekBar brightness = new SeekBar(this); brightness.setMax(200); brightness.setProgress(100);
        SeekBar contrast = new SeekBar(this); contrast.setMax(200); contrast.setProgress(100);
        CheckBox sharp = new CheckBox(this); sharp.setText("Pertajam detail (sharpen)"); sharp.setTextColor(Color.WHITE);
        TextView bLabel = makeInfoText("Brightness 0");
        TextView cLabel = makeInfoText("Contrast 0");
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int p,boolean f){bLabel.setText("Brightness "+(p-100));} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} });
        contrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int p,boolean f){cLabel.setText("Contrast "+(p-100));} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} });
        box.addView(info); box.addView(bLabel); box.addView(brightness); box.addView(cLabel); box.addView(contrast); box.addView(sharp);
        new AlertDialog.Builder(this).setTitle("Detail gambar").setView(box)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Terapkan", (d,w) -> applyImageAdjustments(brightness.getProgress()-100, contrast.getProgress()-100, sharp.isChecked()))
                .show();
    }

    private void applyImageAdjustments(int brightness, int contrast, boolean sharpen) {
        if (frozenBitmap == null) return;
        try {
            Bitmap src = frozenBitmap;
            Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
            float c = (contrast + 100f) / 100f;
            c *= c;
            int[] px = new int[src.getWidth() * src.getHeight()];
            src.getPixels(px,0,src.getWidth(),0,0,src.getWidth(),src.getHeight());
            for (int i=0;i<px.length;i++) {
                int col=px[i], r=Color.red(col), g=Color.green(col), b=Color.blue(col);
                r=clamp((int)(((r-128)*c)+128+brightness));
                g=clamp((int)(((g-128)*c)+128+brightness));
                b=clamp((int)(((b-128)*c)+128+brightness));
                px[i]=Color.argb(Color.alpha(col),r,g,b);
            }
            out.setPixels(px,0,out.getWidth(),0,0,out.getWidth(),out.getHeight());
            if (sharpen) out = sharpenBitmap(out);
            frozenBitmap = out;
            frozenZoom=1f; frozenPanX=0f; frozenPanY=0f;
            if (zoomBar != null) zoomBar.setProgress(0);
            // Brightness/contrast/sharpening do not change image dimensions or
            // coordinates, so existing annotations remain valid and visible.
            updateFrozenImage();
            status.setText("Detail gambar diterapkan");
        } catch (Exception e) { status.setText("Pengaturan gambar gagal"); }
    }

    private int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private Bitmap sharpenBitmap(Bitmap src) {
        int w=src.getWidth(), h=src.getHeight();
        if (w<3 || h<3) return src;
        Bitmap out=src.copy(Bitmap.Config.ARGB_8888,true);
        int[] in=new int[w*h], dst=new int[w*h]; src.getPixels(in,0,w,0,0,w,h);
        for(int y=1;y<h-1;y++) for(int x=1;x<w-1;x++) {
            int i=y*w+x; int center=in[i];
            int rr=5*Color.red(center)-Color.red(in[i-1])-Color.red(in[i+1])-Color.red(in[i-w])-Color.red(in[i+w]);
            int gg=5*Color.green(center)-Color.green(in[i-1])-Color.green(in[i+1])-Color.green(in[i-w])-Color.green(in[i+w]);
            int bb=5*Color.blue(center)-Color.blue(in[i-1])-Color.blue(in[i+1])-Color.blue(in[i-w])-Color.blue(in[i+w]);
            dst[i]=Color.argb(Color.alpha(center),clamp(rr),clamp(gg),clamp(bb));
        }
        for(int x=0;x<w;x++){dst[x]=in[x];dst[(h-1)*w+x]=in[(h-1)*w+x];}
        for(int y=0;y<h;y++){dst[y*w]=in[y*w];dst[y*w+w-1]=in[y*w+w-1];}
        out.setPixels(dst,0,w,0,0,w,h); return out;
    }

    private void saveAnnotatedFreeze() {
        if (!frozen || frozenBitmap == null || annotationView == null) {
            status.setText("Bekukan gambar dulu");
            return;
        }
        Bitmap result = buildAnnotatedBitmap();
        if (result == null) {
            status.setText("Gagal menyiapkan gambar");
            return;
        }
        if (saveBitmapToGallery(result) != null) status.setText("💾 Hasil inspeksi tersimpan • V3.9.0");
    }

    private Bitmap buildAnnotatedBitmap() {
        Bitmap base = getFrozenZoomedBitmap();
        if (base == null) return null;
        Bitmap result = base.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        CropInfo crop = getCurrentCropInfo();
        if (crop == null) return result;
        annotationView.drawAnnotationsToCrop(canvas, crop.left, crop.top, crop.width, crop.height,
                result.getWidth(), result.getHeight());
        return result;
    }

    private static class CropInfo {
        int left, top, width, height;
        CropInfo(int l, int t, int w, int h) { left=l; top=t; width=w; height=h; }
    }

    private CropInfo getCurrentCropInfo() {
        if (frozenBitmap == null) return null;
        int w = frozenBitmap.getWidth(), h = frozenBitmap.getHeight();
        float cropScale = 1f / frozenZoom;
        int cw = Math.max(1, Math.min(w, Math.round(w * cropScale)));
        int ch = Math.max(1, Math.min(h, Math.round(h * cropScale)));
        float cx = w / 2f - frozenPanX / Math.max(1f, freezeView == null ? 1f : freezeView.getWidth()) * w;
        float cy = h / 2f - frozenPanY / Math.max(1f, freezeView == null ? 1f : freezeView.getHeight()) * h;
        int left = Math.max(0, Math.min(w - cw, Math.round(cx - cw / 2f)));
        int top = Math.max(0, Math.min(h - ch, Math.round(cy - ch / 2f)));
        return new CropInfo(left, top, cw, ch);
    }

    private void showTextInput(final float x, final float y) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Contoh: VCC / jalur putus");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);

        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), 0);
        box.addView(input, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView sizeLabel = makeInfoText("Ukuran teks");
        sizeLabel.setTextSize(12);
        box.addView(sizeLabel, new LinearLayout.LayoutParams(-1, dp(28)));
        final SeekBar sizeBar = new SeekBar(this);
        sizeBar.setMax(100);
        sizeBar.setProgress(Math.max(0, Math.min(100, Math.round((annotationView == null ? 5f : annotationView.getTextSizePreset()) * 10f))));
        box.addView(sizeBar, new LinearLayout.LayoutParams(-1, dp(42)));

        final CheckBox bold = new CheckBox(this);
        bold.setText("Teks tebal (Bold)");
        bold.setTextColor(Color.WHITE);
        box.addView(bold, new LinearLayout.LayoutParams(-1, dp(40)));
        final CheckBox background = new CheckBox(this);
        background.setText("Latar belakang hitam");
        background.setTextColor(Color.WHITE);
        box.addView(background, new LinearLayout.LayoutParams(-1, dp(40)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Teks PRO")
                .setView(box)
                .setPositiveButton("Tambah", (d, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty() && annotationView != null) {
                        float textSize = 1f + sizeBar.getProgress() / 10f;
                        annotationView.addTextAtView(x, y, value, textSize, bold.isChecked(), background.isChecked());
                        status.setText("Teks PRO ditambahkan");
                    }
                })
                .setNegativeButton("Batal", null).create();
        dialog.setOnShowListener(d -> {
            input.requestFocus();
            input.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }, 150);
        });
        dialog.show();
    }

    private void editSelectedText() {
        if (annotationView == null || !annotationView.hasSelectedText()) {
            status.setText("Pilih objek Teks dulu");
            return;
        }
        final Annotation a = annotationView.getSelectedText();
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(a.text == null ? "" : a.text);
        input.setTextColor(Color.WHITE);

        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), 0);
        box.addView(input, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView sizeLabel = makeInfoText("Ukuran teks");
        sizeLabel.setTextSize(12);
        box.addView(sizeLabel, new LinearLayout.LayoutParams(-1, dp(28)));
        final SeekBar sizeBar = new SeekBar(this);
        sizeBar.setMax(100);
        sizeBar.setProgress(Math.max(0, Math.min(100, Math.round((a.size) * 10f))));
        box.addView(sizeBar, new LinearLayout.LayoutParams(-1, dp(42)));
        final CheckBox bold = new CheckBox(this);
        bold.setText("Teks tebal (Bold)");
        bold.setTextColor(Color.WHITE);
        bold.setChecked(a.bold);
        box.addView(bold, new LinearLayout.LayoutParams(-1, dp(40)));
        final CheckBox background = new CheckBox(this);
        background.setText("Latar belakang hitam");
        background.setTextColor(Color.WHITE);
        background.setChecked(a.textBackground);
        box.addView(background, new LinearLayout.LayoutParams(-1, dp(40)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Teks PRO")
                .setView(box)
                .setPositiveButton("Simpan", (d, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        float textSize = 1f + sizeBar.getProgress() / 10f;
                        annotationView.changeSelectedText(value, textSize, bold.isChecked(), background.isChecked());
                        status.setText("Teks PRO diperbarui");
                    }
                })
                .setNegativeButton("Batal", null).create();
        dialog.setOnShowListener(d -> { input.requestFocus(); input.selectAll();
            input.postDelayed(() -> { InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); if(imm!=null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT); }, 120);
        });
        dialog.show();
    }

    private void shareAnnotatedFreeze() {
        if (!frozen || frozenBitmap == null || annotationView == null) {
            status.setText("Bekukan gambar dulu");
            return;
        }
        Bitmap result = buildAnnotatedBitmap();
        Uri uri = result == null ? null : saveBitmapToGallery(result);
        if (uri == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Bagikan hasil inspeksi PCB • Deroid Unbricker Nota"));
    }

    private class OverlayView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float focusX = -1f, focusY = -1f;
        private long focusUntil = 0L;
        OverlayView(Context context) {
            super(context);
            setBackgroundColor(Color.TRANSPARENT);
            setClickable(false);
            setFocusable(false);
            setWillNotDraw(false);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!crosshairOn && !gridOn) return;
            int w=getWidth(), h=getHeight();
            if (w<=0 || h<=0) return;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.BUTT);
            if (gridOn) {
                p.setColor(Color.argb(overlayAlpha, Color.red(overlayColor), Color.green(overlayColor), Color.blue(overlayColor)));
                p.setStrokeWidth(dp(1));
                int cols=gridDivisions, rows=Math.max(6, Math.round(cols*h/(float)w));
                for(int i=1;i<cols;i++) { float x=w*i/(float)cols; canvas.drawLine(x,0,x,h,p); }
                for(int j=1;j<rows;j++) { float y=h*j/(float)rows; canvas.drawLine(0,y,w,y,p); }
            }
            if (crosshairOn) {
                p.setColor(Color.argb(overlayAlpha, Color.red(crosshairColor), Color.green(crosshairColor), Color.blue(crosshairColor)));
                p.setStrokeWidth(dp(2));
                float cx=w/2f, cy=h/2f;
                canvas.drawLine(cx,0,cx,h,p);
                canvas.drawLine(0,cy,w,cy,p);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(cx,cy,dp(13),p);
                p.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx,cy,dp(3),p);
            }
            if (focusX >= 0f && focusY >= 0f && System.currentTimeMillis() < focusUntil) {
                float pulse = dp(22) + dp(5) * (float)Math.sin((focusUntil - System.currentTimeMillis()) / 90.0);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp(2));
                p.setColor(Color.YELLOW);
                canvas.drawCircle(focusX, focusY, pulse, p);
                canvas.drawLine(focusX-dp(32),focusY,focusX-dp(18),focusY,p);
                canvas.drawLine(focusX+dp(18),focusY,focusX+dp(32),focusY,p);
                canvas.drawLine(focusX,focusY-dp(32),focusX,focusY-dp(18),p);
                canvas.drawLine(focusX,focusY+dp(18),focusX,focusY+dp(32),p);
                postInvalidateDelayed(50);
            }
        }
        void showFocus(float x, float y) {
            focusX = x; focusY = y; focusUntil = System.currentTimeMillis() + 900L; invalidate();
        }
        void refresh(){ invalidate(); }
    }

    private void showOverlaySettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(2), dp(8), dp(2));

        CheckBox cross = new CheckBox(this);
        cross.setText("Crosshair"); cross.setTextColor(Color.WHITE); cross.setTextSize(16); cross.setChecked(crosshairOn);
        CheckBox grid = new CheckBox(this);
        grid.setText("Grid PCB"); grid.setTextColor(Color.WHITE); grid.setTextSize(16); grid.setChecked(gridOn);
        box.addView(cross); box.addView(grid);

        TextView sizeLabel = makeInfoText("Ukuran Grid"); sizeLabel.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        box.addView(sizeLabel, new LinearLayout.LayoutParams(-1, dp(32)));
        LinearLayout sizeRow = new LinearLayout(this); sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        Button small = makeButton("Kecil"); Button medium = makeButton("Sedang"); Button large = makeButton("Besar");
        sizeRow.addView(small,new LinearLayout.LayoutParams(0,dp(42),1));
        sizeRow.addView(medium,new LinearLayout.LayoutParams(0,dp(42),1));
        sizeRow.addView(large,new LinearLayout.LayoutParams(0,dp(42),1));
        box.addView(sizeRow);

        TextView colorLabel = makeInfoText("Warna Grid / Crosshair"); colorLabel.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        box.addView(colorLabel,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout colorRow=new LinearLayout(this); colorRow.setOrientation(LinearLayout.HORIZONTAL);
        Button red=makeButton("●"); Button green=makeButton("●"); Button yellow=makeButton("●"); Button blue=makeButton("●"); Button white=makeButton("●");
        red.setTextColor(Color.RED); green.setTextColor(Color.GREEN); yellow.setTextColor(Color.YELLOW); blue.setTextColor(Color.CYAN); white.setTextColor(Color.WHITE);
        Button[] colors={red,green,yellow,blue,white};
        for(Button b:colors) colorRow.addView(b,new LinearLayout.LayoutParams(0,dp(40),1));
        box.addView(colorRow);

        TextView opacityLabel = makeInfoText("Transparansi Overlay  " + Math.round(overlayAlpha*100f/255f) + "%");
        opacityLabel.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        box.addView(opacityLabel,new LinearLayout.LayoutParams(-1,dp(32)));
        SeekBar opacity=new SeekBar(this); opacity.setMax(80); opacity.setProgress(Math.round(overlayAlpha*80f/255f));
        box.addView(opacity,new LinearLayout.LayoutParams(-1,dp(44)));

        cross.setOnCheckedChangeListener((b,checked)->{crosshairOn=checked; overlayView.refresh();});
        grid.setOnCheckedChangeListener((b,checked)->{gridOn=checked; overlayView.refresh();});
        small.setOnClickListener(v->{gridDivisions=16; overlayView.refresh(); sizeLabel.setText("Ukuran Grid: Kecil");});
        medium.setOnClickListener(v->{gridDivisions=10; overlayView.refresh(); sizeLabel.setText("Ukuran Grid: Sedang");});
        large.setOnClickListener(v->{gridDivisions=6; overlayView.refresh(); sizeLabel.setText("Ukuran Grid: Besar");});
        View.OnClickListener colorClick=v->{
            if(v==red){overlayColor=Color.RED;crosshairColor=Color.RED;}
            else if(v==green){overlayColor=Color.GREEN;crosshairColor=Color.GREEN;}
            else if(v==yellow){overlayColor=Color.YELLOW;crosshairColor=Color.YELLOW;}
            else if(v==blue){overlayColor=Color.CYAN;crosshairColor=Color.CYAN;}
            else {overlayColor=Color.WHITE;crosshairColor=Color.WHITE;}
            overlayView.refresh();
        };
        for(Button b:colors) b.setOnClickListener(colorClick);
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int value,boolean fromUser){overlayAlpha=Math.max(30,Math.round(value*255f/80f));opacityLabel.setText("Transparansi Overlay  "+Math.round(overlayAlpha*100f/255f)+"%");overlayView.refresh();}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });

        new AlertDialog.Builder(this).setTitle("V3.0 • Crosshair + Grid")
                .setView(box).setPositiveButton("Selesai",(d,w)->status.setText((crosshairOn||gridOn)?"Overlay aktif • Crosshair + Grid":"Overlay OFF"))
                .setNeutralButton("Reset",(d,w)->{crosshairOn=false;gridOn=false;gridDivisions=10;overlayColor=Color.GREEN;crosshairColor=Color.RED;overlayAlpha=150;overlayView.refresh();status.setText("Overlay di-reset");}).show();
    }

    private class AnnotationView extends View {
        private boolean cropSelecting=false;
        private float cropStartX, cropStartY, cropEndX, cropEndY;
        private int cropDragMode=0; // 1 move, 2 TL, 3 TR, 4 BL, 5 BR, 6 T, 7 R, 8 B, 9 L
        private float cropLastX, cropLastY;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final java.util.ArrayList<Annotation> items = new java.util.ArrayList<>();
        private AnnotationMode mode = AnnotationMode.NONE;
        private float startX, startY, endX, endY;
        private boolean drawing = false;
        private int currentColor = Color.RED;
        private float strokeDp = 5f;
        private Annotation selected;
        private float lastSelectX, lastSelectY;
        private float selectDownX, selectDownY;
        private boolean selectMoved = false;
        private float lastRotateAngle = 0f;
        private boolean rotatingSelected = false;
        private boolean transformingSelected = false;
        private boolean moveHistoryPushed = false;
        // Selection/transform state for the 8 resize handles + rotation handle.
        private int selectedHandle = 0;
        private Annotation transformStart = null;
        private float transformStartAngle = 0f;
        private float transformStartDistance = 1f;
        private float lastTransformDistance = 0f;
        private float transformDownX = 0f, transformDownY = 0f;
        private final java.util.ArrayDeque<java.util.ArrayList<Annotation>> undoStack = new java.util.ArrayDeque<>();
        private final java.util.ArrayDeque<java.util.ArrayList<Annotation>> redoStack = new java.util.ArrayDeque<>();
        private ScaleGestureDetector jumperScaleDetector;
        private boolean jumperPinching = false;

        AnnotationView(Context context) {
            super(context);
            setBackground(new ColorDrawable(Color.TRANSPARENT));
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            jumperScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                    jumperPinching = true;
                    return true;
                }
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    if (!frozen || frozenBitmap == null) return false;
                    frozenZoom = Math.max(1f, Math.min(8f, frozenZoom * detector.getScaleFactor()));
                    updateFrozenImage();
                    return true;
                }
                @Override public void onScaleEnd(ScaleGestureDetector detector) {
                    jumperPinching = false;
                }
            });
        }

        void setMode(AnnotationMode m) { mode=m; drawing=false; selected=null; rotatingSelected=false; transformingSelected=false; selectedHandle=0; transformStart=null; selectMoved=false; invalidate(); }
        void prepareOcrHistory(){ pushUndo(); }
        void addOcr(float x1, float y1, float x2, float y2, String text) {
            items.add(Annotation.ocr(x1, y1, x2, y2, text, Color.YELLOW, 3f));
        }
        void setColor(int color) {
            currentColor=color;
            if (selected != null && selected.color != color) { pushUndo(); selected.color=color; }
            invalidate();
        }
        void setSize(float size) {
            strokeDp=size;
            if (selected != null && Math.abs(selected.size-size) > 0.01f) { pushUndo(); selected.size=size; }
            invalidate();
        }

        private java.util.ArrayList<Annotation> copyItems() {
            java.util.ArrayList<Annotation> out=new java.util.ArrayList<>();
            for(Annotation a:items) out.add(a.copy());
            return out;
        }
        private void pushUndo() { undoStack.push(copyItems()); redoStack.clear(); }
        void undo() {
            if (undoStack.isEmpty()) return;
            redoStack.push(copyItems()); items.clear(); items.addAll(undoStack.pop()); selected=null; invalidate();
        }
        void redo() {
            if (redoStack.isEmpty()) return;
            undoStack.push(copyItems()); items.clear(); items.addAll(redoStack.pop()); selected=null; invalidate();
        }
        void clearAll() { if (!items.isEmpty()) pushUndo(); items.clear(); selected=null; invalidate(); }
        boolean deleteSelected() {
            if (selected == null) return false;
            pushUndo();
            items.remove(selected);
            selected = null;
            invalidate();
            return true;
        }
        boolean duplicateSelected() {
            if (selected==null) return false;
            pushUndo(); Annotation c=selected.copy(); float dx=dp(18)/Math.max(0.35f,frozenMatrix.mapRadius(1f));
            c.x1+=dx; c.y1+=dx; c.x2+=dx; c.y2+=dx;
            if (c.points != null) for (PointF pt : c.points) { pt.x += dx; pt.y += dx; }
            items.add(c); selected=c; invalidate(); return true;
        }
        boolean rotateSelectedBy(float degrees) {
            if (selected == null) return false;
            pushUndo();
            selected.rotation += degrees;
            invalidate();
            return true;
        }
        boolean hasSelectedText(){ return selected!=null && selected.type==AnnotationMode.TEXT; }
        Annotation getSelectedText(){ return selected; }

        int getSelectedIndex() { return selected == null ? -1 : items.indexOf(selected); }

        String[] getLayerLabels() {
            String[] out = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                Annotation a = items.get(i);
                String label = annotationTypeLabel(a);
                String extra = (a.type == AnnotationMode.TEXT && a.text != null && !a.text.trim().isEmpty()) ? " • " + a.text : "";
                out[i] = String.format(java.util.Locale.US, "%02d • %s%s", i + 1, label, extra);
            }
            return out;
        }

        boolean selectIndex(int index) {
            if (index < 0 || index >= items.size()) return false;
            selected = items.get(index);
            mode = AnnotationMode.SELECT;
            invalidate();
            return true;
        }

        boolean bringSelectedToFront() {
            if (selected == null) return false;
            int idx = items.indexOf(selected);
            if (idx < 0) return false;
            if (idx == items.size() - 1) { invalidate(); return true; }
            pushUndo();
            items.remove(idx);
            items.add(selected);
            invalidate();
            return true;
        }

        boolean sendSelectedToBack() {
            if (selected == null) return false;
            int idx = items.indexOf(selected);
            if (idx < 0) return false;
            if (idx == 0) { invalidate(); return true; }
            pushUndo();
            items.remove(idx);
            items.add(0, selected);
            invalidate();
            return true;
        }

        void changeSelectedText(String value){ if(hasSelectedText()){ pushUndo(); selected.text=value; invalidate(); } }
        void changeSelectedText(String value, float size, boolean bold, boolean background){
            if(hasSelectedText()){
                pushUndo(); selected.text=value; selected.size=Math.max(1f, Math.min(11f, size)); selected.bold=bold; selected.textBackground=background; invalidate();
            }
        }
        float getTextSizePreset(){ return strokeDp; }

        // Android's 90° rotation used by rotateFrozenImage maps an old source
        // point (x,y) to the new bitmap as (oldHeight-y, x). Preserve every
        // annotation instead of clearing the technician's markings.
        private PointF rotatePoint90(PointF p, int oldW, int oldH) {
            return new PointF(oldH - p.y, p.x);
        }

        // Crop changes the source origin from (left,top) to (0,0). Translate
        // every annotation, including freehand PEN points, into the new space.
        void transformAnnotationsForCrop(int left, int top) {
            for (Annotation a : items) {
                if (a.type == AnnotationMode.PEN && a.points != null) {
                    for (PointF p : a.points) {
                        p.x -= left;
                        p.y -= top;
                    }
                } else {
                    a.x1 -= left; a.y1 -= top;
                    a.x2 -= left; a.y2 -= top;
                }
            }
            invalidate();
        }

        void transformAnnotationsForRotate(int oldW, int oldH) {
            for (Annotation a : items) {
                if (a.type == AnnotationMode.PEN && a.points != null) {
                    for (int i = 0; i < a.points.size(); i++) {
                        a.points.set(i, rotatePoint90(a.points.get(i), oldW, oldH));
                    }
                } else {
                    PointF p1 = rotatePoint90(new PointF(a.x1, a.y1), oldW, oldH);
                    PointF p2 = rotatePoint90(new PointF(a.x2, a.y2), oldW, oldH);
                    a.x1 = p1.x; a.y1 = p1.y;
                    a.x2 = p2.x; a.y2 = p2.y;
                }
                a.rotation = (a.rotation + 90f) % 360f;
            }
            invalidate();
        }

        private float[] viewToSource(float x, float y) {
            Matrix inv = new Matrix();
            frozenMatrix.invert(inv);
            float[] pts = new float[]{x,y};
            inv.mapPoints(pts);
            return pts;
        }

        private float[] sourceToView(float x, float y) {
            float[] pts = new float[]{x,y};
            frozenMatrix.mapPoints(pts);
            return pts;
        }

        float[] viewToSourcePublic(float x, float y) { return viewToSource(x,y); }

        void addTextAtView(float x, float y, String text) {
            addTextAtView(x, y, text, Math.max(1f, strokeDp), false, false);
        }
        void addTextAtView(float x, float y, String text, float size, boolean bold, boolean background) {
            float[] p=viewToSource(x,y);
            pushUndo();
            Annotation a = Annotation.text(p[0],p[1],text,currentColor,size);
            a.bold = bold;
            a.textBackground = background;
            items.add(a);
            selected = a;
            invalidate();
        }

        void beginCrop() {
            if (getWidth() <= 0 || getHeight() <= 0 || frozenBitmap == null) return;
            float[] p1 = sourceToView(0, 0);
            float[] p2 = sourceToView(frozenBitmap.getWidth(), frozenBitmap.getHeight());
            float il = Math.max(0, Math.min(p1[0], p2[0]));
            float it = Math.max(0, Math.min(p1[1], p2[1]));
            float ir = Math.min(getWidth(), Math.max(p1[0], p2[0]));
            float ib = Math.min(getHeight(), Math.max(p1[1], p2[1]));
            float w = ir - il, h = ib - it;
            if (w < 40 || h < 40) { il=8; it=8; ir=getWidth()-8; ib=getHeight()-8; w=ir-il; h=ib-it; }
            float marginX=w*0.12f, marginY=h*0.12f;
            cropStartX=il+marginX; cropStartY=it+marginY;
            cropEndX=ir-marginX; cropEndY=ib-marginY;
            cropSelecting=true; cropDragMode=0;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawAnnotations(canvas);
            if (drawing && mode != AnnotationMode.TEXT && mode != AnnotationMode.MARKER && mode != AnnotationMode.SELECT && mode != AnnotationMode.PEN && mode != AnnotationMode.CROP && mode != AnnotationMode.JUMPER) {
                float[] a=viewToSource(startX,startY), b=viewToSource(endX,endY);
                drawOne(canvas, Annotation.shape(mode,a[0],a[1],b[0],b[1],currentColor,strokeDp));
            }
            if (drawing && mode == AnnotationMode.JUMPER) {
                float[] a=viewToSource(startX,startY), b=viewToSource(endX,endY);
                drawOne(canvas, Annotation.shape(AnnotationMode.JUMPER,a[0],a[1],b[0],b[1],Color.YELLOW,Math.max(4f,strokeDp)));
            }
            if (mode == AnnotationMode.CROP && cropSelecting) {
                Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
                cp.setStyle(Paint.Style.FILL); cp.setColor(Color.argb(90,0,0,0));
                canvas.drawRect(0,0,getWidth(),getHeight(),cp);
                cp.setStyle(Paint.Style.STROKE); cp.setStrokeWidth(dp(2)); cp.setColor(Color.WHITE);
                float l=Math.min(cropStartX,cropEndX), t=Math.min(cropStartY,cropEndY);
                float r=Math.max(cropStartX,cropEndX), b=Math.max(cropStartY,cropEndY);
                canvas.drawRect(l,t,r,b,cp);
                cp.setColor(Color.YELLOW); cp.setStrokeWidth(dp(1));
                float thirdW=(r-l)/3f, thirdH=(b-t)/3f;
                canvas.drawLine(l+thirdW,t,l+thirdW,b,cp); canvas.drawLine(l+2*thirdW,t,l+2*thirdW,b,cp);
                canvas.drawLine(l,t+thirdH,r,t+thirdH,cp); canvas.drawLine(l,t+2*thirdH,r,t+2*thirdH,cp);
                // 8 handle: 4 sudut + 4 sisi, lebih mudah untuk crop presisi.
                cp.setStyle(Paint.Style.FILL); cp.setColor(Color.WHITE);
                float hs=dp(12);
                float[] hx={l,r,l,r,(l+r)/2f,r,(l+r)/2f,l};
                float[] hy={t,t,b,b,t,(t+b)/2f,b,(t+b)/2f};
                for(int i=0;i<8;i++) canvas.drawRoundRect(hx[i]-hs/2,hy[i]-hs/2,hx[i]+hs/2,hy[i]+hs/2,dp(2),dp(2),cp);
                cp.setStyle(Paint.Style.STROKE); cp.setStrokeWidth(dp(2)); cp.setColor(Color.WHITE);
                canvas.drawRect(l,t,r,b,cp);
                cp.setStyle(Paint.Style.FILL); cp.setColor(Color.argb(220,255,255,255));
                cp.setTextSize(dp(12)); cp.setTypeface(Typeface.DEFAULT_BOLD);
                canvas.drawText("GESER AREA • TARIK 8 TITIK • ✓ TERAPKAN", Math.max(8,l), Math.max(dp(18),t-dp(8)), cp);
            }
        }

        private void drawAnnotations(Canvas canvas) {
            canvas.save();
            canvas.concat(frozenMatrix);
            for (Annotation a:items) drawOneSource(canvas,a);
            canvas.restore();
            if (mode == AnnotationMode.SELECT && selected != null) drawSelectionOverlay(canvas, selected);
        }

        private String annotationTypeLabel(Annotation a) {
            if (a == null) return "Objek";
            switch (a.type) {
                case PEN: return "Pena";
                case MARKER: return "Marker";
                case ARROW: return "Panah";
                case LINE: return "Garis";
                case CIRCLE: return "Lingkaran";
                case RECT: return "Kotak";
                case HIGHLIGHT: return "Highlight";
                case TEXT: return "Teks";
                case OCR: return "OCR";
                case JUMPER: return "Jumper";
                default: return "Objek";
            }
        }

        private float normalizeRotation(float angle) {
            float r = angle % 360f;
            if (r > 180f) r -= 360f;
            if (r < -180f) r += 360f;
            return r;
        }

        private boolean jumperSnapActive = false;
        private float jumperSnapX = 0f, jumperSnapY = 0f;

        private void drawSelectionOverlay(Canvas canvas, Annotation a) {
            float[] box = annotationViewBounds(a);
            float l=box[0], t=box[1], r=box[2], b=box[3];
            Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
            // Clean selection UI: solid cyan outline instead of a distracting
            // white dashed box with yellow handles. The cyan treatment is
            // easier to follow over PCB/camera imagery and is consistent
            // for every selected object.
            sp.setStyle(Paint.Style.STROKE);
            // Minimal selection frame: thin cyan line and four corner handles.
            // The touch hit areas remain larger in hitSelectionHandle(), so the
            // UI stays clean without making small PCB objects difficult to edit.
            sp.setStrokeWidth(dp(2));
            sp.setColor(Color.argb(130,0,0,0));
            sp.setPathEffect(null);
            canvas.drawRect(l,t,r,b,sp);
            sp.setStrokeWidth(dp(2));
            sp.setColor(Color.rgb(0, 220, 255));
            canvas.drawRect(l,t,r,b,sp);

            float hs=dp(5);
            float[][] corners={{l,t},{r,t},{l,b},{r,b}};
            sp.setStyle(Paint.Style.FILL);
            sp.setColor(Color.WHITE);
            for(float[] pt:corners) canvas.drawCircle(pt[0],pt[1],hs,sp);
            sp.setStyle(Paint.Style.STROKE);
            sp.setStrokeWidth(dp(2));
            sp.setColor(Color.rgb(0, 220, 255));
            for(float[] pt:corners) canvas.drawCircle(pt[0],pt[1],hs,sp);

            // Rotation handle: cyan ring + white center.
            float ry=t-dp(28);
            sp.setStyle(Paint.Style.STROKE); sp.setStrokeWidth(dp(2));
            sp.setColor(Color.rgb(0, 220, 255));
            canvas.drawLine((l+r)/2f,t,(l+r)/2f,ry+dp(6),sp);
            sp.setStyle(Paint.Style.FILL); sp.setColor(Color.WHITE);
            canvas.drawCircle((l+r)/2f,ry,dp(7),sp);
            sp.setStyle(Paint.Style.STROKE); sp.setStrokeWidth(dp(2)); sp.setColor(Color.rgb(0, 220, 255));
            canvas.drawCircle((l+r)/2f,ry,dp(7),sp);

            if (a.type==AnnotationMode.JUMPER && jumperSnapActive) {
                float[] sv = sourceToView(jumperSnapX, jumperSnapY);
                Paint sg = new Paint(Paint.ANTI_ALIAS_FLAG);
                sg.setStyle(Paint.Style.STROKE); sg.setStrokeWidth(dp(2)); sg.setColor(Color.CYAN);
                canvas.drawCircle(sv[0], sv[1], dp(22), sg);
                canvas.drawLine(sv[0]-dp(28), sv[1], sv[0]+dp(28), sv[1], sg);
                canvas.drawLine(sv[0], sv[1]-dp(28), sv[0], sv[1]+dp(28), sg);
            }

            // Jumper PRO endpoint markers: A = source pad, B = destination pad.
            // They are shown only for a selected jumper so the edit screen stays clean.
            if (a.type==AnnotationMode.JUMPER) {
                float cx=(a.x1+a.x2)/2f, cy=(a.y1+a.y2)/2f;
                double rad=Math.toRadians(a.rotation), cs=Math.cos(rad), sn=Math.sin(rad);
                float ax=(float)(cx+(a.x1-cx)*cs-(a.y1-cy)*sn);
                float ay=(float)(cy+(a.x1-cx)*sn+(a.y1-cy)*cs);
                float bx=(float)(cx+(a.x2-cx)*cs-(a.y2-cy)*sn);
                float by=(float)(cy+(a.x2-cx)*sn+(a.y2-cy)*cs);
                float[] av=sourceToView(ax,ay), bv=sourceToView(bx,by);
                Paint ep=new Paint(Paint.ANTI_ALIAS_FLAG);
                ep.setStyle(Paint.Style.FILL); ep.setTypeface(Typeface.DEFAULT_BOLD); ep.setTextSize(dp(11));
                ep.setColor(Color.BLACK);
                canvas.drawCircle(av[0],av[1],dp(18),ep); canvas.drawCircle(bv[0],bv[1],dp(18),ep);
                ep.setStyle(Paint.Style.STROKE); ep.setStrokeWidth(dp(3)); ep.setColor(Color.rgb(0, 220, 255));
                canvas.drawCircle(av[0],av[1],dp(17),ep); canvas.drawCircle(bv[0],bv[1],dp(17),ep);
                ep.setStyle(Paint.Style.FILL); ep.setColor(Color.WHITE);
                canvas.drawText("A",av[0]-dp(4),av[1]+dp(4),ep);
                canvas.drawText("B",bv[0]-dp(4),bv[1]+dp(4),ep);
            }

            // Rotation angle is shown only while the user is actively rotating.
            // This keeps the editor clean while still giving precise feedback.
            if (rotatingSelected) {
                sp.setTypeface(Typeface.DEFAULT_BOLD);
                sp.setTextSize(dp(12));
                String angleText = String.format(java.util.Locale.US, "Rotasi %+.0f°", normalizeAngle(a.rotation));
                float tw = sp.measureText(angleText);
                float tx = Math.max(dp(8), Math.min(getWidth()-tw-dp(8), (l+r)/2f-tw/2f));
                float ty = Math.max(dp(24), ry-dp(10));
                sp.setStyle(Paint.Style.FILL);
                sp.setColor(Color.argb(220,0,0,0));
                canvas.drawRoundRect(tx-dp(7), ty-dp(16), tx+tw+dp(7), ty+dp(5), dp(7), dp(7), sp);
                sp.setColor(Color.WHITE);
                canvas.drawText(angleText, tx, ty, sp);
            }
        }

        private float normalizeAngle(float angle) {
            float a = angle % 360f;
            if (a > 180f) a -= 360f;
            if (a < -180f) a += 360f;
            return a;
        }

        /**
         * Returns an axis-aligned VIEW-space bounding box for the actual
         * annotation geometry.  The previous implementation built a box from
         * x1/x2/y1/y2 and then clipped it to the View, which made the yellow
         * handles jump to the screen edge when an object was partly off-screen.
         * This version measures the real geometry first, applies object
         * rotation, then applies frozenMatrix.  Nothing is clipped here; the
         * Canvas itself clips what is outside the visible image.
         */
        private float[] annotationViewBounds(Annotation a) {
            if (a == null) return new float[]{0,0,0,0};
            float scale = Math.max(0.05f, frozenMatrix.mapRadius(1f));
            float padView = dp(12);
            float pad = padView / scale;
            float minX, minY, maxX, maxY;

            if (a.type==AnnotationMode.PEN && a.points!=null && !a.points.isEmpty()) {
                minX=maxX=a.points.get(0).x; minY=maxY=a.points.get(0).y;
                for(PointF p:a.points){
                    minX=Math.min(minX,p.x); maxX=Math.max(maxX,p.x);
                    minY=Math.min(minY,p.y); maxY=Math.max(maxY,p.y);
                }
                pad += a.size * 0.5f;
            } else if (a.type==AnnotationMode.TEXT) {
                // Text bounds are computed in VIEW pixels so zoom does not enlarge the label.
                Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
                tp.setTypeface(a.bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                float textPx = dp((int)(18 + a.size));
                tp.setTextSize(textPx / scale);
                String txt=a.text==null?"":a.text;
                float twSrc=tp.measureText(txt);
                float thSrc=tp.getTextSize();
                float[] anchor=sourceToView(a.x1,a.y1);
                float halfPad=dp(a.textBackground ? 9 : 6);
                float[] vx={anchor[0], anchor[0]+twSrc*scale, anchor[0]+twSrc*scale, anchor[0]};
                float[] vy={anchor[1]-thSrc*scale, anchor[1]-thSrc*scale, anchor[1], anchor[1]};
                float cxV=anchor[0], cyV=anchor[1];
                float radV=(float)Math.toRadians(a.rotation), csV=(float)Math.cos(radV), snV=(float)Math.sin(radV);
                minX=Float.POSITIVE_INFINITY; minY=Float.POSITIVE_INFINITY; maxX=Float.NEGATIVE_INFINITY; maxY=Float.NEGATIVE_INFINITY;
                for(int i=0;i<4;i++){
                    float dx=vx[i]-cxV, dy=vy[i]-cyV;
                    float rxV=cxV+dx*csV-dy*snV, ryV=cyV+dx*snV+dy*csV;
                    minX=Math.min(minX,rxV); maxX=Math.max(maxX,rxV); minY=Math.min(minY,ryV); maxY=Math.max(maxY,ryV);
                }
                return new float[]{minX-halfPad,minY-halfPad,maxX+halfPad,maxY+halfPad};
            } else if (a.type==AnnotationMode.MARKER) {
                float rr=dp((int)a.size);
                minX=a.x1-rr; maxX=a.x1+rr; minY=a.y1-rr; maxY=a.y1;
            } else if (a.type==AnnotationMode.JUMPER) {
                float rr=dp(8);
                minX=Math.min(a.x1,a.x2)-rr; maxX=Math.max(a.x1,a.x2)+rr;
                minY=Math.min(a.y1,a.y2)-rr; maxY=Math.max(a.y1,a.y2)+rr;
            } else if (a.type==AnnotationMode.HIGHLIGHT) {
                float rr=dp(11);
                minX=Math.min(a.x1,a.x2)-rr; maxX=Math.max(a.x1,a.x2)+rr;
                minY=Math.min(a.y1,a.y2)-rr; maxY=Math.max(a.y1,a.y2)+rr;
            } else if (a.type==AnnotationMode.ARROW) {
                float len=dp(18);
                float ang=(float)Math.atan2(a.y2-a.y1,a.x2-a.x1);
                float ax=a.x2-len*(float)Math.cos(ang-.45f);
                float ay=a.y2-len*(float)Math.sin(ang-.45f);
                float bx=a.x2-len*(float)Math.cos(ang+.45f);
                float by=a.y2-len*(float)Math.sin(ang+.45f);
                minX=Math.min(Math.min(a.x1,a.x2),Math.min(ax,bx));
                maxX=Math.max(Math.max(a.x1,a.x2),Math.max(ax,bx));
                minY=Math.min(Math.min(a.y1,a.y2),Math.min(ay,by));
                maxY=Math.max(Math.max(a.y1,a.y2),Math.max(ay,by));
                pad += a.size * 0.5f;
            } else {
                minX=Math.min(a.x1,a.x2); maxX=Math.max(a.x1,a.x2);
                minY=Math.min(a.y1,a.y2); maxY=Math.max(a.y1,a.y2);
                pad += a.size * 0.5f;
            }

            minX-=pad; minY-=pad; maxX+=pad; maxY+=pad;
            float cx=(minX+maxX)/2f, cy=(minY+maxY)/2f;

            // Rotate the source-space bounds around the object's visual center.
            float rot=a.rotation;
            float rad=(float)Math.toRadians(rot);
            float cs=(float)Math.cos(rad), sn=(float)Math.sin(rad);
            float[] xs={minX,maxX,maxX,minX};
            float[] ys={minY,minY,maxY,maxY};
            float outL=Float.POSITIVE_INFINITY,outT=Float.POSITIVE_INFINITY;
            float outR=Float.NEGATIVE_INFINITY,outB=Float.NEGATIVE_INFINITY;
            for(int i=0;i<4;i++){
                float dx=xs[i]-cx, dy=ys[i]-cy;
                float sx=cx+dx*cs-dy*sn, sy=cy+dx*sn+dy*cs;
                float[] v=sourceToView(sx,sy);
                outL=Math.min(outL,v[0]); outR=Math.max(outR,v[0]);
                outT=Math.min(outT,v[1]); outB=Math.max(outB,v[1]);
            }
            return new float[]{outL,outT,outR,outB};
        }

        private void drawOneSource(Canvas canvas, Annotation a) {
            float scale = Math.max(0.05f, frozenMatrix.mapRadius(1f));
            paint.setColor(a.color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp((int)a.size) * scale);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setTextSize(dp((int)(18 + a.size)) / scale);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            if (a.type==AnnotationMode.PEN) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp((int)a.size) * scale);
                paint.setStrokeCap(Paint.Cap.ROUND);
                if (a.points != null && a.points.size() > 1) {
                    Path path = new Path();
                    path.moveTo(a.points.get(0).x, a.points.get(0).y);
                    for (int i=1;i<a.points.size();i++) path.lineTo(a.points.get(i).x,a.points.get(i).y);
                    canvas.drawPath(path,paint);
                }
            } else if (a.type==AnnotationMode.MARKER) {
                paint.setStyle(Paint.Style.FILL); canvas.drawCircle(a.x1,a.y1,dp((int)a.size),paint);
            } else if (a.type==AnnotationMode.ARROW) {
                canvas.save();
                float cx=(a.x1+a.x2)/2f, cy=(a.y1+a.y2)/2f;
                canvas.rotate(a.rotation, cx, cy);
                canvas.drawLine(a.x1,a.y1,a.x2,a.y2,paint);
                double ang=Math.atan2(a.y2-a.y1,a.x2-a.x1); float len=dp(18) * scale;
                canvas.drawLine(a.x2,a.y2,a.x2-len*(float)Math.cos(ang-.45),a.y2-len*(float)Math.sin(ang-.45),paint);
                canvas.drawLine(a.x2,a.y2,a.x2-len*(float)Math.cos(ang+.45),a.y2-len*(float)Math.sin(ang+.45),paint);
                canvas.restore();
            } else if (a.type==AnnotationMode.LINE) {
                canvas.drawLine(a.x1,a.y1,a.x2,a.y2,paint);
            } else if (a.type==AnnotationMode.JUMPER) {
                // Jumper PRO: insulated wire + clearly visible A/B pad endpoints.
                canvas.save();
                float cx=(a.x1+a.x2)/2f, cy=(a.y1+a.y2)/2f;
                canvas.rotate(a.rotation, cx, cy);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(dp(6) * scale);
                paint.setColor(a.color);
                canvas.drawLine(a.x1,a.y1,a.x2,a.y2,paint);
                paint.setStyle(Paint.Style.FILL);
                float rr = dp(7) * scale;
                canvas.drawCircle(a.x1,a.y1,rr,paint);
                canvas.drawCircle(a.x2,a.y2,rr,paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2) * scale);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(a.x1,a.y1,rr+dp(2) * scale,paint);
                canvas.drawCircle(a.x2,a.y2,rr+dp(2) * scale,paint);
                canvas.restore();
            } else if (a.type==AnnotationMode.CIRCLE) {
                canvas.drawOval(new RectF(Math.min(a.x1,a.x2),Math.min(a.y1,a.y2),Math.max(a.x1,a.x2),Math.max(a.y1,a.y2)),paint);
            } else if (a.type==AnnotationMode.RECT) {
                canvas.drawRect(new RectF(Math.min(a.x1,a.x2),Math.min(a.y1,a.y2),Math.max(a.x1,a.x2),Math.max(a.y1,a.y2)),paint);
            } else if (a.type==AnnotationMode.HIGHLIGHT) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(dp(22) * scale);
                paint.setColor(a.color == Color.RED || a.color == Color.YELLOW || a.color == Color.GREEN || a.color == Color.CYAN ? a.color : Color.YELLOW);
                paint.setAlpha(105);
                canvas.drawLine(a.x1,a.y1,a.x2,a.y2,paint);
                paint.setAlpha(255);
            } else if (a.type==AnnotationMode.TEXT) {
                // Text PRO: readable at any frozen zoom, freely rotatable, optional bold/background.
                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(a.bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                paint.setTextSize(dp((int)(18 + a.size)) / scale);
                String txt = a.text == null ? "" : a.text;
                float pad = dp(7) / scale;
                float tw = paint.measureText(txt);
                float th = paint.getTextSize();
                canvas.save();
                canvas.rotate(a.rotation, a.x1, a.y1);
                if (a.textBackground) {
                    paint.setColor(Color.argb(205, 0, 0, 0));
                    canvas.drawRoundRect(a.x1 - pad, a.y1 - th - pad, a.x1 + tw + pad, a.y1 + pad, dp(6) / scale, dp(6) / scale, paint);
                    paint.setColor(a.color);
                }
                paint.setShadowLayer(dp(3) / scale, 1f / scale, 1f / scale, Color.BLACK);
                canvas.drawText(txt, a.x1, a.y1, paint);
                paint.clearShadowLayer();
                canvas.restore();
            } else if (a.type==AnnotationMode.OCR) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2) * scale);
                canvas.drawRect(new RectF(a.x1, a.y1, a.x2, a.y2), paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(dp(14) * scale);
                paint.setShadowLayer(dp(3), 1, 1, Color.BLACK);
                canvas.drawText(a.text, a.x1, Math.max(a.y1 - dp(3) * scale, a.y1), paint);
                paint.clearShadowLayer();
            }
        }

        private void drawOne(Canvas canvas, Annotation a) { drawOneSource(canvas,a); }

        void drawAnnotationsToCrop(Canvas canvas,int left,int top,int cw,int ch,int outW,int outH) {
            canvas.save();
            float sx=(float)outW/cw, sy=(float)outH/ch;
            canvas.scale(sx,sy); canvas.translate(-left,-top);
            for (Annotation a:items) drawOneSource(canvas,a);
            canvas.restore();
        }

        private float distancePointToSegment(float px,float py,float x1,float y1,float x2,float y2) {
            float dx=x2-x1, dy=y2-y1;
            if (dx==0f && dy==0f) return (float)Math.hypot(px-x1,py-y1);
            float t=((px-x1)*dx+(py-y1)*dy)/(dx*dx+dy*dy);
            t=Math.max(0f,Math.min(1f,t));
            float qx=x1+t*dx, qy=y1+t*dy;
            return (float)Math.hypot(px-qx,py-qy);
        }

        private Annotation hitTest(float vx,float vy) {
            float[] p=viewToSource(vx,vy);
            float tolerance=dp(28)/Math.max(0.35f,frozenMatrix.mapRadius(1f));
            for (int i=items.size()-1;i>=0;i--) {
                Annotation a=items.get(i);
                if (a.type==AnnotationMode.PEN) {
                    if (a.points != null) {
                        for (int j=1;j<a.points.size();j++) {
                            PointF p0=a.points.get(j-1), p1=a.points.get(j);
                            if (distancePointToSegment(p[0],p[1],p0.x,p0.y,p1.x,p1.y)<=tolerance) return a;
                        }
                    }
                } else if (a.type==AnnotationMode.ARROW || a.type==AnnotationMode.LINE || a.type==AnnotationMode.HIGHLIGHT || a.type==AnnotationMode.JUMPER) {
                    float tx=p[0], ty=p[1];
                    if (a.rotation!=0f) {
                        float cx=(a.x1+a.x2)/2f, cy=(a.y1+a.y2)/2f;
                        double rr=Math.toRadians(-a.rotation), cs=Math.cos(rr), sn=Math.sin(rr);
                        float dx=tx-cx, dy=ty-cy;
                        tx=(float)(cx+dx*cs-dy*sn); ty=(float)(cy+dx*sn+dy*cs);
                    }
                    float tol=tolerance + dp(a.type==AnnotationMode.HIGHLIGHT?12:4)/Math.max(0.35f,frozenMatrix.mapRadius(1f));
                    if (a.type==AnnotationMode.JUMPER) {
                        float endTol=tol + dp(12)/Math.max(0.35f,frozenMatrix.mapRadius(1f));
                        if (Math.hypot(tx-a.x1,ty-a.y1)<=endTol || Math.hypot(tx-a.x2,ty-a.y2)<=endTol) return a;
                    }
                    if (distancePointToSegment(tx,ty,a.x1,a.y1,a.x2,a.y2)<=tol) return a;
                    if (a.type==AnnotationMode.ARROW) {
                        float len=dp(18);
                        double ang=Math.atan2(a.y2-a.y1,a.x2-a.x1);
                        float ax=a.x2-len*(float)Math.cos(ang-.45), ay=a.y2-len*(float)Math.sin(ang-.45);
                        float bx=a.x2-len*(float)Math.cos(ang+.45), by=a.y2-len*(float)Math.sin(ang+.45);
                        if(distancePointToSegment(tx,ty,a.x2,a.y2,ax,ay)<=tol || distancePointToSegment(tx,ty,a.x2,a.y2,bx,by)<=tol) return a;
                    }
                } else if (a.type==AnnotationMode.TEXT) {
                    // Hit-test text in VIEW space so the label keeps a constant screen size.
                    float[] av=sourceToView(a.x1,a.y1);
                    float dxv=vx-av[0], dyv=vy-av[1];
                    double r=Math.toRadians(-a.rotation), cs=Math.cos(r), sn=Math.sin(r);
                    float rx=(float)(dxv*cs-dyv*sn), ry=(float)(dxv*sn+dyv*cs);
                    Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG);
                    tp.setTypeface(Typeface.DEFAULT_BOLD);
                    float textPx=dp((int)(18+a.size));
                    tp.setTextSize(textPx);
                    float textW=tp.measureText(a.text==null?"":a.text), textH=tp.getTextSize();
                    float tolV=dp(14);
                    if(rx>=-tolV && rx<=textW+tolV && ry>=-textH-tolV && ry<=tolV) return a;
                } else if (a.type==AnnotationMode.CIRCLE) {
                    float l=Math.min(a.x1,a.x2), r=Math.max(a.x1,a.x2), t=Math.min(a.y1,a.y2), b=Math.max(a.y1,a.y2);
                    float cx=(l+r)/2f, cy=(t+b)/2f, rx=Math.max(1f,(r-l)/2f), ry=Math.max(1f,(b-t)/2f);
                    float nx=(p[0]-cx)/rx, ny=(p[1]-cy)/ry;
                    if(nx*nx+ny*ny<=1.25f) return a;
                } else if (a.type==AnnotationMode.RECT || a.type==AnnotationMode.OCR) {
                    float l=Math.min(a.x1,a.x2)-tolerance, r=Math.max(a.x1,a.x2)+tolerance;
                    float t=Math.min(a.y1,a.y2)-tolerance, b=Math.max(a.y1,a.y2)+tolerance;
                    if(p[0]>=l && p[0]<=r && p[1]>=t && p[1]<=b) return a;
                } else if (a.type==AnnotationMode.MARKER) {
                    float rr=tolerance+dp((int)a.size);
                    if(Math.hypot(p[0]-a.x1,p[1]-a.y1)<=rr) return a;
                }
            }
            return null;
        }

        private int hitJumperEndpointHandle(float x, float y, Annotation a) {
            if (a == null || a.type != AnnotationMode.JUMPER) return 0;
            float cx=(a.x1+a.x2)/2f, cy=(a.y1+a.y2)/2f;
            double rad=Math.toRadians(a.rotation), cs=Math.cos(rad), sn=Math.sin(rad);
            float ax=(float)(cx+(a.x1-cx)*cs-(a.y1-cy)*sn);
            float ay=(float)(cy+(a.x1-cx)*sn+(a.y1-cy)*cs);
            float bx=(float)(cx+(a.x2-cx)*cs-(a.y2-cy)*sn);
            float by=(float)(cy+(a.x2-cx)*sn+(a.y2-cy)*cs);
            float[] av=sourceToView(ax,ay), bv=sourceToView(bx,by);
            float hit=dp(30);
            if (Math.hypot(x-av[0],y-av[1])<=hit) return 11;
            if (Math.hypot(x-bv[0],y-bv[1])<=hit) return 12;
            return 0;
        }

        private float[] snapJumperPoint(float sx, float sy, Annotation ignore) {
            float threshold = dp(24) / Math.max(0.25f, frozenMatrix.mapRadius(1f));
            float best = threshold * threshold;
            float bx = sx, by = sy; boolean found = false;
            for (Annotation o : items) {
                if (o == null || o == ignore || o.type == AnnotationMode.PEN) continue;
                float[][] c = {{o.x1,o.y1},{o.x2,o.y2},{o.x1,o.y2},{o.x2,o.y1}};
                if (o.type == AnnotationMode.MARKER || o.type == AnnotationMode.TEXT) c = new float[][]{{o.x1,o.y1}};
                for (float[] pt : c) {
                    float dx=sx-pt[0], dy=sy-pt[1], d2=dx*dx+dy*dy;
                    if (d2 <= best) { best=d2; bx=pt[0]; by=pt[1]; found=true; }
                }
            }
            jumperSnapActive=found; jumperSnapX=bx; jumperSnapY=by;
            return new float[]{bx,by};
        }

        private float[] rotatePoint(float x,float y,float cx,float cy,float degrees) {
            double rad=Math.toRadians(degrees), cs=Math.cos(rad), sn=Math.sin(rad);
            float dx=x-cx, dy=y-cy;
            return new float[]{(float)(cx+dx*cs-dy*sn),(float)(cy+dx*sn+dy*cs)};
        }

        private void moveJumperEndpointFromView(Annotation a, int handle, float vx, float vy) {
            if (a==null || a.type!=AnnotationMode.JUMPER) return;
            float[] desired=viewToSource(vx,vy);
            float cx=(a.x1+a.x2)/2f, cy=(a.y1+a.y2)/2f;
            float[] aw=rotatePoint(a.x1,a.y1,cx,cy,a.rotation);
            float[] bw=rotatePoint(a.x2,a.y2,cx,cy,a.rotation);
            if(handle==11) aw=snapJumperPoint(desired[0],desired[1],a);
            else if(handle==12) bw=snapJumperPoint(desired[0],desired[1],a);
            else { jumperSnapActive=false; return; }
            float ncx=(aw[0]+bw[0])/2f, ncy=(aw[1]+bw[1])/2f;
            float[] al=rotatePoint(aw[0],aw[1],ncx,ncy,-a.rotation);
            float[] bl=rotatePoint(bw[0],bw[1],ncx,ncy,-a.rotation);
            a.x1=al[0]; a.y1=al[1]; a.x2=bl[0]; a.y2=bl[1];
        }

        private int hitSelectionHandle(float x, float y, Annotation a) {
            float[] box=annotationViewBounds(a);
            float l=box[0], t=box[1], r=box[2], b=box[3];
            // The Select UI displays only four corner handles. Keep the touch
            // map consistent with what is visible; invisible side handles made
            // the object feel as if it resized from an empty area.
            float hs=dp(26);
            float[][] pts={{l,t},{r,t},{r,b},{l,b}};
            int[] handleIds={1,3,5,7};
            for(int i=0;i<pts.length;i++) if(Math.hypot(x-pts[i][0],y-pts[i][1])<=hs) return handleIds[i];
            float rx=(l+r)/2f, ry=t-dp(28);
            if(Math.hypot(x-rx,y-ry)<=dp(22)) return 10;
            return 0;
        }

        private void scaleAnnotationFromStart(Annotation a, Annotation base, float sx, float sy) {
            if(base==null || a==null) return;
            float cx,cy;
            if(base.type==AnnotationMode.PEN && base.points!=null && !base.points.isEmpty()) {
                float minX=base.points.get(0).x,maxX=minX,minY=base.points.get(0).y,maxY=minY;
                for(PointF pt:base.points){minX=Math.min(minX,pt.x);maxX=Math.max(maxX,pt.x);minY=Math.min(minY,pt.y);maxY=Math.max(maxY,pt.y);}
                cx=(minX+maxX)/2f; cy=(minY+maxY)/2f;
                if(a.points==null) a.points=new ArrayList<>(); else a.points.clear();
                for(PointF pt:base.points) a.points.add(new PointF(cx+(pt.x-cx)*sx,cy+(pt.y-cy)*sy));
            } else {
                cx=(Math.min(base.x1,base.x2)+Math.max(base.x1,base.x2))/2f;
                cy=(Math.min(base.y1,base.y2)+Math.max(base.y1,base.y2))/2f;
                a.x1=cx+(base.x1-cx)*sx; a.x2=cx+(base.x2-cx)*sx;
                a.y1=cy+(base.y1-cy)*sy; a.y2=cy+(base.y2-cy)*sy;
            }
            a.rotation=base.rotation;
            a.size=base.size*Math.max(0.25f,Math.min(4f,(Math.abs(sx)+Math.abs(sy))/2f));
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (!frozen || mode==AnnotationMode.NONE) return false;
            float x=e.getX(), y=e.getY();
            if (mode == AnnotationMode.JUMPER) {
                jumperScaleDetector.onTouchEvent(e);
                if (jumperPinching || e.getPointerCount() > 1) {
                    if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) jumperPinching = false;
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    startX=x; startY=y; endX=x; endY=y; drawing=true; invalidate(); return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE && drawing) {
                    endX=x; endY=y; invalidate(); return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_UP && drawing) {
                    endX=x; endY=y;
                    float[] a=viewToSource(startX,startY), b=viewToSource(endX,endY);
                    float distance=(float)Math.hypot(b[0]-a[0], b[1]-a[1]);
                    if (distance >= dp(8)) {
                        pushUndo();
                        items.add(Annotation.shape(AnnotationMode.JUMPER,a[0],a[1],b[0],b[1],currentColor,Math.max(4f,strokeDp)));
                    }
                    drawing=false; invalidate(); return true;
                }
                return true;
            }
            if (mode==AnnotationMode.SELECT) {
                int action=e.getActionMasked();

                // Multi-touch gets priority over single-handle transforms so a two-finger
                // rotate/scale gesture can always take over cleanly, even if the first
                // finger started on a resize/rotation handle.
                if (selected!=null && e.getPointerCount()>=2 &&
                        (action==MotionEvent.ACTION_POINTER_DOWN || action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_POINTER_UP)) {
                    if (!transformingSelected) {
                        pushUndo();
                        transformingSelected=true;
                        rotatingSelected=true;
                        selectedHandle=0;
                        float x0=e.getX(0), y0=e.getY(0), x1=e.getX(1), y1=e.getY(1);
                        lastRotateAngle=(float)Math.toDegrees(Math.atan2(y1-y0,x1-x0));
                        lastTransformDistance=(float)Math.hypot(x1-x0,y1-y0);
                    } else if (action==MotionEvent.ACTION_MOVE) {
                        float x0=e.getX(0), y0=e.getY(0), x1=e.getX(1), y1=e.getY(1);
                        float angle=(float)Math.toDegrees(Math.atan2(y1-y0,x1-x0));
                        float dist=(float)Math.hypot(x1-x0,y1-y0);
                        float delta=angle-lastRotateAngle;
                        while(delta>180f) delta-=360f;
                        while(delta<-180f) delta+=360f;
                        selected.rotation=normalizeAngle(selected.rotation+delta);
                        float factor=dist/Math.max(1f,lastTransformDistance);
                        if (Math.abs(factor-1f)>0.003f) {
                            selected.size=Math.max(2f,Math.min(20f,selected.size*factor));
                            lastTransformDistance=dist;
                        }
                        lastRotateAngle=angle;
                        invalidate();
                    }
                    if(action==MotionEvent.ACTION_POINTER_UP) {
                        rotatingSelected=false;
                        transformingSelected=false;
                        selectedHandle=0;
                        transformStart=null;
                    }
                    return true;
                }

                if (action==MotionEvent.ACTION_DOWN) {
                    // If the current object is already selected, give its handles priority.
                    if(selected!=null) {
                        int h = hitJumperEndpointHandle(x,y,selected);
                        if (h==0) h=hitSelectionHandle(x,y,selected);
                        if(h!=0) {
                            selectedHandle=h;
                            // A handle transform is one undoable editor action.
                            // V3.10.7 marked the history as pushed here but did not
                            // actually save a snapshot, so resize/rotate could not be
                            // undone reliably.
                            pushUndo();
                            moveHistoryPushed=true;
                            transformStart=selected.copy();
                            float[] sp=viewToSource(x,y);
                            float[] box=annotationViewBounds(selected);
                            float cx=(box[0]+box[2])/2f, cy=(box[1]+box[3])/2f;
                            float[] cp=viewToSource(cx,cy);
                            transformStartAngle=(float)Math.toDegrees(Math.atan2(sp[1]-cp[1],sp[0]-cp[0]));
                            transformStartDistance=(float)Math.hypot(sp[0]-cp[0],sp[1]-cp[1]);
                            rotatingSelected=(h==10);
                            transformingSelected=(h!=10);
                            if (h==11 || h==12) {
                                transformingSelected=true;
                                rotatingSelected=false;
                            }
                            return true;
                        }
                    }
                    selected=hitTest(x,y);
                    selectedHandle=0;
                    moveHistoryPushed=false;
                    selectDownX=x; selectDownY=y;
                    lastSelectX=x; lastSelectY=y;
                    selectMoved=false;
                    rotatingSelected=false;
                    transformingSelected=false;
                    lastRotateAngle=0f;
                    invalidate();
                    return true;
                }
                if(selected!=null && selectedHandle!=0 && action==MotionEvent.ACTION_MOVE) {
                    if(transformStart==null) transformStart=selected.copy();
                    float[] cur=viewToSource(x,y);
                    float[] box=annotationViewBounds(transformStart);
                    float[] csrc=viewToSource((box[0]+box[2])/2f,(box[1]+box[3])/2f);
                    if (selectedHandle==11 || selectedHandle==12) {
                        moveJumperEndpointFromView(selected, selectedHandle, x, y);
                    } else if(selectedHandle==10) {
                        float ang=(float)Math.toDegrees(Math.atan2(cur[1]-csrc[1],cur[0]-csrc[0]));
                        selected.rotation=normalizeAngle(transformStart.rotation+(ang-transformStartAngle));
                    } else {
                        // Corner handles resize from the finger direction instead of using only
                        // radial distance from the center. This makes horizontal/vertical
                        // finger movement feel natural while keeping the four-corner UI.
                        float startDx=0f, startDy=0f, curDx=0f, curDy=0f;
                        float[] startView=annotationViewBounds(transformStart);
                        float sx0=(startView[0]+startView[2])/2f, sy0=(startView[1]+startView[3])/2f;
                        if(selectedHandle==1){ startDx=startView[0]-sx0; startDy=startView[1]-sy0; }
                        else if(selectedHandle==3){ startDx=startView[2]-sx0; startDy=startView[1]-sy0; }
                        else if(selectedHandle==5){ startDx=startView[2]-sx0; startDy=startView[3]-sy0; }
                        else { startDx=startView[0]-sx0; startDy=startView[3]-sy0; }
                        curDx=(x-sx0); curDy=(y-sy0);
                        float ratioX=Math.abs(curDx)/Math.max(1f,Math.abs(startDx));
                        float ratioY=Math.abs(curDy)/Math.max(1f,Math.abs(startDy));
                        float uniform=Math.max(0.12f,Math.min(4f,Math.max(ratioX,ratioY)));
                        float sx=uniform, sy=uniform;
                        scaleAnnotationFromStart(selected,transformStart,sx,sy);
                    }
                    invalidate(); return true;
                }
                if(action==MotionEvent.ACTION_MOVE && selected!=null && !rotatingSelected) {
                    float totalMove=(float)Math.hypot(x-selectDownX,y-selectDownY);
                    // Ignore tiny finger jitter so a simple tap stays a tap.
                    if (!selectMoved && totalMove < dp(10)) return true;
                    selectMoved=true;
                    if (!moveHistoryPushed) { pushUndo(); moveHistoryPushed=true; }
                    float[] p1=viewToSource(lastSelectX,lastSelectY), p2=viewToSource(x,y);
                    float dx=p2[0]-p1[0], dy=p2[1]-p1[1];
                    if (selected.type==AnnotationMode.PEN && selected.points!=null) {
                        for(PointF pt:selected.points){ pt.x+=dx; pt.y+=dy; }
                    } else {
                        selected.x1+=dx; selected.y1+=dy; selected.x2+=dx; selected.y2+=dy;
                    }
                    lastSelectX=x; lastSelectY=y;
                    invalidate();
                    return true;
                }
                if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
                    jumperSnapActive=false;
                    // A simple tap on a text object opens the editor directly.
                    // A real drag must never accidentally open the text editor.
                    boolean simpleTap = !selectMoved && Math.hypot(x-selectDownX, y-selectDownY) < dp(10);
                    rotatingSelected=false;
                    transformingSelected=false;
                    selectedHandle=0;
                    transformStart=null;
                    lastSelectX=x; lastSelectY=y;
                    selectMoved=false;
                    invalidate();
                    if (action==MotionEvent.ACTION_UP && simpleTap && selected != null && selected.type==AnnotationMode.TEXT) {
                        editSelectedText();
                    }
                    return true;
                }
                return true;
            }
        if (mode==AnnotationMode.TEXT && e.getActionMasked()==MotionEvent.ACTION_UP) { showTextInput(x,y); return true; }
            if (mode==AnnotationMode.MARKER && e.getActionMasked()==MotionEvent.ACTION_UP) { float[] p=viewToSource(x,y); pushUndo(); items.add(Annotation.marker(p[0],p[1],currentColor,strokeDp)); invalidate(); return true; }
            if (mode==AnnotationMode.CROP) {
                float l=Math.min(cropStartX,cropEndX), t=Math.min(cropStartY,cropEndY);
                float r=Math.max(cropStartX,cropEndX), b=Math.max(cropStartY,cropEndY);
                float handle=dp(28);
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        cropLastX=x; cropLastY=y;
                        if (Math.hypot(x-l,y-t)<=handle) cropDragMode=2;
                        else if (Math.hypot(x-r,y-t)<=handle) cropDragMode=3;
                        else if (Math.hypot(x-l,y-b)<=handle) cropDragMode=4;
                        else if (Math.hypot(x-r,y-b)<=handle) cropDragMode=5;
                        else if (Math.abs(y-t)<=handle && x>=l && x<=r) cropDragMode=6;
                        else if (Math.abs(x-r)<=handle && y>=t && y<=b) cropDragMode=7;
                        else if (Math.abs(y-b)<=handle && x>=l && x<=r) cropDragMode=8;
                        else if (Math.abs(x-l)<=handle && y>=t && y<=b) cropDragMode=9;
                        else if (x>=l && x<=r && y>=t && y<=b) cropDragMode=1;
                        else cropDragMode=0;
                        cropSelecting=true; invalidate(); return true;
                    case MotionEvent.ACTION_MOVE:
                        if(!cropSelecting || cropDragMode==0) return true;
                        float dx=x-cropLastX, dy=y-cropLastY;
                        float nl=l, nt=t, nr=r, nb=b;
                        if(cropDragMode==1){ nl+=dx; nr+=dx; nt+=dy; nb+=dy; }
                        else if(cropDragMode==2){ nl+=dx; nt+=dy; }
                        else if(cropDragMode==3){ nr+=dx; nt+=dy; }
                        else if(cropDragMode==4){ nl+=dx; nb+=dy; }
                        else if(cropDragMode==5){ nr+=dx; nb+=dy; }
                        else if(cropDragMode==6){ nt+=dy; }
                        else if(cropDragMode==7){ nr+=dx; }
                        else if(cropDragMode==8){ nb+=dy; }
                        else if(cropDragMode==9){ nl+=dx; }
                        float min=dp(50);
                        if(nr-nl<min){ if(cropDragMode==2||cropDragMode==4) nl=nr-min; else nr=nl+min; }
                        if(nb-nt<min){ if(cropDragMode==2||cropDragMode==3) nt=nb-min; else nb=nt+min; }
                        float maxW=getWidth(), maxH=getHeight();
                        if(cropDragMode==1){
                            if(nl<0){nr-=nl;nl=0;} if(nr>maxW){nl-=nr-maxW;nr=maxW;}
                            if(nt<0){nb-=nt;nt=0;} if(nb>maxH){nt-=nb-maxH;nb=maxH;}
                        } else {
                            nl=Math.max(0,nl); nt=Math.max(0,nt); nr=Math.min(maxW,nr); nb=Math.min(maxH,nb);
                            if(nr-nl<min){ if(cropDragMode==2||cropDragMode==4||cropDragMode==9) nl=Math.max(0,nr-min); else nr=Math.min(maxW,nl+min); }
                            if(nb-nt<min){ if(cropDragMode==2||cropDragMode==3||cropDragMode==6) nt=Math.max(0,nb-min); else nb=Math.min(maxH,nt+min); }
                        }
                        cropStartX=nl; cropStartY=nt; cropEndX=nr; cropEndY=nb;
                        cropLastX=x; cropLastY=y; invalidate(); return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cropDragMode=0; invalidate(); return true;
                }
                return true;
            }
            if (mode==AnnotationMode.PEN) {
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        drawing=true; startX=x; startY=y; endX=x; endY=y;
                        pushUndo();
                        Annotation penStroke=Annotation.pen(currentColor,strokeDp);
                        float[] first=viewToSource(x,y); penStroke.points.add(new PointF(first[0],first[1]));
                        items.add(penStroke);
                        invalidate(); return true;
                    case MotionEvent.ACTION_MOVE:
                        if (drawing && !items.isEmpty()) {
                            Annotation currentPenStroke=items.get(items.size()-1);
                            float[] q=viewToSource(x,y);
                            currentPenStroke.points.add(new PointF(q[0],q[1]));
                            invalidate();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        drawing=false; invalidate(); return true;
                }
                return true;
            }
            switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: startX=x;startY=y;endX=x;endY=y;drawing=true;return true;
                case MotionEvent.ACTION_MOVE: endX=x;endY=y;invalidate();return true;
                case MotionEvent.ACTION_UP:
                    endX=x;endY=y; if(drawing){float[] a=viewToSource(startX,startY),b=viewToSource(endX,endY);pushUndo(); items.add(Annotation.shape(mode,a[0],a[1],b[0],b[1],currentColor,strokeDp));} drawing=false;invalidate();return true;
            }
            return true;
        }
    }

    private static class Annotation {
        AnnotationMode type; float x1,y1,x2,y2; String text; int color=Color.RED; float size=5f; float rotation=0f; boolean bold=false; boolean textBackground=false; ArrayList<PointF> points;
        static Annotation marker(float x,float y,int c,float s){return shape(AnnotationMode.MARKER,x,y,x,y,c,s);}
        static Annotation pen(int c,float s){ Annotation a=new Annotation(); a.type=AnnotationMode.PEN; a.color=c; a.size=s; a.points=new ArrayList<>(); return a; }
        static Annotation text(float x,float y,String t,int c,float s){Annotation a=marker(x,y,c,s);a.type=AnnotationMode.TEXT;a.text=t;return a;}
        static Annotation ocr(float x1,float y1,float x2,float y2,String t,int c,float s){Annotation a=shape(AnnotationMode.OCR,x1,y1,x2,y2,c,s);a.text=t;return a;}
        static Annotation shape(AnnotationMode m,float x1,float y1,float x2,float y2,int c,float s){Annotation a=new Annotation();a.type=m;a.x1=x1;a.y1=y1;a.x2=x2;a.y2=y2;a.color=c;a.size=s;return a;}
        Annotation copy(){ Annotation a=new Annotation(); a.type=type; a.x1=x1;a.y1=y1;a.x2=x2;a.y2=y2;a.text=text;a.color=color;a.size=size;a.rotation=rotation;a.bold=bold;a.textBackground=textBackground; if(points!=null){a.points=new ArrayList<>(); for(PointF p:points)a.points.add(new PointF(p.x,p.y));} return a; }
    }

    private void snapBottomSheet() {
        if (bottomSheetHost == null) return;

        int current = bottomSheetHost.getLayoutParams().height;
        int low = dp(BOTTOM_SHEET_MIN_DP) + navigationBarBottomInset;
        int mid = dp(BOTTOM_SHEET_MID_DP) + navigationBarBottomInset;
        int high = dp(BOTTOM_SHEET_MAX_DP) + navigationBarBottomInset;

        int target = low;
        int dLow = Math.abs(current - low);
        int dMid = Math.abs(current - mid);
        int dHigh = Math.abs(current - high);
        if (dMid < dLow) target = mid;
        if (dHigh < Math.min(dLow, dMid)) target = high;

        ValueAnimator animator = ValueAnimator.ofInt(current, target);
        animator.setDuration(220);
        animator.addUpdateListener(a -> {
            if (bottomSheetHost == null) return;
            ViewGroup.LayoutParams lp = bottomSheetHost.getLayoutParams();
            lp.height = (Integer) a.getAnimatedValue();
            bottomSheetHost.setLayoutParams(lp);
        });
        animator.start();

        if (bottomSheetHandle != null) {
            bottomSheetHandle.setText(target == high ? "⌄" : (target == low ? "⌃" : "━"));
        }
        bottomSheetMoved = false;
    }

    private void setLivePanelVisible(boolean visible) {
        livePanelVisible = visible;
        if (frozen) visible = true;

        int v = visible ? View.VISIBLE : View.GONE;

        // LIVE auto-hide: ONLY the top/header and adjustment panels are hidden.
        // The bottom action bar is intentionally ALWAYS visible in LIVE so the
        // technician can reach Lampu, Beku, Foto, Fokus, Grid and +/- at any time.
        if (topBar != null) topBar.setVisibility(v);
        if (infoRow != null) infoRow.setVisibility(v);
        if (zoomBar != null) zoomBar.setVisibility(v);
        if (exposureRow != null) exposureRow.setVisibility(v);
        if (detailRow != null) detailRow.setVisibility(v);

        if (bottomSheetHost != null) bottomSheetHost.setVisibility(View.VISIBLE);
        if (controlsBar != null) controlsBar.setVisibility(View.VISIBLE);
        if (cameraMinusBtn != null) cameraMinusBtn.setVisibility(View.VISIBLE);
        if (freezeBtn != null) freezeBtn.setVisibility(View.VISIBLE);
        if (overlayBtn != null) overlayBtn.setVisibility(View.VISIBLE);
        if (cameraPlusBtn != null) cameraPlusBtn.setVisibility(View.VISIBLE);
        if (torchBtn != null) torchBtn.setVisibility(View.VISIBLE);
        if (photoBtn != null) photoBtn.setVisibility(View.VISIBLE);
        if (cameraFocusBtn != null) cameraFocusBtn.setVisibility(View.VISIBLE);
    }

    private void scheduleLivePanelHide() {
        livePanelHandler.removeCallbacks(hideLivePanelRunnable);
        if (!frozen) livePanelHandler.postDelayed(hideLivePanelRunnable, 1800);
    }

    private void showLivePanelTemporarily() {
        if (frozen) return;
        setLivePanelVisible(true);
        scheduleLivePanelHide();
    }

    private void applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (topBar != null) {
                topBar.setPadding(dp(8), bars.top + dp(3), dp(8), dp(3));
            }

            navigationBarBottomInset = bars.bottom;
            updateControlsBarPadding();

            return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(android.R.id.content));

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
    }

    private void updateControlsBarPadding() {
        if (controlsBar != null) {
            // Keep the bottom camera/edit controls above Android's navigation bar
            // in BOTH LIVE and FREEZE modes. setFreezeFullscreen() must not reset this.
            int bottomPad = navigationBarBottomInset + dp(6);
            controlsBar.setPadding(dp(4), dp(2), dp(4), bottomPad);

            // The sheet content must grow with the system navigation inset so its
            // buttons are never clipped when the phone uses a 3-button navigation bar.
            ViewGroup.LayoutParams cp = controlsBar.getLayoutParams();
            if (cp != null) {
                cp.height = dp(72) + navigationBarBottomInset;
                controlsBar.setLayoutParams(cp);
            }

            if (bottomSheetHost != null && !bottomSheetMoved) {
                ViewGroup.LayoutParams hp = bottomSheetHost.getLayoutParams();
                if (hp != null) {
                    int minimum = dp(BOTTOM_SHEET_MIN_DP) + navigationBarBottomInset;
                    int start = dp(BOTTOM_SHEET_START_DP) + navigationBarBottomInset;
                    if (hp.height < minimum) hp.height = minimum;
                    if (hp.height < start) hp.height = start;
                    bottomSheetHost.setLayoutParams(hp);
                }
            }
        }
    }

    private void stopCameraForDatabase() {
        try {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        } catch (Exception ignored) {}
        camera = null;
        if (preview != null) preview.setVisibility(View.VISIBLE);
    }

    private void startCamera() {
        status.setText("Menyiapkan kamera...");

        final ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                cameraProvider = provider;

                Preview.Builder previewBuilder = new Preview.Builder();
                Camera2Interop.Extender<Preview> previewExtender = new Camera2Interop.Extender<>(previewBuilder);
                applyLiveDetailRequest(previewExtender);
                // Keep the rear camera in continuous autofocus so the microscope
                // preview can settle on small chip markings/text naturally.
                try {
                    previewExtender.setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    );
                } catch (Throwable ignored) {}
                Preview previewUseCase = previewBuilder.build();

                        ImageCapture.Builder captureBuilder = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setJpegQuality(98);
                Camera2Interop.Extender<ImageCapture> extender = new Camera2Interop.Extender<>(captureBuilder);
                extender.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
                extender.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
                capture = captureBuilder.build();

                provider.unbindAll();

                camera = provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        previewUseCase,
                        capture
                );

                previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());
                updateZoomText();
                android.util.Range<Integer> expRange = camera.getCameraInfo().getExposureState().getExposureCompensationRange();
                exposureLower = expRange.getLower();
                exposureUpper = expRange.getUpper();
                int expSpan = Math.max(1, exposureUpper - exposureLower);
                exposureBar.setMax(expSpan);
                // Restore the user's previous zoom/exposure after a detail-mode restart.
                android.util.Range<Float> zoomRange = camera.getCameraInfo().getZoomState().getValue() != null
                        ? new android.util.Range<>(1f, camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio())
                        : new android.util.Range<>(1f, 1f);
                float restoreZoom = Math.max(zoomRange.getLower(), Math.min(zoomRange.getUpper(), lastLiveZoom));
                camera.getCameraControl().setZoomRatio(restoreZoom);
                lastLiveZoom = restoreZoom;
                setExposure(lastLiveExposure);
                status.setText(detailOn ? "Kamera LIVE • Ultra Detail ON" : "Kamera LIVE • Detail normal");
                setLivePanelVisible(true);
                scheduleLivePanelHide();

            } catch (Exception e) {
                status.setText("Kamera gagal");
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void applyLiveDetailRequest(Camera2Interop.Extender<Preview> extender) {
        try {
            int edge = detailOn ? CaptureRequest.EDGE_MODE_HIGH_QUALITY : CaptureRequest.EDGE_MODE_FAST;
            int nr = detailOn ? CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY : CaptureRequest.NOISE_REDUCTION_MODE_FAST;
            extender.setCaptureRequestOption(CaptureRequest.EDGE_MODE, edge);
            extender.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, nr);
        } catch (Throwable ignored) {}
    }

    private void restartLivePreview() {
        if (frozen || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
        startCamera();
    }

    private void updateZoomText() {
        if (camera == null) return;
        if (camera.getCameraInfo().getZoomState().getValue() == null) return;

        float current = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
        float max = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();

        zoomText.setText(String.format("Zoom %.1f×  (maks %.1f×)", current, max));
    }

    private void setZoomFromProgress(int progress) {
        if (camera == null) return;
        if (camera.getCameraInfo().getZoomState().getValue() == null) return;

        float max = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
        if (max <= 1f) return;

        float ratio = 1f + (max - 1f) * progress / 100f;
        camera.getCameraControl().setZoomRatio(ratio);
        lastLiveZoom = ratio;
        updateZoomText();
    }

    private void changeZoom(float delta) {
        if (camera == null) return;
        if (camera.getCameraInfo().getZoomState().getValue() == null) return;

        float current = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
        float max = camera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
        float newZoom = Math.max(1f, Math.min(max, current + delta));

        camera.getCameraControl().setZoomRatio(newZoom);
        lastLiveZoom = newZoom;

        if (max > 1f) {
            zoomBar.setProgress((int) ((newZoom - 1f) / (max - 1f) * 100f));
        }

        updateZoomText();
    }


    private void setExposure(int value) {
        if (camera == null) return;
        android.util.Range<Integer> range = camera.getCameraInfo().getExposureState().getExposureCompensationRange();
        exposureLower = range.getLower();
        exposureUpper = range.getUpper();
        int clamped = Math.max(exposureLower, Math.min(exposureUpper, value));
        camera.getCameraControl().setExposureCompensationIndex(clamped);
        lastLiveExposure = clamped;
        if (exposureBar != null) {
            exposureBar.setMax(Math.max(1, exposureUpper - exposureLower));
            exposureBar.setProgress(clamped - exposureLower);
        }
        exposureText.setText(clamped == 0 ? "Exposure 0 • NORMAL" : String.format("Exposure %+d", clamped));
    }

    private void setFreezeFullscreen(boolean freezeMode) {
        livePanelHandler.removeCallbacks(hideLivePanelRunnable);
        if (freezeMode) setLivePanelVisible(true);
        if (topBar != null) topBar.setVisibility(freezeMode ? View.GONE : View.VISIBLE);
        if (status != null) status.setVisibility(freezeMode ? View.GONE : View.VISIBLE);

        // In Freeze mode, give almost the entire screen to the frozen PCB image.
        // Only the annotation toolbar and a single LIVE return button remain.
        if (cameraMinusBtn != null) cameraMinusBtn.setVisibility(freezeMode ? View.GONE : View.VISIBLE);
        if (torchBtn != null) torchBtn.setVisibility(freezeMode ? View.GONE : View.VISIBLE);
        if (photoBtn != null) photoBtn.setVisibility(freezeMode ? View.GONE : View.VISIBLE);
        if (cameraFocusBtn != null) cameraFocusBtn.setVisibility(freezeMode ? View.GONE : View.VISIBLE);
        if (cameraPlusBtn != null) cameraPlusBtn.setVisibility(freezeMode ? View.GONE : View.VISIBLE);
        if (overlayBtn != null) overlayBtn.setVisibility(freezeMode ? View.GONE : View.VISIBLE);

        if (freezeBtn != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) freezeBtn.getLayoutParams();
            if (freezeMode) {
                lp.width = 0;
                lp.weight = 1f;
                lp.height = dp(56);
                freezeBtn.setText("▶️  LIVE");
            } else {
                lp.width = 0;
                lp.weight = 1f;
                lp.height = dp(56);
                freezeBtn.setText("❄️\nBeku");
            }
            freezeBtn.setLayoutParams(lp);
        }
        updateControlsBarPadding();
        if (!freezeMode) scheduleLivePanelHide();
    }

    private void toggleFreeze() {
        if (preview == null) return;

        if (!frozen) {
            Bitmap bitmap = preview.getBitmap();
            if (bitmap == null) {
                status.setText("Frame belum siap");
                return;
            }

            frozenBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            freezeView = new ImageView(this);
            freezeView.setImageBitmap(frozenBitmap);
            freezeView.setScaleType(ImageView.ScaleType.MATRIX);
            freezeView.setBackgroundColor(Color.BLACK);
            freezeView.setOnTouchListener((v, event) -> {
                if (!frozen || annotationMode != AnnotationMode.NONE) return false;
                frozenScaleDetector.onTouchEvent(event);
                if (event.getPointerCount() == 1) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN: lastTouchX=event.getX(); lastTouchY=event.getY(); return true;
                        case MotionEvent.ACTION_MOVE: frozenPanX += event.getX()-lastTouchX; frozenPanY += event.getY()-lastTouchY; lastTouchX=event.getX(); lastTouchY=event.getY(); updateFrozenImage(); return true;
                    }
                }
                return true;
            });
            FrameLayout parent = (FrameLayout) preview.getParent();
            parent.addView(freezeView, new FrameLayout.LayoutParams(-1, -1));
            frozen = true;
            frozenZoom = 1f;
            frozenPanX = 0f;
            frozenPanY = 0f;
            zoomBar.setProgress(0);
            updateFrozenImage();
            showAnnotationTools();
            setFreezeFullscreen(true);
            status.setText("❄️ BEKU • pilih alat untuk menandai PCB");
        } else {
            hideAnnotationTools();
            frozenBitmap = null;
            if (freezeView != null) {
                ViewParent parent = freezeView.getParent();
                if (parent instanceof FrameLayout) {
                    ((FrameLayout) parent).removeView(freezeView);
                }
                freezeView = null;
            }
            frozen = false;
            frozenZoom = 1f;
            frozenPanX = 0f;
            frozenPanY = 0f;
            setFreezeFullscreen(false);
            updateZoomText();
            status.setText("Kamera LIVE");
        }
    }

    private void setFrozenZoomFromProgress(int progress) {
        if (!frozen) return;
        frozenZoom = 1f + (7f * progress / 100f);
        updateFrozenImage();
    }

    private void changeFrozenZoom(float delta) {
        if (!frozen) return;
        frozenZoom = Math.max(1f, Math.min(8f, frozenZoom + delta));
        zoomBar.setProgress((int) (((frozenZoom - 1f) / 7f) * 100f));
        updateFrozenImage();
    }

    private void updateFrozenImage() {
        if (!frozen || freezeView == null || frozenBitmap == null) return;
        int vw = freezeView.getWidth();
        int vh = freezeView.getHeight();
        if (vw <= 0 || vh <= 0) return;

        float base = Math.max((float) vw / frozenBitmap.getWidth(),
                (float) vh / frozenBitmap.getHeight());
        float scale = base * frozenZoom;
        float drawW = frozenBitmap.getWidth() * scale;
        float drawH = frozenBitmap.getHeight() * scale;
        float centerX = (vw - drawW) / 2f + frozenPanX;
        float centerY = (vh - drawH) / 2f + frozenPanY;

        // Limit panning so the image never leaves empty gaps.
        float minX = vw - drawW;
        float minY = vh - drawH;
        float maxX = 0f;
        if (drawW <= vw) centerX = (vw - drawW) / 2f;
        else centerX = Math.max(minX, Math.min(maxX, centerX));
        if (drawH <= vh) centerY = (vh - drawH) / 2f;
        else centerY = Math.max(minY, Math.min(0f, centerY));

        frozenMatrix.reset();
        frozenMatrix.setScale(scale, scale);
        frozenMatrix.postTranslate(centerX, centerY);
        freezeView.setImageMatrix(frozenMatrix);
        // Annotations live in source-image coordinates and share this exact matrix.
        // Repaint immediately whenever zoom/pan changes so every mark follows the PCB.
        if (annotationView != null) annotationView.invalidate();

        zoomText.setText(String.format("Freeze Zoom %.1f×", frozenZoom));
        status.setText(String.format("❄️ BEKU • %.1f× • geser untuk melihat area", frozenZoom));
    }

    private Bitmap getFrozenZoomedBitmap() {
        CropInfo crop = getCurrentCropInfo();
        if (crop == null) return null;
        return Bitmap.createBitmap(frozenBitmap, crop.left, crop.top, crop.width, crop.height);
    }

    private void saveBitmap(Bitmap bitmap) {
        Uri uri = saveBitmapToGallery(bitmap);
        if (uri != null) status.setText("Foto tersimpan • Deroid Unbricker Nota");
    }

    private Uri saveBitmapToGallery(Bitmap bitmap) {
        if (bitmap == null) return null;
        try {
            String name = "DeroidUnbrickerNota_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DeroidUnbrickerNota/Microscope");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) { status.setText("Gagal menyimpan foto"); return null; }
            try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    status.setText("Gagal menyimpan foto"); return null;
                }
            }
            return uri;
        } catch (Exception e) {
            status.setText("Gagal menyimpan foto"); e.printStackTrace(); return null;
        }
    }

    private void toggleTorch() {
        if (camera == null) {
            status.setText("Kamera belum siap");
            return;
        }

        if (!camera.getCameraInfo().hasFlashUnit()) {
            status.setText("Flash tidak tersedia");
            return;
        }

        torch = !torch;
        camera.getCameraControl().enableTorch(torch);
        torchBtn.setText(torch ? "🔦\nLampu ON" : "🔦\nLampu");
    }

    private void focusDatabaseTextArea() {
        if (preview == null || camera == null) return;
        if (preview.getWidth() <= 0 || preview.getHeight() <= 0) return;
        // The eMMC marking is normally near the visual center. Meter a compact
        // point there so autofocus targets the chip marking instead of the
        // background/board edges. Repeat briefly because the lens may still be
        // settling immediately after the Database panel appears.
        final float x = preview.getWidth() * 0.50f;
        final float y = preview.getHeight() * 0.40f;
        focusAt(x, y);
    }

    private void focusAt(float x, float y) {
        if (camera == null) {
            status.setText("Kamera belum siap");
            return;
        }

        x = Math.max(0f, Math.min(preview.getWidth(), x));
        y = Math.max(0f, Math.min(preview.getHeight(), y));
        if (overlayView != null) overlayView.showFocus(x, y);

        MeteringPoint point = preview.getMeteringPointFactory().createPoint(x, y);

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(
                        point,
                        FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE
                ).setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS).build();

        status.setText("Fokus area...");
        camera.getCameraControl().startFocusAndMetering(action)
                .addListener(() -> status.setText("Fokus siap"),
                        ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (frozen && frozenBitmap != null) {
            Bitmap zoomed = getFrozenZoomedBitmap();
            saveBitmap(zoomed != null ? zoomed : frozenBitmap);
            return;
        }

        if (capture == null) {
            status.setText("Kamera belum siap");
            return;
        }

        photoBtn.setEnabled(false);
        photoBtn.setText("📸\nMEMOTRET...");

        ContentValues values = new ContentValues();
        values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "DeroidUnbrickerNota_" + System.currentTimeMillis() + ".jpg"
        );
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/DeroidUnbrickerNota/Microscope"
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
                            @NonNull ImageCapture.OutputFileResults result) {
                        photoBtn.setEnabled(true);
                        photoBtn.setText("📸\nFOTO");
                        status.setText("Foto tersimpan");
                        showPhotoOptions(result.getSavedUri());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        photoBtn.setEnabled(true);
                        photoBtn.setText("📸\nFOTO");
                        status.setText("Foto gagal");
                    }
                }
        );
    }

    private void showPhotoOptions(Uri uri) {
        if (uri == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Foto berhasil disimpan")
                .setMessage("Foto tersimpan di Pictures/DeroidUnbrickerNota/Microscope")
                .setPositiveButton("🔎 Google Lens",
                        (dialog, which) -> sendToGoogleLens(uri))
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void sendToGoogleLens(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage("com.google.android.googlequicksearchbox");

        try {
            startActivity(intent);
        } catch (Exception e) {
            intent.setPackage(null);
            startActivity(Intent.createChooser(intent, "Buka foto dengan"));
        }
    }


    // ========================= eMMC DATABASE V2 =========================
    private static class EmmcRecord {
        String part, brand, capacity, grade, category, notes;
        EmmcRecord(String part, String brand, String capacity, String grade, String category, String notes) {
            this.part = part; this.brand = brand; this.capacity = capacity;
            this.grade = grade; this.category = category; this.notes = notes;
        }
    }

    private ArrayList<EmmcRecord> loadEmmcDatabase() {
        ArrayList<EmmcRecord> rows = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        try (java.io.InputStream in = getAssets().open("emmc_database.csv");
             java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#") || line.toLowerCase(Locale.US).startsWith("part_number,")) continue;
                String[] p = line.split(",", -1);
                if (p.length < 5) continue;
                String part = p[0].trim();
                String key = normalizeEmmc(part);
                if (key.isEmpty()) continue;
                String brand = p.length > 1 ? p[1].trim() : "Tidak dicantumkan";
                String capacity = p.length > 2 ? p[2].trim() : "Tidak dicantumkan";
                String grade = p.length > 3 ? p[3].trim() : "Tidak dicantumkan";
                String category = p.length > 4 ? p[4].trim() : "Tidak dicantumkan";
                String notes = p.length > 5 ? p[5].trim() : "";
                rows.add(new EmmcRecord(part, brand, capacity, grade, category, notes));
            }
        } catch (Exception e) {
            status.setText("EMMC DB • gagal dibaca");
        }
        return rows;
    }

    private GradientDrawable roundedBg(int color, int strokeColor, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (strokeColor != Color.TRANSPARENT) g.setStroke(dp(1), strokeColor);
        return g;
    }

    private Button emmcChip(String text, boolean selected) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setMinHeight(0); b.setMinWidth(0);
        b.setBackground(roundedBg(selected ? Color.rgb(0, 130, 255) : Color.rgb(8, 30, 55),
                Color.rgb(0, 145, 255), 22));
        return b;
    }

    private int emmcCategoryColor(String category) {
        String c = category == null ? "" : category.toUpperCase(Locale.US);
        if (c.startsWith("A+++") || c.startsWith("A++")) return Color.rgb(255, 225, 35);
        if (c.startsWith("A+B") || c.equals("A+")) return Color.rgb(70, 225, 125);
        if (c.startsWith("PILIHAN")) return Color.rgb(70, 225, 125);
        if (c.contains("SAMSUNG") || c.contains("KHUSUS")) return Color.rgb(225, 115, 225);
        return Color.rgb(0, 145, 255);
    }

    private TextView emmcSectionTitle(String title, int count) {
        TextView t = new TextView(this);
        t.setText(title + "   •   " + count + " kode");
        t.setTextColor(Color.BLACK);
        t.setTextSize(14);
        t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(14), 0, dp(10), 0);
        t.setBackground(roundedBg(emmcCategoryColor(title), Color.TRANSPARENT, 10));
        return t;
    }

    private TextView emmcCategoryCard(String title, int count, String capacity) {
        TextView t = new TextView(this);
        String cap = capacity == null || capacity.isEmpty() ? "" : "\nKapasitas: " + capacity;
        t.setText(title + "\n" + count + " kode" + cap);
        t.setTextColor(Color.WHITE);
        t.setTextSize(13);
        t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(14), dp(8), dp(10), dp(8));
        t.setBackground(roundedBg(Color.rgb(6, 34, 61), emmcCategoryColor(title), 10));
        return t;
    }

    private static final String EMMC_USER_PREFS = "emmc_user_records_v1";

    private ArrayList<EmmcRecord> loadUserEmmcDatabase() {
        ArrayList<EmmcRecord> rows = new ArrayList<>();
        try {
            String raw = getSharedPreferences(EMMC_USER_PREFS, MODE_PRIVATE).getString("records", "[]");
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                String part = o.optString("part", "").trim();
                if (normalizeEmmc(part).isEmpty()) continue;
                rows.add(new EmmcRecord(part,
                        o.optString("brand", "Tidak dicantumkan"),
                        o.optString("capacity", "Tidak dicantumkan"),
                        o.optString("grade", "Tidak dicantumkan"),
                        o.optString("category", "Tidak dicantumkan"),
                        o.optString("notes", "")));
            }
        } catch (Exception ignored) {}
        return rows;
    }

    private void saveUserEmmcDatabase(ArrayList<EmmcRecord> rows) {
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (EmmcRecord r : rows) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("part", r.part == null ? "" : r.part);
                o.put("brand", r.brand == null ? "" : r.brand);
                o.put("capacity", r.capacity == null ? "" : r.capacity);
                o.put("grade", r.grade == null ? "" : r.grade);
                o.put("category", r.category == null ? "" : r.category);
                o.put("notes", r.notes == null ? "" : r.notes);
                arr.put(o);
            }
            getSharedPreferences(EMMC_USER_PREFS, MODE_PRIVATE).edit().putString("records", arr.toString()).apply();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan database", Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<EmmcRecord> loadCombinedEmmcDatabase() {
        ArrayList<EmmcRecord> rows = loadEmmcDatabase();
        java.util.LinkedHashMap<String, EmmcRecord> merged = new java.util.LinkedHashMap<>();
        for (EmmcRecord r : rows) merged.put(normalizeEmmc(r.part), r);
        for (EmmcRecord r : loadUserEmmcDatabase()) merged.put(normalizeEmmc(r.part), r);
        return new ArrayList<>(merged.values());
    }

    private void saveOrUpdateUserEmmc(EmmcRecord record) {
        ArrayList<EmmcRecord> rows = loadUserEmmcDatabase();
        String key = normalizeEmmc(record.part);
        boolean updated = false;
        for (int i = 0; i < rows.size(); i++) {
            if (normalizeEmmc(rows.get(i).part).equals(key)) {
                rows.set(i, record);
                updated = true;
                break;
            }
        }
        if (!updated) rows.add(record);
        saveUserEmmcDatabase(rows);
    }

    private EditText emmcFormField(LinearLayout box, String hint, String value) {
        EditText e = new EditText(this);
        e.setSingleLine(false);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.rgb(145, 175, 195));
        e.setTextSize(14);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackground(roundedBg(Color.rgb(7, 36, 65), Color.rgb(0, 110, 145), 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.setMargins(0, dp(4), 0, dp(4));
        box.addView(e, lp);
        return e;
    }

    private void showEmmcEditor(EmmcRecord existing, String suggestedPart, Runnable afterSave) {
        boolean edit = existing != null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6), dp(2), dp(6), dp(2));

        EditText part = emmcFormField(box, "Kode / Part Number eMMC *", edit ? existing.part : suggestedPart);
        EditText brand = emmcFormField(box, "Brand", edit ? existing.brand : "");
        EditText capacity = emmcFormField(box, "Kapasitas", edit ? existing.capacity : "");
        EditText grade = emmcFormField(box, "Grade", edit ? existing.grade : "");
        EditText category = emmcFormField(box, "Kategori", edit ? existing.category : "");
        EditText notes = emmcFormField(box, "Catatan teknisi", edit ? existing.notes : "");
        notes.setMinLines(2);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(edit ? "Edit Data eMMC" : "Tambah Data eMMC")
                .setView(scroll)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Simpan", null)
                .create();
        dialog.setOnShowListener(v -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(x -> {
                String partText = part.getText().toString().trim();
                if (normalizeEmmc(partText).isEmpty()) {
                    part.setError("Kode eMMC wajib diisi");
                    part.requestFocus();
                    return;
                }
                EmmcRecord r = new EmmcRecord(
                        partText,
                        brand.getText().toString().trim().isEmpty() ? "Tidak dicantumkan" : brand.getText().toString().trim(),
                        capacity.getText().toString().trim().isEmpty() ? "Tidak dicantumkan" : capacity.getText().toString().trim(),
                        grade.getText().toString().trim().isEmpty() ? "Tidak dicantumkan" : grade.getText().toString().trim(),
                        category.getText().toString().trim().isEmpty() ? "Tidak dicantumkan" : category.getText().toString().trim(),
                        notes.getText().toString().trim());
                saveOrUpdateUserEmmc(r);
                Toast.makeText(this, edit ? "Data eMMC diperbarui" : "Data eMMC ditambahkan", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (afterSave != null) afterSave.run();
            });
        });
        dialog.show();
    }

    private void showGradeTableImage() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.setBackgroundColor(Color.rgb(5, 14, 22));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("📊 TABEL GRADE eMMC");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button close = makeButton("✕");
        close.setTextSize(16);
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(header);

        TextView info = new TextView(this);
        info.setText("Cubit gambar untuk memperbesar • Cubit kembali untuk mengecilkan");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(11);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 0, 0, dp(6));
        root.addView(info);

        ImageView image = new ImageView(this);
        image.setImageResource(com.deroid.unbrickernota.R.drawable.tabel_grade_emmc);
        image.setScaleType(ImageView.ScaleType.MATRIX);
        root.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));

        final Matrix matrix = new Matrix();
        final float[] values = new float[9];
        final float[] last = new float[2];
        final boolean[] dragging = {false};
        final ScaleGestureDetector[] scaleDetector = new ScaleGestureDetector[1];
        final float[] minScale = {1f};

        image.post(() -> {
            Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.tabel_grade_emmc);
            if (b == null || image.getWidth() <= 0 || image.getHeight() <= 0) return;
            float fit = Math.min((float) image.getWidth() / b.getWidth(), (float) image.getHeight() / b.getHeight());
            minScale[0] = fit;
            matrix.setScale(fit, fit);
            float dx = (image.getWidth() - b.getWidth() * fit) / 2f;
            float dy = (image.getHeight() - b.getHeight() * fit) / 2f;
            matrix.postTranslate(dx, dy);
            image.setImageMatrix(matrix);
        });

        scaleDetector[0] = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
                matrix.getValues(values);
                float current = values[Matrix.MSCALE_X];
                float min = minScale[0];
                if (current < min) {
                    float fix = min / Math.max(0.01f, current);
                    matrix.postScale(fix, fix, detector.getFocusX(), detector.getFocusY());
                } else if (current > min * 5f) {
                    float fix = (min * 5f) / current;
                    matrix.postScale(fix, fix, detector.getFocusX(), detector.getFocusY());
                }
                image.setImageMatrix(matrix);
                return true;
            }
        });

        image.setOnTouchListener((v, event) -> {
            scaleDetector[0].onTouchEvent(event);
            if (event.getPointerCount() == 1) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    last[0] = event.getX(); last[1] = event.getY(); dragging[0] = true;
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && dragging[0]) {
                    matrix.postTranslate(event.getX() - last[0], event.getY() - last[1]);
                    last[0] = event.getX(); last[1] = event.getY();
                    image.setImageMatrix(matrix);
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    dragging[0] = false;
                }
            } else {
                dragging[0] = false;
            }
            return true;
        });

        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setView(root);
        dialog.setOnShowListener(d -> {
            android.view.Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setDimAmount(0.85f);
                w.setLayout(-1, -1);
            }
        });
        dialog.show();
        android.view.Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setDimAmount(0.85f);
            w.setLayout(-1, -1);
        }
    }

    private void showEmmcDatabase() {
        if (emmcDialog != null && emmcDialog.isShowing()) return;

        final ArrayList<EmmcRecord> all = loadCombinedEmmcDatabase();

        // Database memakai layar penuh tanpa kamera. Kamera tidak berguna saat
        // hasil pencarian menutupi preview, jadi hentikan preview sementara.
        stopCameraForDatabase();
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (infoRow != null) infoRow.setVisibility(View.GONE);
        if (zoomBar != null) zoomBar.setVisibility(View.GONE);
        if (exposureRow != null) exposureRow.setVisibility(View.GONE);
        if (detailRow != null) detailRow.setVisibility(View.GONE);
        if (bottomSheetHost != null) bottomSheetHost.setVisibility(View.GONE);
        if (controlsBar != null) controlsBar.setVisibility(View.GONE);
        if (status != null) status.setVisibility(View.GONE);
        if (cameraBox != null) cameraBox.setVisibility(View.GONE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(8), dp(12), dp(8));
        panel.setBackground(roundedBg(Color.rgb(3, 20, 38), Color.rgb(0, 150, 115), 18));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("DATABASE eMMC");
        title.setTextColor(Color.WHITE);
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView count = new TextView(this);
        count.setText(all.size() + " kode");
        count.setTextColor(Color.LTGRAY);
        count.setTextSize(11);
        count.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(count, new LinearLayout.LayoutParams(dp(62), dp(42)));
        Button close = makeButton("✕");
        close.setTextSize(16);
        close.setPadding(0, 0, 0, 0);
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(42)));
        panel.addView(header);

        EditText code = new EditText(this);
        code.setSingleLine(true);
        code.setHint("🔍  Cari kode eMMC...");
        code.setTextColor(Color.WHITE);
        code.setHintTextColor(Color.rgb(145, 175, 195));
        code.setTextSize(15);
        code.setPadding(dp(14), 0, dp(10), 0);
        code.setBackground(roundedBg(Color.rgb(7, 36, 65), Color.rgb(0, 150, 115), 24));
        panel.addView(code, new LinearLayout.LayoutParams(-1, dp(50)));

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams qr = new LinearLayout.LayoutParams(-1, dp(46));
        qr.setMargins(0, dp(6), 0, dp(4));
        Button quizBtn = makeButton("🎯 KUIS");
        quizBtn.setTextSize(12);
        Button learnBtn = makeButton("📚 BELAJAR CEPAT");
        learnBtn.setTextSize(12);
        quickRow.addView(quizBtn, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, dp(44), 1);
        lp2.setMargins(dp(6), 0, 0, 0);
        quickRow.addView(learnBtn, lp2);
        panel.addView(quickRow, qr);

        Button gradeTableBtn = makeButton("📊  TABEL GRADE eMMC");
        gradeTableBtn.setTextSize(13);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(44));
        gp.setMargins(0, 0, 0, dp(4));
        panel.addView(gradeTableBtn, gp);

        Button addBtn = makeButton("＋  Tambah Data");
        addBtn.setTextSize(13);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(44));
        ap.setMargins(0, 0, 0, dp(4));
        panel.addView(addBtn, ap);

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.setFillViewport(true);
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        resultScroll.addView(results);
        panel.addView(resultScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        final Runnable[] renderer = new Runnable[1];
        renderer[0] = () -> {
            ArrayList<EmmcRecord> current = loadCombinedEmmcDatabase();
            count.setText(current.size() + " kode");
            String rawQuery = code.getText().toString().trim();
            String q = normalizeEmmc(rawQuery);
            results.removeAllViews();

            ArrayList<EmmcRecord> matches = new ArrayList<>();
            for (EmmcRecord r : current) {
                if (q.isEmpty()) {
                    matches.add(r);
                } else {
                    String hay = normalizeEmmc(r.part + r.brand + r.capacity + r.grade + r.category + r.notes);
                    if (hay.contains(q)) matches.add(r);
                }
            }

            if (matches.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText(q.isEmpty() ? "Belum ada data." : "Kode tidak ditemukan di database.");
                empty.setTextColor(Color.LTGRAY);
                empty.setTextSize(13);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(dp(18), dp(18), dp(18), dp(10));
                results.addView(empty, new LinearLayout.LayoutParams(-1, dp(70)));
                if (!q.isEmpty()) {
                    Button addMissing = makeButton("＋ Tambahkan " + rawQuery);
                    addMissing.setTextSize(12);
                    addMissing.setOnClickListener(v -> showEmmcEditor(null, rawQuery, renderer[0]));
                    results.addView(addMissing, new LinearLayout.LayoutParams(-1, dp(46)));
                }
                return;
            }

            TextView summary = new TextView(this);
            summary.setText(matches.size() + " hasil" + (q.isEmpty() ? "" : " untuk \"" + rawQuery + "\""));
            summary.setTextColor(Color.LTGRAY);
            summary.setTextSize(10);
            summary.setPadding(dp(4), dp(4), dp(4), dp(3));
            results.addView(summary);

            for (EmmcRecord r : matches) {
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setPadding(dp(12), dp(8), dp(8), dp(8));
                item.setBackground(roundedBg(Color.rgb(6, 34, 61), Color.rgb(0, 75, 105), 10));

                TextView name = new TextView(this);
                name.setText(r.part);
                name.setTextColor(Color.WHITE);
                name.setTextSize(14);
                name.setTypeface(null, Typeface.BOLD);
                item.addView(name);

                TextView meta = new TextView(this);
                meta.setText(r.brand + "  •  " + r.capacity + "  •  " + r.grade);
                meta.setTextColor(Color.rgb(170, 195, 210));
                meta.setTextSize(10);
                item.addView(meta);

                TextView editHint = new TextView(this);
                editHint.setText("Tap untuk detail / edit");
                editHint.setTextColor(Color.rgb(0, 210, 145));
                editHint.setTextSize(9);
                editHint.setPadding(0, dp(3), 0, 0);
                item.addView(editHint);

                item.setOnClickListener(v -> showEmmcDetailEditable(r, renderer[0]));
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(72));
                ip.setMargins(dp(2), dp(2), dp(2), dp(2));
                results.addView(item, ip);
            }
        };

        code.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count2) { renderer[0].run(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        addBtn.setOnClickListener(v -> showEmmcEditor(null, code.getText().toString().trim(), renderer[0]));
        quizBtn.setOnClickListener(v -> showGradeQuiz());
        learnBtn.setOnClickListener(v -> showGradeMemorization());
        gradeTableBtn.setOnClickListener(v -> showGradeTableImage());
        close.setOnClickListener(v -> { if (emmcDialog != null) emmcDialog.dismiss(); });
        renderer[0].run();

        AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).create();
        emmcDialog = dialog;

        dialog.setOnShowListener(d -> {
            android.view.Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setDimAmount(0.0f);
                w.setGravity(Gravity.CENTER);
                w.setLayout(-1, -1);
            }
        });

        dialog.setOnDismissListener(d -> {
            emmcDialog = null;
            if (topBar != null) topBar.setVisibility(View.VISIBLE);
            if (cameraBox != null) cameraBox.setVisibility(View.VISIBLE);
            if (infoRow != null) infoRow.setVisibility(View.VISIBLE);
            if (zoomBar != null) zoomBar.setVisibility(View.VISIBLE);
            if (exposureRow != null) exposureRow.setVisibility(View.VISIBLE);
            if (detailRow != null) detailRow.setVisibility(View.VISIBLE);
            if (bottomSheetHost != null) bottomSheetHost.setVisibility(View.VISIBLE);
            if (controlsBar != null) controlsBar.setVisibility(View.VISIBLE);
            if (status != null) status.setVisibility(View.VISIBLE);
            setLivePanelVisible(true);

            // Database temporarily unbinds CameraX. Rebind it when the
            // full-screen Database dialog closes so returning to Microscope
            // never leaves the live preview blank.
            if (!frozen && ContextCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                preview.postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed() && !frozen) {
                        startCamera();
                    }
                }, 180);
            }
            scheduleLivePanelHide();
        });

        dialog.show();
    }

    private void showEmmcDetailEditable(EmmcRecord r, Runnable afterSave) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6), dp(4), dp(6), dp(4));
        String[] labels = {"Part Number", "Kapasitas", "Grade", "Kategori", "Brand", "Catatan"};
        String[] values = {r.part, r.capacity, r.grade, r.category, r.brand, r.notes};
        for (int i = 0; i < labels.length; i++) {
            TextView t = new TextView(this);
            t.setText(labels[i] + "\n" + (values[i] == null || values[i].isEmpty() ? "Tidak dicantumkan" : values[i]));
            t.setTextColor(Color.WHITE); t.setTextSize(i == 0 ? 16 : 13);
            t.setTypeface(null, i == 0 ? Typeface.BOLD : Typeface.NORMAL);
            t.setPadding(dp(8), dp(7), dp(8), dp(7));
            box.addView(t);
        }
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        Button edit = makeButton("✏ Edit");
        edit.setTextSize(12);
        Button close = makeButton("Tutup");
        close.setTextSize(12);
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(44), 1);
        cp.setMargins(dp(6), 0, 0, 0);
        actions.addView(close, cp);
        box.addView(actions);

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Detail eMMC").setView(box).create();
        edit.setOnClickListener(v -> {
            dialog.dismiss();
            showEmmcEditor(r, null, afterSave);
        });
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private boolean matchesEmmcFilter(String category, String filter) {
        if ("Semua".equals(filter)) return true;
        if ("Pilihan".equals(filter)) return category.startsWith("Pilihan");
        if ("Samsung / A Khusus".equals(filter)) return category.equalsIgnoreCase("A+ Samsung/A Khusus");
        return category.startsWith(filter);
    }

    private void showEmmcDetail(EmmcRecord r) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(6), dp(4), dp(6), dp(4));
        String[] labels = {"Part Number", "Kapasitas", "Grade", "Kategori", "Brand", "Catatan"};
        String[] values = {r.part, r.capacity, r.grade, r.category, r.brand, r.notes};
        for (int i = 0; i < labels.length; i++) {
            TextView t = new TextView(this);
            t.setText(labels[i] + "\n" + (values[i] == null || values[i].isEmpty() ? "Tidak dicantumkan" : values[i]));
            t.setTextColor(Color.WHITE); t.setTextSize(i == 0 ? 16 : 13);
            t.setTypeface(null, i == 0 ? Typeface.BOLD : Typeface.NORMAL);
            t.setPadding(dp(8), dp(7), dp(8), dp(7));
            box.addView(t);
        }
        new AlertDialog.Builder(this).setTitle("Detail eMMC").setView(box).setPositiveButton("Tutup", null).show();
    }

    private void searchEmmc(String raw, TextView result) {
        String query = normalizeEmmc(raw);
        if (query.isEmpty()) {
            result.setText("Masukkan kode/part number eMMC terlebih dahulu.");
            return;
        }
        String found = findEmmcInAsset(query);
        if (found == null) {
            result.setText("KODE: " + query + "\n\nTidak ditemukan di database offline.\n\nGunakan SCAN OCR untuk membaca ulang, atau tambahkan data ke database/emmc_database.csv.");
        } else {
            result.setText(found);
        }
    }

    private String normalizeEmmc(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", "").toUpperCase(Locale.US);
    }

    private String findEmmcInAsset(String query) {
        try (java.io.InputStream in = getAssets().open("emmc_database.csv");
             java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(",", -1);
                if (p.length < 5) continue;
                String part = normalizeEmmc(p[0]);
                if (query.equals(part) || query.contains(part) || part.contains(query)) {
                    String category = p.length >= 5 ? p[4] : "Tidak dicantumkan";
                    String notes = p.length >= 6 ? p[5] : (p.length >= 5 ? p[4] : "");
                    return "eMMC DITEMUKAN\n\n"
                            + "Part Number : " + p[0] + "\n"
                            + "Brand       : " + p[1] + "\n"
                            + "Kapasitas   : " + p[2] + "\n"
                            + "Grade       : " + p[3] + "\n"
                            + "Kategori    : " + category + "\n"
                            + "Catatan     : " + notes;
                }
            }
        } catch (Exception e) {
            return "Database gagal dibaca: " + e.getMessage();
        }
        return null;
    }

    /**
     * Kuis eMMC: setiap kali dibuka, soal pertama dan pilihan diacak.
     * Setiap soal berikutnya juga mengambil record secara acak dari database.
     */
    private void showGradeQuiz() {
        final ArrayList<EmmcRecord> all = loadEmmcDatabase();
        if (all.size() < 4) {
            Toast.makeText(this, "Database eMMC belum cukup untuk kuis.", Toast.LENGTH_LONG).show();
            return;
        }

        final String[] modes = {
                "MUDAH • Tebak kapasitas",
                "SEDANG • Tebak grade",
                "SULIT • Tebak kapasitas + grade",
                "KESALAHAN SAYA • Ulangi yang salah",
                "CEPAT • 30 detik"
        };
        new AlertDialog.Builder(this)
                .setTitle("🎯 KUIS eMMC")
                .setMessage("Pilih mode latihan. Soal dan pilihan jawaban diacak setiap kali bermain.")
                .setItems(modes, (d, which) -> {
                    if (which == 3) {
                        ArrayList<EmmcRecord> wrong = getWrongRecords(all);
                        if (wrong.isEmpty()) {
                            Toast.makeText(this, "Belum ada soal yang salah. Coba kuis dulu.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        startEmmcQuiz(wrong, which);
                    } else {
                        startEmmcQuiz(all, which);
                    }
                })
                .setNegativeButton("BATAL", null)
                .show();
    }

    private java.util.ArrayList<EmmcRecord> getWrongRecords(java.util.ArrayList<EmmcRecord> all) {
        java.util.HashSet<String> wrongParts = new java.util.HashSet<>();
        String saved = getSharedPreferences("emmc_quiz", MODE_PRIVATE).getString("wrong_parts", "");
        if (saved != null && !saved.isEmpty()) {
            for (String part : saved.split("\\|")) if (!part.trim().isEmpty()) wrongParts.add(part.trim());
        }
        java.util.ArrayList<EmmcRecord> result = new java.util.ArrayList<>();
        for (EmmcRecord r : all) if (r != null && wrongParts.contains(r.part)) result.add(r);
        return result;
    }

    private void rememberWrongPart(String part) {
        if (part == null || part.trim().isEmpty()) return;
        java.util.HashSet<String> set = new java.util.LinkedHashSet<>();
        String saved = getSharedPreferences("emmc_quiz", MODE_PRIVATE).getString("wrong_parts", "");
        if (saved != null && !saved.isEmpty()) for (String x : saved.split("\\|")) if (!x.trim().isEmpty()) set.add(x.trim());
        set.add(part.trim());
        while (set.size() > 100) set.remove(set.iterator().next());
        getSharedPreferences("emmc_quiz", MODE_PRIVATE).edit().putString("wrong_parts", android.text.TextUtils.join("|", set)).apply();
    }

    private void startEmmcQuiz(java.util.ArrayList<EmmcRecord> source, int mode) {
        if (source.size() < 1) return;
        final java.util.ArrayList<EmmcRecord> all = loadEmmcDatabase();
        final java.util.ArrayList<EmmcRecord> pool = new java.util.ArrayList<>(source);
        java.util.Collections.shuffle(pool);
        final boolean speedMode = mode == 4;
        final int totalQuestions = speedMode ? Math.min(999, pool.size()) : Math.min(10, pool.size());
        final int[] questionIndex = {0};
        final int[] score = {0};
        final int bestBefore = getSharedPreferences("emmc_quiz", MODE_PRIVATE).getInt("best_mode_" + mode, 0);
        final java.util.HashSet<String> usedParts = new java.util.HashSet<>();
        final android.os.Handler timerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final long endTime = System.currentTimeMillis() + 30000L;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(10));
        root.setBackgroundColor(Color.rgb(3, 20, 38));

        TextView title = new TextView(this);
        title.setText("🎯 KUIS eMMC"); title.setTextColor(Color.WHITE); title.setTextSize(21); title.setTypeface(null, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView progress = new TextView(this); progress.setTextColor(Color.LTGRAY); progress.setTextSize(13);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(30)));
        TextView codeText = new TextView(this); codeText.setTextColor(Color.WHITE); codeText.setTextSize(24); codeText.setTypeface(null, Typeface.BOLD); codeText.setGravity(Gravity.CENTER); codeText.setPadding(dp(8), dp(18), dp(8), dp(18));
        codeText.setBackground(roundedBg(Color.rgb(7, 36, 65), Color.rgb(0, 145, 255), 14)); root.addView(codeText, new LinearLayout.LayoutParams(-1, dp(90)));
        TextView question = new TextView(this); question.setTextColor(Color.WHITE); question.setTextSize(16); question.setGravity(Gravity.CENTER);
        root.addView(question, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout choices = new LinearLayout(this); choices.setOrientation(LinearLayout.VERTICAL); root.addView(choices, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView result = new TextView(this); result.setTextColor(Color.WHITE); result.setTextSize(14); result.setGravity(Gravity.CENTER); result.setPadding(dp(4), dp(6), dp(4), dp(6)); root.addView(result, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout bottom = new LinearLayout(this); bottom.setGravity(Gravity.CENTER_VERTICAL);
        Button guide = makeButton("📚 BELAJAR"); guide.setTextSize(11); bottom.addView(guide, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button close = makeButton("TUTUP"); close.setTextSize(11); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(46), 1); cp.leftMargin = dp(8); bottom.addView(close, cp); root.addView(bottom);

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(root).create();
        close.setOnClickListener(v -> dialog.dismiss()); guide.setOnClickListener(v -> showGradeMemorization());
        final java.util.Random random = new java.util.Random();

        class QuizController {
            void finish(String message) {
                timerHandler.removeCallbacksAndMessages(null);
                int best = Math.max(bestBefore, score[0]);
                if (score[0] > bestBefore) {
                    getSharedPreferences("emmc_quiz", MODE_PRIVATE).edit().putInt("best_mode_" + mode, score[0]).apply();
                }
                progress.setText("SELESAI • Skor " + score[0] + (speedMode ? "" : "/" + totalQuestions) + " • Rekor " + best);
                codeText.setText("🎉 SELESAI"); question.setText(message); choices.removeAllViews();
                result.setText(score[0] > bestBefore ? "🏆 Rekor baru!" : "Rekor terbaik: " + best + " • Coba lagi untuk meningkatkannya.");
            }
            void next() {
                if (speedMode && System.currentTimeMillis() >= endTime) { finish("Waktu habis! Benar: " + score[0]); return; }
                if (!speedMode && questionIndex[0] >= totalQuestions) { finish("Nilai kamu: " + score[0] + " benar dari " + totalQuestions); return; }
                EmmcRecord rec = null;
                for (int tries=0; tries<30 && rec==null; tries++) {
                    EmmcRecord candidate = pool.get(random.nextInt(pool.size()));
                    if (candidate != null && candidate.part != null && usedParts.add(candidate.part)) rec = candidate;
                }
                if (rec == null && speedMode) { usedParts.clear(); rec = pool.get(random.nextInt(pool.size())); }
                if (rec == null) { finish("Semua soal sudah digunakan."); return; }
                final EmmcRecord current = rec;
                progress.setText(speedMode ? "⏱ CEPAT • Skor " + score[0] + " • Rekor " + bestBefore : "Soal " + (questionIndex[0]+1) + "/" + totalQuestions + " • Skor " + score[0] + " • Rekor " + bestBefore);
                codeText.setText(current.part); result.setText(""); choices.removeAllViews();
                String capacity = current.capacity == null || current.capacity.trim().isEmpty() ? "Tidak diketahui" : current.capacity.trim();
                String grade = current.grade == null || current.grade.trim().isEmpty() ? "Tidak diketahui" : current.grade.trim();
                final String answer;
                if (mode == 0) { question.setText("Kapasitas eMMC ini berapa?"); answer = capacity; }
                else if (mode == 1 || mode == 3) { question.setText("Grade eMMC ini apa?"); answer = grade; }
                else if (mode == 2) { question.setText("Kapasitas + grade yang benar?"); answer = capacity + " • " + grade; }
                else { question.setText("Kapasitas eMMC ini berapa?"); answer = capacity; }
                java.util.ArrayList<String> options = new java.util.ArrayList<>();
                options.add(answer);
                java.util.HashSet<String> seen = new java.util.HashSet<>(); seen.add(answer.toUpperCase(Locale.US));
                for (EmmcRecord r : all) {
                    if (options.size() >= 4 || r == null) continue;
                    String opt;
                    if (mode == 1 || mode == 3) opt = r.grade;
                    else if (mode == 2) opt = (r.capacity == null ? "Tidak diketahui" : r.capacity.trim()) + " • " + (r.grade == null ? "Tidak diketahui" : r.grade.trim());
                    else opt = r.capacity;
                    if (opt == null || opt.trim().isEmpty()) continue; opt = opt.trim();
                    if (seen.add(opt.toUpperCase(Locale.US))) options.add(opt);
                }
                java.util.Collections.shuffle(options);
                for (String opt : options) {
                    Button b = makeButton(opt); b.setTextSize(14); b.setAllCaps(false); LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(46)); bp.topMargin=dp(6); choices.addView(b,bp);
                    b.setOnClickListener(v -> {
                        for (int i=0;i<choices.getChildCount();i++) choices.getChildAt(i).setEnabled(false);
                        String picked=((Button)v).getText().toString().trim(); boolean ok=picked.equalsIgnoreCase(answer);
                        if(ok){ score[0]++; result.setText("✓ BENAR — " + current.part); }
                        else { rememberWrongPart(current.part); result.setText("✗ SALAH — Jawaban: " + answer); }
                        questionIndex[0]++; v.postDelayed(this::next, 650);
                    });
                }
            }
        }
        QuizController controller = new QuizController();
        dialog.setOnShowListener(d -> controller.next());
        dialog.show();
    }

    private void showGradeMemorization() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(10));
        root.setBackgroundColor(Color.rgb(3, 20, 38));

        TextView title = new TextView(this);
        title.setText("📚 BELAJAR CEPAT — GRADE eMMC");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(44)));

        TextView sub = new TextView(this);
        sub.setText("Pilih merek untuk melihat pola kode kapasitas. Gunakan sebagai panduan cepat teknisi.");
        sub.setTextColor(Color.LTGRAY);
        sub.setTextSize(13);
        sub.setPadding(0, 0, 0, dp(6));
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(46)));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(brandRow, new LinearLayout.LayoutParams(-1, dp(48)));

        ScrollView scroll = new ScrollView(this);
        TextView rules = new TextView(this);
        rules.setTextColor(Color.WHITE);
        rules.setTextSize(15);
        rules.setLineSpacing(dp(2), 1.05f);
        rules.setPadding(dp(6), dp(10), dp(6), dp(14));
        scroll.addView(rules, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        final String[] names = {"SAMSUNG", "TOSHIBA", "ASUS", "YME", "YMUS", "SKYNIX"};
        final String[] texts = {
                "SAMSUNG\n\n" +
                "KLM A  → 16 GB\n\n" +
                "KLM B / KLU B  → 32 GB\n\n" +
                "KLM C / KLU C  → 64 GB\n\n" +
                "KLM D / KLU D  → 128 GB\n\n" +
                "KLM E / KLU E  → 256 GB\n\n" +
                "KLU F  → 512 GB\n\n" +
                "💡 Fokus pada huruf seri KLM/KLU untuk mengingat kapasitas.",

                "TOSHIBA\n\n" +
                "THGBMBG7  → 16 GB\n\n" +
                "THGBMBG8  → 32 GB\n\n" +
                "THGBMBG9  → 64 GB\n\n" +
                "💡 Ingat angka 7 → 16, 8 → 32, 9 → 64.",

                "ASUS\n\n" +
                "H26M4  → 8 GB\n\n" +
                "H26M5  → 16 GB\n\n" +
                "H26M6  → 32 GB\n\n" +
                "H26M7  → 64 GB\n\n" +
                "H28U6  → Pilihan 32 GB\n\n" +
                "H28U7  → Pilihan 64 GB\n\n" +
                "H28U8  → Pilihan 128 GB",

                "YME\n\n" +
                "C6  → 32 GB\n\n" +
                "C7  → 64 GB\n\n" +
                "C8  → 128 GB\n\n" +
                "C9  → 256 GB",

                "YMUS\n\n" +
                "6  → 32 GB\n\n" +
                "7  → 64 GB\n\n" +
                "8  → 128 GB\n\n" +
                "9  → 256 GB",

                "SKYNIX\n\n" +
                "32  → 4 GB\n\n" +
                "64 / 65  → 8 GB\n\n" +
                "17 / 18  → 16 GB\n\n" +
                "26 / 27  → 32 GB\n\n" +
                "26 / 27 AB  → 2/32 GB\n\n" +
                "52 / 53  → 64 GB\n\n" +
                "15 / 16  → 128 GB\n\n" +
                "21 / 22  → 256 GB"
        };

        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            Button b = makeButton(names[i]);
            b.setTextSize(10);
            b.setAllCaps(false);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(42), 1);
            if (i > 0) bp.leftMargin = dp(4);
            brandRow.addView(b, bp);
            b.setOnClickListener(v -> rules.setText(texts[idx]));
        }
        rules.setText(texts[0]);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        Button quiz = makeButton("🎯 TEBAK GRADE");
        quiz.setTextSize(13);
        bottom.addView(quiz, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button close = makeButton("TUTUP");
        close.setTextSize(13);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(48), 1);
        cp.leftMargin = dp(8);
        bottom.addView(close, cp);
        root.addView(bottom);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(root).create();
        close.setOnClickListener(v -> dialog.dismiss());
        quiz.setOnClickListener(v -> { dialog.dismiss(); showGradeQuiz(); });
        dialog.show();
    }

    private void scanEmmcOcr(EditText target, TextView result) {
        if (capture == null) {
            result.setText("Kamera belum siap.");
            return;
        }
        result.setText("Memotret eMMC untuk OCR...\nFoto hanya dipakai sementara dan tidak disimpan ke Galeri.");
        java.io.File file = new java.io.File(getCacheDir(), "emmc_ocr_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(file).build();

        capture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                            if (bitmap == null) throw new Exception("Bitmap kosong");
                            InputImage image = InputImage.fromBitmap(bitmap, 0);
                            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                            recognizer.process(image)
                                    .addOnSuccessListener(text -> {
                                        String ocr = text.getText() == null ? "" : text.getText().trim();
                                        String cleaned = cleanOcrEmmc(ocr);
                                        target.setText(cleaned);
                                        if (cleaned.isEmpty()) {
                                            result.setText("OCR belum menemukan kode. Coba zoom/fokus lalu SCAN OCR lagi.");
                                        } else {
                                            searchEmmc(cleaned, result);
                                        }
                                        try { bitmap.recycle(); } catch (Exception ignored) {}
                                        try { file.delete(); } catch (Exception ignored) {}
                                        recognizer.close();
                                    })
                                    .addOnFailureListener(e -> {
                                        result.setText("OCR gagal: " + e.getMessage());
                                        try { bitmap.recycle(); } catch (Exception ignored) {}
                                        try { file.delete(); } catch (Exception ignored) {}
                                        recognizer.close();
                                    });
                        } catch (Exception e) {
                            result.setText("Foto OCR gagal: " + e.getMessage());
                            try { file.delete(); } catch (Exception ignored) {}
                        }
                    }
                    @Override public void onError(@NonNull ImageCaptureException exception) {
                        result.setText("Pengambilan foto OCR gagal: " + exception.getMessage());
                    }
                });
    }

    private String cleanOcrEmmc(String text) {
        if (text == null) return "";
        String[] lines = text.toUpperCase(Locale.US).split("\\R+");
        String best = "";
        for (String line : lines) {
            String x = line.replaceAll("[^A-Z0-9-]", "");
            if (x.length() >= 5 && x.length() > best.length()) best = x;
        }
        return best;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else if (status != null) {
                status.setText("Izin kamera diperlukan");
            }
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (camera != null) {
                camera.getCameraControl().enableTorch(false);
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }
}
