#!/usr/bin/env python3
"""
Generates valid, professional Fastlane metadata images and Android launcher icons:
- fastlane/metadata/android/en-US/images/icon.png (512x512)
- metadata/com.aistudio.fivegguardian.qywtpa/en-US/images/icon.png (512x512)
- All mipmap density launcher icons (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- Phone screenshots 1, 2, 3
"""

import math
import os
import struct
import zlib

def write_png(filename, width, height, rgba_data):
    def make_chunk(chunk_type, data):
        return struct.pack('>I', len(data)) + chunk_type + data + struct.pack('>I', zlib.crc32(chunk_type + data) & 0xffffffff)

    header = b'\x89PNG\r\n\x1a\n'
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    ihdr_chunk = make_chunk(b'IHDR', ihdr)
    
    raw = bytearray()
    row_bytes = width * 4
    for y in range(height):
        raw.append(0) # Filter None
        raw.extend(rgba_data[y*row_bytes : (y+1)*row_bytes])
        
    compressed = zlib.compress(bytes(raw), 6)
    idat_chunk = make_chunk(b'IDAT', compressed)
    iend_chunk = make_chunk(b'IEND', b'')
    
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, 'wb') as f:
        f.write(header + ihdr_chunk + idat_chunk + iend_chunk)

def dist_segment(px, py, ax, ay, bx, by):
    dx = bx - ax
    dy = by - ay
    l2 = dx*dx + dy*dy
    if l2 == 0:
        return math.hypot(px - ax, py - ay)
    t = max(0.0, min(1.0, ((px - ax)*dx + (py - ay)*dy) / l2))
    proj_x = ax + t * dx
    proj_y = ay + t * dy
    return math.hypot(px - proj_x, py - proj_y)

def point_in_tri(px, py, x1, y1, x2, y2, x3, y3):
    d1 = (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2)
    d2 = (px - x3) * (y2 - y3) - (x2 - x3) * (py - y3)
    d3 = (px - x1) * (y3 - y1) - (x3 - x1) * (py - y1)
    has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (has_neg and has_pos)

