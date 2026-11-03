V3.2.8 • Jumper Tool + Pinch Zoom

# Jejak Teknisi Microscope V3.2

Tahap V3.2 fokus pada Editor Gambar teknisi.

Alur utama:
- Microscope LIVE -> BEKU -> Editor
- Galeri -> pilih foto -> Editor

Editor mencakup zoom/pan, pilih objek, titik/marker, panah, garis, lingkaran, kotak, highlight, teks, OCR, undo/redo, duplikat, hapus semua, crop, putar, pengaturan brightness/contrast/sharpen, reset tampilan, simpan dan share.

Mode Jumper tersedia di Editor; jalur jumper dapat digambar dari pad awal ke pad tujuan dan mengikuti zoom/pan gambar.

Catatan build: proyek menggunakan Android Gradle Plugin/AndroidX dan workflow GitHub Actions yang sudah ada di repository.


## V3.2.1 – Editor Profesional
- Referensi desain mengikuti mockup Jejak Teknisi yang disepakati.
- Senter/Lampu tetap berada di tampilan awal Microscope (LIVE).
- Edit hanya dimulai dari FREEZE atau Galeri.
- Editor menambahkan Pen bebas berbasis objek, bersama marker, panah, garis, lingkaran, kotak, highlight, teks, OCR, undo/redo, crop, rotate, adjustment, simpan dan share.
- Marking tetap berada pada koordinat gambar sehingga mengikuti zoom/pan.


### V3.2.2 – Jumper Mode
- Tombol JUMPER ditambahkan ke toolbar Editor.
- Mode JUMPER menggambar jalur dengan titik pad di kedua ujung.
- Jalur mengikuti transformasi zoom/pan gambar seperti anotasi lainnya.


V3.2.7 LIVE UI: only the top/header and adjustment panels auto-hide after inactivity.
The bottom action bar (Lampu, Beku, Foto, Fokus, Grid and +/-) stays visible at all times.
Tapping the microscope preview reveals the top controls; tapping bottom controls never reveals the top UI. LIVE keeps the screen awake.


## V3.3.0 Editor transform
- Semua anotasi disimpan dalam koordinat gambar/source.
- Zoom dan pan memakai Matrix yang sama untuk gambar dan anotasi.
- Ukuran visual tanda ikut membesar/mengecil saat zoom.
- Saat gambar digeser kiri/kanan/atas/bawah, semua tanda ikut bergerak.
- Repaint anotasi dipicu setiap perubahan transform.
