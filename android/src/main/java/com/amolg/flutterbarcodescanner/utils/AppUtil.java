package com.amolg.flutterbarcodescanner.utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class AppUtil {
    public static int dpToPx(Context context,int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
