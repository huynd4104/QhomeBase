# 📋 Contract Management - Postman Collection

## 🎯 TỔNG QUAN

Postman collection để test các API endpoints của Contract Management service.

---

## 📥 IMPORT

### **1. Import Collection:**
```
File → Import → Chọn file: Contract_Management.postman_collection.json
```

### **2. Import Environment:**
```
File → Import → Chọn file: Contract_Management.postman_environment.json
```

### **3. Select Environment:**
- Chọn environment: **"Contract Management - Local"** ở góc trên bên phải

---

## 🔧 ENVIRONMENT VARIABLES

### **Required Variables:**
- `baseUrl`: `http://localhost:8082` (default)
- `unitId`: UUID của unit (cần có sẵn trong database)
- `userId`: UUID của user (có thể dùng random UUID)

### **Auto-set Variables (sau khi tạo contract/file):**
- `contractId`: Được set tự động sau khi tạo contract
- `contractNumber`: Được set tự động sau khi tạo contract
- `contractFileId`: Được set tự động sau khi upload file
- `contractFileName`: Được set tự động sau khi upload file

---

## 📋 API ENDPOINTS

### **1. Contract Management**

#### **1.1. Create RENTAL Contract**
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/contracts?createdBy={{userId}}`
- **Body:**
```json
{
  "unitId": "{{unitId}}",
  "contractNumber": "HD-RENTAL-{{$timestamp}}",
  "contractType": "RENTAL",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "monthlyRent": 5000000,
  "notes": "Hợp đồng thuê căn hộ (đã thanh toán đầy đủ)",
  "status": "ACTIVE"
}
```
- **Required:** `monthlyRent` (tiền thuê đã thanh toán)

#### **1.2. Create PURCHASE Contract**
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/contracts?createdBy={{userId}}`
- **Body:**
```json
{
  "unitId": "{{unitId}}",
  "contractNumber": "HD-PURCHASE-{{$timestamp}}",
  "contractType": "PURCHASE",
  "startDate": "2024-01-01",
  "purchasePrice": 5000000000,
  "purchaseDate": "2024-01-01",
  "notes": "Hợp đồng mua căn hộ (đã thanh toán đầy đủ)",
  "status": "ACTIVE"
}
```
- **Required:** `purchasePrice` và `purchaseDate` (đã thanh toán đầy đủ)

#### **1.3. Get Contract By ID**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}`
- **Response:** Contract với tất cả files

#### **1.4. Get Contracts By Unit**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/contracts/unit/{{unitId}}`

#### **1.5. Get Active Contracts**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/contracts/active`

#### **1.6. Get Active Contracts By Unit**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/contracts/unit/{{unitId}}/active`

#### **1.7. Update Contract**
- **Method:** `PUT`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}?updatedBy={{userId}}`
- **Body:**
```json
{
  "notes": "Updated notes",
  "status": "ACTIVE"
}
```

#### **1.8. Delete Contract**
- **Method:** `DELETE`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}`
- **Note:** Soft delete

---

### **2. Contract File Management**

#### **2.1. Upload Single Contract File**
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}/files`
- **Body:** `multipart/form-data`
  - `file`: **Select File** (PDF, JPEG, PNG, HEIC) - **Max 20MB** ⚠️ **MUST SELECT FILE**
  - `isPrimary`: `true` (optional)
  - `uploadedBy`: `{{userId}}` (optional)
- **Note:** File được lưu với UUID filename

**⚠️ IMPORTANT:** 
- Phải **chọn file thực sự** trong Postman (click "Select Files" và chọn file từ máy tính)
- Không để trống field `file`
- File phải có kích thước > 0 bytes

#### **2.2. Upload Multiple Contract Files**
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}/files/multiple`
- **Body:** `multipart/form-data`
  - `files`: Multiple files
  - `uploadedBy`: `{{userId}}` (optional)
- **Note:** File đầu tiên sẽ được set làm primary

#### **2.3. Get Contract Files**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}/files`
- **Response:** Danh sách tất cả files của contract

