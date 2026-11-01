# SmartCharge

SmartCharge is a web application for managing EV charging stations and scheduling charging requests. It demonstrates how limited chargers can be allocated to competing requests, with separate workflows for drivers, station operators, and administrators.

## Features

- Driver registration and login, vehicle management, and charging requests with current and target battery levels.
- Public driver signup with database-backed accounts, BCrypt password hashing, and signed, short-lived login tokens. Each browser tab keeps its own role/account login. Public signup cannot create operator or administrator accounts.
- Charger and connector selection, request status and charging history, estimated charging duration, and simulated charging sessions.
- Simulated advance payment for charging bookings at a demo rate of ₹8.50/kWh. Drivers see estimated energy, charging duration, queue wait and amount before choosing a simulated success or failure. A failure cancels the still-unallocated booking; success makes it eligible for scheduling. Each attempt is saved in the user's payment history. No real payment provider or payment credentials are used.
- FCFS (First Come, First Served), SJF (Shortest Job First), and Priority scheduling. An administrator can change the active scheduling algorithm.
- Pre-trained, local zero-shot NLP classification of emergency reasons. Serious predictions are automatically approved, routine predictions are declined, and NLP service failures remain pending for admin review. Admins can override either model decision from the emergency dashboard.
- Station and charger management for operators, including operational status updates.
- Admin analytics for charging performance and all-time revenue by station from successful payment records.
- Station wait estimates and route lookup from a driver's location. Routes use OpenRouteService when configured and fall back to the public OSRM table service if ORS is unreachable.
- Guided SmartCharge Help chatbot with menu-based FAQs, safe troubleshooting guidance, and authenticated lookup of the signed-in user's latest request. It runs without an external LLM or chatbot API.
- PostgreSQL persistence through Spring Data JPA and Hibernate.

The app simulates charging-session progress and completion; it does not connect to physical chargers or a payment processor. Advance payment is a demonstration only: drivers can simulate success or failure before a booking enters the scheduler. Failed attempts are kept in `booking_payment_attempts`; successful/current booking payment state is stored in `booking_payments`. Failed bookings remain as cancelled request records for audit/history, with no charger allocation or session. Older post-session demo payment records remain in `payments` and `payment_attempts`. The guided chatbot uses prepared help content and menu selections; it does not accept arbitrary chat text or call an external AI model.

Passwords for existing accounts are upgraded from legacy plaintext storage to BCrypt hashes at startup. The demo credentials below are only for local demonstrations; replace them and configure HTTPS before exposing the application beyond a trusted local environment.

`start-backend.bat` reads the signing key from the ignored `.jwt-secret` file. A random key has been generated for this checkout so tokens remain valid across local backend restarts. For another deployment, configure `SMARTCHARGE_JWT_SECRET` with a private value of at least 32 UTF-8 bytes, and keep it stable across application instances. If the key changes, users must sign in again.

## Configure email OTP signup with Brevo

New public signups remain pending until the email address is verified with a six-digit OTP. Codes expire after 10 minutes, allow up to five verification attempts, and can be resent after a cooldown. To enable delivery:

1. Create a Brevo API key and register/verify a sender address in Brevo. Brevo requires a registered sender for transactional mail; domain authentication is recommended for reliable delivery.
2. Put the API key by itself in the ignored `.brevo-key` file at the project root.
3. Put the verified sender email address by itself in the ignored `.brevo-sender` file.
4. Start the project with `start-backend.bat`. These files are ignored by Git. Alternatively set `BREVO_API_KEY` and `BREVO_SENDER_EMAIL` in the backend environment.

The API key stays on the backend and is sent to Brevo's transactional email endpoint. The app stores only a hash of each OTP and deletes expired pending signups during periodic cleanup. Brevo sender setup and API usage are documented in its [transactional email guide](https://developers.brevo.com/docs/send-a-transactional-email).

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
- Local emergency NLP: Python, CPU-only PyTorch, Hugging Face Transformers, and a pinned DeBERTa-v3-base zero-shot model. The weights are downloaded locally and are not committed to Git.
- Database: PostgreSQL.
- Build: Apache Maven.
- Optional routing: OpenRouteService (ORS); OSRM is used as a fallback when an ORS request cannot be completed.

## Requirements

Install the following on your machine:

1. **Java Development Kit (JDK) 25** and ensure `java` is on your `PATH` (`java -version`).
2. **Apache Maven** and ensure `mvn` is on your `PATH` (`mvn -version`). This repository does not include the Maven wrapper.
3. **PostgreSQL** running locally and accepting connections on port `5432`.
4. Git, if you are cloning the repository.
5. **64-bit Python 3.11** for the local emergency NLP classifier.

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

## Set up local emergency NLP

SmartCharge uses the pre-trained [`MoritzLaurer/deberta-v3-base-zeroshot-v1.1-all-33`](https://huggingface.co/MoritzLaurer/deberta-v3-base-zeroshot-v1.1-all-33) model. It compares the emergency reason with two fixed descriptions; it does not train a model or call a hosted inference API. The model is English-only. Its model card reports an MIT license for the base model and discusses the training-data licenses and limitations; review the card and cite the associated paper for an academic demonstration.

On Windows, from the project root, run these once:

```powershell
Set-Location .\backend\nlp
powershell -ExecutionPolicy Bypass -File .\setup-nlp.ps1
powershell -ExecutionPolicy Bypass -File .\download-model.ps1
```

This creates a project-local Python environment, installs CPU-only PyTorch and the service dependencies, and downloads the pinned model snapshot into `backend/nlp/model/` (about 380 MB). The environment and model weights are ignored by Git. Internet access is needed for setup and the first model download; inference then runs locally.

For each local run, start `start-backend.bat`. It opens `backend/nlp/start-nlp.bat` in a separate NLP window when the matching NLP service is not already running, waits for the model to load, then starts Spring Boot in the original window. Keep both windows open while using the application. The local NLP service is bound to loopback and is called only by Spring Boot. If it is stopped, fails to load the model, or returns an error, the booking still succeeds and its emergency request remains pending for the administrator.

The classifier treats genuine immediate danger, accidents or injuries, urgent medical needs, and safety hazards as serious emergencies. VIP status or urgency based only on work, meetings, appointments, deadlines, or convenience does not qualify; those priority-only claims are classified as `ROUTINE` unless the reason also states a medical or safety emergency. `SERIOUS` approves emergency priority and `ROUTINE` declines priority. The stored score is a model score, not a calibrated probability or a guarantee. Admins can change either decision on the Emergency Requests page. If a session has already started, changing its emergency decision does not stop that active simulated session. Requests without an NLP result remain pending. The dashboard shows the original NLP result and whether the latest decision came from NLP or an administrator.

The model card's recommended Transformers pipeline and zero-shot behavior are documented [here](https://huggingface.co/MoritzLaurer/deberta-v3-base-zeroshot-v1.1-all-33). For academic citation, see the paper linked from that card: [Building Efficient Universal Classifiers with Natural Language Inference](https://arxiv.org/abs/2312.17543).

## Run the application

### Windows

From the project root, run:

```bat
start-backend.bat
```

The script checks PostgreSQL and port `8080`, starts the local NLP service in a separate terminal if needed, waits until it is ready, then starts Spring Boot and opens the frontend at `http://localhost:8080/` when the server responds. Keep both terminals open while using the application; closing either service stops that part of the project.

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
│   ├── nlp/                            # Local Python service and model setup scripts
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

## License

No license file is currently included. Add a `LICENSE` file if you intend to grant others explicit reuse rights.
