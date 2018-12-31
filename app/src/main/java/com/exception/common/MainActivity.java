package com.exception.common;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView onclickCash;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onclickCash =(TextView)findViewById(R.id.onclickCash);
        onclickCash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                int a=0/19;
            }
        });
    }




}