#### **2.4. View Contract File (Inline)**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}/files/{{contractFileId}}/view`
- **Response:** File content (PDF viewer trong browser)

#### **2.5. Download Contract File**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}/files/{{contractFileId}}/download`
- **Response:** File download

#### **2.6. Direct File Access (Alternative)**
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/files/contracts/{{contractId}}/{{contractFileName}}`
- **Note:** Truy cập trực tiếp bằng fileName (không cần fileId)

#### **2.7. Set Primary File**
- **Method:** `PUT`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}/files/{{contractFileId}}/primary`
- **Note:** Set file làm primary (file hiện tại sẽ bị bỏ primary)

#### **2.8. Delete Contract File**
- **Method:** `DELETE`
- **URL:** `{{baseUrl}}/api/contracts/{{contractId}}/files/{{contractFileId}}`
- **Note:** Soft delete

---

## 🧪 TEST WORKFLOW

### **Workflow 1: Tạo RENTAL Contract + Upload File**
```
1. Create RENTAL Contract
   ↓ (contractId được set tự động)
2. Upload Single Contract File
   ↓ (contractFileId được set tự động)
3. Get Contract By ID (verify contract + files)
4. View Contract File (test PDF viewer)
```

### **Workflow 2: Tạo PURCHASE Contract + Upload Multiple Files**
```
1. Create PURCHASE Contract
   ↓ (contractId được set tự động)
2. Upload Multiple Contract Files
   ↓ (contractFileId được set tự động)
3. Get Contract Files (verify files list)
4. Set Primary File (change primary)
5. Download Contract File (test download)
```

### **Workflow 3: Full CRUD**
```
1. Create RENTAL Contract
2. Get Contract By ID
3. Upload Single Contract File
4. Get Contract Files
5. Update Contract
6. Set Primary File
7. Delete Contract File
8. Delete Contract
```

---

## ✅ VALIDATION RULES

### **RENTAL Contract:**
- ✅ **Required:** `startDate`, `monthlyRent`
- ✅ **Optional:** `endDate`
- ❌ **Cannot have:** `purchasePrice`, `purchaseDate`

### **PURCHASE Contract:**
- ✅ **Required:** `startDate`, `purchasePrice`, `purchaseDate`
- ❌ **Cannot have:** `monthlyRent`, `endDate`

### **File Upload:**
- ✅ **Max size:** 20MB
- ✅ **Allowed types:** PDF, JPEG, PNG, HEIC, HEIF
- ✅ **Auto-generate:** UUID filename

---

## 🔍 TROUBLESHOOTING

### **Error: "Contract not found"**
- Kiểm tra `contractId` có đúng không
- Kiểm tra contract đã tồn tại trong database chưa

### **Error: "Monthly rent is required for RENTAL contracts"**
- Thêm `monthlyRent` vào request body cho RENTAL contract

### **Error: "Purchase price is required for PURCHASE contracts"**
- Thêm `purchasePrice` và `purchaseDate` vào request body cho PURCHASE contract

### **Error: "File size exceeds 20MB limit"**
- Giảm kích thước file xuống dưới 20MB

### **Error: "File type not allowed"**
- Chỉ chấp nhận: PDF, JPEG, PNG, HEIC, HEIF

### **Error: "Contract number already exists"**
- Thay đổi `contractNumber` (dùng timestamp để unique)

---

## 📝 NOTES

1. **Auto-set Variables:** Collection tự động set `contractId`, `contractFileId` sau khi tạo thành công
2. **File Storage:** File được lưu tại: `./uploads/contracts/{contractId}/{UUID}.{ext}`
3. **UUID Filename:** File được lưu với UUID filename (không dùng tên gốc)
4. **Primary File:** File đầu tiên tự động được set làm primary
5. **Soft Delete:** Delete contract/file chỉ set flag `is_deleted = true`, không xóa thật

---

## 🚀 READY TO TEST!

Import collection và environment, sau đó chọn workflow phù hợp để test! 🎉

