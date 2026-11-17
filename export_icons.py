import os
from PIL import Image, ImageDraw

from generate_logo import create_attendance_logo

def generate_all_icons():
    # 512x512 master
    master = create_attendance_logo(512)
    master.save("app/src/main/res/drawable/app_logo.png")
    master.save("app_logo.png")

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    base_res = "app/src/main/res"

    for folder, size in densities.items():
        dir_path = os.path.join(base_res, folder)
        os.makedirs(dir_path, exist_ok=True)

        # 1. Standard Squircle Icon
        icon_standard = master.resize((size, size), Image.Resampling.LANCZOS)
        icon_standard.save(os.path.join(dir_path, "ic_launcher.webp"), "WEBP")
        # Also as png if needed
        icon_standard.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")

        # 2. Round Icon (Circular mask)
        circle_mask = Image.new("L", (size, size), 0)
        c_draw = ImageDraw.Draw(circle_mask)
        c_draw.ellipse([0, 0, size - 1, size - 1], fill=255)

        # Scale inner content slightly or apply circle mask
        icon_round = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        icon_round.paste(icon_standard, (0, 0), circle_mask)
        icon_round.save(os.path.join(dir_path, "ic_launcher_round.webp"), "WEBP")
        icon_round.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

        print(f"Generated {folder} ({size}x{size})")

if __name__ == "__main__":
    generate_all_icons()
    print("All mipmap icons generated successfully!")
