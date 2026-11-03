# Jejak Teknisi Microscope V3.8.5

Pengembangan dari baseline V3.8.4.

## Fokus V3.8.5
- Zoom LIVE tetap halus dengan pinch 2 jari.
- Nilai zoom terakhir diingat saat preview LIVE dibuat ulang.
- Slider Zoom tetap sinkron setelah pinch zoom.
- Zoom Freeze tetap sinkron dengan slider setelah pinch zoom.
- Exposure CameraX tetap menggunakan rentang exposure asli kamera dan nilai terakhir dipulihkan saat LIVE dibuat ulang.
- Detail dibuat lebih jujur secara UI: slider memilih `Detail normal` atau `Ultra Detail`, bukan menampilkan persentase palsu.
- Panel LIVE tetap auto-hide agar area preview kamera lebih luas.
- Bottom sheet tetap dapat digeser dan snap dengan animasi.
- Simpan dan Share tetap selalu terlihat di editor.
- Hasil Simpan/Share tetap berupa bitmap flatten yang membawa objek edit.

## Catatan build
Workflow GitHub Actions menggunakan JDK 17 + Gradle 8.9 dan tidak menggunakan `android-actions/setup-android@v3` / `sdkmanager tools`.
