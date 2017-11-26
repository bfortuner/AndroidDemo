package bfortuner.androiddemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CameraPreviewActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final String LOG_TAG = "CameraPreview";

    private AutoFitTextureView textureView;
    private TextView textView;
    private Integer counter = 0;
    private CameraDevice cameraDevice;
    private String cameraId;
    protected CameraCaptureSession cameraCaptureSession;
    protected CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest capturePreviewRequest;
    private Image image = null;
    private ImageReader reader;
    private Integer sensorOrientation;
    private Size previewSize;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private boolean processing = false;
    private AssetManager mgr;
    private String predictedClass = "none";

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    static {
        System.loadLibrary("native-lib");
    }

    public native String classificationFromCaffe2(int h, int w, byte[] Y, byte[] U, byte[] V,
                                                  int rowStride, int pixelStride, boolean r_hwc);
    public native void initCaffe2(AssetManager mgr);
    private class SetUpNeuralNetwork extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void[] v) {
            try {
                initCaffe2(mgr);
                predictedClass = "Neural net loaded! Inferring...";
            } catch (Exception e) {
                Log.d(LOG_TAG, "Couldn't load neural network.");
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview2);

        textureView = findViewById(R.id.autoFitTextureView);
        textureView.setSurfaceTextureListener(textureListener);

        textView = findViewById(R.id.textView2);

        mgr = getResources().getAssets();
        new SetUpNeuralNetwork().execute();
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_CAMERA_PERMISSION);
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);


        setUpCameraOutputs(width, height);
        configureTransform(width, height);

        try {
            manager.openCamera(cameraId, stateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(this.CAMERA_SERVICE);
        try {
            // Only using the rear camera for now
            cameraId = manager.getCameraIdList()[0];

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // For still image captures, we use the largest available size.
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CameraFragment.CompareSizesByArea());

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, maxPreviewWidth, maxPreviewHeight, largest);

            final int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(width, height);
            } else {
                textureView.setAspectRatio(height, width);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(final int viewWidth, final int viewHeight) {
        if (null == textureView || null == previewSize) {
            return;
        }
        final int rotation = getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale =
                    Math.max(
                            (float) viewHeight / previewSize.getHeight(),
                            (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CameraFragment.CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CameraFragment.CompareSizesByArea());
        } else {
            Log.e("chooseOptimalSize", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            Surface surface = new Surface(texture);

            int width = 227;
            int height = 227;
            reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 4);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.v("Image available", "reading image...");
                    image = reader.acquireNextImage();
                    counter++;
                    if (processing) {
                        image.close();
                        return;
                    }
                    processing = true;
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        textView.setText("My man " + counter);
                        processing = false;
                        }
                    });
                    image.close();
                }
            };

            // Adding the reader listener to the background thread's message queue
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(reader.getSurface());

            cameraDevice.createCaptureSession(Arrays.asList(surface, reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession captureSession) {
                    if (null == cameraDevice) {
                        return;
                    }

                    cameraCaptureSession = captureSession;

                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                    capturePreviewRequest = captureRequestBuilder.build();
                    try {
                        cameraCaptureSession.setRepeatingRequest(capturePreviewRequest,
                                null, backgroundHandler);
                    } catch (CameraAccessException e ) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession captureSession) {
                }
            },null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }




}
