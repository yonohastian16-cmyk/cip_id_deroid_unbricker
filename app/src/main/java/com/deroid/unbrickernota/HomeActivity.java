package com.deroid.unbrickernota;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(3, 16, 31));
        getWindow().setNavigationBarColor(Color.rgb(2, 10, 20));
        buildHome();
    }

    private GradientDrawable bg(int strokeColor) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(5, 27, 57), Color.rgb(2, 12, 27)});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(2), strokeColor);
        return g;
    }

    private TextView text(String s, float size, int color, int style) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style); t.setGravity(Gravity.CENTER);
        return t;
    }

    private LinearLayout card(String title, String icon, int border, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(14), dp(12), dp(12));
        card.setBackground(bg(border)); card.setClickable(true); card.setFocusable(true); card.setOnClickListener(click);

        TextView ico = text(icon, 56, Color.WHITE, Typeface.BOLD);
        card.addView(ico, new LinearLayout.LayoutParams(-1, dp(76)));
        TextView name = text(title, 22, Color.WHITE, Typeface.BOLD);
        name.setLetterSpacing(0.04f);
        card.addView(name, new LinearLayout.LayoutParams(-1, dp(62)));

        TextView arrow = text("→", 34, Color.WHITE, Typeface.BOLD);
        GradientDrawable circle = new GradientDrawable(); circle.setShape(GradientDrawable.OVAL); circle.setColor(border);
        arrow.setBackground(circle);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(64), dp(64)));
        return card;
    }

    private void buildHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(2, 12, 25));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(14));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.deroid_unbricker_nota_logo); logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        root.addView(logo, new LinearLayout.LayoutParams(-1, dp(108)));

        TextView brand = text("JEJAK TEKNISI", 29, Color.WHITE, Typeface.BOLD);
        brand.setLetterSpacing(0.08f);
        root.addView(brand, new LinearLayout.LayoutParams(-1, dp(44)));

        TextView sub = text("Solusi di Setiap Kerusakan", 15, Color.LTGRAY, Typeface.NORMAL);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(30)));

        TextView pilih = text("PILIH FITUR", 25, Color.WHITE, Typeface.BOLD);
        pilih.setPadding(0, dp(10), 0, dp(10));
        root.addView(pilih, new LinearLayout.LayoutParams(-1, dp(62)));

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL); cards.setGravity(Gravity.CENTER);
        cards.setPadding(0, dp(4), 0, dp(10));

        LinearLayout microscope = card("MICROSCOPE", "🔬", Color.rgb(20, 120, 255),
                v -> startActivity(new Intent(this, MainActivity.class)));

        // Database eMMC dihapus; halaman awal hanya menampilkan Microscope.
        LinearLayout.LayoutParams microscopeParams =
                new LinearLayout.LayoutParams(-1, dp(360));
        microscopeParams.setMargins(dp(12), 0, dp(12), 0);
        cards.addView(microscope, microscopeParams);
        root.addView(cards, new LinearLayout.LayoutParams(-1, dp(380)));

        TextView footer = text("J E J A K   T E K N I S I", 13, Color.LTGRAY, Typeface.BOLD);
        footer.setLetterSpacing(0.18f);
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(55)));
        scroll.addView(root); setContentView(scroll);
    }
}
