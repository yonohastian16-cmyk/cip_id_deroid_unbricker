# Jejak Teknisi Microscope V3.3.2

Editor selection alignment fix. Selection bounds are calculated from actual annotation geometry in source coordinates, transformed with the same frozen image matrix, and are no longer clipped to the View. Hit testing is geometry-aware for pen, arrow, line, highlight, jumper, circle, rectangle, text, marker and OCR.

Goal: yellow handles and the white dashed selection box stay on the exact annotation while zooming/panning.
