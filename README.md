# SmartCharge

SmartCharge is a web application for managing EV charging stations and scheduling charging requests. It demonstrates how limited chargers can be allocated to competing requests, with separate workflows for drivers, station operators, and administrators.

## Features

- Driver registration and login, vehicle management, and charging requests with current and target battery levels.
- Charger and connector selection, request status and charging history, estimated charging duration, and simulated charging sessions.
- FCFS (First Come, First Served), SJF (Shortest Job First), and Priority scheduling. An administrator can change the active scheduling algorithm.
- Emergency or priority requests that an administrator can review, approve, or decline.
- Station and charger management for operators, including operational status updates.
- Station wait estimates and route lookup from a driver's location. Routes use OpenRouteService when configured and fall back to the public OSRM table service if ORS is unreachable.
- PostgreSQL persistence through Spring Data JPA and Hibernate.

The app simulates charging-session progress and completion; it does not connect to physical chargers or a payment processor. The current repository does not include the chatbot described in older project notes.

## Roles and demo accounts

On first startup, the backend creates sample accounts, a vehicle, a station, and a charger if they do not already exist:

| Role | Email | Password |
| --- | --- | --- |
| Driver | `test@smartcharge.com` | `123456` |
| Station operator | `operator@smartcharge.com` | `123456` |
| Administrator | `admin@smartcharge.com` | `123456` |

These are local demonstration credentials. Change or remove them before using the application in any shared or production environment.

## Technology

- Frontend: HTML, CSS, and browser JavaScript, served as static resources by Spring Boot.
- Backend: Java 25, Spring Boot 4.1.1, Spring Web, Spring Data JPA, and Spring Security.
- Database: PostgreSQL.
- Build: Apache Maven.
- Optional routing: OpenRouteService (ORS); OSRM is used as a fallback when an ORS request cannot be completed.

## Requirements

Install the following on your machine:

1. **Java Development Kit (JDK) 25** and ensure `java` is on your `PATH` (`java -version`).
2. **Apache Maven** and ensure `mvn` is on your `PATH` (`mvn -version`). This repository does not include the Maven wrapper.
3. **PostgreSQL** running locally and accepting connections on port `5432`.
4. Git, if you are cloning the repository.

The provided `start-backend.bat` helper is for Windows. On macOS or Linux, run the Maven command shown below from the `backend` directory.

## Download the project

Clone the repository and enter its directory:

```bash
git clone <repository-url>
cd SmartCharge
```

Replace `<repository-url>` with the HTTPS or SSH clone URL shown by your Git hosting service. Alternatively, download and extract the repository ZIP, then open a terminal in the extracted `SmartCharge` directory.

## Configure PostgreSQL

Create a database named `smartcharge` and make sure the PostgreSQL account matches the connection settings in `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/smartcharge
spring.datasource.username=postgres
spring.datasource.password=postgres
```

For example, with `psql` and a PostgreSQL administrator account:

```sql
CREATE DATABASE smartcharge;
```

If your local PostgreSQL username or password differs, update those three `spring.datasource.*` settings before starting the app. The database user needs permission to create and update tables. Hibernate is configured with `ddl-auto=update`, so tables are created/updated on startup; `database/schema.sql` is provided as a reference and is not automatically executed by the application.

## Optional: enable road routing

Station routes require an OpenRouteService API key. Set `ORS_API_KEY` in the environment where the backend runs. The backend starts without it, but the route endpoint will report that routing is not configured. If ORS is temporarily unreachable, the backend attempts the public OSRM fallback; that fallback also needs internet access.

On Windows, you can put the key in the ignored `.ors-key` file at the project root for the included batch script to read. Do not commit or share this file. Alternatively, set the variable in PowerShell before starting:

```powershell
$env:ORS_API_KEY = "your-openrouteservice-api-key"
```

On macOS or Linux:

```bash
export ORS_API_KEY="your-openrouteservice-api-key"
```

## Run the application

### Windows

From the project root, run:

```bat
start-backend.bat
```

The script checks PostgreSQL on `localhost:5432`, starts Spring Boot, and opens the frontend at `http://localhost:8080/` when the server responds. Keep the terminal open while using the application; closing it stops the backend.

### macOS or Linux

From the project root, start the backend with Maven:

```bash
cd backend
mvn spring-boot:run
```

When startup completes, open [http://localhost:8080/](http://localhost:8080/) in a browser. Leave the terminal running while you use the site. To stop the backend, press `Ctrl+C`.

The frontend is served by Spring Boot, so do not open the HTML files directly from the filesystem. The browser pages expect the backend at `http://localhost:8080`.

## First use

1. Open `http://localhost:8080/` and choose a role.
2. Sign in using one of the demo accounts above, or register a driver account.
3. As a driver, manage vehicles, create a charging request, and view request/session status.
4. As an operator, manage station and charger records and their statuses.
5. As an administrator, review requests and emergency requests, and choose the scheduling algorithm.

## Project layout

```text
SmartCharge/
├── backend/
│   ├── pom.xml                         # Maven dependencies and build configuration
│   └── src/main/
│       ├── java/smartcharge/           # REST API, scheduling, allocation, and services
│       └── resources/application.properties
├── database/schema.sql                 # Reference PostgreSQL schema
├── frontend/                           # User, operator, and admin HTML pages
├── start-backend.bat                   # Windows startup helper
└── open-frontend.ps1                   # Opens the site after backend startup
```

## Troubleshooting

- **Database connection refused:** Start PostgreSQL and verify it listens on `localhost:5432`.
- **Database does not exist:** Create `smartcharge` before starting the backend.
- **Password authentication failed:** Update the datasource username/password in `backend/src/main/resources/application.properties` to match your PostgreSQL setup.
- **Maven or Java version error:** Confirm `mvn -version` uses JDK 25 and `java -version` reports Java 25.
- **Port 8080 is already in use:** Stop the other service using the port or change `server.port` in `application.properties`, then open the corresponding local URL.
- **Routing unavailable:** Set a valid `ORS_API_KEY`; routing also requires internet access and valid coordinates for stations.

