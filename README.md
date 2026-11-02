# SmartCharge

**SmartCharge** is an academic web application for managing EV charging stations and scheduling charging requests. It demonstrates how a system can allocate a limited number of chargers, handle competing requests, and give drivers, station operators, and administrators different tools.

The application includes a browser-based frontend, a Java/Spring Boot backend, a PostgreSQL database, and an optional local Python service that classifies emergency requests. It is designed to run locally. A GitHub repository contains the source code; it does not host the running application or its database.

## Features

- Driver registration and login, vehicle management, charging requests, and request/session history.
- Charger and connector selection, estimated charging duration, and queue wait estimates.
- FCFS (First Come, First Served), SJF (Shortest Job First), and Priority scheduling. Administrators can choose the active scheduling algorithm.
- Station and charger management for station operators.
- Administrator dashboards for requests, emergencies, scheduling, and charging analytics.
- Optional route lookup using OpenRouteService, with a public OSRM fallback when ORS is unavailable.
- A guided SmartCharge Help chatbot with prepared FAQs, troubleshooting guidance, and authenticated lookup of the signed-in driver's latest request.
- Optional email OTP verification for new driver accounts using Brevo.
- A local zero-shot NLP classifier for emergency reasons. If the NLP service is unavailable, an emergency request remains pending for administrator review.
- PostgreSQL persistence through Spring Data JPA and Hibernate.

Charging sessions are simulated; the application does not control physical chargers. Booking payments are simulated and do not process real money. The guided chatbot uses prepared content and does not call an external AI service.

## Project components

| Component | Technology | Purpose |
| --- | --- | --- |
| Frontend | HTML, CSS, browser JavaScript | Driver, operator, and administrator pages |
| Backend | Java 25, Spring Boot, Spring Web, Spring Security, Spring Data JPA | API, authentication, scheduling, allocation, and application logic |
| Database | PostgreSQL | Users, vehicles, stations, chargers, requests, sessions, and payment records |
| Emergency classification | Python 3.11, FastAPI, Transformers, CPU-only PyTorch | Local classification of emergency reasons |
| Build | Apache Maven | Build and run the backend |

## Requirements

For the complete local experience, install:

1. **Git** to clone the repository.
2. **JDK 25** and ensure `java` is on your `PATH` (`java -version`).
3. **Apache Maven** and ensure `mvn` is on your `PATH` (`mvn -version`). This repository does not include the Maven wrapper.
4. **Docker Desktop** (or another Docker Engine) to run PostgreSQL, or an existing PostgreSQL database.
5. **64-bit Python 3.11** for the local emergency classification service.

The Windows startup helper is `start-backend.bat`. The backend can also be started with Maven on macOS and Linux. The NLP setup scripts provided in this repository are for Windows; instructions for running its service manually on macOS/Linux are included below.

## 1. Get the project

Clone the repository and enter the project directory:

```bash
git clone https://github.com/asmita-05/SmartCharge.git
cd SmartCharge
```

You can also use **Code → Download ZIP** on GitHub, then extract the archive. GitHub provides the source files; each person running the application needs their own local database and configuration.

## 2. Start PostgreSQL

The application expects PostgreSQL at `localhost:5432` and a database named `smartcharge` by default. The following command starts a local PostgreSQL 16 container with persistent Docker storage:

```powershell
docker run --name smartcharge-postgres `
  -e POSTGRES_DB=smartcharge `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=choose-a-local-password `
  -p 5432:5432 `
  -v smartcharge-postgres-data:/var/lib/postgresql/data `
  -d postgres:16
