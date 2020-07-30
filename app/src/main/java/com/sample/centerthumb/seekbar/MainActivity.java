package com.sample.centerthumb.seekbar;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import ui.widget.seekbar.CenterThumbSeekBar;

public class MainActivity extends AppCompatActivity {

    private AppCompatTextView tvProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvProgress = findViewById(R.id.tvProgress);

        CenterThumbSeekBar seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnFromValueChangeListener(new CenterThumbSeekBar.OnFromValueChangeListener() {
            @Override
            public void onValueChange(double value) {
                tvProgress.setText(String.valueOf(value));
            }
        });

        seekBar.setOnToValueChangeListener(new CenterThumbSeekBar.OnToValueChangeListener() {
            @Override
            public void onValueChange(double value) {
                tvProgress.setText(String.valueOf(value));
            }
        });
    }
}