from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import random
import numpy as np

W, H = 1200, 750
S = 2
CW, CH = W * S, H * S

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs" / "promo"
OUT.mkdir(parents=True, exist_ok=True)
TOTEM = ROOT / "docs" / "brand" / "totem" / "nhtotem-logo-green-1024.png"

F_CN_B = r"C:\Windows\Fonts\msyhbd.ttc"
F_CN = r"C:\Windows\Fonts\msyh.ttc"
F_EN_B = r"C:\Windows\Fonts\arialbd.ttf"
F_EN = r"C:\Windows\Fonts\arial.ttf"
F_MONO = r"C:\Windows\Fonts\consolab.ttf"
F_MONO_R = r"C:\Windows\Fonts\consola.ttf"

WHITE = (248, 250, 252, 255)
SLATE = (148, 163, 184, 255)
MUTED = (100, 116, 139, 255)
CYAN = (34, 211, 238, 255)
GREEN = (65, 230, 124, 255)
GOLD = (242, 201, 76, 255)
RED = (248, 113, 113, 255)
PURPLE = (192, 132, 252, 255)
DARK = (7, 13, 24, 255)


def px(v):
    return int(round(v * S))


def font(path, size):
    return ImageFont.truetype(path, px(size))


def text(d, xy, s, f, fill, anchor="la", spacing=0, shadow=0, shadow_color=(0, 0, 0, 255)):
    x, y = px(xy[0]), px(xy[1])
    if spacing:
        widths = [d.textlength(ch, font=f) for ch in s]
        total = sum(widths) + px(spacing) * max(0, len(s) - 1)
        if anchor[0] == "m":
            x -= total / 2
        elif anchor[0] == "r":
            x -= total
        for ch, cw in zip(s, widths):
            if shadow:
                d.text((x + px(shadow), y + px(shadow)), ch, font=f, fill=shadow_color, anchor="l" + anchor[1])
            d.text((x, y), ch, font=f, fill=fill, anchor="l" + anchor[1])
            x += cw + px(spacing)
        return total
    if shadow:
        d.text((x + px(shadow), y + px(shadow)), s, font=f, fill=shadow_color, anchor=anchor)
    d.text((x, y), s, font=f, fill=fill, anchor=anchor)
    return d.textlength(s, font=f)


def text_width(d, s, f, spacing=0):
    if spacing:
        return sum(d.textlength(ch, font=f) for ch in s) + px(spacing) * max(0, len(s) - 1)
    return d.textlength(s, font=f)


def rounded(d, box, radius, fill=None, outline=None, width=1):
    d.rounded_rectangle([px(box[0]), px(box[1]), px(box[2]), px(box[3])], radius=px(radius),
                        fill=fill, outline=outline, width=max(1, px(width)))


def gradient_bg(top=(6, 11, 21), bottom=(12, 29, 42)):
    img = Image.new("RGBA", (CW, CH), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    for y in range(CH):
        t = y / (CH - 1)
        c = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3)) + (255,)
        d.line([(0, y), (CW, y)], fill=c)
    return img


def radial_glow(size, color, alpha=140, invert=True):
    n = max(2, px(size))
    yy, xx = np.mgrid[0:n, 0:n]
    cx = cy = (n - 1) / 2.0
    r = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / (n / 2.0)
    a = np.clip(1.0 - r, 0.0, 1.0) ** 1.7
    arr = np.zeros((n, n, 4), dtype=np.uint8)
    arr[..., 0] = color[0]
    arr[..., 1] = color[1]
    arr[..., 2] = color[2]
    arr[..., 3] = (a * alpha).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def add_glow(img, center, size, color, alpha=120):
    glow = radial_glow(size, color, alpha)
    img.alpha_composite(glow, (px(center[0] - size / 2), px(center[1] - size / 2)))


