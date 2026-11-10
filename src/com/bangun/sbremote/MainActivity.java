package com.bangun.sbremote;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Palette
    private static final int BG = 0xFF12151B;
    private static final int PANEL = 0xFF1B2029;
    private static final int KEY = 0xFF2A313D;
    private static final int KEY_ACCENT = 0xFF34507A;
    private static final int KEY_POWER = 0xFFC8373D;
        private static final int TEXT = 0xFFE8ECF2;
    private static final int TEXT_DIM = 0xFF8A94A6;
    private static final int OK = 0xFF4CC38A;
    private static final int ERR = 0xFFFF6B6B;

    private static final long REPEAT_DELAY_MS = 400;
    private static final long REPEAT_INTERVAL_MS = 140;

    private ConsumerIrManager ir;
    private boolean hasIr;
    private TextView status;
    private Handler txHandler;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private int sendCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ir = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        hasIr = ir != null && ir.hasIrEmitter();

        HandlerThread t = new HandlerThread("ir-tx");
        t.start();
        txHandler = new Handler(t.getLooper());

        setContentView(buildUi());
        if (!hasIr) setStatus("Ponsel ini tidak punya IR blaster", ERR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        txHandler.getLooper().quitSafely();
    }

    // ---------------------------------------------------------------- UI

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root = vertical();
        root.setPadding(dp(16), dp(20), dp(16), dp(24));
        scroll.addView(root);

        TextView title = text("Samsung Soundbar", 22, TEXT, true);
        root.addView(title);
        TextView sub = text("HW-B650 · Samsung36 · custom 0x30 0x0F", 12, TEXT_DIM, false);
        root.addView(sub);

        status = text("Siap", 13, OK, false);
        status.setPadding(0, dp(8), 0, dp(12));
        root.addView(status);

        // Power | Mute | Source
        LinearLayout top = row();
        top.addView(key("POWER", null, R.drawable.ic_power, 0x70, KEY_POWER, false), weight(1));
        top.addView(key("MUTE", null, R.drawable.ic_mute, 0x71, KEY, false), weight(1));
        top.addView(key("SOURCE", null, 0x75, KEY_ACCENT, false), weight(1));
        root.addView(panel(top));

        // Settings column | D-pad | Dolby + Woofer column
        LinearLayout mid = row();

        LinearLayout left = vertical();
        left.addView(key("SETTINGS", null, 0xA6, KEY, false), cell());
        left.addView(key("SPEAKER", null, 0x88, KEY, false), cell());
        left.addView(key("BT PAIR", null, 0xB7, KEY, false), cell());

        LinearLayout pad = vertical();
        LinearLayout r1 = row();
        r1.addView(spacer(), weight(1));
        r1.addView(key("▲", null, 0xB4, KEY, true), weight(1));
        r1.addView(spacer(), weight(1));
        LinearLayout r2 = row();
        r2.addView(key("◀", null, 0xB0, KEY, true), weight(1.5f));
        r2.addView(key("▶", null, 0xB1, KEY, true), weight(1.5f));
        LinearLayout r3 = row();
        r3.addView(spacer(), weight(1));
        r3.addView(key("▼", null, 0xB5, KEY, true), weight(1));
        r3.addView(spacer(), weight(1));
        pad.addView(r1, tall());
        pad.addView(r2, tall());
        pad.addView(r3, tall());

        LinearLayout right = vertical();
        right.addView(key("DOLBY", null, 0xC2, KEY, false), cell());
        right.addView(key("WOOF +", null, 0x82, KEY, true), cell());
        right.addView(key("WOOF −", null, 0x83, KEY, true), cell());

        mid.addView(left, column(1.2f));
        mid.addView(pad, column(2.8f));
        mid.addView(right, column(1.2f));
        root.addView(panel(mid));

        // Volume
        LinearLayout vol = row();
        vol.addView(key("VOL −", null, 0x78, KEY, true), weight(1, 80));
        vol.addView(key("VOL +", null, 0x77, KEY, true), weight(1, 80));
        root.addView(panel(vol));

        // Sound
        root.addView(section("Suara"));
        root.addView(panel(grid(new Object[][]{
                {"SOUND MODE", 0x7B, false}, {"TREBLE/BASS", 0x98, false},
        }, KEY, 2)));

        return scroll;
    }

    private LinearLayout grid(Object[][] items, int color) {
        return grid(items, color, 3);
    }

    private LinearLayout grid(Object[][] items, int color, int cols) {
        LinearLayout g = vertical();
        LinearLayout r = null;
        for (int i = 0; i < items.length; i++) {
            if (i % cols == 0) {
                r = row();
                g.addView(r, tall());
            }
            Object[] it = items[i];
            r.addView(key((String) it[0], null, (Integer) it[1], color, (Boolean) it[2]), weight(1));
        }
        return g;
    }

    private View key(String label, String sub, int data, int color, boolean repeat) {
        return key(label, sub, 0, data, color, repeat);
    }

    /** icon != 0 shows a drawable instead of the text label (label is still used in the status line). */
    private View key(final String label, String sub, int icon, final int data, int color, final boolean repeat) {
        final LinearLayout b = vertical();
        b.setGravity(Gravity.CENTER);
        b.setMinimumHeight(dp(52));
        b.setPadding(dp(4), dp(6), dp(4), dp(6));

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(14));
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(14));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x55FFFFFF), shape, mask));
        b.setClickable(true);

        if (icon != 0) {
            ImageView iv = new ImageView(this);
            iv.setImageResource(icon);
            iv.setContentDescription(label);
            b.addView(iv, new LinearLayout.LayoutParams(dp(28), dp(28)));
        } else {
            TextView l = text(label, label.length() <= 2 ? 20 : label.length() > 7 ? 11 : 13, TEXT, true);
            l.setMaxLines(1);
            l.setGravity(Gravity.CENTER);
            b.addView(l);
        }
        if (sub != null) {
            TextView s = text(sub, 11, 0xFFFFD9A0, false);
            s.setGravity(Gravity.CENTER);
            b.addView(s);
        }

        final Runnable repeater = new Runnable() {
            @Override public void run() {
                send(label, data);
                ui.postDelayed(this, REPEAT_INTERVAL_MS);
            }
        };

        b.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        send(label, data);
                        if (repeat) ui.postDelayed(repeater, REPEAT_DELAY_MS);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        ui.removeCallbacks(repeater);
                        break;
                }
                return false; // keep ripple / pressed state
            }
        });
        return b;
    }

    // ---------------------------------------------------------------- IR

    private void send(final String label, final int data) {
        final String code = String.format("0x%02X", data);
        if (!hasIr) {
            setStatus("Tidak ada IR blaster · " + label + " (" + code + ")", ERR);
            return;
        }
        final int[] pattern = IrCodes.pattern(data);
        txHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    ir.transmit(IrCodes.CARRIER_HZ, pattern);
                    sendCount++;
                    setStatus("Terkirim: " + label + " · " + code + "  (#" + sendCount + ")", OK);
                } catch (Exception ex) {
                    setStatus("Gagal kirim: " + ex.getMessage(), ERR);
                }
            }
        });
    }

    private void setStatus(final String msg, final int color) {
        ui.post(new Runnable() {
            @Override public void run() {
                status.setText(msg);
                status.setTextColor(color);
            }
        });
    }

    // ---------------------------------------------------------------- helpers

    private View panel(View content) {
        LinearLayout p = vertical();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL);
        bg.setCornerRadius(dp(20));
        p.setBackground(bg);
        p.setPadding(dp(8), dp(8), dp(8), dp(8));
        p.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        p.setLayoutParams(lp);
        return p;
    }

    private TextView section(String s) {
        TextView t = text(s.toUpperCase(), 11, TEXT_DIM, true);
        t.setLetterSpacing(0.08f);
        t.setPadding(dp(4), dp(6), 0, dp(6));
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private View spacer() {
        return new View(this);
    }

    private LinearLayout.LayoutParams weight(float w) {
        return weight(w, 58);
    }

    private LinearLayout.LayoutParams weight(float w, int heightDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(heightDp), w);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }

    /** Column inside a horizontal row: weighted width, height follows its children. */
    private LinearLayout.LayoutParams column(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    /** Key stacked in a side column: same 58dp height as a D-pad row, so rows line up. */
    private LinearLayout.LayoutParams cell() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        // D-pad rows get 4dp key margin + 4dp row margin on each side, so match that.
        lp.setMargins(dp(4), dp(8), dp(4), dp(8));
        return lp;
    }

    private LinearLayout.LayoutParams tall() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
