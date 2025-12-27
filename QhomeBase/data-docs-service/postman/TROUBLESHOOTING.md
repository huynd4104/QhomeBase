# 🔧 Troubleshooting - Contract Management API

## ❌ LỖI THƯỜNG GẶP

### **1. Error: "Error at index 0 in: \"g7h8\""**

**Nguyên nhân:**
- UUID format không đúng
- Biến environment (`unitId`, `userId`) chưa được set hoặc format sai

**Cách fix:**
1. Kiểm tra environment variables:
   - `unitId` phải là UUID format: `550e8400-e29b-41d4-a716-446655440011`
   - `userId` phải là UUID format: `a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6`

2. Set environment variables đúng format:
   ```
   unitId: 550e8400-e29b-41d4-a716-446655440011
   userId: a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6
   ```

3. Đảm bảo không có ký tự đặc biệt hoặc khoảng trắng trong UUID

---

### **2. Error: "Contract not found"**

**Nguyên nhân:**
- `contractId` không tồn tại trong database
- Biến `contractId` chưa được set sau khi tạo contract

**Cách fix:**
1. Tạo contract trước (POST /api/contracts)
2. Kiểm tra `contractId` đã được set trong environment
3. Sử dụng `contractId` đúng trong các request tiếp theo

---

### **3. Error: "Monthly rent is required for RENTAL contracts"**

**Nguyên nhân:**
- Thiếu field `monthlyRent` trong request body cho RENTAL contract

**Cách fix:**
Thêm `monthlyRent` vào request body:
```json
{
  "contractType": "RENTAL",
  "monthlyRent": 5000000,
  ...
}
```

---

### **4. Error: "Purchase price is required for PURCHASE contracts"**

**Nguyên nhân:**
- Thiếu field `purchasePrice` hoặc `purchaseDate` trong request body cho PURCHASE contract

**Cách fix:**
Thêm `purchasePrice` và `purchaseDate` vào request body:
```json
{
  "contractType": "PURCHASE",
  "purchasePrice": 5000000000,
  "purchaseDate": "2024-01-01",
  ...
}
```

---

### **5. Error: "Contract number already exists"**

**Nguyên nhân:**
- `contractNumber` đã tồn tại trong database

**Cách fix:**
1. Thay đổi `contractNumber` (dùng timestamp để unique)
2. Hoặc xóa contract cũ trước khi tạo mới

---

### **6. Error: "Failed to store empty file" hoặc "File is empty or not provided"**

**Nguyên nhân:**
- File không được chọn trong Postman
- File bị rỗng (0 bytes)
- Form data field `file` không có file được attach

**Cách fix:**
1. **Chọn file thực sự trong Postman:**
   - Body tab → form-data
   - Click vào field `file` → Chọn type là "File" (không phải "Text")
   - Click "Select Files" → Chọn file từ máy tính
   - Đảm bảo file đã được attach (có tên file hiển thị)

2. **Kiểm tra file không rỗng:**
   - File phải có kích thước > 0 bytes
   - File phải là file thực sự (không phải empty file)

3. **Kiểm tra file format:**
   - Chỉ chấp nhận: PDF, JPEG, PNG, HEIC, HEIF

**Hình ảnh minh họa Postman:**
```
Body → form-data → 
  file [File] [Select Files] ← Click đây để chọn file
  isPrimary [Text] [true]
```

---

### **7. Error: "File size exceeds 20MB limit"**

**Nguyên nhân:**
- File upload lớn hơn 20MB

**Cách fix:**
1. Giảm kích thước file xuống dưới 20MB
2. Hoặc compress file trước khi upload

---

### **8. Error: "File type not allowed"**

**Nguyên nhân:**
- File type không nằm trong danh sách cho phép

**Cách fix:**
Chỉ upload các file type sau:
- `application/pdf` - PDF files
- `image/jpeg` - JPEG images
- `image/png` - PNG images
- `image/heic` - HEIC images
- `image/heif` - HEIF images

---