def add_grid(img, step=40, alpha=12, major=200, major_alpha=20):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for x in range(0, W + 1, step):
        a = major_alpha if x % major == 0 else alpha
        d.line([(px(x), 0), (px(x), CH)], fill=(148, 163, 184, a), width=1)
    for y in range(0, H + 1, step):
        a = major_alpha if y % major == 0 else alpha
        d.line([(0, px(y)), (CW, px(y))], fill=(148, 163, 184, a), width=1)
    img.alpha_composite(layer)


def add_particles(img, count=40, seed=7):
    rnd = random.Random(seed)
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for _ in range(count):
        x = rnd.randint(0, W)
        y = rnd.randint(0, H)
        size = rnd.choice([2, 3, 4, 6, 8])
        color = rnd.choice([CYAN, GREEN, GOLD, (148, 163, 184, 255)])
        a = rnd.randint(18, 55)
        d.rectangle([px(x), px(y), px(x + size), px(y + size)], fill=color[:3] + (a,))
    img.alpha_composite(layer)


def add_vignette(img, strength=120):
    g = Image.radial_gradient("L").resize((CW, CH))
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    layer.putalpha(g.point(lambda v: int(v * strength / 255)))
    img.alpha_composite(layer)


def load_totem(height):
    im = Image.open(TOTEM).convert("RGBA")
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    w = int(im.width * height / im.height)
    return im.resize((px(w), px(height)), Image.NEAREST)


def paste_totem(img, totem, center, glow_color=CYAN, glow_alpha=150, glow_size=700):
    add_glow(img, center, glow_size, glow_color, glow_alpha)
    x = px(center[0]) - totem.width // 2
    y = px(center[1]) - totem.height // 2
    shadow = Image.new("RGBA", totem.size, (0, 0, 0, 0))
    shadow.putalpha(totem.split()[3].point(lambda a: int(a * 0.55)))
    shadow = shadow.filter(ImageFilter.GaussianBlur(px(18)))
    img.alpha_composite(shadow, (x + px(10), y + px(18)))
    img.alpha_composite(totem, (x, y))


def pill(d, x, y, label, f, fg, bg=(30, 41, 59, 255), border=(71, 85, 105, 255), h=38, padx=18):
    w = text_width(d, label, f) / S + padx * 2
    rounded(d, (x, y, x + w, y + h), h / 2, fill=bg, outline=border, width=1)
    d.text((px(x + padx), px(y + h / 2)), label, font=f, fill=fg, anchor="lm")
    return w


def header(d, kicker, title, subtitle=None):
    f_kick = font(F_CN_B, 15)
    f_title = font(F_CN_B, 48)
    f_sub = font(F_CN, 21)
    label = kicker
    w = text_width(d, label, f_kick, spacing=2) / S + 44
    rounded(d, (60, 48, 60 + w, 84), 18, fill=(13, 25, 38, 215), outline=(34, 211, 238, 80), width=1)
    d.ellipse([px(74), px(61), px(82), px(69)], fill=CYAN)
    text(d, (92, 66), label, f_kick, (165, 243, 252, 255), anchor="lm", spacing=2)
    text(d, (58, 104), title, f_title, WHITE, anchor="la", shadow=2, shadow_color=(0, 0, 0, 160))
    if subtitle:
        text(d, (62, 176), subtitle, f_sub, (203, 213, 225, 255))


