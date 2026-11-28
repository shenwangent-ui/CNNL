#!/usr/bin/env python3
import argparse, csv, json, os, subprocess

def load_existing(out_file):
    if os.path.exists(out_file):
        with open(out_file, "r", encoding="utf-8") as f:
            return json.load(f)
    return []

def parse_input_file(filepath, level):
    ext = os.path.splitext(filepath)[1].lower()
    records = []
    if ext == ".json":
        with open(filepath, "r", encoding="utf-8") as f:
            for idx, item in enumerate(json.load(f), 1):
                simp = str(item.get("simplified") or item.get("hanzi") or item.get("word") or "").strip()
                if not simp:
                    continue
                trad = str(item.get("traditional") or item.get("hant") or simp).strip()
                records.append({
                    "id": item.get("id") or f"hsk3_remote_{level}_{idx}",
                    "level": level,
                    "simplified": simp,
                    "traditional": trad,
                    "pinyin": str(item.get("pinyin", "")).strip(),
                    "pos": str(item.get("pos", "n")).strip(),
                    "meaningTh": str(item.get("meaningTh") or item.get("meaning_th") or item.get("th") or "").strip(),
                    "meaningEn": str(item.get("meaningEn") or item.get("meaning_en") or item.get("en") or "").strip(),
                    "exampleSimp": str(item.get("exampleSimp") or item.get("example_simp") or "").strip(),
                    "exampleTrad": str(item.get("exampleTrad") or item.get("example_trad") or "").strip(),
                    "examplePinyin": str(item.get("examplePinyin") or item.get("example_pinyin") or "").strip(),
                    "exampleTh": str(item.get("exampleTh") or item.get("example_th") or "").strip()
                })
    else:
        delimiter = "\t" if ext == ".tsv" else ","
        with open(filepath, "r", encoding="utf-8-sig") as f:
            for idx, row in enumerate(csv.DictReader(f, delimiter=delimiter), 1):
                simp = (row.get("simplified") or row.get("hanzi") or row.get("word") or "").strip()
                if not simp:
                    continue
                trad = (row.get("traditional") or row.get("hant") or simp).strip()
                records.append({
                    "id": f"hsk3_remote_{level}_{idx}",
                    "level": level,
                    "simplified": simp,
                    "traditional": trad,
                    "pinyin": (row.get("pinyin") or "").strip(),
                    "pos": (row.get("pos") or "n").strip(),
                    "meaningTh": (row.get("meaningTh") or row.get("th") or "").strip(),
                    "meaningEn": (row.get("meaningEn") or row.get("en") or "").strip(),
                    "exampleSimp": (row.get("exampleSimp") or "").strip(),
                    "exampleTrad": (row.get("exampleTrad") or "").strip(),
                    "examplePinyin": (row.get("examplePinyin") or "").strip(),
                    "exampleTh": (row.get("exampleTh") or "").strip()
                })
    return records

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--level", type=int, required=True, choices=[1, 2, 3, 4, 5, 6])
    parser.add_argument("--file", type=str, required=True)
    parser.add_argument("--no-push", action="store_true")
    args = parser.parse_args()

    os.makedirs("curriculum", exist_ok=True)
    out_file = f"curriculum/hsk{args.level}.json"
    existing = load_existing(out_file)
    seen = {x["simplified"] for x in existing}

    added = 0
    for item in parse_input_file(args.file, args.level):
        if item["simplified"] not in seen:
            seen.add(item["simplified"])
            existing.append(item)
            added += 1

    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(existing, f, ensure_ascii=False, indent=2)

    print(f"✅ บันทึก {out_file} สำเร็จ (เพิ่มใหม่ {added} คำ | รวม HSK {args.level} = {len(existing)} คำ)")
    if not args.no_push:
        subprocess.run(["git", "add", out_file], check=True)
        subprocess.run(["git", "commit", "-m", f"Upload HSK {args.level} curriculum ({len(existing)} words)"], check=True)
        subprocess.run(["git", "push"], check=True)
        print("🚀 อัปโหลดขึ้นหลังบ้านเรียบร้อย!")

if __name__ == "__main__":
    main()
