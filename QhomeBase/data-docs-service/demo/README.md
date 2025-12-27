# 🎨 Contract File Viewer - Demo Frontend

## 📋 Mô tả

Demo frontend đơn giản để upload và view contract files (PDF, Images) sử dụng HTML, CSS và JavaScript thuần.

---

## 🚀 Cách sử dụng

### **1. Mở file trong browser:**
```
Mở file: contract-file-viewer.html trong browser
```

### **2. Configuration:**
- **API Base URL:** `http://localhost:8082` (default)
- **Contract ID:** Nhập UUID của contract đã tạo
- **User ID:** Nhập UUID của user (optional)

### **3. Upload File:**
1. Click "Choose File" → Chọn file (PDF, JPEG, PNG, HEIC)
2. Check "Set as Primary File" (nếu muốn)
3. Click "Upload File"
4. Đợi upload thành công

### **4. View Files:**
1. Click "Refresh Files List" để load danh sách files
2. Click "View" trên file card để xem file
3. PDF sẽ hiển thị trong iframe
4. Images sẽ hiển thị trực tiếp

### **5. Delete File:**
- Click "Delete" trên file card → Confirm → File sẽ bị xóa

---

## ✨ Tính năng

### **1. Upload File:**
- ✅ Upload file (PDF, JPEG, PNG, HEIC)
- ✅ Validation file size (max 20MB)
- ✅ Validation file type
- ✅ Set primary file
- ✅ Auto-refresh files list sau khi upload

### **2. View Files:**
- ✅ Hiển thị danh sách files với card layout
- ✅ Hiển thị primary file với badge ⭐
- ✅ View PDF trong iframe
- ✅ View Images trực tiếp
- ✅ Download file nếu không preview được

### **3. Delete File:**
- ✅ Xóa file với confirmation
- ✅ Auto-refresh files list sau khi xóa

### **4. UI/UX:**
- ✅ Modern gradient design
- ✅ Responsive layout
- ✅ Loading states
- ✅ Success/Error alerts
- ✅ Hover effects
- ✅ Smooth animations

---

## 📁 File Structure

```
demo/
└── contract-file-viewer.html    ← Demo HTML file
```

---

## 🔧 API Endpoints Used

### **1. Upload File:**
```
POST /api/contracts/{contractId}/files
Content-Type: multipart/form-data
Body:
  - file: File
  - isPrimary: boolean
  - uploadedBy: UUID (optional)
```

### **2. Get Files List:**
```
GET /api/contracts/{contractId}/files
```

### **3. View File:**
```
GET /api/contracts/{contractId}/files/{fileId}/view
```

### **4. Delete File:**
```
DELETE /api/contracts/{contractId}/files/{fileId}
```

---

## 🎨 Screenshots

### **Upload Section:**
- File input với drag & drop style
- File name và size hiển thị sau khi chọn
- Upload button với loading state

### **Files List:**
- Grid layout với file cards
- Primary file có badge ⭐
- File info: name, type, size, upload date
- Action buttons: View, Delete

### **File Viewer:**
- PDF viewer trong iframe
- Image viewer trực tiếp
- Close button để đóng viewer

---

## ⚙️ Configuration

### **Default Settings:**
```javascript
baseUrl: 'http://localhost:8082'
contractId: '' (user input)
userId: '' (user input, optional)
```

### **File Validation:**
- Max size: 20MB
- Allowed types: PDF, JPEG, PNG, HEIC, HEIF

---

## 🔍 Troubleshooting

### **Error: "CORS policy blocked"**
- Cần cấu hình CORS trong backend để cho phép frontend access

### **Error: "File upload failed"**
- Kiểm tra service đang chạy: `http://localhost:8082`
- Kiểm tra Contract ID đúng
- Kiểm tra file size <= 20MB

### **Error: "Cannot load files"**
- Kiểm tra Contract ID đúng
- Kiểm tra service đang chạy

---

## 🚀 Next Steps

Để tích hợp vào project thực tế:

1. **Convert to React/Vue/Angular:**
   - Tách components
   - State management
   - Routing

2. **Add Authentication:**
   - JWT tokens
   - User context

3. **Add More Features:**
   - Drag & drop upload
   - Multiple file upload
   - Progress bar
   - File preview thumbnails
   - Set primary file action
   - File rename
   - Download file

4. **Error Handling:**
   - Better error messages
   - Retry logic
   - Network error handling

---

## ✅ Demo Ready!

File `contract-file-viewer.html` đã sẵn sàng để test! 🎉

Mở file trong browser và bắt đầu test! 🚀


