package com.nkrhelper.carousel;

/**
 * Created 09/05/2017.
 */

public class ColorItem {
    private String mColorName;
    private String mColorValue;

    public ColorItem(String colorName, String colorValue) {
        mColorName = colorName;
        mColorValue = colorValue;
    }

    public void setColorName(String colorName) {
        mColorName = colorName;
    }

    public String getColorName() {
        return mColorName;
    }

    public void setColorValue(String colorValue) {
        mColorValue = colorValue;
    }

    public String getColorValue() {
        return mColorValue;
    }
}
