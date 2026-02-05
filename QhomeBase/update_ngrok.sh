#!/bin/bash
# Lấy URL Public từ ngrok (hỗ trợ cả Python 3 có sẵn trên Mac)
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | python3 -c "import sys, json; print(json.load(sys.stdin)['tunnels'][0]['public_url'])" 2>/dev/null)

if [ -z "$NGROK_URL" ]; then
    echo "❌ Ngrok chưa chạy!"
else
    # Xóa file cũ và ghi mới
    echo "VNPAY_BASE_URL=$NGROK_URL" > .env.ngrok
    echo "✅ Đã cập nhật .env.ngrok: VNPAY_BASE_URL=$NGROK_URL"
fi