def icon_tile(d, x, y, kind):
    rounded(d, (x, y, x + 38, y + 38), 10, fill=(19, 34, 51, 255), outline=(45, 212, 191, 90), width=1)
    cx, cy = x + 19, y + 19
    if kind == "inventory":
        for i in range(3):
            for j in range(3):
                d.rectangle([px(x + 9 + i * 7), px(y + 9 + j * 7), px(x + 13 + i * 7), px(y + 13 + j * 7)], fill=CYAN)
    elif kind == "xp":
        d.ellipse([px(x + 9), px(y + 9), px(x + 29), px(y + 29)], fill=(16, 185, 129, 255))
        d.text((px(cx), px(cy)), "XP", font=font(F_EN_B, 9), fill=(6, 24, 18, 255), anchor="mm")
    elif kind == "random":
        d.text((px(cx), px(cy)), "%", font=font(F_EN_B, 21), fill=GOLD, anchor="mm")
    elif kind == "curse":
        d.polygon([(px(cx), px(y + 7)), (px(x + 31), px(cy)), (px(cx), px(y + 31)), (px(x + 7), px(cy))],
                  outline=PURPLE, width=max(1, px(1.5)))
        d.line([(px(x + 9), px(y + 9)), (px(x + 29), px(y + 29))], fill=RED, width=px(2))
    elif kind == "world":
        d.ellipse([px(x + 8), px(y + 8), px(x + 30), px(y + 30)], outline=GREEN, width=px(1.5))
        d.ellipse([px(x + 14), px(y + 8), px(x + 24), px(y + 30)], outline=GREEN, width=1)
        d.line([(px(x + 8), px(cy)), (px(x + 30), px(cy))], fill=GREEN, width=1)
    elif kind == "migrate":
        d.line([(px(x + 8), px(cy)), (px(x + 29), px(cy))], fill=GOLD, width=px(2))
        d.line([(px(x + 21), px(cy - 6)), (px(x + 29), px(cy))], fill=GOLD, width=px(2))
        d.line([(px(x + 21), px(cy + 6)), (px(x + 29), px(cy))], fill=GOLD, width=px(2))


def check_icon(d, cx, cy, size, color):
    d.line([(px(cx - size), px(cy)), (px(cx - size * 0.25), px(cy + size * 0.7)), (px(cx + size), px(cy - size * 0.8))],
           fill=color, width=px(3), joint="curve")


def cross_icon(d, cx, cy, size, color):
    d.line([(px(cx - size), px(cy - size)), (px(cx + size), px(cy + size))], fill=color, width=px(3))
    d.line([(px(cx - size), px(cy + size)), (px(cx + size), px(cy - size))], fill=color, width=px(3))


def footer(d, left="NowHere Studios  ·  开发与维护", right="NHTotem 0.1.3  ·  1200 × 750"):
    d.line([(px(60), px(694)), (px(1140), px(694))], fill=(51, 65, 85, 160), width=1)
    text(d, (60, 708), left, font(F_CN, 17), (100, 116, 139, 255))
    text(d, (1140, 708), right, font(F_EN_B, 14), (71, 85, 105, 255), anchor="ra")


def banner_hero():
    img = gradient_bg((6, 11, 21), (12, 29, 42))
    add_glow(img, (1010, 530), 1050, GREEN, 80)
    add_glow(img, (740, 120), 760, CYAN, 65)
    add_grid(img)
    add_particles(img, 45, 11)

    d = ImageDraw.Draw(img)
    f_pill = font(F_EN_B, 16)
    f_title = font(F_EN_B, 112)
    f_cn = font(F_CN_B, 62)
    f_sub = font(F_CN, 26)
    f_chip = font(F_CN_B, 19)
    f_foot = font(F_CN, 20)
    f_small = font(F_EN_B, 15)

    label = "NOWHERE STUDIOS   /   MINECRAFT PLUGIN"
    w = text_width(d, label, f_pill, spacing=2) / S + 50
    rounded(d, (66, 56, 66 + w, 96), 20, fill=(13, 25, 38, 210), outline=(34, 211, 238, 90), width=1)
    d.ellipse([px(82), px(71), px(90), px(79)], fill=CYAN)
    text(d, (100, 76), label, f_pill, (165, 243, 252, 255), anchor="lm", spacing=2)

    text(d, (64, 135), "NH", f_title, CYAN, anchor="la", shadow=3, shadow_color=(0, 0, 0, 180))
    nh_w = text_width(d, "NH", f_title) / S
    text(d, (64 + nh_w - 6, 135), "Totem", f_title, WHITE, anchor="la", shadow=3, shadow_color=(0, 0, 0, 180))

    d.rounded_rectangle([px(68), px(286), px(230), px(292)], radius=px(3), fill=CYAN)

    text(d, (64, 316), "死亡不掉落图腾", f_cn, WHITE, anchor="la", spacing=1)
    text(d, (68, 402), "带着图腾倒下，物品栏与经验默认保留。", f_sub, (203, 213, 225, 255))

    x = 68
    y = 470
    for label in ("Paper / Purpur / Spigot", "1.20.1 – 26.3", "v0.1.3"):
        w = pill(d, x, y, label, f_chip, (226, 232, 240, 255), h=40)
        x += w + 10

    d.line([(px(66), px(658)), (px(520), px(658))], fill=(51, 65, 85, 180), width=1)
    text(d, (66, 678), "NowHere Studios  ·  开发与维护", f_foot, (148, 163, 184, 255))
    text(d, (1134, 678), "v0.1.3", f_small, (100, 116, 139, 255), anchor="ra")

    totem = load_totem(540)
    paste_totem(img, totem, (922, 390), CYAN, 220, 760)
    text(d, (922, 694), "STILL HERE.", f_small, (34, 211, 238, 210), anchor="ma", spacing=5)

    add_vignette(img, 120)
    img.convert("RGB").resize((W, H), Image.LANCZOS).save(OUT / "01-hero.png")
    print("saved 01-hero.png")