def generate_icon(path, size=512, is_round=False):
    buf = bytearray(size * size * 4)
    for y in range(size):
        ny = y / size
        row_offset = y * size * 4
        for x in range(size):
            nx = x / size
            idx = row_offset + x * 4

            if is_round:
                d_round = math.hypot(nx - 0.5, ny - 0.5)
                if d_round > 0.485:
                    buf[idx+3] = 0
                    continue

            # 1. Background gradient (Midnight Blue to Deep Navy)
            t_bg = (nx + ny) / 2.0
            r = int(12 * (1 - t_bg) + 6 * t_bg)
            g = int(24 * (1 - t_bg) + 12 * t_bg)
            b = int(48 * (1 - t_bg) + 26 * t_bg)
            a = 255

            # Ambient radial light behind shield
            d_glow = math.hypot(nx - 0.5, ny - 0.48)
            if d_glow < 0.45:
                glow = ((0.45 - d_glow) / 0.45) ** 2.0
                r = min(255, int(r + 35 * glow))
                g = min(255, int(g + 130 * glow))
                b = min(255, int(b + 210 * glow))

            # 2. Celestial Energy Halo above shield
            dx_h = (nx - 0.5) / 0.18
            dy_h = (ny - 0.22) / 0.045
            d_h = math.hypot(dx_h, dy_h)
            if abs(d_h - 1.0) < 0.32:
                h_weight = (1.0 - abs(d_h - 1.0) / 0.32) ** 1.4
                # blend cyan -> white -> amber
                t_halo = max(0.0, min(1.0, (nx - 0.32) / 0.36))
                hr = int(56 * (1 - t_halo) + 251 * t_halo)
                hg = int(189 * (1 - t_halo) + 146 * t_halo)
                hb = int(248 * (1 - t_halo) + 60 * t_halo)
                r = min(255, int(r * (1 - h_weight) + hr * h_weight))
                g = min(255, int(g * (1 - h_weight) + hg * h_weight))
                b = min(255, int(b * (1 - h_weight) + hb * h_weight))

            # 3. Cyber Shield shape
            top_y = 0.26 + 0.032 * (abs(nx - 0.5) / 0.24)
            w_max = 0.235
            if ny < 0.52:
                cur_w = w_max
            else:
                cur_w = w_max * max(0.0, 1.0 - ((ny - 0.52) / 0.29) ** 1.35)

            dx = abs(nx - 0.5)
            if (ny >= top_y) and (ny <= 0.81) and (dx <= cur_w):
                edge_d = min(cur_w - dx, ny - top_y, (0.81 - ny) * 0.75)

                if edge_d < 0.022:
                    # Outer Ice-Cyan / Crisp White Border
                    r, g, b = 226, 242, 254
                elif edge_d < 0.052:
                    # Neon Vibrant Safety Orange Accent Rim
                    t_or = (edge_d - 0.022) / 0.030
                    r = int(249 * (1 - t_or) + 234 * t_or)
                    g = int(115 * (1 - t_or) + 88 * t_or)
                    b = int(22 * (1 - t_or) + 12 * t_or)
                else:
                    # Inner Shield Core
                    if nx < 0.5:
                        r, g, b = 9, 18, 36
                    else:
                        r, g, b = 21, 36, 62
                    if abs(nx - 0.5) < 0.004:
                        r, g, b = 70, 115, 175

                    # 4. "5G" Typography inside inner core
                    in_5 = False
                    # Top bar
                    if 0.33 <= nx <= 0.45 and 0.44 <= ny <= 0.47: in_5 = True
                    # Left vertical
                    if 0.33 <= nx <= 0.375 and 0.47 <= ny <= 0.525: in_5 = True
                    # Middle bar
                    if 0.33 <= nx <= 0.45 and 0.525 <= ny <= 0.555: in_5 = True
                    # Right loop
                    if 0.41 <= nx <= 0.455 and 0.53 <= ny <= 0.615: in_5 = True
                    # Bottom bar
                    if 0.33 <= nx <= 0.455 and 0.595 <= ny <= 0.625: in_5 = True

                    in_g = False
                    # Top arc
                    if 0.54 <= nx <= 0.665 and 0.44 <= ny <= 0.47: in_g = True
                    # Left spine
                    if 0.54 <= nx <= 0.585 and 0.46 <= ny <= 0.61: in_g = True
                    # Bottom arc
                    if 0.54 <= nx <= 0.665 and 0.595 <= ny <= 0.625: in_g = True
                    # Right lower
                    if 0.625 <= nx <= 0.665 and 0.53 <= ny <= 0.61: in_g = True
                    # Spur
                    if 0.59 <= nx <= 0.665 and 0.53 <= ny <= 0.56: in_g = True

                    if in_5 or in_g:
                        r, g, b = 56, 189, 248

            # 5. Dynamic Speed Lightning Arrow cutting across diagonally
            # Shaft from (0.41, 0.72) to (0.71, 0.32)
            d_shaft = dist_segment(nx, ny, 0.41, 0.72, 0.71, 0.32)
            if d_shaft < 0.016:
                if d_shaft < 0.007:
                    r, g, b = 255, 255, 255
                else:
                    r, g, b = 56, 189, 248

            # Arrowhead tip at (0.76, 0.25)
            in_arrow_head = point_in_tri(nx, ny, 0.76, 0.25, 0.67, 0.29, 0.72, 0.36)
            if in_arrow_head:
                d_tip = math.hypot(nx - 0.76, ny - 0.25)
                if d_tip < 0.035:
                    r, g, b = 255, 255, 255
                else:
                    r, g, b = 56, 189, 248

            buf[idx] = r
            buf[idx+1] = g
            buf[idx+2] = b
            buf[idx+3] = a

    write_png(path, size, size, buf)
    print(f"Generated Icon: {path} ({size}x{size})")

def draw_rect(buf, w, h, x1, y1, x2, y2, color):
    r, g, b, a = color
    x1, x2 = max(0, min(x1, x2)), min(w, max(x1, x2))
    y1, y2 = max(0, min(y1, y2)), min(h, max(y1, y2))
    for y in range(y1, y2):
        row_offset = y * w * 4
        for x in range(x1, x2):
            idx = row_offset + x * 4
            buf[idx] = r
            buf[idx+1] = g
            buf[idx+2] = b
            buf[idx+3] = a

