# GIS Analytics Platform

Hệ thống GIS nâng cao được xây dựng với Spring Boot 3 + PostgreSQL/PostGIS và ReactJS + MapLibre GL. Hệ thống hỗ trợ các tính năng không gian địa lý nâng cao, quản lý layer, truy vấn không gian, geofencing, và live tracking.

## 🚀 Tính năng chính

### Backend (Spring Boot 3)
- ✅ Quản lý Layer (POINT, LINE, POLYGON)
- ✅ CRUD Features với GeoJSON
- ✅ Truy vấn không gian nâng cao:
  - Buffer query
  - Within/Contains/Intersects
  - Nearest neighbor (KNN)
  - Spatial join
- ✅ Geofencing với WebSocket
- ✅ Live device tracking
- ✅ Upload GeoJSON/Shapefile/CSV
- ✅ JWT Authentication & Authorization
- ✅ Redis caching
- ✅ MinIO/S3 storage
- ✅ OpenAPI/Swagger documentation

### Frontend (React 18 + TypeScript)
- ✅ MapView với MapLibre GL
- ✅ Layer control & legend
- ✅ Draw/Edit features
- ✅ Spatial query builder
- ✅ Geofence management
- ✅ Upload interface
- ✅ Responsive UI với TailwindCSS

## 📋 Yêu cầu hệ thống

- Docker & Docker Compose
- Java 17+ (nếu chạy backend trực tiếp)
- Node.js 20+ (nếu chạy frontend trực tiếp)
- PostgreSQL 15+ với PostGIS (nếu chạy DB trực tiếp)

## 🛠️ Cài đặt và chạy

### 1. Clone repository

```bash
git clone <repository-url>
cd gis-analytics-platform
```

### 2. Cấu hình môi trường

Copy file `.env.example` thành `.env` và chỉnh sửa nếu cần:

```bash
cp .env.example .env
```

### 3. Chạy với Docker Compose

```bash
docker-compose up -d
```

Lệnh này sẽ khởi động tất cả các services:
- **PostgreSQL + PostGIS** (port 5432)
- **Redis** (port 6379)
- **MinIO** (port 9000, console 9001)
- **Backend** (port 8081)
- **Frontend** (port 5173)
- **TileServer GL** (port 8082)

### 4. Kiểm tra services

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)

### 5. Tài khoản mặc định

Hệ thống đã được seed với 3 tài khoản mặc định:

| Username | Password | Role | Quyền |
|----------|----------|------|-------|
| `admin` | `admin123` | ADMIN | Quản trị toàn bộ hệ thống |
| `editor` | `editor123` | EDITOR | CRUD layers và features |
| `viewer` | `viewer123` | VIEWER | Chỉ xem và truy vấn |

## 📊 Dữ liệu mẫu

Sau khi chạy migrations, hệ thống sẽ tự động tạo:

- **3 Layers**:
  - `poi` - Points of Interest (~500 điểm)
  - `roads` - Road Network (~50 đường)
  - `zones` - Planning Zones (~10 vùng)

- **1 Geofence**: Central District

- **3 Devices**: DEV001, DEV002, DEV003

## 🔌 API Endpoints

### Authentication

```
POST /api/auth/login
POST /api/auth/refresh
```

### Layers

```
GET    /api/layers
GET    /api/layers/{id}
GET    /api/layers/code/{code}
POST   /api/layers
PUT    /api/layers/{id}
PUT    /api/layers/{id}/style
DELETE /api/layers/{id}
```

### Features

```
GET    /api/features?layerId={id}
GET    /api/features/bbox?layerId={id}&minLng={}&minLat={}&maxLng={}&maxLat={}
GET    /api/features/{id}
POST   /api/features
PUT    /api/features/{id}
DELETE /api/features/{id}
```

### Spatial Queries

```
POST /api/query/buffer
POST /api/query/within
POST /api/query/intersect
GET  /api/query/nearest?layerId={id}&lng={}&lat={}&k={}
POST /api/query/spatial-join
GET  /api/query/geojson?layerId={id}
```

### Geofences

```
GET    /api/geofences
GET    /api/geofences/{id}
POST   /api/geofences
PUT    /api/geofences/{id}
DELETE /api/geofences/{id}
```

### Devices

```
GET  /api/devices
GET  /api/devices/{id}
POST /api/devices/position
```

