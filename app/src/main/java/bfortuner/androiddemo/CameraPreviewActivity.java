package bfortuner.androiddemo;


import android.app.Activity;
import android.content.res.AssetManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;


public abstract class CameraPreviewActivity extends Activity
        implements ImageReader.OnImageAvailableListener {

    private static final String LOG_TAG = "CameraPreview";

    private Image image = null;

    private Integer counter = 0;
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
        super.onCreate(null);
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
