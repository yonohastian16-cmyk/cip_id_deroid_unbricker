# Jejak Teknisi — eMMC Database V1

Fitur:
- Tombol eMMC DB di header tanpa mengganti halaman microscope.
- Kamera tetap LIVE.
- Cari part number secara offline.
- Voice untuk memasukkan kode.
- Scan OCR mengambil foto sementara ke cache; tidak masuk Galeri.
- Hasil: brand, kapasitas, grade, catatan.
- Database ada di `app/src/main/assets/emmc_database.csv`.

Format CSV:
`part_number,brand,capacity,grade,notes`

Data di CSV adalah starter/demo dan sebaiknya diganti dengan database eMMC milik teknisi yang sudah diverifikasi.
