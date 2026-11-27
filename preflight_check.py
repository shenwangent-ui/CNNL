import glob, sys, re

errors = []
for path in glob.glob("app/src/main/java/**/*.kt", recursive=True):
    txt = open(path, encoding="utf-8").read()
    if txt.count("{") != txt.count("}"):
        errors.append(f"{path}: วงเล็บปีกกา {{ }} ไม่สมดุล ({txt.count('{')} vs {txt.count('}')})")
    if re.search(r"Surface\s*\([^)]*\bbrush\s*=", txt, re.DOTALL):
        errors.append(f"{path}: ห้ามส่งพารามิเตอร์ brush เข้า Material3 Surface (ให้ใช้ Modifier.background แทน)")
    for bad in ["AppRepository.questZones", "AppRepository.activeCinemaNodeId", "AppRepository.completeQuestNode"]:
        if bad in txt:
            errors.append(f"{path}: พบการเรียก {bad} ที่ไม่มีอยู่จริง")

bridge_txt = open("app/src/main/java/com/shenhan/app/data/CinemaGameBridge.kt", encoding="utf-8").read()
if "_questZones.value.isEmpty()" in bridge_txt:
    errors.append("CinemaGameBridge.kt: ห้ามใส่เงื่อนไข _questZones.value.isEmpty() บล็อกการอัปเดตข้อมูลสด")

if errors:
    print("❌ Pre-Flight Check ไม่ผ่าน! พบจุดที่จะทำให้ Build พัง:")
    for e in errors:
        print("  - " + e)
    sys.exit(1)
else:
    print("✅ Pre-Flight Check ผ่านฉลุย! โครงสร้าง Kotlin ถูกต้องพร้อม Build")
