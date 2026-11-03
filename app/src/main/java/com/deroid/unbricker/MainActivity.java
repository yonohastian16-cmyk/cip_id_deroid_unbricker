package com.deroid.unbricker;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Deroid Unbricker - CIP ID\nWatermark by.deroid unbricker");
        tv.setTextSize(20);
        tv.setPadding(32, 32, 32, 32);
        setContentView(tv);
    }
}
