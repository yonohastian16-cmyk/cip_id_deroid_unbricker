# Jejak Teknisi Microscope V3.10.20

## Database eMMC — kamera + pencarian + data teknisi

Perubahan utama:
- Database eMMC sekarang tetap berada di atas kamera LIVE.
- Kamera tidak dimatikan saat Database dibuka.
- Saat Database dibuka, autofocus awal diarahkan ke area tengah-atas preview, tempat tulisan chip biasanya berada.
- Database menggunakan satu kolom Search; Voice Search tidak digunakan.
- Tidak ada logo khusus di halaman Database.
- Jika kode tidak ditemukan, tersedia tombol **Tambahkan** dengan kode pencarian sebagai isian awal.
- Data baru dapat disimpan langsung dari aplikasi.
- Data yang sudah ada dapat dibuka lalu **Edit** dan disimpan kembali.
- Data buatan teknisi disimpan lokal menggunakan SharedPreferences sehingga tetap tersedia setelah aplikasi ditutup.
- Data lokal menimpa data asset bila Part Number yang sama diedit.
- Fitur Microscope, Freeze, Editor, Crop, Teks, Select, Jumper, Undo/Redo, Simpan dan Share tidak diubah.

## Catatan
Versi ini fokus pada struktur dan alur Database. Pembacaan tulisan otomatis/OCR dari kamera belum dijadikan pemicu autofocus; autofocus awal menggunakan titik fokus area chip agar perubahan tetap kecil dan aman.