```

Replace `choose-a-local-password` with a password you choose for your local database. Keep it private. If the container already exists, start it instead of creating it again:

```powershell
docker start smartcharge-postgres
```

Check that it is running:

```powershell
docker ps
```

The named Docker volume keeps the database data when the container stops. The application creates and updates its tables at startup with Hibernate (`spring.jpa.hibernate.ddl-auto=update`). `database/schema.sql` is a reference schema; Spring Boot does not automatically execute it.

If PostgreSQL is already installed on your computer, you can use that instead. Create a database named `smartcharge` and use the matching database username and password in the environment variables below. If you map the container to a different host port, update `spring.datasource.url` in `backend/src/main/resources/application.properties` accordingly.

## 3. Configure local environment variables

The committed `application.properties` uses environment variables for sensitive settings. Configure them in the terminal where you will start the backend. Do not commit real passwords, API keys, or signing secrets.

In PowerShell, set the database credentials. Use the same password you provided to Docker:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "choose-a-local-password"
```

Create a random JWT signing secret for this terminal session:

```powershell
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:SMARTCHARGE_JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

`SMARTCHARGE_JWT_SECRET` should be at least 32 UTF-8 bytes and should stay stable while the same local app is running. If it changes, users may need to sign in again. For longer-term local use, configure the variables in your operating system's environment-variable settings rather than generating a new JWT secret every time.

Optional integrations:

- Set `ORS_API_KEY` to enable OpenRouteService routing. Without it, the backend starts but ORS routing is unavailable; the route service may use its public OSRM fallback.
- Set `BREVO_API_KEY` and `BREVO_SENDER_EMAIL` to deliver OTP emails through a verified Brevo sender. Without these, email verification cannot deliver signup codes.

These are environment variables, not a `.env` file: Spring Boot does not automatically load a `.env` file in this project.

## 4. Set up the emergency NLP service

The emergency classifier uses the pinned `MoritzLaurer/deberta-v3-base-zeroshot-v1.1-all-33` model. Its weights are downloaded locally and are not stored in GitHub. The initial setup needs internet access and several hundred megabytes of disk space.

### Windows

From the project root, run:

```powershell
Set-Location .\backend\nlp
powershell -ExecutionPolicy Bypass -File .\setup-nlp.ps1
powershell -ExecutionPolicy Bypass -File .\download-model.ps1
Set-Location ..\..
```

The scripts create `backend/nlp/.venv`, install CPU-only PyTorch and the Python dependencies, and download the pinned model snapshot to `backend/nlp/model/`. These generated folders are ignored by Git.

### macOS/Linux

The included setup/download helpers are PowerShell scripts. To set up the NLP service manually, use Python 3.11 and run from the project root:

```bash
cd backend/nlp
python3.11 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install torch
python -m pip install -r requirements.txt
python -c "from huggingface_hub import snapshot_download; snapshot_download(repo_id='MoritzLaurer/deberta-v3-base-zeroshot-v1.1-all-33', revision='2a8a9dd6777d61f3b6331fca71436bf8f75b50d6', local_dir='model', allow_patterns=['config.json','model.safetensors','added_tokens.json','special_tokens_map.json','spm.model','tokenizer.json','tokenizer_config.json'])"
```

Keep the virtual environment active and start the service in its own terminal:

```bash
python emergency_classifier.py
```

The local NLP service listens on `127.0.0.1:8090`. If it is not running, the backend can still run, but emergency requests will remain pending for administrator review until they can be classified.

## 5. Run SmartCharge

### Windows

Make sure PostgreSQL is running and the environment variables from step 3 are set in the current PowerShell session. From the project root, run:

```powershell
.\start-backend.bat
```

Set up the NLP environment and model in step 4 before using this helper. The script checks PostgreSQL and port `8080`, starts the NLP service in a separate window if it is not already running, then starts Spring Boot. Keep the backend and NLP windows open while using the application. The browser should open at [http://localhost:8080/](http://localhost:8080/).

### macOS/Linux

Start PostgreSQL and the NLP service first. In another terminal, set the required variables and run Maven from `backend`:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD='choose-a-local-password'
export SMARTCHARGE_JWT_SECRET='replace-with-a-random-secret-at-least-32-bytes-long'
cd backend
mvn spring-boot:run
```

