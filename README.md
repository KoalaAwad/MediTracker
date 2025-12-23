# 🏥 MediTracker

A full-stack web application for managing prescriptions and medicine databases. Built as an end-to-end learning project showcasing modern web development practices.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen)
![React](https://img.shields.io/badge/React-18-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)

## 📋 About

MediTracker is an **amateur full-stack web development project** designed to explore and implement various modern technologies in a real-world application. The app allows patients to manage their prescriptions, doctors to track patient medications, and administrators to manage the medicine database using OpenFDA data.

**Tech Stack:**
- **Backend:** Spring Boot + PostgreSQL
- **Frontend:** React + TypeScript + Vite
- **Deployment:** Docker Compose

---

## 🛠️ Technologies Used

### Backend
- **[Spring Boot 3.4.2](https://spring.io/projects/spring-boot)** - Java web framework
- **[Spring Data JPA](https://spring.io/projects/spring-data-jpa)** - Database abstraction layer
- **[Spring Security](https://spring.io/projects/spring-security)** - Authentication & authorization
- **[PostgreSQL 16](https://www.postgresql.org/)** - Relational database
- **[Flyway](https://flywaydb.org/)** - Database migration tool
- **[Lombok](https://projectlombok.org/)** - Boilerplate code reduction
- **[JWT (JSON Web Tokens)](https://jwt.io/)** - Token-based authentication
- **[Maven](https://maven.apache.org/)** - Build automation & dependency management
- **[HikariCP](https://github.com/brettwooldridge/HikariCP)** - JDBC connection pool

### Frontend
- **[React 18](https://react.dev/)** - UI library
- **[TypeScript](https://www.typescriptlang.org/)** - Type-safe JavaScript
- **[Vite](https://vitejs.dev/)** - Build tool & dev server
- **[Material-UI (MUI)](https://mui.com/)** - Component library
- **[Tailwind CSS](https://tailwindcss.com/)** - Utility-first CSS framework
- **[Zustand](https://zustand-demo.pmnd.rs/)** - State management
- **[React Router](https://reactrouter.com/)** - Client-side routing
- **[Axios](https://axios-http.com/)** - HTTP client

### DevOps & Deployment
- **[Docker](https://www.docker.com/)** - Containerization
- **[Docker Compose](https://docs.docker.com/compose/)** - Multi-container orchestration
- **[Nginx](https://nginx.org/)** - Reverse proxy & static file server

---

## ✨ Features

- 🔐 **User Authentication** - JWT-based secure login/registration
- 👤 **Role-Based Access Control** - Patient, Doctor, and Admin roles
- 💊 **Medicine Database** - Browse and search FDA-approved medicines
- 📝 **Prescription Management** - Create, edit, and track prescriptions
- 🗓️ **Smart Scheduling** - Daily/weekly medication schedules with timezone support
- 📊 **User Profiles** - Patient and doctor profile management
- 🌓 **Dark Mode** - Theme toggle with persistent preferences
- 📱 **Responsive Design** - Mobile-friendly UI
- 🔄 **Real-time Updates** - OpenFDA data import functionality

---

## 🚀 Installation & Setup

### Prerequisites

- **[Docker](https://docs.docker.com/get-docker/)** (version 20.10+)
- **[Docker Compose](https://docs.docker.com/compose/install/)** (version 2.0+)
- **Git** (for cloning the repository)

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/MediTracker.git
   cd MediTracker
   ```

2. **Run the deployment script**
   ```bash
   cd deploy
   ./deploy.sh
   ```

   **On Windows:**
   ```powershell
   cd deploy
   ./deploy.sh
   ```

3. **Access the application**
   - Frontend: [http://localhost](http://localhost)
   - Backend API: [http://localhost:8080/api](http://localhost:8080/api)

### What the deployment script does:
1. Builds the Spring Boot backend (Maven)
2. Builds the React frontend (npm)
3. Creates Docker images for frontend, backend, and PostgreSQL
4. Starts all containers via Docker Compose
5. Applies database migrations (Flyway)
6. Seeds initial data (optional)

---

## 📦 Manual Installation (Development)

### Backend Setup

```bash
# Navigate to project root
cd MediTracker

# Build the backend
mvn clean install

# Run the application
mvn spring-boot:run
```

**Backend runs on:** `http://localhost:8080`

### Frontend Setup

```bash
# Navigate to frontend directory
cd medi-tracker-frontend

# Install dependencies
npm install

# Run development server
npm run dev
```

**Frontend runs on:** `http://localhost:5173` (Vite dev server)

### Database Setup

**Using Docker (Recommended):**
```bash
docker run -d \
  --name meditracker-db \
  -e POSTGRES_USER=myuser \
  -e POSTGRES_PASSWORD=mypassword \
  -e POSTGRES_DB=meditracker \
  -p 5432:5432 \
  postgres:16-alpine
```

**Manual PostgreSQL:**
1. Install PostgreSQL 16
2. Create database: `CREATE DATABASE meditracker;`
3. Update `application.properties` with your credentials

---

## 🗂️ Project Structure

```
MediTracker/
├── src/                          # Spring Boot backend
│   └── main/
│       ├── java/
│       │   └── org/springbozo/meditracker/
│       │       ├── config/       # Security, CORS, JWT config
│       │       ├── controller/   # REST API endpoints
│       │       ├── model/        # JPA entities
│       │       ├── repository/   # Data access layer
│       │       └── service/      # Business logic
│       └── resources/
│           ├── application.properties
│           └── db/migration/     # Flyway SQL migrations
├── medi-tracker-frontend/        # React frontend
│   ├── src/
│   │   ├── api/                  # API client functions
│   │   ├── components/           # React components
│   │   ├── pages/                # Page components
│   │   ├── zustand/              # State management
│   │   └── lib/                  # Utilities & theme
│   ├── public/                   # Static assets
│   └── nginx.conf                # Nginx configuration
├── deploy/                       # Deployment scripts
│   ├── deploy.sh                 # Main deployment script
│   ├── docker-compose.yaml       # Docker orchestration
│   └── db-backup/                # Database seed files
├── pom.xml                       # Maven dependencies
└── Dockerfile                    # Backend Docker image
```

---

## 🐳 Docker Architecture

The application consists of 3 containers:

1. **meditracker-backend** (Spring Boot)
   - Port: 8080
   - Connects to PostgreSQL
   - Runs Flyway migrations on startup

2. **meditracker-frontend** (React + Nginx)
   - Port: 80
   - Serves static files
   - Proxies `/api` requests to backend

3. **meditracker-db** (PostgreSQL 16)
   - Port: 5432
   - Persistent volume for data storage

---

## 🔑 Default Credentials

After initial setup, you can register a new user or use test accounts (if seeded):

**Admin User:**
- Email: `admin@meditracker.com`
- Password: `password`

**Test Patient:**
- Email: `patient@test.com`
- Password: `password`

---

## 📚 API Documentation

### Authentication Endpoints
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and receive JWT token
- `GET /api/auth/me` - Get current user info

### Medicine Endpoints
- `GET /api/medicine` - Get all medicines (paginated)
- `GET /api/medicine/{id}` - Get medicine by ID
- `POST /api/medicine/import` - Import OpenFDA data (Admin only)
- `DELETE /api/medicine/{id}` - Soft delete medicine (Admin only)

### Prescription Endpoints
- `GET /api/prescriptions/me` - Get user's prescriptions
- `POST /api/prescriptions` - Create prescription
- `PUT /api/prescriptions/{id}` - Update prescription
- `DELETE /api/prescriptions/{id}` - Delete prescription

### Profile Endpoints
- `GET /api/profile` - Get user profile
- `PUT /api/profile/patient` - Update patient profile
- `PUT /api/profile/doctor` - Update doctor profile

### Admin Endpoints
- `GET /api/admin/users` - List all users
- `PUT /api/admin/users/{id}/role` - Update user role
- `DELETE /api/admin/users/{id}` - Delete user

---

## 🧪 Testing

### Backend Tests
```bash
mvn test
```

### Frontend Tests
```bash
cd medi-tracker-frontend
npm test
```

---

## 🛠️ Development

### Backend Hot Reload
```bash
mvn spring-boot:run
```

### Frontend Hot Reload
```bash
cd medi-tracker-frontend
npm run dev
```

### Database Migrations
Create new migration in `src/main/resources/db/migration/`:
```sql
-- V{version}__{description}.sql
-- Example: V2025_12_23_001__add_user_preferences.sql
```

---

## 🤝 Contributing

This is a personal learning project, but contributions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

---

## 🙏 Acknowledgments

- **OpenFDA** - For providing the medicine database
- **Spring Boot** - For the excellent backend framework
- **React** - For the powerful frontend library
- **Material-UI** - For beautiful UI components
- **ShadCN UI** - For design inspiration

---

## 📧 Contact

**Project Link:** [https://github.com/yourusername/MediTracker](https://github.com/yourusername/MediTracker)

---

## 🎯 Future Enhancements

- [ ] Email notifications for prescription reminders
- [ ] Mobile app (React Native)
- [ ] Medicine interaction checker
- [ ] Analytics dashboard
- [ ] Export prescriptions to PDF
- [ ] Multi-language support
- [ ] Integration with pharmacy APIs

---

**Built with ❤️ as a learning project**

