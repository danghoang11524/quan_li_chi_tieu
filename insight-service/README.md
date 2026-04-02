# Insight Service - Dịch vụ Thông minh Tài chính

Service cung cấp các tính năng thông minh để giúp người dùng quản lý tài chính tốt hơn.

## Tính năng chính

### 🌤️ 1. Tích hợp Weather API
- Lấy thông tin thời tiết hiện tại
- Gợi ý chi tiêu dựa trên thời tiết
- API Key: `35c28d82c7c8469aa5512731252711`

**Endpoints:**
- `GET /api/insights/weather/current?location=Hanoi` - Lấy thời tiết hiện tại
- `GET /api/insights/weather/advice?location=Hanoi` - Gợi ý dựa trên thời tiết

### 💡 2. Gợi ý theo thói quen chi tiêu
- Gợi ý dựa trên ngày trong tuần
- Gợi ý dựa trên thời điểm trong tháng
- Phân tích cá nhân hóa theo thu nhập/chi tiêu
- Tích hợp thông tin thời tiết

**Endpoints:**
- `GET /api/insights/spending-habits/suggestions?location=Hanoi` - Lấy gợi ý chung
- `GET /api/insights/spending-habits/personalized?monthlyIncome=15000000&monthlySpending=10000000` - Phân tích cá nhân

### 📚 3. Tips tài chính cá nhân
- Hơn 20 tips tài chính chất lượng cao
- Phân loại theo chủ đề: Tiết kiệm, Đầu tư, Ngân sách, Nợ...
- Tips ngẫu nhiên hàng ngày

**Endpoints:**
- `GET /api/insights/tips` - Lấy tất cả tips
- `GET /api/insights/tips/category/SAVING` - Tips theo category
- `GET /api/insights/tips/random` - Tip ngẫu nhiên
- `GET /api/insights/tips/daily?count=3` - Tips hàng ngày

**Categories:**
- `SAVING` - Tiết kiệm
- `INVESTING` - Đầu tư
- `DEBT_MANAGEMENT` - Quản lý nợ
- `BUDGETING` - Lập ngân sách
- `EMERGENCY_FUND` - Quỹ khẩn cấp
- `SHOPPING` - Mua sắm thông minh
- `GENERAL` - Tổng quát

### 🎁 4. Thử thách tiết kiệm

#### Challenge 7 ngày
Tiết kiệm một số tiền cố định mỗi ngày trong 7 ngày.

**Endpoints:**
```bash
# Tạo thử thách 7 ngày
POST /api/insights/challenges
{
  "type": "7_DAY",
  "dailyAmount": 50000
}

# Ghi nhận tiết kiệm
POST /api/insights/challenges/{id}/record
{
  "amount": 50000
}
```

#### Challenge 52 tuần
Tiết kiệm tăng dần mỗi tuần: Tuần 1 = 10,000đ, Tuần 2 = 20,000đ, ..., Tuần 52 = 520,000đ
**Tổng tiết kiệm: 13,780,000đ**

**Endpoints:**
```bash
# Tạo thử thách 52 tuần
POST /api/insights/challenges
{
  "type": "52_WEEK",
  "reverse": false
}

# Lấy chi tiết challenge
GET /api/insights/challenges/{id}

# Lấy động lực
GET /api/insights/challenges/{id}/motivation

# Số tiền cần tiết kiệm tuần này
GET /api/insights/challenges/{id}/next-amount

# Tạm dừng/tiếp tục
PUT /api/insights/challenges/{id}/pause
PUT /api/insights/challenges/{id}/resume

# Hủy challenge
DELETE /api/insights/challenges/{id}
```

### 🔔 5. Nhắc nhở chi tiêu

#### Nhắc ghi chép giao dịch hàng ngày
```bash
POST /api/insights/reminders/daily-transaction?time=21:00
```

#### Nhắc thanh toán hóa đơn
```bash
POST /api/insights/reminders/bill-payment
{
  "title": "Tiền điện",
  "dueDate": "2024-11-15",
  "reminderTime": "10:00"
}
```

#### Nhắc nhở tùy chỉnh
```bash
POST /api/insights/reminders/custom
{
  "title": "Đóng bảo hiểm",
  "description": "Nhớ đóng bảo hiểm y tế",
  "dueDate": "2024-12-01",
  "reminderTime": "09:00",
  "frequency": "MONTHLY"
}
```

**Quản lý reminders:**
- `GET /api/insights/reminders` - Tất cả reminders
- `GET /api/insights/reminders/active` - Reminders đang active
- `GET /api/insights/reminders/type/{type}` - Lọc theo loại
- `PUT /api/insights/reminders/{id}` - Cập nhật
- `PUT /api/insights/reminders/{id}/toggle` - Bật/tắt
- `DELETE /api/insights/reminders/{id}` - Xóa
- `POST /api/insights/reminders/initialize-defaults` - Tạo reminders mẫu

**Reminder Types:**
- `DAILY_TRANSACTION` - Nhắc ghi giao dịch hàng ngày
- `BILL_PAYMENT` - Nhắc thanh toán hóa đơn
- `CUSTOM` - Tùy chỉnh

**Reminder Frequency:**
- `DAILY` - Hàng ngày
- `WEEKLY` - Hàng tuần
- `MONTHLY` - Hàng tháng
- `ONCE` - Một lần

## Cấu hình

### Application Properties
```yaml
weather:
  api:
    key: 35c28d82c7c8469aa5512731252711
    url: http://api.weatherapi.com/v1
```

### Database
Service sử dụng database riêng: `expense_insight_db`

**Tables:**
- `saving_challenges` - Lưu thử thách tiết kiệm
- `reminders` - Lưu nhắc nhở
- `financial_tips` - Lưu tips tài chính

## Chạy service

### Local
```bash
cd insight-service
mvn spring-boot:run
```

Service chạy tại: `http://localhost:8087`

### Docker
```bash
docker-compose up insight-service
```

## Headers cần thiết

Các API yêu cầu header `X-User-Id`:
```bash
curl -H "X-User-Id: 1" http://localhost:8087/api/insights/challenges
```

## Ví dụ sử dụng

### 1. Lấy gợi ý dựa trên thời tiết và thói quen
```bash
curl -H "X-User-Id: 1" \
  "http://localhost:8087/api/insights/spending-habits/suggestions?location=Hanoi"
```

### 2. Tạo thử thách 52 tuần
```bash
curl -X POST -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"type":"52_WEEK","reverse":false}' \
  http://localhost:8087/api/insights/challenges
```

### 3. Lấy tips ngẫu nhiên
```bash
curl http://localhost:8087/api/insights/tips/daily?count=5
```

### 4. Tạo nhắc nhở thanh toán tiền điện
```bash
curl -X POST -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tiền điện",
    "dueDate": "2024-11-15",
    "reminderTime": "10:00"
  }' \
  http://localhost:8087/api/insights/reminders/bill-payment
```

## Tính năng tự động

### Scheduled Jobs
- **Reminder Checker**: Chạy mỗi giờ để kiểm tra và gửi nhắc nhở
- **Financial Tips**: Tự động khởi tạo 20+ tips khi service start lần đầu

## Port
- **8087** - HTTP Port

## Dependencies
- Spring Boot 3.2.0
- Spring Data JPA
- MySQL 8.0
- WebFlux (cho Weather API calls)
- Lombok

## Tích hợp với các service khác

Service này có thể tích hợp với:
- **Notification Service** - Để gửi reminders qua email/SMS
- **Transaction Service** - Để phân tích thói quen chi tiêu thực tế
- **User Service** - Để lấy thông tin user và preferences