def draw_rounded_rect(buf, w, h, x1, y1, x2, y2, radius, color):
    r, g, b, a = color
    x1, x2 = max(0, min(x1, x2)), min(w, max(x1, x2))
    y1, y2 = max(0, min(y1, y2)), min(h, max(y1, y2))
    r2 = radius * radius
    for y in range(y1, y2):
        row_offset = y * w * 4
        for x in range(x1, x2):
            dx, dy = 0, 0
            if x < x1 + radius:
                dx = (x1 + radius) - x
            elif x > x2 - radius:
                dx = x - (x2 - radius)
            if y < y1 + radius:
                dy = (y1 + radius) - y
            elif y > y2 - radius:
                dy = y - (y2 - radius)
                
            if dx > 0 and dy > 0:
                if dx*dx + dy*dy > r2:
                    continue
            idx = row_offset + x * 4
            buf[idx] = r
            buf[idx+1] = g
            buf[idx+2] = b
            buf[idx+3] = a

def generate_screenshot(path, screen_type):
    w, h = 1080, 2400
    buf = bytearray(w * h * 4)
    draw_rect(buf, w, h, 0, 0, w, h, (15, 23, 42, 255))
    draw_rect(buf, w, h, 0, 0, w, 100, (11, 19, 34, 255))
    draw_rounded_rect(buf, w, h, 920, 35, 990, 65, 8, (148, 163, 184, 255))
    draw_rect(buf, w, h, 80, 40, 180, 60, (241, 245, 249, 255))

    draw_rounded_rect(buf, w, h, 50, 130, 140, 220, 45, (30, 58, 138, 255))
    draw_rect(buf, w, h, 170, 145, 600, 185, (248, 250, 252, 255))
    draw_rect(buf, w, h, 170, 195, 780, 215, (148, 163, 184, 255))

    if screen_type == 1:
        draw_rounded_rect(buf, w, h, 50, 260, 1030, 680, 36, (6, 78, 59, 255))
        draw_rounded_rect(buf, w, h, 60, 270, 1020, 670, 30, (4, 120, 87, 255))
        draw_rect(buf, w, h, 100, 320, 450, 370, (209, 250, 229, 255))
        draw_rect(buf, w, h, 100, 400, 750, 500, (255, 255, 255, 255))
        draw_rect(buf, w, h, 100, 530, 620, 570, (167, 243, 208, 255))
        draw_rounded_rect(buf, w, h, 100, 590, 380, 640, 25, (6, 95, 70, 255))

        draw_rounded_rect(buf, w, h, 50, 720, 1030, 1240, 36, (30, 41, 59, 255))
        draw_rect(buf, w, h, 100, 760, 500, 800, (241, 245, 249, 255))
        draw_rounded_rect(buf, w, h, 100, 830, 520, 930, 24, (56, 189, 248, 255))
        draw_rounded_rect(buf, w, h, 550, 830, 980, 930, 24, (51, 65, 85, 255))

        draw_rect(buf, w, h, 100, 980, 450, 1015, (203, 213, 225, 255))
        draw_rounded_rect(buf, w, h, 100, 1040, 980, 1070, 15, (51, 65, 85, 255))
        draw_rounded_rect(buf, w, h, 100, 1040, 780, 1070, 15, (56, 189, 248, 255))

        draw_rounded_rect(buf, w, h, 50, 1280, 1030, 1420, 36, (225, 29, 72, 255))
        draw_rect(buf, w, h, 350, 1330, 730, 1370, (255, 255, 255, 255))

        draw_rounded_rect(buf, w, h, 50, 1460, 1030, 2200, 36, (30, 41, 59, 255))
        draw_rect(buf, w, h, 100, 1510, 450, 1545, (148, 163, 184, 255))
        for i in range(5):
            y_log = 1580 + i * 110
            draw_rect(buf, w, h, 100, y_log, 950, y_log + 35, (203, 213, 225, 255))

    elif screen_type == 2:
        draw_rounded_rect(buf, w, h, 50, 260, 1030, 750, 36, (159, 18, 57, 255))
        draw_rounded_rect(buf, w, h, 60, 270, 1020, 740, 30, (190, 18, 60, 255))
        draw_rect(buf, w, h, 100, 320, 600, 370, (254, 205, 211, 255))
        draw_rect(buf, w, h, 100, 410, 850, 510, (255, 255, 255, 255))
        draw_rect(buf, w, h, 100, 540, 750, 580, (254, 205, 211, 255))
        draw_rounded_rect(buf, w, h, 100, 620, 480, 690, 25, (225, 29, 72, 255))

        draw_rounded_rect(buf, w, h, 50, 790, 1030, 1260, 36, (30, 41, 59, 255))
        draw_rect(buf, w, h, 100, 840, 500, 880, (248, 250, 252, 255))
        draw_rect(buf, w, h, 100, 920, 880, 955, (203, 213, 225, 255))
        draw_rect(buf, w, h, 100, 980, 820, 1015, (203, 213, 225, 255))
        draw_rect(buf, w, h, 100, 1040, 750, 1075, (203, 213, 225, 255))

        draw_rounded_rect(buf, w, h, 50, 1300, 1030, 1440, 36, (2, 132, 199, 255))
        draw_rect(buf, w, h, 380, 1350, 700, 1390, (255, 255, 255, 255))

        draw_rounded_rect(buf, w, h, 50, 1480, 1030, 2200, 36, (30, 41, 59, 255))
        draw_rect(buf, w, h, 100, 1530, 480, 1565, (148, 163, 184, 255))
        for i in range(5):
            y_log = 1600 + i * 110
            c = (253, 164, 175, 255) if i == 0 else (203, 213, 225, 255)
            draw_rect(buf, w, h, 100, y_log, 950, y_log + 35, c)

    elif screen_type == 3:
        draw_rounded_rect(buf, w, h, 50, 260, 1030, 620, 36, (14, 116, 144, 255))
        draw_rounded_rect(buf, w, h, 60, 270, 1020, 610, 30, (8, 145, 178, 255))
        draw_rect(buf, w, h, 100, 320, 520, 365, (207, 250, 254, 255))
        draw_rect(buf, w, h, 100, 400, 800, 460, (255, 255, 255, 255))
        draw_rounded_rect(buf, w, h, 100, 490, 420, 560, 24, (14, 165, 233, 255))
        draw_rounded_rect(buf, w, h, 460, 490, 780, 560, 24, (244, 63, 94, 255))

        draw_rounded_rect(buf, w, h, 50, 660, 1030, 1180, 36, (30, 41, 59, 255))
        draw_rect(buf, w, h, 100, 710, 550, 750, (241, 245, 249, 255))
        draw_rounded_rect(buf, w, h, 100, 780, 980, 860, 20, (15, 23, 42, 255))
        draw_rounded_rect(buf, w, h, 100, 890, 980, 970, 20, (56, 189, 248, 255))
        draw_rounded_rect(buf, w, h, 100, 1000, 980, 1080, 20, (15, 23, 42, 255))

        draw_rounded_rect(buf, w, h, 50, 1220, 1030, 2200, 36, (30, 41, 59, 255))
        draw_rect(buf, w, h, 100, 1270, 550, 1305, (148, 163, 184, 255))
        for i in range(7):
            y_log = 1340 + i * 110
            draw_rect(buf, w, h, 100, y_log, 950, y_log + 35, (203, 213, 225, 255))

    write_png(path, w, h, buf)
    print(f"Generated Screenshot: {path}")