def banner_features():
    img = gradient_bg((6, 12, 22), (10, 27, 35))
    add_glow(img, (235, 620), 700, GREEN, 60)
    add_glow(img, (1010, 330), 820, CYAN, 65)
    add_grid(img)
    add_particles(img, 35, 21)

    d = ImageDraw.Draw(img)
    header(d, "FEATURES  /  核心功能", "死亡时，把东西留下", "一个图腾，保住整包家当。")

    cards = [
        ("inventory", "保留物品栏", "默认保留，可配置随机掉落"),
        ("xp", "保留经验", "默认保留等级与经验"),
        ("random", "随机掉落", "按概率掉落部分物品"),
        ("curse", "清除诅咒", "按配置清除诅咒物品"),
        ("world", "世界限制", "指定世界不触发"),
        ("migrate", "旧配置迁移", "自动读取旧版配置"),
    ]
    f_card = font(F_CN_B, 19)
    f_desc = font(F_CN, 15)
    for idx, (kind, title, desc) in enumerate(cards):
        col = idx % 2
        row = idx // 2
        x = 60 + col * 326
        y = 224 + row * 128
        rounded(d, (x, y, x + 310, y + 112), 14, fill=(12, 24, 38, 200), outline=(45, 65, 88, 210), width=1)
        icon_tile(d, x + 16, y + 18, kind)
        text(d, (x + 68, y + 18), title, f_card, WHITE)
        text(d, (x + 68, y + 51), desc, f_desc, (148, 163, 184, 255))

    # right totem + badge
    totem = load_totem(420)
    paste_totem(img, totem, (968, 382), GREEN, 200, 620)
    rounded(d, (812, 592, 1124, 636), 12, fill=(9, 30, 27, 220), outline=(65, 230, 124, 90), width=1)
    text(d, (968, 614), "17 TESTS PASSED", font(F_EN_B, 15), (134, 239, 172, 255), anchor="mm", spacing=3)

    d.line([(px(60), px(674)), (px(1140), px(674))], fill=(51, 65, 85, 160), width=1)
    text(d, (60, 690), "Paper / Purpur / Spigot  ·  1.20.1 – 26.3  ·  Java 17+", font(F_EN_B, 14), (148, 163, 184, 255))
    text(d, (1140, 690), "v0.1.3", font(F_EN_B, 14), (100, 116, 139, 255), anchor="ra")

    add_vignette(img, 110)
    img.convert("RGB").resize((W, H), Image.LANCZOS).save(OUT / "02-features.png")
    print("saved 02-features.png")


