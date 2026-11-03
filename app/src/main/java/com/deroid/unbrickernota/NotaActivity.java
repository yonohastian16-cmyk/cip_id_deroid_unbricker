package com.deroid.unbrickernota;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Nota servis Deroid Unbricker Nota.
 * Daftar nota, buat/ubah nota, ambil barang dari database eMMC, PDF, bagikan (WhatsApp dll.), cetak.
 * Berdiri sendiri: hanya memakai API framework + androidx yang sudah ada di proyek.
 */
public class NotaActivity extends AppCompatActivity {

    // ---------------------------------------------------------------- konstanta
    private static final String PREFS = "nota_v1";
    private static final String SHOP_PREFS = "nota_shop_v1";
    private static final String EMMC_USER_PREFS = "emmc_user_records_v1";
    private static final int DIALOG_THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert;

    private static final int BG = Color.rgb(10, 12, 14);
    private static final int CARD = Color.rgb(22, 27, 32);
    private static final int FIELD = Color.rgb(35, 40, 44);
    private static final int LINE = Color.rgb(60, 66, 72);
    private static final int ACCENT = Color.rgb(20, 120, 255);
    private static final int GREEN = Color.rgb(30, 160, 90);
    private static final int RED = Color.rgb(210, 70, 70);
    private static final int ORANGE = Color.rgb(230, 140, 30);
    private static final int DIM = Color.rgb(150, 160, 170);

    private static final String[] QUICK_ITEMS = {
            "Jasa servis", "Ganti LCD", "Ganti baterai", "Ganti IC eMMC",
            "Flash / software", "Jasa reball", "Ganti konektor cas", "Cek / diagnosa"
    };

    // ---------------------------------------------------------------- model
    static class Item {
        String name = "";
        int qty = 1;
        long price = 0;

        long total() { return (long) qty * price; }
    }

    static class Nota {
        long id;
        String number = "";
        long createdAt;
        String customer = "";
        String phone = "";
        String device = "";
        String complaint = "";
        String note = "";
        long discount = 0;
        long paid = 0;
        int warrantyDays = 0;
        ArrayList<Item> items = new ArrayList<>();

        long subtotal() {
            long s = 0;
            for (Item i : items) s += i.total();
            return s;
        }

        long total() { return Math.max(0, subtotal() - discount); }

        long remaining() { return Math.max(0, total() - paid); }

        String status() {
            if (total() <= 0) return "-";
            if (remaining() == 0) return "LUNAS";
            if (paid > 0) return "DP";
            return "BELUM BAYAR";
        }
    }

    private static class Part {
        String part = "", brand = "", capacity = "", grade = "";
    }

    // ---------------------------------------------------------------- state
    private static final int MODE_LIST = 0;
    private static final int MODE_EDIT = 1;
    private int mode = MODE_LIST;

    private Nota editing;
    private boolean editingIsNew;
    private LinearLayout itemsHost;
    private TextView totalsView;
    private EditText fCustomer, fPhone, fDevice, fComplaint, fNote, fDiscount, fPaid, fWarranty;

