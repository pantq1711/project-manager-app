# Project Manager App

Ứng dụng Android quản lý dự án nhóm với các tính năng phân chia công việc, chat nhóm và quản lý ngân sách.

## 📋 Tính năng chính

### 🔐 Xác thực người dùng
- Đăng ký/Đăng nhập với Firebase Authentication
- Quản lý thông tin người dùng và phân quyền
- Bảo mật và xác thực session

### 📝 Quản lý nhiệm vụ (Task Management)
- Tạo, chỉnh sửa và xóa nhiệm vụ
- Phân công nhiệm vụ cho thành viên trong nhóm
- Theo dõi trạng thái nhiệm vụ (Chờ xử lý, Đang thực hiện, Hoàn thành)
- Thiết lập mức độ ưu tiên và hạn chót
- Lọc nhiệm vụ theo người được giao
- Phân công lại nhiệm vụ (Reassign)
- Hỗ trợ phân trang (Pagination) cho hiệu suất cao

### 💬 Chat nhóm (Group Chat)
- Gửi tin nhắn thời gian thực
- Đính kèm tệp (hình ảnh, tài liệu)
- Phân biệt tin nhắn của bản thân và người khác
- Hiển thị thời gian gửi tin nhắn
- Hỗ trợ preview tệp đính kèm

### 💰 Quản lý ngân sách (Budget Management)
- Tạo và theo dõi các khoản chi phí
- Phân loại ngân sách theo danh mục (Nhân sự, Thiết bị, Vật liệu, Khác)
- Phê duyệt/Hủy phê duyệt ngân sách
- Biểu đồ thống kê ngân sách (Pie Chart, Bar Chart)
- Sắp xếp và lọc danh sách ngân sách

## 🛠️ Công nghệ sử dụng

### Frontend (Android)
- **Language**: Java
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Components**: Material Design Components
- **Charts**: MPAndroidChart
- **Image Loading**: Glide (nếu có)

### Backend (Firebase)
- **Authentication**: Firebase Authentication
- **Database**: Cloud Firestore
- **Storage**: Firebase Storage
- **Push Notifications**: Firebase Cloud Messaging (FCM)

### Libraries chính
```gradle
// Firebase
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-storage'
implementation 'com.google.firebase:firebase-messaging'

// UI Components
implementation 'com.google.android.material:material'
implementation 'androidx.cardview:cardview'
implementation 'androidx.recyclerview:recyclerview'

// Charts
implementation 'com.github.PhilJay:MPAndroidChart'

// Architecture Components
implementation 'androidx.lifecycle:lifecycle-viewmodel'
implementation 'androidx.lifecycle:lifecycle-livedata'
```

## 📁 Cấu trúc dự án

```
app/src/main/java/com/example/projectmanager/
├── activities/          # Các Activity
│   ├── MainActivity.java
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── TaskManagerActivity.java
│   ├── GroupChatActivity.java
│   ├── BudgetActivity.java
│   └── UserManagementActivity.java
├── adapters/           # Các Adapter cho RecyclerView
│   ├── TaskAdapter.java
│   ├── ChatAdapter.java
│   ├── BudgetAdapter.java
│   └── UserManagementAdapter.java
├── models/             # Các model class
│   ├── Task.java
│   ├── Message.java
│   ├── Budget.java
│   └── User.java
├── repositories/       # Data layer
│   ├── TaskRepository.java
│   └── BudgetRepository.java
├── viewmodels/         # ViewModel classes
│   ├── TaskViewModel.java
│   └── BudgetViewModel.java
├── services/           # Background services
│   ├── FileUploadService.java
│   ├── UserSearchService.java
│   └── MyFirebaseMessagingService.java
├── utils/              # Utility classes
│   ├── FirebaseManager.java
│   ├── UserManager.java
│   ├── DateUtils.java
│   ├── BudgetSorter.java
│   └── FCMTokenManager.java
└── dialogs/            # Custom dialogs
    └── UserSelectionDialog.java
```

