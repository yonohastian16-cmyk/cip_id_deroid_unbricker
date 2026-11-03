# Jejak Teknisi Microscope V3.10.11

## Pilih PRO
- Seleksi memakai border cyan solid tipis dengan halo gelap.
- Tidak memakai garis putih putus-putus.
- Tidak memakai handle kuning.
- Handle visual kecil, area sentuh tetap besar.
- Handle resize: putih + outline cyan.
- Handle rotasi: ring cyan + pusat putih.
- Endpoint Jumper A/B saat objek dipilih menggunakan cyan.
- Tap objek lain berpindah seleksi; tap area kosong melepas seleksi.
- Transform drag/resize/rotate, Layer, Undo/Redo dipertahankan.
- Overlay seleksi hanya tampilan editor dan tidak ikut hasil Simpan/Share.

## Build fix
- Memperbaiki compile error pada pemanggilan `dp(1.5f)`. Helper `dp()` menerima integer, sehingga stroke seleksi menggunakan `dp(2)`.
- versionCode: 3111
- versionName: 3.10.11
