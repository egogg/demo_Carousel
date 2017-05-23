package com.nkrhelper.carousel;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.nkrhelper.mylibrary.carousel.CarouselView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CarouselView carouselView = (CarouselView) findViewById(R.id.recycler_color_list);

        Resources resources = getResources();
        String[] colorNames = resources.getStringArray(R.array.color_names);
        String[] colorValues = resources.getStringArray(R.array.color_values);
        ColorItemAdapter colorListAdapter = new ColorItemAdapter(colorNames, colorValues);
        carouselView.setCarouselAdapter(colorListAdapter);
    }
}
