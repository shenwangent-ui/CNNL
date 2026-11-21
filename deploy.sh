#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "🔄 1. กำลังส่งโค้ดที่อัปเดตขึ้น GitHub..."
git add .
git commit -m "Update UX/UI and performance $(date +'%Y-%m-%d %H:%M')" || echo "ไม่มีไฟล์เปลี่ยนแปลงใหม่"
git pull --rebase origin main
git push origin main

REMOTE_URL=$(git remote get-url origin)
TOKEN=$(echo "$REMOTE_URL" | sed -n 's|https://[^:]*:\([^@]*\)@github.com/.*|\1|p')

echo "⏳ 2. รอ GitHub Actions คอมไพล์ไฟล์ APK (ประมาณ 2-3 นาที)..."
sleep 12
while true; do
  RUN_JSON=$(curl -s -H "Authorization: token $TOKEN" "https://api.github.com/repos/shenwangent-ui/CNNL/actions/runs?per_page=1")
  STATUS=$(echo "$RUN_JSON" | jq -r '.workflow_runs[0].status')
  CONCLUSION=$(echo "$RUN_JSON" | jq -r '.workflow_runs[0].conclusion')
  if [ "$STATUS" = "completed" ]; then
    if [ "$CONCLUSION" = "success" ]; then
      echo ""
      echo "✅ คอมไพล์ APK สำเร็จ!"
      break
    else
      echo ""
      echo "❌ Build ไม่ผ่าน ($CONCLUSION) กรุณาเช็ก Log บน GitHub Actions"
      exit 1
    fi
  fi
  printf "."
  sleep 10
done

echo "📥 3. กำลังดาวน์โหลด ShenHan-debug.apk..."
curl -L -s -o /storage/emulated/0/Download/ShenHan-debug.apk "https://github.com/shenwangent-ui/CNNL/releases/download/latest-debug/ShenHan-debug.apk"

if adb devices | grep -w "device" >/dev/null 2>&1; then
  echo "📲 4. กำลังติดตั้งอัปเดตทับแอปเดิมอัตโนมัติ..."
  PKG_NAME=$(grep -oE 'applicationId\s*=\s*"[^"]+"' app/build.gradle.kts | cut -d'"' -f2)
  adb install -r -d /storage/emulated/0/Download/ShenHan-debug.apk || (adb uninstall "$PKG_NAME" 2>/dev/null || true; adb install /storage/emulated/0/Download/ShenHan-debug.apk)
  echo "🎉 อัปเดตแอป ShenHan เสร็จสมบูรณ์พร้อมใช้งาน!"
else
  echo "⚠️ ยังไม่ได้เชื่อมต่อ Wireless ADB (ไฟล์พร้อมติดตั้งอยู่ที่ Download/ShenHan-debug.apk)"
fi
