import glob, sys, re

def clean_kt(src):
    out, i, n = [], 0, len(src)
    while i < n:
        if src[i:i+2] == "//":
            while i < n and src[i] != "\n": i += 1
        elif src[i:i+2] == "/*":
            i += 2
            while i < n and src[i:i+2] != "*/":
                if src[i] == "\n": out.append("\n")
                i += 1
            i += 2
        elif src[i:i+3] == '"""':
            i += 3
            while i < n and src[i:i+3] != '"""':
                if src[i] == "\n": out.append("\n")
                i += 1
            i += 3
        elif src[i] == '"':
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\": i += 2; continue
                i += 1
            i += 1
        elif src[i] == "'":
            i += 1
            while i < n and src[i] != "'":
                if src[i] == "\\": i += 2; continue
                i += 1
            i += 1
        else:
            out.append(src[i]); i += 1
    return "".join(out)

errors = []
for path in glob.glob("app/src/main/java/**/*.kt", recursive=True):
    raw = open(path, encoding="utf-8", errors="ignore").read()
    txt = clean_kt(raw)
    if txt.count("{") != txt.count("}"):
        errors.append(f"{path}: วงเล็บปีกกา {{ }} ไม่สมดุล ({txt.count('{')} vs {txt.count('}')})")
    if re.search(r"Surface\s*\([^)]*\bbrush\s*=", txt, re.DOTALL):
        errors.append(f"{path}: ห้ามส่งพารามิเตอร์ brush เข้า Material3 Surface")

if errors:
    print("❌ Pre-Flight Check ไม่ผ่าน:")
    for e in errors:
        print("  - " + e)
    sys.exit(1)
print("✅ Pre-Flight Check ผ่านฉลุย!")
