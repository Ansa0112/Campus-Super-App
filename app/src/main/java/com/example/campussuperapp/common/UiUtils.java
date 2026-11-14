package com.example.campussuperapp.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.campussuperapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public final class UiUtils {

    private UiUtils() {}

    public static void toast(Context c, String message) {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
    }

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    /** One colour per category, used for the card strip and the small tag. */
    public static int categoryColor(String category) {
        if (category == null) return Color.parseColor("#64748B");
        switch (category.toLowerCase(Locale.ROOT)) {
            case "emergency": return Color.parseColor("#D93B3B");
            case "exam":      return Color.parseColor("#F08A24");
            case "career":    return Color.parseColor("#F08A24");
            case "fee":       return Color.parseColor("#7C4DDB");
            case "society":   return Color.parseColor("#7C4DDB");
            case "academic":  return Color.parseColor("#2F6BE0");
            case "seminar":   return Color.parseColor("#2F6BE0");
            case "library":   return Color.parseColor("#0E9AA7");
            case "workshop":  return Color.parseColor("#0E9AA7");
            case "sports":    return Color.parseColor("#1E9E6A");
            case "cultural":  return Color.parseColor("#D6479A");
            default:          return Color.parseColor("#64748B");
        }
    }

    /** Makes a TextView look like a small rounded tag in the given colour. */
    public static void styleTag(TextView tv, int color) {
        tv.setTextColor(color);
        tv.setBackgroundResource(R.drawable.ae_bg_tag);
        tv.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 36)));
    }

    /** Adds the status-bar / navigation-bar height as padding so content is never hidden. */
    public static void padForSystemBars(View v, final boolean top, final boolean bottom) {
        final int l = v.getPaddingLeft();
        final int t = v.getPaddingTop();
        final int r = v.getPaddingRight();
        final int b = v.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(l, top ? t + bars.top : t, r, bottom ? b + bars.bottom : b);
            return insets;
        });
        ViewCompat.requestApplyInsets(v);
    }

    public static void confirm(Context c, String title, String message, String positive, Runnable onYes) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(positive, (d, w) -> onYes.run())
                .show();
    }
}