def banner_compare():
    img = gradient_bg((8, 9, 18), (12, 22, 34))
    add_glow(img, (300, 420), 700, RED, 50)
    add_glow(img, (900, 380), 760, GREEN, 60)
    add_grid(img)
    add_particles(img, 28, 33)

    d = ImageDraw.Draw(img)
    header(d, "BEFORE / AFTER  ·  对比", "死亡那一刻，结果完全不同", "有没有图腾，背包的命运完全相反。")

    f_panel = font(F_CN_B, 27)
    f_row = font(F_CN_B, 18)
    f_val = font(F_CN, 17)
    panels = [
        (60, (42, 18, 24, 225), (239, 68, 68, 120), "没有图腾", RED),
        (620, (10, 32, 28, 225), (65, 230, 124, 95), "带着图腾", GREEN),
    ]
    for x, bg, border, title, color in panels:
        rounded(d, (x, 206, x + 520, 628), 18, fill=bg, outline=border, width=1.5)
        d.ellipse([px(x + 24), px(228), px(x + 60), px(264)], fill=(0, 0, 0, 0), outline=color, width=px(2))
        if x < 300:
            cross_icon(d, x + 42, 246, 7, color)
        else:
            check_icon(d, x + 42, 246, 7, color)
        text(d, (x + 74, 232), title, f_panel, WHITE)

        rows = [
            ("物品栏", "全部掉落" if x < 300 else "保留*"),
            ("经验", "掉落" if x < 300 else "保留"),
            ("装备", "丢失" if x < 300 else "保留"),
            ("图腾", "—" if x < 300 else "消耗 1 个"),
        ]
        for i, (label, value) in enumerate(rows):
            yy = 322 + i * 70
            d.line([(px(x + 26), px(yy - 18)), (px(x + 494), px(yy - 18))], fill=(71, 85, 105, 90), width=1)
            text(d, (x + 30, yy), label, f_row, (203, 213, 225, 255))
            text(d, (x + 490, yy), value, f_val, color, anchor="ra")

    # small totem on right panel top
    totem = load_totem(86)
    img.alpha_composite(totem, (px(1050) - totem.width // 2, px(214)))

    rounded(d, (380, 650, 820, 692), 21, fill=(13, 25, 38, 220), outline=(34, 211, 238, 70), width=1)
    text(d, (600, 671), "NHTotem  ·  死亡不掉落图腾  ·  NowHere Studios", font(F_CN_B, 16), (165, 243, 252, 255), anchor="mm")
    text(d, (600, 720), "* 掉落概率 / 清除诅咒 / 经验保留 均可在 config.yml 调整", font(F_CN, 13), (100, 116, 139, 255), anchor="ma")

    add_vignette(img, 110)
    img.convert("RGB").resize((W, H), Image.LANCZOS).save(OUT / "03-compare.png")
    print("saved 03-compare.png")


def banner_quickstart():
    img = gradient_bg((6, 11, 21), (12, 27, 38))
    add_glow(img, (880, 410), 780, CYAN, 65)
    add_glow(img, (180, 650), 620, GREEN, 45)
    add_grid(img)
    add_particles(img, 30, 44)

    d = ImageDraw.Draw(img)
    header(d, "QUICK START  /  快速开始", "旧服升级，只需要三步", "删掉旧 jar，放入新 jar，启动即迁移。")

    f_step = font(F_CN_B, 22)
    f_desc = font(F_CN, 16)
    steps = [
        ("1", "放入 NHTotem-0.1.3.jar", "丢进服务器的 plugins/ 文件夹"),
        ("2", "启动服务器", "自动迁移 QcyTotem / NowHereTotem 配置"),
        ("3", "输入 /nhtotem reload", "完成，开始使用"),
    ]
    for i, (num, title, desc) in enumerate(steps):
        y = 225 + i * 118
        d.ellipse([px(60), px(y), px(104), px(y + 44)], fill=(13, 35, 42, 255), outline=CYAN, width=px(1.5))
        text(d, (82, y + 22), num, font(F_EN_B, 20), CYAN, anchor="mm")
        text(d, (126, y - 2), title, f_step, WHITE)
        text(d, (126, y + 30), desc, f_desc, (148, 163, 184, 255))
        if i < 2:
            d.line([(px(82), px(y + 50)), (px(82), px(y + 100))], fill=(34, 211, 238, 60), width=px(1.5))

    # terminal panel
    tx, ty, tw, th = 644, 190, 500, 420
    rounded(d, (tx, ty, tx + tw, ty + th), 16, fill=(6, 11, 20, 235), outline=(45, 65, 88, 255), width=1.5)
    rounded(d, (tx, ty, tx + tw, ty + 42), 16, fill=(15, 27, 41, 255))
    d.rectangle([px(tx), px(ty + 26), px(tx + tw), px(ty + 42)], fill=(15, 27, 41, 255))
    for i, color in enumerate([(248, 113, 113, 255), (242, 201, 76, 255), (65, 230, 124, 255)]):
        d.ellipse([px(tx + 16 + i * 16), px(ty + 15), px(tx + 24 + i * 16), px(ty + 23)], fill=color)
    text(d, (tx + 76, ty + 21), "console  —  NHTotem", font(F_MONO, 12), (148, 163, 184, 255), anchor="lm")

    f_code = font(F_MONO, 15)
    f_code_cn = font(F_CN, 15)
    lines = [
        ("> nhtotem give 图腾A Naohuah", CYAN),
        ("  [NHTotem] 已给予 Naohuah 图腾: 图腾A", (203, 213, 225, 255)),
        ("> nhtotem list", CYAN),
        ("  可用图腾: 图腾A, 图腾B", (203, 213, 225, 255)),
        ("> qcyreload", CYAN),
        ("  配置重载完成，共 2 个图腾。", (134, 239, 172, 255)),
    ]
    yy = ty + 70
    for line, color in lines:
        text(d, (tx + 22, yy), line, f_code if line.isascii() else f_code_cn, color)
        yy += 40
    text(d, (tx + 22, ty + 334), "# /gettotem 与 /qcyreload 继续可用", font(F_CN, 14), (100, 116, 139, 255))

    d.line([(px(60), px(694)), (px(600), px(694))], fill=(51, 65, 85, 160), width=1)
    text(d, (60, 708), "兼容 1.20.1 – 26.3  ·  Java 17+  ·  Paper / Purpur / Spigot", font(F_CN, 15), (100, 116, 139, 255))
    text(d, (1140, 708), "STILL HERE.", font(F_EN_B, 22), (34, 211, 238, 55), anchor="ra", spacing=4)

    add_vignette(img, 110)
    img.convert("RGB").resize((W, H), Image.LANCZOS).save(OUT / "04-quickstart.png")
    print("saved 04-quickstart.png")


def banner_brand():
    img = gradient_bg((5, 9, 17), (8, 20, 30))
    add_glow(img, (600, 360), 1000, CYAN, 80)
    add_glow(img, (600, 420), 700, GREEN, 45)
    add_grid(img, 50, 8, 250, 14)
    add_particles(img, 55, 55)

    d = ImageDraw.Draw(img)
    text(d, (600, 58), "NOWHERE STUDIOS", font(F_EN_B, 16), (148, 163, 184, 255), anchor="ma", spacing=8)

    totem = load_totem(490)
    paste_totem(img, totem, (600, 350), CYAN, 220, 820)

    text(d, (600, 626), "STILL HERE.", font(F_EN_B, 70), WHITE, anchor="ma", spacing=10)
    text(d, (600, 700), "NHTotem  ·  死亡不掉落图腾", font(F_CN_B, 19), (34, 211, 238, 255), anchor="ma")
    text(d, (600, 730), "NowHere Studios  ·  Paper / Purpur / Spigot 1.20.1 – 26.3", font(F_CN, 13), (100, 116, 139, 255), anchor="ma")

    add_vignette(img, 130)
    img.convert("RGB").resize((W, H), Image.LANCZOS).save(OUT / "05-brand.png")
    print("saved 05-brand.png")


if __name__ == "__main__":
    banner_hero()
    banner_features()
    banner_compare()
    banner_quickstart()
    banner_brand()
