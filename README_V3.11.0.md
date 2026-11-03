# Deroid Unbricker Nota Microscope V3.11.0 — NOTA SERVIS

Tombol **🧾 Nota** baru di bar atas kamera (di sebelah Galeri).

## Fitur nota
- Daftar nota + pencarian (nomor, nama, perangkat), ringkasan hari ini.
- Nota baru: pelanggan, no. HP, perangkat, keluhan, item barang/jasa (qty × harga), diskon, DP / dibayar, tombol LUNAS, garansi (hari), catatan.
- **Dari database eMMC**: cari kode di `emmc_database.csv` (plus kode buatan sendiri) lalu langsung jadi item nota lengkap dengan kapasitas dan grade.
- Chip cepat: Jasa servis, Ganti LCD, Ganti baterai, Ganti IC eMMC, Flash/software, Jasa reball, dst.
- Nomor otomatis `JT-yyMMdd-0001`.
- **PDF** (A5, ada logo, kolom tanda tangan pelanggan/teknisi, stempel LUNAS/DP/BELUM BAYAR, otomatis lanjut halaman).
- **Bagikan PDF** (WhatsApp dll.), **Cetak**, atau **Kirim sebagai teks**.
- Aksi per nota: lihat/ubah, tandai lunas, duplikat, hapus.
- ⚙ Toko: nama, alamat, telp/WA, teks bawah nota.

## File yang berubah
- BARU `app/src/main/java/com/jejakteknisi/magnifier/NotaActivity.java`
- `MainActivity.java`: tombol Nota di bar atas (5 baris).
- `AndroidManifest.xml`: NotaActivity + FileProvider (`${applicationId}.fileprovider`).
- `res/xml/file_paths.xml`: folder cache `nota/`.
- `app/build.gradle`: versi 3.11.0 (code 3125).

Tidak ada dependensi baru. Data nota disimpan di SharedPreferences (`nota_v1`).
