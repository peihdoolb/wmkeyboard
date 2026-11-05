#!/usr/bin/env python3
"""
Converts sound packs, themes, palettes, and skins from Kyng Keyboard (kyng.apks)
into native wmkeyboard formats (.wmsoundpack and .wmtheme.json).
"""

import zipfile
import json
import os
import io
import sys
import shutil

def hex_to_argb_long(hex_str):
    if not hex_str:
        return None
    hex_str = hex_str.strip().lstrip('#')
    if len(hex_str) == 6:
        hex_str = 'FF' + hex_str
    try:
        return int(hex_str, 16)
    except Exception:
        return None

def convert_sound_pack(z, sound_file_path, out_dir):
    sound_meta = json.loads(z.read(sound_file_path).decode('utf-8'))
    pack_id = sound_meta.get('id', '')
    pack_file = sound_meta.get('packFile', pack_id)
    manifest_path = f'assets/sounds/{pack_file}/pack.json'
    
    if manifest_path not in z.namelist():
        return None
        
    kyng_pack = json.loads(z.read(manifest_path).decode('utf-8'))
    name = sound_meta.get('name', pack_id.title())
    category = sound_meta.get('category', 'mechanical')
    
    # Calculate gain from gainDb
    gain_db = kyng_pack.get("gainDb", 0.0)
    if gain_db <= 0:
        gain = round(min(1.0, max(0.0, 10 ** (gain_db / 20.0))), 2)
    else:
        gain = 1.0

    press_letter = [f"sounds/{os.path.basename(f)}" for f in kyng_pack.get("press", {}).get("letter", [])]
    
    wm_manifest = {
        "format": "wmkeyboard-sound-pack",
        "version": 1,
        "id": f"kyng_{pack_id}",
        "name": f"Kyng {name}",
        "author": "Kyng contributors",
        "packVersion": "1.0.0",
        "description": f"Kyng {name} ({category}) sound pack.",
        "gain": gain,
        "press": press_letter,
        "release": [],
        "roles": {}
    }
    
    role_mapping = {
        "space": "space",
        "enter": "enter",
        "delete": "delete",
        "modifier": "modifier"
    }
    
    for k_role, wm_role in role_mapping.items():
        samples = kyng_pack.get("press", {}).get(k_role, [])
        if samples:
            wm_manifest["roles"][wm_role] = {
                "press": [f"sounds/{os.path.basename(s)}" for s in samples]
            }
            
    out_file = os.path.join(out_dir, f"{pack_id}.wmsoundpack")
    with zipfile.ZipFile(out_file, 'w', compression=zipfile.ZIP_DEFLATED) as out_zip:
        out_zip.writestr("pack.json", json.dumps(wm_manifest, indent=2))
        
        # Collect all referenced audio files
        all_samples = set()
        for cat in ["letter", "space", "enter", "delete", "modifier"]:
            for sample in kyng_pack.get("press", {}).get(cat, []):
                all_samples.add(sample)
                
        for sample in sorted(all_samples):
            sample_apk_path = f"assets/sounds/{pack_file}/{sample}"
            if sample_apk_path in z.namelist():
                out_zip.writestr(f"sounds/{os.path.basename(sample)}", z.read(sample_apk_path))
                
    return out_file