## 🚀 Cài đặt và chạy

### Yêu cầu hệ thống
- Android Studio Arctic Fox trở lên
- Android SDK API 21+
- Java 8+
- Google Play Services

### Các bước cài đặt

1. **Clone repository**
```bash
git clone https://github.com/your-username/project-manager-app.git
cd project-manager-app
```

2. **Cấu hình Firebase**
   - Tạo project mới trên [Firebase Console](https://console.firebase.google.com/)
   - Thêm Android app với package name `com.example.projectmanager`
   - Tải file `google-services.json` và đặt vào thư mục `app/`
   - Bật các services: Authentication, Firestore, Storage, Cloud Messaging

3. **Cấu hình Firebase Authentication**
   - Trong Firebase Console, bật Email/Password provider
   - Cấu hình domain cho ứng dụng (nếu cần)

4. **Cấu hình Firestore Database**
   - Tạo database với chế độ Test mode
   - Cấu hình rules phù hợp cho production

5. **Build và chạy**
```bash
./gradlew assembleDebug
```

## 📱 Hướng dẫn sử dụng

### Đăng ký/Đăng nhập
1. Mở ứng dụng
2. Nhập email và mật khẩu để đăng nhập
3. Hoặc chọn "Đăng ký" để tạo tài khoản mới

### Quản lý nhiệm vụ
1. Vào màn hình "Quản lý nhiệm vụ"
2. Nhập thông tin nhiệm vụ: tiêu đề, mô tả, người được giao
3. Chọn mức độ ưu tiên và hạn chót
4. Nhấn "Thêm nhiệm vụ"
5. Xem danh sách nhiệm vụ và cập nhật trạng thái

### Chat nhóm
1. Vào màn hình "Chat nhóm"
2. Nhập tin nhắn và nhấn gửi
3. Sử dụng nút đính kèm để gửi file
4. Xem lịch sử chat theo thời gian thực

### Quản lý ngân sách
1. Vào màn hình "Quản lý ngân sách"
2. Nhập thông tin khoản chi: tên, số tiền, danh mục
3. Xem biểu đồ thống kê ngân sách
4. Phê duyệt/từ chối các khoản chi

## 🔧 Cấu hình nâng cao

### Firebase Security Rules

**Firestore Rules:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Tasks collection
    match /tasks/{taskId} {
      allow read, write: if request.auth != null;
    }
    
    // Messages collection
    match /messages/{messageId} {
      allow read, write: if request.auth != null;
    }
    
    // Budgets collection
    match /budgets/{budgetId} {
      allow read, write: if request.auth != null;
    }
    
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

**Storage Rules:**
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## 🐛 Troubleshooting

### Lỗi thường gặp

1. **Lỗi kết nối Firebase**
   - Kiểm tra file `google-services.json`
   - Đảm bảo package name khớp với Firebase project

2. **Lỗi permissions**
   - Kiểm tra Firebase Security Rules
   - Đảm bảo user đã đăng nhập

3. **Lỗi tải file**
   - Kiểm tra Firebase Storage configuration
   - Đảm bảo có quyền truy cập Internet

## 🤝 Đóng góp

1. Fork project
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## 📝 Changelog

### Version 1.0.0
- Tính năng quản lý nhiệm vụ cơ bản
- Chat nhóm với đính kèm file
- Quản lý ngân sách với biểu đồ
- Xác thực người dùng với Firebase

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 👥 Tác giả

- **Tên của bạn** - [GitHub Profile](https://github.com/pantq1711)
- Email: hongan.gv10@gmail.com

## 🙏 Lời cảm ơn

- Firebase team cho các công cụ phát triển tuyệt vời
- Material Design team cho các component UI
- MPAndroidChart cho thư viện biểu đồ

---

⭐ Nếu project này hữu ích, hãy cho một star nhé!
