package bfortuner.androiddemo;

import android.media.ImageReader.OnImageAvailableListener;
import android.util.Log;
import android.util.Size;


public class ClassifierActivity extends CameraPreviewActivity implements OnImageAvailableListener {

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_camera_preview;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        Log.d("HI COLIN","WAS UP");
        previewHeight = size.getWidth();
        previewHeight = size.getHeight();
    }
}