def convert_theme(z, theme_path, out_dir):
    th = json.loads(z.read(theme_path).decode('utf-8'))
    theme_id = th.get('id', '')
    theme_name = th.get('name', theme_id.title())
    style_id = th.get('styleId')
    palette_id = th.get('paletteId')
    
    palette_file = f'palettes/{palette_id}.json'
    if palette_file not in z.namelist():
        return None
    palette = json.loads(z.read(palette_file).decode('utf-8'))
    roles = palette.get('roles', {})
    
    style_file = f'styles/{style_id}.json'
    style = {}
    tokens = {}
    skin_id = None
    if style_file in z.namelist():
        style = json.loads(z.read(style_file).decode('utf-8'))
        tokens = style.get('tokens', {})
        skin_id = style.get('skin')
        
    skin_spec = None
    if skin_id and f'skins/{skin_id}.json' in z.namelist():
        skin_raw = json.loads(z.read(f'skins/{skin_id}.json').decode('utf-8'))
        skin_spec = {
            "slots": skin_raw.get("slots", {}),
            "board": skin_raw.get("board")
        }
        
    board_color = hex_to_argb_long(roles.get('board', '#FF17181C'))
    key_color = hex_to_argb_long(roles.get('key', '#FF303338'))
    pressed_key_color = hex_to_argb_long(roles.get('keyPressed', roles.get('key', '#FF303338')))
    special_key_color = hex_to_argb_long(roles.get('keySpecial', roles.get('key', '#FF303338')))
    special_pressed_key_color = hex_to_argb_long(roles.get('keySpecialPressed', roles.get('keyPressed', roles.get('key', '#FF303338'))))
    label_color = hex_to_argb_long(roles.get('label', '#FFFFFFFF'))
    label_sec_color = hex_to_argb_long(roles.get('labelSecondary', '#FFA0A0A0'))
    accent_color = hex_to_argb_long(roles.get('accent', '#FF8AB4F8'))
    border_color = hex_to_argb_long(roles.get('border', '#00000000'))
    
    corner_radius = tokens.get('cornerRadiusDp', 10.0)
    border_width = tokens.get('borderWidthDp', 0.0)
    shape_str = tokens.get('shape', 'rect').upper()
    key_shape = "ROUNDED"
    if shape_str in ["PILL", "CAPSULE"]:
        key_shape = "PILL"
    elif shape_str in ["RECT", "SQUARE"]:
        key_shape = "ROUNDED" if corner_radius > 0 else "SHARP"
    elif shape_str == "CUT":
        key_shape = "CUT"
        
    wm_theme = {
        "id": f"kyng_{theme_id}",
        "name": f"Kyng {theme_name}",
        "dark": True,
        "boardBackground": board_color,
        "keyBackground": key_color,
        "pressedKeyBackground": pressed_key_color,
        "specialKeyBackground": special_key_color,
        "specialKeyPressedBackground": special_pressed_key_color,
        "keyTextColor": label_color,
        "keySubtextColor": label_sec_color,
        "accentColor": accent_color,
        "keyBorderColor": border_color,
        "keyBorderWidthDp": border_width,
        "keyCornerRadiusDp": corner_radius,
        "keyShape": key_shape,
        "keySkin": skin_spec
    }
    
    out_file = os.path.join(out_dir, f"{theme_id}.wmtheme.json")
    with open(out_file, 'w', encoding='utf-8') as f:
        json.dump(wm_theme, f, indent=2)
    return out_file

def main():
    apk_candidates = [
        r'F:\AI\kyng.apks',
        r'C:\Users\MSI\.gemini\antigravity\brain\fd082a6d-e200-44d6-8fec-29ae96f32bd3\scratch\kyng_unpack\base.apk'
    ]
    apk_file = None
    for c in apk_candidates:
        if os.path.exists(c):
            apk_file = c
            break
            
    if not apk_file:
        print("Error: Could not locate kyng.apks or base.apk")
        sys.exit(1)
        
    print(f"Reading from: {apk_file}")
    
    # If .apks, open base.apk from inside or extract
    if apk_file.endswith('.apks'):
        temp_zip = zipfile.ZipFile(apk_file)
        base_apk_bytes = temp_zip.read('base.apk')
        z = zipfile.ZipFile(io.BytesIO(base_apk_bytes))
    else:
        z = zipfile.ZipFile(apk_file)
        
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
    sounds_out = os.path.join(base_dir, 'tools', 'kyng', 'output', 'soundpacks')
    themes_out = os.path.join(base_dir, 'tools', 'kyng', 'output', 'themes')
    os.makedirs(sounds_out, exist_ok=True)
    os.makedirs(themes_out, exist_ok=True)
    
    sound_files = [n for n in z.namelist() if n.startswith('sounds/') and n.endswith('.json')]
    print(f"Converting {len(sound_files)} sound packs...")
    converted_sounds = []
    for sf in sound_files:
        p = convert_sound_pack(z, sf, sounds_out)
        if p:
            converted_sounds.append(p)
    print(f"Successfully converted {len(converted_sounds)} sound packs to {sounds_out}")
    
    theme_files = [n for n in z.namelist() if n.startswith('themes/') and n.endswith('.json')]
    print(f"Converting {len(theme_files)} themes...")
    converted_themes = []
    for tf in theme_files:
        p = convert_theme(z, tf, themes_out)
        if p:
            converted_themes.append(p)
    print(f"Successfully converted {len(converted_themes)} themes to {themes_out}")
    
    # Also seed top 6 sound packs into app/src/main/assets/soundpacks/
    seed_dir = os.path.join(base_dir, 'app', 'src', 'main', 'assets', 'soundpacks')
    os.makedirs(seed_dir, exist_ok=True)
    
    top_packs = [
        'brass-mech.wmsoundpack',
        'deep-thock.wmsoundpack',
        'butter-linear.wmsoundpack',
        'bubbles.wmsoundpack',
        'typewriter.wmsoundpack',
        'retro.wmsoundpack'
    ]
    for tp in top_packs:
        src = os.path.join(sounds_out, tp)
        if os.path.exists(src):
            dst = os.path.join(seed_dir, tp)
            shutil.copyfile(src, dst)
            print(f"Bundled seed soundpack: {tp} -> {dst}")

if __name__ == '__main__':
    main()