## 📝 Ví dụ API

### 1. Login

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "...",
    "username": "admin",
    "email": "admin@gis.local",
    "role": "ADMIN"
  }
}
```

### 2. Get Layers

```bash
curl -X GET http://localhost:8081/api/layers \
  -H "Authorization: Bearer {accessToken}"
```

### 3. Get Features in Bounding Box

```bash
curl -X GET "http://localhost:8081/api/features/bbox?layerId={layerId}&minLng=106.6&minLat=10.7&maxLng=106.7&maxLat=10.8" \
  -H "Authorization: Bearer {accessToken}"
```

### 4. Buffer Query

```bash
curl -X POST http://localhost:8081/api/query/buffer \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "layerId": "{layerId}",
    "center": [106.67, 10.77],
    "radiusMeters": 1000
  }'
```

### 5. Nearest Neighbor Query

```bash
curl -X GET "http://localhost:8081/api/query/nearest?layerId={layerId}&lng=106.67&lat=10.77&k=5" \
  -H "Authorization: Bearer {accessToken}"
```

## 🗂️ Cấu trúc dự án

```
gis-analytics-platform/
├── backend/                 # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/gis/
│   │   │   │   ├── config/      # Configuration classes
│   │   │   │   ├── controller/  # REST controllers
│   │   │   │   ├── dto/         # Data Transfer Objects
│   │   │   │   ├── entity/      # JPA entities
│   │   │   │   ├── repository/  # JPA repositories
│   │   │   │   ├── service/     # Business logic
│   │   │   │   ├── security/    # JWT & security
│   │   │   │   └── util/        # Utilities
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/ # Flyway migrations
│   │   └── test/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/                # React frontend
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── pages/          # Page components
│   │   ├── lib/            # Utilities & API client
│   │   ├── store/          # Zustand stores
│   │   └── types/          # TypeScript types
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
│
├── docker/
│   ├── tileserver/
│   │   └── config.json
│   └── ...
│
├── docker-compose.yml
├── .env.example
└── README.md
```

## 🔧 Cấu hình

### Backend Configuration (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/gisdb
    username: gisuser
    password: gispass
  
  jpa:
    database-platform: org.hibernate.spatial.dialect.postgis.PostgisDialect

jwt:
  secret: your-256-bit-secret-key
  expiration: 86400000  # 24 hours
```

### Frontend Configuration

Tạo file `.env` trong thư mục `frontend/`:

```env
VITE_API_URL=http://localhost:8081
```

## 🧪 Testing

### Backend Tests

```bash
cd backend
mvn test
```

### Frontend Tests

```bash
cd frontend
npm test
```

## 📚 Tài liệu API

Sau khi khởi động backend, truy cập Swagger UI:

http://localhost:8081/swagger-ui.html

## 🐛 Troubleshooting

### Database connection issues

Kiểm tra PostgreSQL đã khởi động:

```bash
docker-compose ps postgres
```

### Port conflicts

Nếu các port đã được sử dụng, chỉnh sửa file `.env` hoặc `docker-compose.yml`.

### Frontend không kết nối được backend

Kiểm tra:
1. Backend đã chạy: http://localhost:8081/actuator/health
2. CORS đã được cấu hình đúng
3. API URL trong frontend config đúng

## 🔐 Bảo mật

- JWT tokens với expiration
- Password hashing với BCrypt
- Role-based access control (RBAC)
- CORS configuration
- Input validation

## 📈 Performance

- Redis caching cho queries
- GiST indexes cho spatial queries
- KNN operator cho nearest neighbor
- Pagination cho large datasets
- Connection pooling

## 🚧 Roadmap

- [ ] Vector tiles (MVT) support
- [ ] Raster analysis (elevation, slope)
- [ ] Isochrone calculation
- [ ] Heatmap generation
- [ ] Clustering
- [ ] Export to various formats
- [ ] Offline MBTiles support
- [ ] Advanced styling with MapLibre

## 📄 License

MIT License

## 👥 Contributors

- Initial development

## 📞 Support

Nếu gặp vấn đề, vui lòng tạo issue trên repository.

---

**Lưu ý**: Đây là phiên bản development. Để sử dụng trong production, cần:
- Thay đổi JWT secret
- Cấu hình HTTPS
- Thiết lập backup database
- Cấu hình monitoring & logging
- Review security settings