def main():
    # 1. Fastlane and F-Droid Metadata Icons
    destinations = [
        "fastlane/metadata/android/en-US/images",
        "metadata/com.aistudio.fivegguardian.qywtpa/en-US/images"
    ]
    for d in destinations:
        icon_path = os.path.join(d, "icon.png")
        generate_icon(icon_path, size=512, is_round=False)
        shots_dir = os.path.join(d, "phoneScreenshots")
        generate_screenshot(os.path.join(shots_dir, "1.png"), 1)
        generate_screenshot(os.path.join(shots_dir, "2.png"), 2)
        generate_screenshot(os.path.join(shots_dir, "3.png"), 3)

    # 2. Android Mipmap Icons
    mipmaps = [
        ("app/src/main/res/mipmap-mdpi", 48),
        ("app/src/main/res/mipmap-hdpi", 72),
        ("app/src/main/res/mipmap-xhdpi", 96),
        ("app/src/main/res/mipmap-xxhdpi", 144),
        ("app/src/main/res/mipmap-xxxhdpi", 192),
    ]
    for mdir, sz in mipmaps:
        generate_icon(os.path.join(mdir, "ic_launcher.png"), size=sz, is_round=False)
        generate_icon(os.path.join(mdir, "ic_launcher_round.png"), size=sz, is_round=True)

if __name__ == "__main__":
    main()
