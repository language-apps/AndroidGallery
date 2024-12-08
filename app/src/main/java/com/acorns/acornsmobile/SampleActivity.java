package com.acorns.acornsmobile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SampleActivity  extends AppCompatActivity {
    public static String TAG = "Sample Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.samples);

        Log.v(TAG, "Activate hyperlink");
        TextView mTextView = this.findViewById(R.id.textView);
        mTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mTextView.setLinkTextColor(Color.BLUE);

        Log.v(TAG, "Set Start App button and Click Listener");
        Button loadApp = findViewById(R.id.start_app_button);
        loadApp.setOnClickListener(v ->this.onBackPressed());

        Log.v(TAG, "Set Load Button  and Click Listener");
        Button loadSamples = findViewById(R.id.load_samples_button);
        loadSamples.setOnClickListener(v ->  {
            LessonHandler handler = new LessonHandler(this);
            int result = RESULT_OK;
            try {
                handler.copySampleLessons();
            } catch (InterruptedException e) {
                result = RESULT_CANCELED;
            }

            Intent data = getIntent();
            setResult(result);
           finish();
        });

    }
}
