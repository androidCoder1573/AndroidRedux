package com.cyworks.demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.cyworks.R;

/**
 * Demo，用于展示live-redux基本能力，本示例是Activity嵌套Fragment，也可以直接用Activity承载Page
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new TestFragment());
        transaction.commitAllowingStateLoss();
    }
}
