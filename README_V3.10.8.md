# Jejak Teknisi Microscope V3.10.9 — Editor PRO Stability

Perubahan dari V3.10.7:
- Memperbaiki Undo/Redo transform objek: resize handle dan rotation handle sekarang benar-benar menyimpan snapshot sebelum perubahan.
- Rotasi bebas dinormalisasi ke rentang -180° sampai +180° agar nilai rotasi tidak terus membesar setelah banyak gesture.
- Rotasi 2-jari juga memakai normalisasi yang sama.
- Perbaikan kecil pada persiapan history OCR agar selalu membuat satu snapshot Undo yang konsisten.
- VersionCode 3108 / VersionName 3.10.8.
- Nama artifact GitHub Actions diperbarui menjadi jejak-teknisi-microscope-v3.10.8-debug.

Fitur V3.10.7 tetap dipertahankan: Text PRO, Bold/normal, background teks, rotasi, Layer, Copy, Undo/Redo, Jumper PRO, Crop PRO, serta Simpan/Share dengan seluruh anotasi.

Build target: Gradle 8.9 + JDK 17 + compileSdk 35.
