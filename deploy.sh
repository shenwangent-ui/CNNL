set -e
python3 preflight_check.py
git add -A
#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "📦 1. กำลังส่งโค้ดที่อัปเดตขึ้น GitHub..."
git add .
git commit -m "Update UX/UI and performance $(date +'%Y-%m-%d %H:%M')" || echo "ไม่มีไฟล์เปลี่ยนแปลงใหม่"
git pull --rebase origin main
git push origin main

HEAD_SHA=$(git rev-parse HEAD)
REMOTE_URL=$(git remote get-url origin)
TOKEN=$(echo "$REMOTE_URL" | sed -n 's#https://[^:]*:\([^@]*\)@github.com.*#\1#p')

echo "⏳ 2. รอ GitHub Actions คอมไพล์ไฟล์ APK (ประมาณ 1-2 นาที)..."
python3 - "$HEAD_SHA" "$TOKEN" << 'PYEOF'
import sys, time, json, urllib.request, subprocess, os

head_sha = sys.argv[1]
token = sys.argv[2]

class StripAuthRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        new_req = super().redirect_request(req, fp, code, msg, headers, newurl)
        if new_req is not None:
            new_req.remove_header("Authorization")
        return new_req

opener = urllib.request.build_opener(StripAuthRedirect)
api_headers = {
    "User-Agent": "Termux-ShenHan",
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {token}"
}

def api_get(url):
    req = urllib.request.Request(url, headers=api_headers)
    with opener.open(req, timeout=20) as r:
        return json.loads(r.read().decode("utf-8"))

time.sleep(5)
while True:
    try:
        data = api_get("https://api.github.com/repos/shenwangent-ui/CNNL/actions/runs?per_page=5")
        runs = data.get("workflow_runs", [])
        target = next((r for r in runs if r.get("head_sha") == head_sha), runs[0] if runs else None)
        if target and target.get("status") == "completed":
            print("")
            conclusion = target.get("conclusion")
            if conclusion == "success":
                print("✅ คอมไพล์ APK สำเร็จ!")
                break
            else:
                print(f"❌ Build ไม่ผ่าน ({conclusion})")
                run_id = target["id"]
                jobs = api_get(f"https://api.github.com/repos/shenwangent-ui/CNNL/actions/runs/{run_id}/jobs").get("jobs", [])
                if jobs:
                    l_req = urllib.request.Request(
                        f"https://api.github.com/repos/shenwangent-ui/CNNL/actions/jobs/{jobs[0]['id']}/logs",
                        headers=api_headers
                    )
                    logs = opener.open(l_req, timeout=20).read().decode("utf-8", errors="replace")
                    for line in logs.splitlines():
                        if "e: file://" in line or "Unresolved reference" in line or "Type mismatch" in line:
                            print(line.split("Z ", 1)[-1])
                sys.exit(1)
    except Exception:
        pass
    print(".", end="", flush=True)
    time.sleep(6)

print("📥 3. กำลังดาวน์โหลด ShenHan-debug.apk ผ่าน GitHub API...")
releases = api_get("https://api.github.com/repos/shenwangent-ui/CNNL/releases")
target_asset = None
for rel in releases:
    for asset in rel.get("assets", []):
        if asset["name"].endswith(".apk"):
            target_asset = asset
            break
    if target_asset:
        break

if not target_asset:
    print("❌ ไม่พบไฟล์ .apk ใน GitHub Releases")
    sys.exit(1)

dest = "/storage/emulated/0/Download/ShenHan-debug.apk"
dl_headers = {
    "User-Agent": "Termux-ShenHan",
    "Accept": "application/octet-stream",
    "Authorization": f"Bearer {token}"
}
dl_req = urllib.request.Request(target_asset["url"], headers=dl_headers)
with opener.open(dl_req, timeout=60) as r, open(dest, "wb") as f:
    f.write(r.read())

if not os.path.exists(dest) or os.path.getsize(dest) < 1000000:
    print("❌ ไฟล์ที่โหลดมาไม่สมบูรณ์ กรุณาลองใหม่อีกครั้ง")
    sys.exit(1)

size_mb = os.path.getsize(dest) / (1024 * 1024)
print(f"📲 4. ดาวน์โหลดเสร็จแล้ว ({size_mb:.1f} MB) กำลังเปิดหน้าติดตั้ง...")
subprocess.run(["termux-open", dest])
PYEOF