### **9. Error: "JSON parse error" hoặc "Invalid JSON"**

**Nguyên nhân:**
- JSON format không đúng (dấu phẩy thừa, dấu ngoặc sai, etc.)

**Cách fix:**
1. Kiểm tra JSON format trong request body
2. Loại bỏ dấu phẩy thừa ở cuối object
3. Đảm bảo tất cả strings có dấu ngoặc kép
4. Validate JSON bằng JSON validator

**Example JSON đúng:**
```json
{
  "unitId": "550e8400-e29b-41d4-a716-446655440011",
  "contractNumber": "HD-RENTAL-1234567890",
  "contractType": "RENTAL",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 5000000,
  "notes": "Hợp đồng thuê căn hộ",
  "status": "ACTIVE"
}
```

**Example JSON sai (dấu phẩy thừa):**
```json
{
  "monthlyRent": 5000000,  ← Dấu phẩy thừa ở đây
}
```

---

### **10. Error: "Connection refused" hoặc "Network error"**

**Nguyên nhân:**
- Service chưa chạy
- Port không đúng
- baseUrl sai

**Cách fix:**
1. Kiểm tra service đã chạy chưa: `http://localhost:8082`
2. Kiểm tra `baseUrl` trong environment: `http://localhost:8082`
3. Kiểm tra port trong application.properties: `server.port=8082`

---

### **11. Error: "Variable not found: {{unitId}}"**

**Nguyên nhân:**
- Environment variable chưa được set
- Environment chưa được select

**Cách fix:**
1. Select environment đúng: **"Contract Management - Local"**
2. Set environment variables:
   - `unitId`: UUID của unit
   - `userId`: UUID của user
   - `baseUrl`: `http://localhost:8082`

---

## ✅ CHECKLIST TRƯỚC KHI TEST

1. ✅ Service đã chạy: `http://localhost:8082`
2. ✅ Environment đã được select: **"Contract Management - Local"**
3. ✅ Environment variables đã được set:
   - `baseUrl`: `http://localhost:8082`
   - `unitId`: UUID format đúng
   - `userId`: UUID format đúng
4. ✅ JSON format đúng (không có dấu phẩy thừa)
5. ✅ Required fields đã có trong request body

---

## 🔍 DEBUG TIPS

### **1. Check Environment Variables:**
```
Click vào environment → Xem tất cả variables
```

### **2. Check Request Body:**
```
Click vào request → Body → Kiểm tra JSON format
```

### **3. Check Response:**
```
Click vào response → Body → Xem error message chi tiết
```

### **4. Check Console:**
```
View → Show Postman Console → Xem request/response details
```

---

## 📝 COMMON FIXES

### **Fix UUID Format:**
```
Sai: 550e8400-e29b-41d4-a716-4466554400111  (thừa 1 ký tự)
Đúng: 550e8400-e29b-41d4-a716-446655440011
```

### **Fix JSON Format:**
```
Sai: {
  "monthlyRent": 5000000,
}
Đúng: {
  "monthlyRent": 5000000
}
```

### **Fix Timestamp:**
```
Sai: HD-RENTAL-{{timestamp}}
Đúng: HD-RENTAL-{{$timestamp}}
```

---

## 🚀 QUICK TEST

1. **Test Environment Variables:**
   - Create RENTAL Contract → Check response có 201
   - Nếu 400 → Check UUID format

2. **Test JSON Format:**
   - Copy JSON body → Paste vào JSON validator
   - Nếu invalid → Fix format

3. **Test Service:**
   - GET http://localhost:8082/actuator/health
   - Nếu 200 → Service đang chạy

---

## ✅ NẾU VẪN LỖI

1. Check logs trong application console
2. Check database connection
3. Check Flyway migrations đã chạy chưa
4. Check application.properties configuration

---

## 📞 SUPPORT

Nếu vẫn gặp lỗi, cung cấp:
1. Error message đầy đủ
2. Request body (JSON)
3. Environment variables
4. Response body
5. Application logs

