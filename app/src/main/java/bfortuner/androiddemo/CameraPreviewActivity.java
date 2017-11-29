package bfortuner.androiddemo;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class CameraPreviewActivity extends Activity
        implements ImageReader.OnImageAvailableListener {

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
    private Size previewSize;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private boolean processing = false;
    private AssetManager mgr;
    private String predictedClass = "none";
    private boolean run_HWC = false;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

        setFragment();

//        mgr = getResources().getAssets();
//        new SetUpNeuralNetwork().execute();
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }

        Log.v("Image available", "reading image...");
        image = reader.acquireNextImage();
//        counter++;
//        if (processing) {
//            image.close();
//            return;
//        }
//        processing = true;
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//        int w = image.getWidth();
//        int h = image.getHeight();
//        ByteBuffer Ybuffer = image.getPlanes()[0].getBuffer();
//        ByteBuffer Ubuffer = image.getPlanes()[1].getBuffer();
//        ByteBuffer Vbuffer = image.getPlanes()[2].getBuffer();
//
//        // TODO: use these for proper image processing on different formats.
//        int rowStride = image.getPlanes()[1].getRowStride();
//        int pixelStride = image.getPlanes()[1].getPixelStride();
//        byte[] Y = new byte[Ybuffer.capacity()];
//        byte[] U = new byte[Ubuffer.capacity()];
//        byte[] V = new byte[Vbuffer.capacity()];
//        Ybuffer.get(Y);
//        Ubuffer.get(U);
//        Vbuffer.get(V);
//
//        predictedClass = classificationFromCaffe2(h, w, Y, U, V,
//                rowStride, pixelStride, run_HWC);
//        Log.v(LOG_TAG, "class:"+predictedClass);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                textView.setText("hi!");
//                processing = false;
//            }
//        });
        image.close();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    protected void setFragment() {
        CameraPreviewFragment cameraPreviewFragment =
            CameraPreviewFragment.newInstance(
                new CameraPreviewFragment.PreviewCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                    previewHeight = size.getHeight();
                    previewWidth = size.getWidth();
                    CameraPreviewActivity.this.onPreviewSizeChosen(size, rotation);
                }
            },
            this,
            getLayoutId(),
            getDesiredPreviewFrameSize());

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, cameraPreviewFragment)
                .commit();
    }



    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
    protected abstract int getLayoutId();
    protected abstract Size getDesiredPreviewFrameSize();
}
