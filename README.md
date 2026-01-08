# MediTracker
Hi!

This is my full-stack web application for managing prescriptions and a medicine database. Built as an end-to-end learning project showcasing modern web development practices.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen)
![React](https://img.shields.io/badge/React-18-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

MediTracker is an **amateur full-stack web development project** designed by me as an educational project to learn and implement various web frameworks. This web app allows users to register and track their prescriptions from a list of FDA approved medicine.

**Tech Stack:**
- **Backend:** Spring Boot + PostgreSQL
- **Frontend:** React + TypeScript + Vite
- **Deployment:** Docker Compose

---

## Technologies Used
These are some of the frameworks, libraries, or plugins used in the creation of this web application (that i can remember)

### Backend
- **[Spring Boot 3.4.2](https://spring.io/projects/spring-boot)** - Java web framework
- **[Spring Data JPA](https://spring.io/projects/spring-data-jpa)** - Database abstraction layer
- **[Spring Security](https://spring.io/projects/spring-security)** - Token-based authentication & authorization
- **[PostgreSQL 16](https://www.postgresql.org/)** - Relational database
- **[Flyway](https://flywaydb.org/)** - Database migration tool
- **[Lombok](https://projectlombok.org/)** - Boilerplate code reduction
- **[JWT (JSON Web Tokens)](https://jwt.io/)** - Token-based authentication
- **[Maven](https://maven.apache.org/)** - Build automation & dependency management
- **[HikariCP](https://github.com/brettwooldridge/HikariCP)** - JDBC connection pool

### Frontend
- **[React 18](https://react.dev/)** - React library
- **[TypeScript](https://www.typescriptlang.org/)** - Type-safe JavaScript
- **[Vite](https://vitejs.dev/)** - Build tool & dev server
- **[Material-UI (MUI)](https://mui.com/)** - Component library
- **[Tailwind CSS](https://tailwindcss.com/)** - Utility-first CSS framework
- **[Zustand](https://zustand-demo.pmnd.rs/)** - State management
- **[React Router](https://reactrouter.com/)** - Client-side routing
- **[Axios](https://axios-http.com/)** - HTTP client

### DevOps & Deployment
- **[Docker](https://www.docker.com/)** - Containerization
- **[Nginx](https://nginx.org/)** - Reverse proxy & static file server 
- **[Postman](https://www.postman.com/)** - Testing Controllers and Endpoints

---

## Installation & Setup

### Prerequisites

- **[Docker](https://docs.docker.com/get-docker/)** (version 20.10+)
- **[Docker Compose](https://docs.docker.com/compose/install/)** (version 2.0+)

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

3. **Access the application**
   - Frontend: [http://localhost](http://localhost) (port 80)
   - Default Creds:
       * Email: `admin@meditracker.com`
       * Password: `password`
   - Backend API: [http://localhost:8080/api](http://localhost:8080/api)
   - Default 

### What the deployment script does:
1. Builds the Spring Boot backend (Maven)
2. Builds the React frontend (npm)
3. Uses Docker files to create Docker images for frontend, backend, and PostgreSQL
4. Starts all containers via Docker Compose
5. Applies the database migrations in /src/main/resources/db/migration (Flyway)
   
---

## Manual Installation (Development)

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

# optional - eslint for code analysis 
npm run lint

# Run development server
npm run dev
```

**Frontend runs on:** `http://localhost:####` (Check console for port)

### Database Setup

```bash
docker run -d \
  --name meditracker-db \
  -e POSTGRES_USER=myuser \
  -e POSTGRES_PASSWORD=mypassword \
  -e POSTGRES_DB=meditracker \
  -p 5432:5432 \
  postgres:16-alpine
```

---

## Project Structure

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

## Docker Architecture

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

## APIs

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

### Database Migrations
Flyway is in the project dependencies so to migrate data, create a flyway migration at `src/main/resources/db/migration/`:
```sql
-- V{version}__{description}.sql
-- Example: V2025_12_23_001__add_user_preferences.sql
```

---

## Contributing

This is a personal learning project, however contributions forks, or issues opened for recommendations/ pointers are more than welcome! <3
---


## Sources

- **OpenFDA** - For providing the medicine database
- **[Eazy Bytes, Madan Reddy](www.udemy.com/course/spring-springboot-jpa-hibernate-zero-to-master)** - For the excellent spring core course
- **[OpenFDA](https://open.fda.gov/apis/drug/event/)** - For providing API documentation and the medicine database
- **[Scrimba](https://scrimba.com)** - For learning JavaScript, TypeScript, and React
- **[LeetJourney](https://www.youtube.com/watch?v=5TY9V5xLW8o)** - for implementing Token based authentication
- **Material-UI and ShadCN UI** - For prebuilt UI components and styling

<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />
<br />


<pre>
       z
    z
 z
 |\__/,|   (`\
 |_ _  |.--.) )
 ( T   )     /
(((^_(((/(((_/
</pre>

