package bfortuner.androiddemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "bfortuner.androiddemo.MESSAGE";
    private static final String LOG_TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void sendMessage(View view) {
        Log.v(LOG_TAG, "Sending message!");
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void openCamera(View view) {
        Log.v(LOG_TAG, "Opening camera!");
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public void openCameraPreview(View view) {
        Log.v(LOG_TAG, "Opening camera preview!");
        Intent intent = new Intent(this, CameraPreview.class);
        startActivity(intent);
    }
}