    // ---------------------------------------------------------------- siklus hidup
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mode == MODE_EDIT) {
                    confirmDiscard();
                } else {
                    finish();
                }
            }
        });
        showList();
    }

    // ---------------------------------------------------------------- util UI
    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView label(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String s, int bg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(rounded(bg, 12, 0));
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setOnClickListener(l);
        return b;
    }

    private EditText input(String hint, String value, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setInputType(inputType);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(DIM);
        e.setTextSize(15);
        e.setBackground(rounded(FIELD, 10, LINE));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(topDp);
        return p;
    }

    private LinearLayout column(int padDp) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp));
        return c;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private static long num(String s) {
        if (s == null) return 0;
        String d = s.replaceAll("[^0-9]", "");
        if (d.isEmpty()) return 0;
        if (d.length() > 15) d = d.substring(0, 15);
        return Long.parseLong(d);
    }

    static String rp(long v) {
        String s = Long.toString(Math.abs(v));
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) b.append('.');
            b.append(s.charAt(i));
        }
        return (v < 0 ? "-" : "") + "Rp " + b;
    }

    private static String fmtDate(long t) {
        return new SimpleDateFormat("dd MMM yyyy HH:mm", new Locale("id", "ID")).format(new Date(t));
    }

    private static String dayKey(long t) {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date(t));
    }

    private int statusColor(String status) {
        if ("LUNAS".equals(status)) return GREEN;
        if ("DP".equals(status)) return ORANGE;
        return RED;
    }

    // ---------------------------------------------------------------- penyimpanan
    private ArrayList<Nota> loadNotas() {
        ArrayList<Nota> out = new ArrayList<>();
        try {
            String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString("list", "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                out.add(fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void saveNotas(ArrayList<Nota> list) {
        try {
            JSONArray arr = new JSONArray();
            for (Nota n : list) arr.put(toJson(n));
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("list", arr.toString()).apply();
        } catch (Exception e) {
            toast("Gagal menyimpan nota");
        }
    }

    private JSONObject toJson(Nota n) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", n.id);
        o.put("number", n.number);
        o.put("createdAt", n.createdAt);
        o.put("customer", n.customer);
        o.put("phone", n.phone);
        o.put("device", n.device);
        o.put("complaint", n.complaint);
        o.put("note", n.note);
        o.put("discount", n.discount);
        o.put("paid", n.paid);
        o.put("warranty", n.warrantyDays);
        JSONArray items = new JSONArray();
        for (Item i : n.items) {
            JSONObject io = new JSONObject();
            io.put("name", i.name);
            io.put("qty", i.qty);
            io.put("price", i.price);
            items.put(io);
        }
        o.put("items", items);
        return o;
    }

    private Nota fromJson(JSONObject o) throws Exception {
        Nota n = new Nota();
        n.id = o.optLong("id");
        n.number = o.optString("number", "");
        n.createdAt = o.optLong("createdAt");
        n.customer = o.optString("customer", "");
        n.phone = o.optString("phone", "");
        n.device = o.optString("device", "");
        n.complaint = o.optString("complaint", "");
        n.note = o.optString("note", "");
        n.discount = o.optLong("discount");
        n.paid = o.optLong("paid");
        n.warrantyDays = o.optInt("warranty");
        JSONArray items = o.optJSONArray("items");
        if (items != null) {
            for (int k = 0; k < items.length(); k++) {
                JSONObject io = items.getJSONObject(k);
                Item i = new Item();
                i.name = io.optString("name", "");
                i.qty = Math.max(1, io.optInt("qty", 1));
                i.price = io.optLong("price");
                n.items.add(i);
            }
        }
        return n;
    }

    private String nextNumber() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        int c = sp.getInt("counter", 0) + 1;
        sp.edit().putInt("counter", c).apply();
        String d = new SimpleDateFormat("yyMMdd", Locale.US).format(new Date());
        return String.format(Locale.US, "JT-%s-%04d", d, c);
    }

    private SharedPreferences shop() {
        return getSharedPreferences(SHOP_PREFS, MODE_PRIVATE);
    }

    private String shopName() { return shop().getString("name", "JEJAK TEKNISI"); }

    private String shopAddress() { return shop().getString("address", ""); }

    private String shopPhone() { return shop().getString("phone", ""); }

    private String shopFooter() {
        return shop().getString("footer", "Terima kasih atas kepercayaan Anda.");
    }

    // ---------------------------------------------------------------- layar: DAFTAR
    private void showList() {
        mode = MODE_LIST;
        editing = null;

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);

        LinearLayout col = column(14);
        sv.addView(col, new ViewGroup.LayoutParams(-1, -2));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = label("🧾 NOTA SERVIS", 22, Color.WHITE, true);
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(button("⚙ Toko", FIELD, v -> showShopDialog()), new LinearLayout.LayoutParams(-2, -2));
        col.addView(head, lp(-1, -2, 0));

        ArrayList<Nota> all = loadNotas();
        String today = dayKey(System.currentTimeMillis());
        int cnt = 0;
        long sum = 0;
        for (Nota n : all) {
            if (dayKey(n.createdAt).equals(today)) {
                cnt++;
                sum += n.total();
            }
        }
        TextView summary = label("Hari ini: " + cnt + " nota • " + rp(sum)
                + "   |   Total tersimpan: " + all.size(), 12, DIM, false);
        col.addView(summary, lp(-1, -2, 6));

        col.addView(button("＋  NOTA BARU", ACCENT, v -> {
            Nota n = new Nota();
            showEditor(n, true);
        }), lp(-1, dp(52), 12));

        final EditText search = input("Cari nama / nomor / perangkat", "", InputType.TYPE_CLASS_TEXT);
        col.addView(search, lp(-1, -2, 12));

        final LinearLayout host = new LinearLayout(this);
        host.setOrientation(LinearLayout.VERTICAL);
        col.addView(host, lp(-1, -2, 8));
        fillList(host, "");

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                fillList(host, s.toString());
            }
        });

        setContentView(sv);
    }

    private void fillList(LinearLayout host, String query) {
        host.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        ArrayList<Nota> all = loadNotas();
        int shown = 0;
        for (final Nota n : all) {
            String hay = (n.number + " " + n.customer + " " + n.device + " " + n.phone).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !hay.contains(q)) continue;
            shown++;

            LinearLayout card = column(12);
            card.setBackground(rounded(CARD, 14, LINE));
            card.setClickable(true);
            card.setOnClickListener(v -> showActions(n));

            LinearLayout r1 = new LinearLayout(this);
            r1.setOrientation(LinearLayout.HORIZONTAL);
            r1.setGravity(Gravity.CENTER_VERTICAL);
            r1.addView(label(n.number, 14, Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
            String st = n.status();
            TextView chip = label(st, 11, Color.WHITE, true);
            chip.setBackground(rounded(statusColor(st), 8, 0));
            chip.setPadding(dp(8), dp(3), dp(8), dp(3));
            r1.addView(chip, new LinearLayout.LayoutParams(-2, -2));
            card.addView(r1);

            String who = n.customer.isEmpty() ? "Umum" : n.customer;
            if (!n.device.isEmpty()) who += " • " + n.device;
            card.addView(label(who, 13, Color.rgb(220, 226, 232), false), lp(-1, -2, 4));

            LinearLayout r3 = new LinearLayout(this);
            r3.setOrientation(LinearLayout.HORIZONTAL);
            r3.addView(label(fmtDate(n.createdAt), 11, DIM, false), new LinearLayout.LayoutParams(0, -2, 1));
            r3.addView(label(rp(n.total()), 14, Color.WHITE, true), new LinearLayout.LayoutParams(-2, -2));
            card.addView(r3, lp(-1, -2, 4));

            host.addView(card, lp(-1, -2, 8));
        }
        if (shown == 0) {
            TextView empty = label(all.isEmpty()
                    ? "Belum ada nota.\nKetuk “NOTA BARU” untuk membuat."
                    : "Tidak ada nota yang cocok.", 13, DIM, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, dp(30));
            host.addView(empty);
        }
    }

    private void showActions(final Nota n) {
        ArrayList<String> labels = new ArrayList<>();
        final ArrayList<Integer> ids = new ArrayList<>();
        labels.add("Lihat / ubah");            ids.add(0);
        labels.add("Bagikan PDF (WhatsApp dll.)"); ids.add(1);
        labels.add("Cetak");                   ids.add(2);
        labels.add("Kirim sebagai teks");      ids.add(3);
        if (n.remaining() > 0) { labels.add("Tandai lunas"); ids.add(4); }
        labels.add("Duplikat");                ids.add(5);
        labels.add("Hapus");                   ids.add(6);

        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle(n.number)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    switch (ids.get(which)) {
                        case 0: showEditor(n, false); break;
                        case 1: sharePdf(n); break;
                        case 2: printNota(n); break;
                        case 3: shareText(n); break;
                        case 4: markPaid(n); break;
                        case 5: duplicate(n); break;
                        case 6: confirmDelete(n); break;
                        default: break;
                    }
                })
                .show();
    }

    private void markPaid(Nota n) {
        ArrayList<Nota> all = loadNotas();
        for (Nota x : all) {
            if (x.id == n.id) x.paid = x.total();
        }
        saveNotas(all);
        showList();
        toast("Nota ditandai lunas");
    }

    private void duplicate(Nota n) {
        try {
            Nota c = fromJson(toJson(n));
            c.id = System.currentTimeMillis();
            c.number = nextNumber();
            c.createdAt = System.currentTimeMillis();
            c.paid = 0;
            ArrayList<Nota> all = loadNotas();
            all.add(0, c);
            saveNotas(all);
            showList();
            toast("Nota diduplikat: " + c.number);
        } catch (Exception e) {
            toast("Gagal menduplikat");
        }
    }

    private void confirmDelete(final Nota n) {
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Hapus nota?")
                .setMessage(n.number + " akan dihapus permanen.")
                .setPositiveButton("Hapus", (d, w) -> {
                    ArrayList<Nota> all = loadNotas();
                    for (int i = all.size() - 1; i >= 0; i--) {
                        if (all.get(i).id == n.id) all.remove(i);
                    }
                    saveNotas(all);
                    showList();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showShopDialog() {
        LinearLayout box = column(16);
        final EditText name = input("Nama toko", shopName(), InputType.TYPE_CLASS_TEXT);
        final EditText addr = input("Alamat", shopAddress(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        final EditText phone = input("Telp / WA", shopPhone(), InputType.TYPE_CLASS_PHONE);
        final EditText footer = input("Teks bawah nota", shopFooter(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        box.addView(name);
        box.addView(addr, lp(-1, -2, 8));
        box.addView(phone, lp(-1, -2, 8));
        box.addView(footer, lp(-1, -2, 8));
        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Data toko (tampil di nota)")
                .setView(sv)
                .setPositiveButton("Simpan", (d, w) -> {
                    shop().edit()
                            .putString("name", name.getText().toString().trim())
                            .putString("address", addr.getText().toString().trim())
                            .putString("phone", phone.getText().toString().trim())
                            .putString("footer", footer.getText().toString().trim())
                            .apply();
                    toast("Data toko disimpan");
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ---------------------------------------------------------------- layar: EDITOR
    private void showEditor(Nota n, boolean isNew) {
        mode = MODE_EDIT;
        editingIsNew = isNew;
        // Salin agar "Batal" tidak mengubah data tersimpan.
        try {
            editing = fromJson(toJson(n));
        } catch (Exception e) {
            editing = n;
        }

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);

        LinearLayout col = column(14);
        sv.addView(col, new ViewGroup.LayoutParams(-1, -2));

        col.addView(label(isNew ? "🧾 NOTA BARU" : "🧾 " + editing.number, 20, Color.WHITE, true));

        col.addView(sectionTitle("PELANGGAN & PERANGKAT"), lp(-1, -2, 14));
        fCustomer = input("Nama pelanggan", editing.customer,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        fPhone = input("No. HP / WA", editing.phone, InputType.TYPE_CLASS_PHONE);
        fDevice = input("Perangkat (mis. Redmi 9A)", editing.device,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        fComplaint = input("Keluhan / kerusakan", editing.complaint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        col.addView(fCustomer, lp(-1, -2, 6));
        col.addView(fPhone, lp(-1, -2, 8));
        col.addView(fDevice, lp(-1, -2, 8));
        col.addView(fComplaint, lp(-1, -2, 8));

        col.addView(sectionTitle("BARANG & JASA"), lp(-1, -2, 16));
        itemsHost = new LinearLayout(this);
        itemsHost.setOrientation(LinearLayout.VERTICAL);
        col.addView(itemsHost, lp(-1, -2, 6));

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        Button addBtn = button("＋ Tambah item", FIELD, v -> showItemDialog(null, "", () -> refreshItems()));
        Button dbBtn = button("🔎 Dari database eMMC", FIELD, v -> showPartPicker());
        addRow.addView(addBtn, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(0, -2, 1.3f);
        dbLp.leftMargin = dp(8);
        addRow.addView(dbBtn, dbLp);
        col.addView(addRow, lp(-1, -2, 6));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (final String q : QUICK_ITEMS) {
            TextView c = label(q, 12, Color.WHITE, false);
            c.setBackground(rounded(Color.rgb(8, 40, 80), 16, ACCENT));
            c.setPadding(dp(12), dp(6), dp(12), dp(6));
            c.setOnClickListener(v -> showItemDialog(null, q, () -> refreshItems()));
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-2, -2);
            cl.rightMargin = dp(6);
            chips.addView(c, cl);
        }
        hs.addView(chips);
        col.addView(hs, lp(-1, -2, 8));

        col.addView(sectionTitle("PEMBAYARAN"), lp(-1, -2, 16));
        fDiscount = input("Diskon (Rp)", editing.discount > 0 ? Long.toString(editing.discount) : "", InputType.TYPE_CLASS_NUMBER);
        fPaid = input("Dibayar / DP (Rp)", editing.paid > 0 ? Long.toString(editing.paid) : "", InputType.TYPE_CLASS_NUMBER);
        col.addView(fDiscount, lp(-1, -2, 6));
        LinearLayout paidRow = new LinearLayout(this);
        paidRow.setOrientation(LinearLayout.HORIZONTAL);
        paidRow.addView(fPaid, new LinearLayout.LayoutParams(0, -2, 1));
        Button lunas = button("LUNAS", GREEN, v -> {
            collectFields();
            fPaid.setText(Long.toString(editing.total()));
        });
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-2, -2);
        ll.leftMargin = dp(8);
        paidRow.addView(lunas, ll);
        col.addView(paidRow, lp(-1, -2, 8));

        totalsView = label("", 14, Color.WHITE, true);
        totalsView.setBackground(rounded(CARD, 12, LINE));
        totalsView.setPadding(dp(14), dp(12), dp(14), dp(12));
        col.addView(totalsView, lp(-1, -2, 10));

        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refreshTotals(); }
        };
        fDiscount.addTextChangedListener(tw);
        fPaid.addTextChangedListener(tw);

        col.addView(sectionTitle("LAINNYA"), lp(-1, -2, 16));
        fWarranty = input("Garansi (hari, kosongkan bila tidak ada)",
                editing.warrantyDays > 0 ? Integer.toString(editing.warrantyDays) : "", InputType.TYPE_CLASS_NUMBER);
        fNote = input("Catatan (tampil di nota)", editing.note,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        col.addView(fWarranty, lp(-1, -2, 6));
        col.addView(fNote, lp(-1, -2, 8));

        col.addView(button("💾  SIMPAN", ACCENT, v -> saveEditing(false)), lp(-1, dp(52), 18));
        col.addView(button("📤  SIMPAN & BAGIKAN PDF", GREEN, v -> saveEditing(true)), lp(-1, dp(52), 8));
        col.addView(button("Batal", FIELD, v -> confirmDiscard()), lp(-1, -2, 8));

        setContentView(sv);
        refreshItems();
    }

    private TextView sectionTitle(String s) {
        TextView t = label(s, 11, DIM, true);
        t.setLetterSpacing(0.12f);
        return t;
    }

    private void collectFields() {
        if (editing == null || fCustomer == null) return;
        editing.customer = fCustomer.getText().toString().trim();
        editing.phone = fPhone.getText().toString().trim();
        editing.device = fDevice.getText().toString().trim();
        editing.complaint = fComplaint.getText().toString().trim();
        editing.note = fNote.getText().toString().trim();
        editing.discount = num(fDiscount.getText().toString());
        editing.paid = num(fPaid.getText().toString());
        editing.warrantyDays = (int) Math.min(3650, num(fWarranty.getText().toString()));
    }

    private void refreshItems() {
        if (itemsHost == null || editing == null) return;
        itemsHost.removeAllViews();
        if (editing.items.isEmpty()) {
            TextView e = label("Belum ada item. Tambah jasa / barang di bawah.", 12, DIM, false);
            e.setPadding(0, dp(6), 0, dp(6));
            itemsHost.addView(e);
        }
        for (int idx = 0; idx < editing.items.size(); idx++) {
            final Item it = editing.items.get(idx);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(rounded(CARD, 12, LINE));
            row.setPadding(dp(12), dp(8), dp(4), dp(8));
            row.setClickable(true);
            row.setOnClickListener(v -> showItemDialog(it, "", () -> refreshItems()));

            LinearLayout txt = new LinearLayout(this);
            txt.setOrientation(LinearLayout.VERTICAL);
            txt.addView(label(it.name, 14, Color.WHITE, true));
            txt.addView(label(it.qty + " × " + rp(it.price) + " = " + rp(it.total()), 12, DIM, false));
            row.addView(txt, new LinearLayout.LayoutParams(0, -2, 1));

            TextView del = label("✕", 18, DIM, true);
            del.setGravity(Gravity.CENTER);
            del.setOnClickListener(v -> {
                editing.items.remove(it);
                refreshItems();
            });
            row.addView(del, new LinearLayout.LayoutParams(dp(44), dp(44)));

            itemsHost.addView(row, lp(-1, -2, 6));
        }
        refreshTotals();
    }

    private void refreshTotals() {
        if (totalsView == null || editing == null) return;
        long discount = num(fDiscount == null ? "" : fDiscount.getText().toString());
        long paid = num(fPaid == null ? "" : fPaid.getText().toString());
        long sub = editing.subtotal();
        long total = Math.max(0, sub - discount);
        long rest = Math.max(0, total - paid);
        StringBuilder b = new StringBuilder();
        b.append("Subtotal   ").append(rp(sub));
        if (discount > 0) b.append("\nDiskon      -").append(rp(discount));
        b.append("\nTOTAL      ").append(rp(total));
        if (paid > 0) b.append("\nDibayar    ").append(rp(paid));
        if (total > 0) b.append(rest == 0 ? "\n✔ LUNAS" : "\nSisa         " + rp(rest));
        totalsView.setText(b.toString());
    }

    private void saveEditing(boolean thenShare) {
        collectFields();
        if (editing.items.isEmpty()) {
            toast("Tambah minimal satu item");
            return;
        }
        if (editing.customer.isEmpty()) editing.customer = "Umum";

        ArrayList<Nota> all = loadNotas();
        if (editingIsNew || editing.id == 0) {
            editing.id = System.currentTimeMillis();
            editing.number = nextNumber();
            editing.createdAt = System.currentTimeMillis();
            all.add(0, editing);
        } else {
            boolean found = false;
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id == editing.id) {
                    all.set(i, editing);
                    found = true;
                    break;
                }
            }
            if (!found) all.add(0, editing);
        }
        saveNotas(all);
        final Nota saved = editing;
        toast("Tersimpan: " + saved.number);
        showList();
        if (thenShare) sharePdf(saved);
    }

    private void confirmDiscard() {
        new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Keluar tanpa menyimpan?")
                .setMessage("Perubahan pada nota ini akan hilang.")
                .setPositiveButton("Keluar", (d, w) -> showList())
                .setNegativeButton("Lanjut edit", null)
                .show();
    }

    // ---------------------------------------------------------------- dialog item
    private void showItemDialog(final Item existing, String presetName, final Runnable done) {
        LinearLayout box = column(16);
        final EditText name = input("Nama barang / jasa",
                existing != null ? existing.name : presetName,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText qty = input("Jumlah", existing != null ? Integer.toString(existing.qty) : "1", InputType.TYPE_CLASS_NUMBER);
        final EditText price = input("Harga satuan (Rp)",
                existing != null && existing.price > 0 ? Long.toString(existing.price) : "", InputType.TYPE_CLASS_NUMBER);
        box.addView(name);
        box.addView(qty, lp(-1, -2, 8));
        box.addView(price, lp(-1, -2, 8));

        final AlertDialog dlg = new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle(existing == null ? "Tambah item" : "Ubah item")
                .setView(box)
                .setPositiveButton("Simpan", null)
                .setNegativeButton("Batal", null)
                .create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nm = name.getText().toString().trim();
            if (nm.isEmpty()) {
                toast("Nama item masih kosong");
                return;
            }
            Item it = existing != null ? existing : new Item();
            it.name = nm;
            it.qty = (int) Math.max(1, Math.min(9999, num(qty.getText().toString())));
            it.price = num(price.getText().toString());
            if (existing == null) editing.items.add(it);
            dlg.dismiss();
            if (done != null) done.run();
        });
        name.requestFocus();
    }

    // ---------------------------------------------------------------- pemilih eMMC
    private ArrayList<Part> loadParts() {
        LinkedHashMap<String, Part> map = new LinkedHashMap<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(getAssets().open("emmc_database.csv"), "UTF-8"))) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] f = line.split(",", 6);
                if (f.length < 4) continue;
                Part p = new Part();
                p.part = f[0].trim();
                p.brand = clean(f[1]);
                p.capacity = clean(f[2]);
                p.grade = clean(f[3]);
                String key = normalize(p.part);
                if (!key.isEmpty()) map.put(key, p);
            }
        } catch (Exception ignored) {
        }
        try {
            String raw = getSharedPreferences(EMMC_USER_PREFS, MODE_PRIVATE).getString("records", "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Part p = new Part();
                p.part = o.optString("part", "").trim();
                p.brand = clean(o.optString("brand", ""));
                p.capacity = clean(o.optString("capacity", ""));
                p.grade = clean(o.optString("grade", ""));
                String key = normalize(p.part);
                if (!key.isEmpty()) map.put(key, p);
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(map.values());
    }

    private static String clean(String s) {
        String t = s == null ? "" : s.trim();
        return t.equalsIgnoreCase("Tidak dicantumkan") ? "" : t;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String partLine(Part p) {
        StringBuilder b = new StringBuilder(p.part);
        if (!p.brand.isEmpty()) b.append("  •  ").append(p.brand);
        if (!p.capacity.isEmpty()) b.append("  •  ").append(p.capacity);
        if (!p.grade.isEmpty()) b.append("  •  ").append(p.grade);
        return b.toString();
    }

    private void showPartPicker() {
        final ArrayList<Part> all = loadParts();
        final ArrayList<Part> shown = new ArrayList<>();
        final ArrayList<String> lines = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines);

        LinearLayout box = column(12);
        final EditText search = input("Cari kode eMMC (mis. KLMBG)", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        box.addView(search);
        final TextView info = label("", 11, DIM, false);
        box.addView(info, lp(-1, -2, 6));
        final ListView list = new ListView(this);
        list.setAdapter(adapter);
        box.addView(list, lp(-1, dp(320), 4));

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                String q = normalize(search.getText().toString());
                shown.clear();
                lines.clear();
                int matches = 0;
                for (Part p : all) {
                    if (!q.isEmpty() && !(normalize(p.part).contains(q)
                            || normalize(p.brand).contains(q)
                            || normalize(p.grade).contains(q))) continue;
                    matches++;
                    if (shown.size() < 80) {
                        shown.add(p);
                        lines.add(partLine(p));
                    }
                }
                adapter.notifyDataSetChanged();
                info.setText(matches + " kode cocok" + (matches > 80 ? " (tampil 80 pertama, persempit pencarian)" : ""));
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { refresh.run(); }
        });
        refresh.run();

        final AlertDialog dlg = new AlertDialog.Builder(this, DIALOG_THEME)
                .setTitle("Database eMMC")
                .setView(box)
                .setNegativeButton("Tutup", null)
                .create();
        list.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < 0 || pos >= shown.size()) return;
            Part p = shown.get(pos);
            StringBuilder nm = new StringBuilder("eMMC ").append(p.part);
            if (!p.capacity.isEmpty()) nm.append(" ").append(p.capacity);
            if (!p.grade.isEmpty()) nm.append(" • Grade ").append(p.grade);
            dlg.dismiss();
            showItemDialog(null, nm.toString(), () -> refreshItems());
        });
        dlg.show();
    }

    // ---------------------------------------------------------------- teks nota
    private String buildText(Nota n) {
        StringBuilder b = new StringBuilder();
        b.append("*").append(shopName()).append("*\n");
        if (!shopAddress().isEmpty()) b.append(shopAddress()).append("\n");
        if (!shopPhone().isEmpty()) b.append("Telp/WA: ").append(shopPhone()).append("\n");
        b.append("--------------------------\n");
        b.append("NOTA ").append(n.number).append("\n").append(fmtDate(n.createdAt)).append("\n");
        if (!n.customer.isEmpty()) b.append("Pelanggan: ").append(n.customer).append("\n");
        if (!n.device.isEmpty()) b.append("Perangkat: ").append(n.device).append("\n");
        if (!n.complaint.isEmpty()) b.append("Keluhan: ").append(n.complaint).append("\n");
        b.append("--------------------------\n");
        int i = 1;
        for (Item it : n.items) {
            b.append(i++).append(". ").append(it.name).append("\n   ")
                    .append(it.qty).append(" x ").append(rp(it.price)).append(" = ").append(rp(it.total())).append("\n");
        }
        b.append("--------------------------\n");
        if (n.discount > 0) b.append("Diskon: -").append(rp(n.discount)).append("\n");
        b.append("*TOTAL: ").append(rp(n.total())).append("*\n");
        if (n.paid > 0) b.append("Dibayar: ").append(rp(n.paid)).append("\n");
        b.append(n.remaining() == 0 ? "Status: LUNAS\n" : "Sisa: " + rp(n.remaining()) + "\n");
        if (n.warrantyDays > 0) b.append("Garansi: ").append(n.warrantyDays).append(" hari\n");
        if (!n.note.isEmpty()) b.append("Catatan: ").append(n.note).append("\n");
        if (!shopFooter().isEmpty()) b.append("\n").append(shopFooter());
        return b.toString();
    }

    private void shareText(Nota n) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, buildText(n));
        startActivity(Intent.createChooser(i, "Kirim nota"));
    }

    // ---------------------------------------------------------------- PDF
    private File buildPdf(Nota n) throws IOException {
        NotaPdf w = new NotaPdf(n);
        return w.build();
    }

    private class NotaPdf {
        final int W = 420, H = 595, M = 28;
        final Nota n;
        final PdfDocument doc = new PdfDocument();
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        PdfDocument.Page page;
        Canvas c;
        int pageNo = 0;
        float y;
        boolean inTable = false;
        Bitmap logo;

        // kolom tabel
        final float xNo = M, xName = M + 22, nameW = 165, xQty = 228, xPriceR = 308, xTotalR = W - M;

        NotaPdf(Nota n) {
            this.n = n;
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 4;
                logo = BitmapFactory.decodeResource(getResources(), R.drawable.deroid_unbricker_nota_logo, o);
            } catch (Throwable ignored) {
                logo = null;
            }
        }

        File build() throws IOException {
            newPage();
            drawBody();
            finishPage();
            File dir = new File(getCacheDir(), "nota");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Folder cache nota gagal dibuat");
            File f = new File(dir, n.number.replaceAll("[^A-Za-z0-9\\-]", "_") + ".pdf");
            try (OutputStream os = new FileOutputStream(f)) {
                doc.writeTo(os);
            } finally {
                doc.close();
            }
            return f;
        }

        void style(float size, boolean bold, int color, Paint.Align align) {
            p.setTextSize(size);
            p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            p.setColor(color);
            p.setTextAlign(align);
        }

        void text(String s, float x, float size, boolean bold, int color, Paint.Align align) {
            style(size, bold, color, align);
            c.drawText(s, x, y, p);
        }

        void hline(float yy, int color, float width) {
            p.setColor(color);
            p.setStrokeWidth(width);
            c.drawLine(M, yy, W - M, yy, p);
        }

        List<String> wrap(String s, float maxW, float size, boolean bold) {
            style(size, bold, Color.BLACK, Paint.Align.LEFT);
            ArrayList<String> out = new ArrayList<>();
            String[] paras = (s == null ? "" : s).split("\n");
            for (String para : paras) {
                String rest = para.trim();
                if (rest.isEmpty()) { out.add(""); continue; }
                while (!rest.isEmpty()) {
                    int cnt = p.breakText(rest, true, maxW, null);
                    if (cnt <= 0) cnt = 1;
                    if (cnt >= rest.length()) { out.add(rest); break; }
                    int cut = rest.lastIndexOf(' ', cnt);
                    if (cut <= 0) cut = cnt;
                    out.add(rest.substring(0, cut).trim());
                    rest = rest.substring(cut).trim();
                }
            }
            if (out.isEmpty()) out.add("");
            return out;
        }

        void newPage() {
            if (page != null) finishPage();
            pageNo++;
            page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
            c = page.getCanvas();
            y = M;
            if (pageNo == 1) drawHeader(); else drawContinuedHeader();
            if (inTable) drawTableHeader();
        }

        void finishPage() {
            if (page == null) return;
            y = H - 14;
            text("Hal. " + pageNo, W / 2f, 7.5f, false, Color.GRAY, Paint.Align.CENTER);
            doc.finishPage(page);
            page = null;
        }

        void ensureSpace(float h) {
            if (y + h > H - M - 10) newPage();
        }

        void drawHeader() {
            float textX = M;
            if (logo != null) {
                c.drawBitmap(logo, null, new RectF(M, M, M + 40, M + 40), p);
                textX = M + 50;
            }
            y = M + 14;
            text(shopName(), textX, 15, true, Color.BLACK, Paint.Align.LEFT);
            float yy = y + 11;
            if (!shopAddress().isEmpty()) {
                for (String ln : wrap(shopAddress(), W - M - textX, 8, false)) {
                    y = yy;
                    text(ln, textX, 8, false, Color.DKGRAY, Paint.Align.LEFT);
                    yy += 10;
                }
            }
            if (!shopPhone().isEmpty()) {
                y = yy;
                text("Telp/WA: " + shopPhone(), textX, 8, false, Color.DKGRAY, Paint.Align.LEFT);
                yy += 10;
            }
            y = Math.max(M + 46, yy) + 4;
            hline(y, Color.BLACK, 1.2f);

            y += 18;
            text("NOTA SERVIS", M, 12, true, Color.BLACK, Paint.Align.LEFT);
            text(n.number, W - M, 10, true, Color.BLACK, Paint.Align.RIGHT);
            y += 12;
            text("Tanggal: " + fmtDate(n.createdAt), M, 8.5f, false, Color.DKGRAY, Paint.Align.LEFT);
            y += 6;

            infoLine("Pelanggan", n.customer);
            infoLine("No. HP", n.phone);
            infoLine("Perangkat", n.device);
            infoLine("Keluhan", n.complaint);
            y += 8;
        }

        void infoLine(String label, String value) {
            if (value == null || value.trim().isEmpty()) return;
            List<String> lines = wrap(value, W - 2 * M - 60, 9, false);
            for (int i = 0; i < lines.size(); i++) {
                y += 11;
                if (i == 0) text(label, M, 9, true, Color.BLACK, Paint.Align.LEFT);
                text(i == 0 ? ": " + lines.get(i) : lines.get(i), M + 56, 9, false, Color.BLACK, Paint.Align.LEFT);
            }
        }

        void drawContinuedHeader() {
            y = M + 6;
            text(shopName() + " — Nota " + n.number + " (lanjutan)", M, 9, true, Color.BLACK, Paint.Align.LEFT);
            y += 6;
            hline(y, Color.BLACK, 1f);
            y += 10;
        }

        void drawTableHeader() {
            p.setColor(Color.rgb(225, 228, 232));
            c.drawRect(M, y, W - M, y + 16, p);
            y += 11.5f;
            text("No", xNo + 2, 8.5f, true, Color.BLACK, Paint.Align.LEFT);
            text("Barang / Jasa", xName, 8.5f, true, Color.BLACK, Paint.Align.LEFT);
            text("Qty", xQty, 8.5f, true, Color.BLACK, Paint.Align.CENTER);
            text("Harga", xPriceR, 8.5f, true, Color.BLACK, Paint.Align.RIGHT);
            text("Jumlah", xTotalR - 2, 8.5f, true, Color.BLACK, Paint.Align.RIGHT);
            y += 4.5f;
        }

        void drawBody() {
            inTable = true;
            drawTableHeader();
            int no = 1;
            for (Item it : n.items) {
                List<String> lines = wrap(it.name, nameW, 9, false);
                if (lines.size() > 5) lines = lines.subList(0, 5);
                float rowH = lines.size() * 11f + 5f;
                ensureSpace(rowH);
                float base = y + 10;
                y = base;
                text(Integer.toString(no++), xNo + 2, 9, false, Color.BLACK, Paint.Align.LEFT);
                text(Integer.toString(it.qty), xQty, 9, false, Color.BLACK, Paint.Align.CENTER);
                text(rp(it.price), xPriceR, 9, false, Color.BLACK, Paint.Align.RIGHT);
                text(rp(it.total()), xTotalR - 2, 9, false, Color.BLACK, Paint.Align.RIGHT);
                for (int i = 0; i < lines.size(); i++) {
                    y = base + i * 11f;
                    text(lines.get(i), xName, 9, false, Color.BLACK, Paint.Align.LEFT);
                }
                y = base - 10 + rowH;
                hline(y, Color.rgb(205, 208, 212), 0.5f);
            }
            inTable = false;

            ensureSpace(170);
            y += 6;
            float lx = W - M - 190;
            y += 12;
            text("Subtotal", lx, 9, false, Color.BLACK, Paint.Align.LEFT);
            text(rp(n.subtotal()), W - M, 9, false, Color.BLACK, Paint.Align.RIGHT);
            if (n.discount > 0) {
                y += 12;
                text("Diskon", lx, 9, false, Color.BLACK, Paint.Align.LEFT);
                text("-" + rp(n.discount), W - M, 9, false, Color.BLACK, Paint.Align.RIGHT);
            }
            y += 6;
            hline(y, Color.BLACK, 0.8f);
            y += 15;
            text("TOTAL", lx, 12, true, Color.BLACK, Paint.Align.LEFT);
            text(rp(n.total()), W - M, 12, true, Color.BLACK, Paint.Align.RIGHT);
            if (n.paid > 0) {
                y += 13;
                text("Dibayar", lx, 9, false, Color.BLACK, Paint.Align.LEFT);
                text(rp(n.paid), W - M, 9, false, Color.BLACK, Paint.Align.RIGHT);
            }
            if (n.total() > 0 && n.remaining() > 0) {
                y += 13;
                text("Sisa", lx, 9, true, Color.BLACK, Paint.Align.LEFT);
                text(rp(n.remaining()), W - M, 9, true, Color.BLACK, Paint.Align.RIGHT);
            }

            // Stempel status di kiri total
            if (n.total() > 0) {
                boolean lunas = n.remaining() == 0;
                String st = lunas ? "LUNAS" : (n.paid > 0 ? "DP" : "BELUM BAYAR");
                int col = lunas ? Color.rgb(20, 130, 60) : Color.rgb(190, 60, 60);
                style(13, true, col, Paint.Align.LEFT);
                float tw = p.measureText(st);
                float sy = y - 14;
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.5f);
                c.drawRect(M, sy - 14, M + tw + 16, sy + 6, p);
                p.setStyle(Paint.Style.FILL);
                c.drawText(st, M + 8, sy, p);
            }

            y += 16;
            if (n.warrantyDays > 0) {
                y += 11;
                text("Garansi: " + n.warrantyDays + " hari sejak tanggal nota.", M, 9, true, Color.BLACK, Paint.Align.LEFT);
            }
            if (n.note != null && !n.note.trim().isEmpty()) {
                for (String ln : wrap("Catatan: " + n.note, W - 2 * M, 8.5f, false)) {
                    ensureSpace(12);
                    y += 11;
                    text(ln, M, 8.5f, false, Color.DKGRAY, Paint.Align.LEFT);
                }
            }

            ensureSpace(95);
            y += 20;
            float sx1 = M + 70, sx2 = W - M - 70;
            y += 8;
            text("Pelanggan", sx1, 8.5f, false, Color.BLACK, Paint.Align.CENTER);
            text("Teknisi", sx2, 8.5f, false, Color.BLACK, Paint.Align.CENTER);
            y += 40;
            p.setColor(Color.BLACK);
            p.setStrokeWidth(0.6f);
            c.drawLine(sx1 - 45, y, sx1 + 45, y, p);
            c.drawLine(sx2 - 45, y, sx2 + 45, y, p);

            String foot = shopFooter();
            if (!foot.isEmpty()) {
                y += 16;
                for (String ln : wrap(foot, W - 2 * M, 8, false)) {
                    ensureSpace(11);
                    text(ln, W / 2f, 8, false, Color.GRAY, Paint.Align.CENTER);
                    y += 10;
                }
            }
        }
    }

    // ---------------------------------------------------------------- bagikan & cetak
    private void sharePdf(Nota n) {
        try {
            File f = buildPdf(n);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, "Nota " + n.number);
            i.putExtra(Intent.EXTRA_TEXT, "Nota " + n.number + " • Total " + rp(n.total()));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Bagikan nota"));
        } catch (Exception e) {
            toast("Gagal membuat PDF: " + e.getMessage());
        }
    }

    private void printNota(Nota n) {
        try {
            final File f = buildPdf(n);
            final String job = "Nota " + n.number;
            PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (pm == null) {
                toast("Layanan cetak tidak tersedia");
                return;
            }
            pm.print(job, new PrintDocumentAdapter() {
                @Override
                public void onLayout(PrintAttributes oldAttrs, PrintAttributes newAttrs,
                                     CancellationSignal cancel, LayoutResultCallback callback, Bundle extras) {
                    if (cancel.isCanceled()) {
                        callback.onLayoutCancelled();
                        return;
                    }
                    PrintDocumentInfo info = new PrintDocumentInfo.Builder(job)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build();
                    callback.onLayoutFinished(info, true);
                }

                @Override
                public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                    CancellationSignal cancel, WriteResultCallback callback) {
                    try (InputStream in = new FileInputStream(f);
                         OutputStream out = new FileOutputStream(destination.getFileDescriptor())) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
                        callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                    } catch (Exception e) {
                        callback.onWriteFailed(e.getMessage());
                    }
                }
            }, null);
        } catch (Exception e) {
            toast("Gagal mencetak: " + e.getMessage());
        }
    }
}
