# Jejak Teknisi Microscope V3.3.0

## Editor: Select & Transform
- Semua anotasi disimpan pada koordinat gambar/source, lalu dirender memakai transform gambar yang sama.
- Zoom in/out dan pan pada Freeze mempertahankan posisi relatif tanda terhadap PCB.
- Mode Pilih menampilkan bounding box dan 8 handle + rotation handle.
- Objek terpilih dapat digeser; stroke Pen juga ikut berpindah sebagai satu objek.
- Multi-touch pada objek terpilih mempertahankan rotasi dan ukuran visual.
- Tool dan layout LIVE sebelumnya dipertahankan.

## Build
Gradle wrapper/project mengikuti workflow GitHub Actions yang sudah digunakan sebelumnya.
