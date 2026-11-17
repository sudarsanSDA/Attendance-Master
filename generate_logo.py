import math
from PIL import Image, ImageDraw, ImageFilter

def create_attendance_logo(size=512):
    # 4x supersampling for ultra-crisp antialiasing
    scale = 2
    W = size * scale
    H = size * scale

    # Master canvas (transparent RGBA)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Base Squircle / Rounded Background
    pad = int(24 * scale)
    rect = [pad, pad, W - pad, H - pad]
    radius = int(108 * scale)

    # Base gradient: Indigo #4338CA to Deep Purple/Navy #1E1B4B
    c1 = (67, 56, 202)   # #4338CA
    c2 = (30, 27, 75)    # #1E1B4B
    
    # Create mask for squircle
    mask = Image.new("L", (W, H), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle(rect, radius=radius, fill=255)

    # Draw gradient into base_layer
    base_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    base_draw = ImageDraw.Draw(base_layer)
    for y in range(pad, H - pad):
        t = (y - pad) / (H - 2 * pad)
        r = int(c1[0] + (c2[0] - c1[0]) * t)
        g = int(c1[1] + (c2[1] - c1[1]) * t)
        b = int(c1[2] + (c2[2] - c1[2]) * t)
        base_draw.line([(pad, y), (W - pad, y)], fill=(r, g, b, 255))

    # Add soft radial glow at top-left
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    gx, gy = int(W * 0.3), int(H * 0.25)
    for rad in range(int(W * 0.4), 0, -int(4 * scale)):
        alpha = int(35 * (1 - rad / (W * 0.4)))
        glow_draw.ellipse([gx - rad, gy - rad, gx + rad, gy + rad], fill=(129, 140, 248, alpha))
    
    base_layer = Image.alpha_composite(base_layer, glow)

    # Composite base onto img using mask
    img.paste(base_layer, (0, 0), mask)

    # Subtle inner border / highlight
    border_mask = Image.new("L", (W, H), 0)
    b_draw = ImageDraw.Draw(border_mask)
    b_draw.rounded_rectangle(rect, radius=radius, outline=255, width=int(2.5 * scale))
    border_layer = Image.new("RGBA", (W, H), (255, 255, 255, 45))
    img.paste(border_layer, (0, 0), border_mask)

    # 2. Main Clipboard Document (Center)
    # Dimensions
    doc_w = int(240 * scale)
    doc_h = int(300 * scale)
    doc_x = int((W - doc_w) / 2)
    doc_y = int(H * 0.22)
    doc_radius = int(28 * scale)

    # Drop shadow behind clipboard
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle(
        [doc_x - 4 * scale, doc_y + 12 * scale, doc_x + doc_w + 4 * scale, doc_y + doc_h + 16 * scale],
        radius=doc_radius,
        fill=(10, 10, 35, 120)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(int(12 * scale)))
    img = Image.alpha_composite(img, shadow)

    # Draw White Document Sheet
    doc_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d_draw = ImageDraw.Draw(doc_layer)
    d_draw.rounded_rectangle(
        [doc_x, doc_y, doc_x + doc_w, doc_y + doc_h],
        radius=doc_radius,
        fill=(255, 255, 255, 255)
    )

    # Soft subtle vertical gradient on paper (pure white to very soft slate #F8FAFC)
    # Clipboard Top Clip
    clip_w = int(100 * scale)
    clip_h = int(32 * scale)
    clip_x = int((W - clip_w) / 2)
    clip_y = doc_y - int(12 * scale)
    clip_rad = int(10 * scale)

    d_draw.rounded_rectangle(
        [clip_x, clip_y, clip_x + clip_w, clip_y + clip_h],
        radius=clip_rad,
        fill=(79, 70, 229, 255) # Indigo #4F46E5
    )
    # Metallic accent bar on clip
    d_draw.rounded_rectangle(
        [clip_x + int(14 * scale), clip_y + int(6 * scale), clip_x + clip_w - int(14 * scale), clip_y + clip_h - int(6 * scale)],
        radius=int(4 * scale),
        fill=(224, 231, 255, 255)
    )

    # Roster list lines on paper
    # Row 1
    r1_y = doc_y + int(56 * scale)
    d_draw.ellipse([doc_x + int(24 * scale), r1_y, doc_x + int(38 * scale), r1_y + int(14 * scale)], fill=(34, 197, 94, 255)) # Green dot
    d_draw.rounded_rectangle([doc_x + int(48 * scale), r1_y + int(3 * scale), doc_x + int(160 * scale), r1_y + int(11 * scale)], radius=int(4 * scale), fill=(203, 213, 225, 255))

    # Row 2
    r2_y = doc_y + int(88 * scale)
    d_draw.ellipse([doc_x + int(24 * scale), r2_y, doc_x + int(38 * scale), r2_y + int(14 * scale)], fill=(34, 197, 94, 255))
    d_draw.rounded_rectangle([doc_x + int(48 * scale), r2_y + int(3 * scale), doc_x + int(130 * scale), r2_y + int(11 * scale)], radius=int(4 * scale), fill=(203, 213, 225, 255))

    # Row 3
    r3_y = doc_y + int(120 * scale)
    d_draw.ellipse([doc_x + int(24 * scale), r3_y, doc_x + int(38 * scale), r3_y + int(14 * scale)], fill=(244, 63, 94, 255)) # Red dot
    d_draw.rounded_rectangle([doc_x + int(48 * scale), r3_y + int(3 * scale), doc_x + int(145 * scale), r3_y + int(11 * scale)], radius=int(4 * scale), fill=(203, 213, 225, 255))

    # Row 4
    r4_y = doc_y + int(152 * scale)
    d_draw.ellipse([doc_x + int(24 * scale), r4_y, doc_x + int(38 * scale), r4_y + int(14 * scale)], fill=(34, 197, 94, 255))
    d_draw.rounded_rectangle([doc_x + int(48 * scale), r4_y + int(3 * scale), doc_x + int(120 * scale), r4_y + int(11 * scale)], radius=int(4 * scale), fill=(203, 213, 225, 255))

    img = Image.alpha_composite(img, doc_layer)

    # 3. Big Vibrant Emerald Checkmark Badge (Bottom-Right of clipboard)
    badge_rad = int(72 * scale)
    bx = doc_x + doc_w - int(36 * scale)
    by = doc_y + doc_h - int(48 * scale)

    # Badge shadow
    b_shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    bs_draw = ImageDraw.Draw(b_shadow)
    bs_draw.ellipse([bx - badge_rad, by - badge_rad + int(10 * scale), bx + badge_rad, by + badge_rad + int(10 * scale)], fill=(16, 185, 129, 90))
    b_shadow = b_shadow.filter(ImageFilter.GaussianBlur(int(10 * scale)))
    img = Image.alpha_composite(img, b_shadow)

    # Badge outer white ring
    badge_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(badge_layer)
    b_draw.ellipse([bx - badge_rad, by - badge_rad, bx + badge_rad, by + badge_rad], fill=(255, 255, 255, 255))

    # Badge inner green circle (#10B981 to #059669)
    inner_rad = badge_rad - int(6 * scale)
    b_draw.ellipse([bx - inner_rad, by - inner_rad, bx + inner_rad, by + inner_rad], fill=(16, 185, 129, 255))

    # Bold White Checkmark in Badge
    # Points for checkmark: (x1, y1) -> (x2, y2) -> (x3, y3)
    p1 = (bx - int(24 * scale), by - int(2 * scale))
    p2 = (bx - int(8 * scale), by + int(16 * scale))
    p3 = (bx + int(24 * scale), by - int(18 * scale))

    b_draw.line([p1, p2, p3], fill=(255, 255, 255, 255), width=int(12 * scale), joint="curve")
    img = Image.alpha_composite(img, badge_layer)

    # 4. Academic Graduation Cap (GATE / Higher Ed) at top-right
    cap_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    c_draw = ImageDraw.Draw(cap_layer)

    cx = doc_x + doc_w - int(20 * scale)
    cy = doc_y - int(16 * scale)

    # Diamond / rhombus of graduation cap
    cap_w = int(74 * scale)
    cap_h = int(34 * scale)

    diamond = [
        (cx, cy - cap_h // 2),
        (cx + cap_w // 2, cy),
        (cx, cy + cap_h // 2),
        (cx - cap_w // 2, cy)
    ]
    # Cap shadow
    c_draw.polygon([(x, y + int(4 * scale)) for x, y in diamond], fill=(15, 23, 42, 60))
    # Cap top
    c_draw.polygon(diamond, fill=(245, 158, 11, 255)) # Golden accent cap #F59E0B

    # Cap skull band underneath
    band = [
        (cx - int(20 * scale), cy + int(6 * scale)),
        (cx + int(20 * scale), cy + int(6 * scale)),
        (cx + int(16 * scale), cy + int(20 * scale)),
        (cx - int(16 * scale), cy + int(20 * scale))
    ]
    c_draw.polygon(band, fill=(217, 119, 6, 255)) # Dark amber #D97706

    # Cap tassel button & hanging tassel
    c_draw.ellipse([cx - int(4 * scale), cy - int(4 * scale), cx + int(4 * scale), cy + int(4 * scale)], fill=(255, 255, 255, 255))
    c_draw.line([(cx, cy), (cx - int(24 * scale), cy + int(12 * scale))], fill=(255, 255, 255, 255), width=int(3 * scale))
    c_draw.ellipse([cx - int(27 * scale), cy + int(10 * scale), cx - int(21 * scale), cy + int(18 * scale)], fill=(255, 255, 255, 255))

    img = Image.alpha_composite(img, cap_layer)

    # Downsample with Lanczos for smooth output
    final_img = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_img

if __name__ == "__main__":
    logo = create_attendance_logo(512)
    logo.save("app_logo.png")
    print("app_logo.png created successfully!")
