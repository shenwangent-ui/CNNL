#!/usr/bin/env python3
"""
ตัวแปลงไฟล์คำศัพท์ (CSV / JSON) เป็นชุดอัปเดตหลังบ้าน remote_vocab/
วิธีใช้: python3 scripts/import_vocab_file.py --input hsk3.csv --level 3 --version 3
"""
import argparse, csv, json, os

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="ไฟล์ .csv หรือ .json ต้นฉบับ")
    parser.add_argument("--level", type=int, required=True, help="ระดับ HSK (1-6)")
    parser.add_argument("--version", type=int, required=True, help="เลขเวอร์ชันหลังบ้านใหม่")
    args = parser.parse_args()

    base_path = "remote_vocab/hsk3_vocab.json"
    if not os.path.exists(base_path):
        base_path = "app/src/main/assets/hsk3_vocab.json"

    with open(base_path, "r", encoding="utf-8") as f:
        vocab = json.load(f)

    # ลบระดับเดิมที่กำลังอัปเดตเพื่อแทนที่ด้วยชุดใหม่จากไฟล์
    vocab = [v for v in vocab if v.get("level") != args.level]

    added = 0
    if args.input.endswith(".json"):
        with open(args.input, "r", encoding="utf-8") as f:
            incoming = json.load(f)
        for idx, item in enumerate(incoming, start=1):
            item["id"] = item.get("id") or f"hsk{args.level}_imp_{idx}"
            item["level"] = args.level
            vocab.append(item)
            added += 1
    elif args.input.endswith(".csv"):
        with open(args.input, "r", encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            for idx, row in enumerate(reader, start=1):
                simp = row.get("simplified", "").strip()
                if not simp:
                    continue
                vocab.append({
                    "id": f"hsk{args.level}_csv_{idx}",
                    "level": args.level,
                    "simplified": simp,
                    "traditional": row.get("traditional", simp).strip() or simp,
                    "pinyin": row.get("pinyin", "").strip(),
                    "pos": row.get("pos", "").strip(),
                    "meaningTh": row.get("meaningTh", "").strip(),
                    "meaningEn": row.get("meaningEn", "").strip(),
                    "exampleSimp": row.get("exampleSimp", "").strip(),
                    "exampleTrad": row.get("exampleTrad", "").strip(),
                    "examplePinyin": row.get("examplePinyin", "").strip(),
                    "exampleTh": row.get("exampleTh", "").strip()
                })
                added += 1

    os.makedirs("remote_vocab", exist_ok=True)
    with open("remote_vocab/hsk3_vocab.json", "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False, indent=2)
    with open("app/src/main/assets/hsk3_vocab.json", "w", encoding="utf-8") as f:
        json.dump(vocab, f, ensure_ascii=False, indent=2)

    manifest_path = "remote_vocab/vocab_manifest.json"
    manifest = {}
    if os.path.exists(manifest_path):
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
    manifest["version"] = args.version
    manifest["totalEntries"] = len(vocab)
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    print(f"✅ นำเข้า HSK {args.level} จำนวน {added} คำ สำเร็จ! (Version {args.version}, รวม {len(vocab)} คำ)")

if __name__ == "__main__":
    main()