When startup completes, open [http://localhost:8080/](http://localhost:8080/) in a browser. Keep both services running while using the app. Press `Ctrl+C` in the backend terminal to stop Spring Boot.

The frontend is served by Spring Boot. Do not open the HTML files directly from the filesystem; the pages call the backend at `http://localhost:8080`.

## 6. Try the demo

On first startup, the backend creates sample accounts, a vehicle, a station, and a charger if they do not already exist. Use these local demonstration accounts:

| Role | Email | Password |
| --- | --- | --- |
| Driver | `test@smartcharge.com` | `123456` |
| Station operator | `operator@smartcharge.com` | `123456` |
| Administrator | `admin@smartcharge.com` | `123456` |

Suggested walkthrough:

1. Sign in as the driver and review the vehicle, then create a charging request.
2. Try charger/connector selection and review request status, queue estimates, and charging history.
3. Use the operator account to inspect stations and update charger or station status.
4. Use the administrator account to review requests and emergencies, view analytics, and change the scheduling algorithm.
5. Try the guided chatbot from the driver pages.

New driver signups require OTP email verification when Brevo is configured. Demo accounts are available for local testing without email delivery.

**These demo credentials are public and are not safe for a shared or production deployment. Change or remove them before exposing the app to other users.** Use HTTPS and configure deployment secrets through the hosting environment for any internet-facing deployment.

## Configuration reference

| Variable | Required | Purpose |
| --- | --- | --- |
| `DB_USERNAME` | No; defaults to `postgres` | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `SMARTCHARGE_JWT_SECRET` | Yes | Signs authentication tokens; use a private value of at least 32 UTF-8 bytes |
| `ORS_API_KEY` | No | Enables OpenRouteService routing |
| `ORS_BASE_URL` | No | Overrides the routing service base URL |
| `BREVO_API_KEY` | No | Enables Brevo transactional email |
| `BREVO_SENDER_EMAIL` | No | Verified sender address used for OTP email |
| `BREVO_SENDER_NAME` | No | Sender display name; defaults to `SmartCharge` |
| `SMARTCHARGE_NLP_URL` | No | NLP service URL; defaults to `http://127.0.0.1:8090` |
| `SMARTCHARGE_NLP_TIMEOUT_SECONDS` | No | NLP request timeout; defaults to 45 seconds |
| `SESSION_COOKIE_SECURE` | No | Set `true` when deployed behind HTTPS; defaults to `false` for local HTTP |

## Troubleshooting

- **Database connection refused:** Start Docker/the PostgreSQL service and confirm it listens on port `5432`.
- **Database does not exist:** Create the `smartcharge` database or check `spring.datasource.url`.
- **Password authentication failed:** Make sure `DB_USERNAME` and `DB_PASSWORD` match the PostgreSQL container or server.
- **Could not resolve `SMARTCHARGE_JWT_SECRET`:** Set that variable in the same environment used to start the backend.
- **Maven or Java version error:** Confirm `java -version` and `mvn -version` use JDK 25.
- **Port 8080 is already in use:** Stop the other service or change `server.port` in `application.properties`.
- **NLP requests stay pending:** Complete the Python setup/model download and verify the NLP service is ready on `127.0.0.1:8090`.
- **OTP email is not delivered:** Configure a valid Brevo API key and verified sender email address.
- **Routing is unavailable:** Configure `ORS_API_KEY`; routing also needs internet access and valid station coordinates.

## Project structure

```text
SmartCharge/
├── backend/
│   ├── nlp/                            # Python NLP service and setup scripts
│   ├── src/main/java/smartcharge/     # Spring Boot application code
│   └── src/main/resources/            # Backend configuration
├── database/
│   └── schema.sql                      # Reference PostgreSQL schema
├── frontend/                           # Driver, operator, and admin pages
├── start-backend.bat                   # Windows startup helper
└── README.md
```

## License

This project is licensed under the [MIT License](LICENSE).
