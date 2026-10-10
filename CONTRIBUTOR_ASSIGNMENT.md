Individual Contribution Report

Project: SmartCharge – Intelligent EV Charging Station Resource Management and Scheduling System

1. Asmita – User Management, Vehicle Management, Authentication and Database Configuration

Asmita was responsible for developing the user management and vehicle management modules, along with the initial backend configuration and database structure of the SmartCharge system. Her contribution established the foundation on which the other modules of the application operate.

Backend Development: Asmita developed the main Spring Boot application entry point, "SmartChargeApplication.java", which initializes the backend application. She contributed to the backend Maven configuration through "pom.xml", defining the dependencies required for the application, including Spring Boot Web, Spring Data JPA, PostgreSQL, Spring Security, validation, JWT support, and testing libraries. She also worked on "SecurityConfig.java" to configure the application's security settings.

User Management Module: She implemented the user entity, repository, service, and controller. These components represent the user data model, database access layer, business logic, and REST API endpoints. The module provides the foundation for user-related operations, including registration and authentication-related functionality.

Vehicle Management Module: Asmita developed the vehicle entity and its associated repository, service, and controller components. This module manages vehicle information associated with users, such as vehicle registration details, model, vehicle type, and battery capacity. These details are important for calculating charging requirements and creating charging requests.

Database Design and Configuration: She prepared the centralized SQL database schema in "database/schema.sql". The schema defines the principal relational tables for users, vehicles, stations, chargers, charging requests, charging sessions, and payments, along with primary keys, foreign-key relationships, and relevant constraints. This provided the common database structure required by the different modules of the project.

User Interface: Asmita developed the initial user-facing pages, including "frontend/index.html", "frontend/user/login.html", "frontend/user/dashboard.html", and "frontend/user/vehicle.html". These pages provide the starting interface for accessing the application, logging in, viewing the user dashboard, and managing vehicle information.

Execution Support: She also contributed "start-backend.bat", a Windows batch file intended to simplify backend startup.

Overall, Asmita's contribution focused on the application's foundational backend setup, user and vehicle data management, database design, security configuration, and initial user interface.

2. Akarsh – Charging Station Management, Charger Management, Payment Management and Operator Interface

Akarsh was responsible for developing the charging station and charger management modules, the payment module, and the operator-facing frontend. His contribution focused on managing charging infrastructure and providing interfaces through which station operators can interact with the system.

Charging Station Management: Akarsh developed the "Station.java" entity, "StationRepository.java", "StationService.java", and "StationController.java". These components represent charging stations, provide database access, implement station-related business operations, and expose corresponding REST endpoints. The module establishes the structure required to maintain station information such as station names, locations, and operational status.

Charger Management: He developed the charger entity and its supporting repository, service, and controller classes. These include "Charger.java", "ChargerRepository.java", "ChargerService.java", and "ChargerController.java". The module manages charger details such as connector type, charging power, status, and association with a charging station. It provides the foundation for checking charger availability and accessing charger-related information.

Charger Allocation Support: Akarsh implemented "ChargerAllocationService.java" to provide charger allocation functionality. This service searches for available chargers and checks connector compatibility before attempting allocation. It helps connect station and charger management with the charging-session workflow.

Payment Management: He developed the payment module through "Payment.java", "PaymentRepository.java", "PaymentService.java", and "PaymentController.java". These components establish the payment data model, persistence layer, business service, and API endpoints. The payment entity supports information such as the associated charging session, amount, payment status, payment reference, payment method, and payment time.

Operator Frontend: Akarsh developed the operator-facing pages: "frontend/operator/login.html", "frontend/operator/dashboard.html", "frontend/operator/chargers.html", and "frontend/operator/station.html". These interfaces provide the presentation layer for operator access, dashboard information, charger management, and station-related operations.

Overall, Akarsh's contribution focused on the charging infrastructure management layer, charger allocation support, payment-related backend functionality, and the operator interface.

3. Vanshika – Charging Requests, Charging Sessions, Scheduling Algorithms and User Charging Interface

Vanshika was responsible for the charging workflow and scheduling-related modules. Her contribution focused on connecting charging requests with the allocation of charging resources and providing users with interfaces for requesting charging services and monitoring their requests.

Charging Request Management: Vanshika contributed to the charging module, including "ChargingRequest.java", "ChargingRequestRepository.java", "ChargingService.java", and "ChargingController.java". These components represent charging requests, provide persistence and business logic, and expose REST API endpoints. The workflow supports information such as the selected user vehicle, charging station, requested charger, current battery percentage, target battery percentage, request status, priority, and estimated charging duration.

Charging Session Management: She contributed to the charging-session components, including "ChargingSession.java", "ChargingSessionRepository.java", "SessionService.java", and "SessionController.java". These components represent individual charging sessions, maintain session records, and support operations related to session information and status. The charging lifecycle functionality is also represented by "ChargingLifecycleScheduler.java", which supports automated processing of charging-session lifecycle events.

Scheduling Algorithms: Vanshika developed the scheduling-related classes, including "Scheduler.java", "FCFS.java", "Priority.java", "SJF.java", "SchedulerService.java", "ChargingSchedulerService.java", "SchedulerController.java", and "ChargingSchedulerController.java". These components provide the structure for implementing and applying scheduling policies to pending charging requests.

The project incorporates three operating-system scheduling approaches:

- First Come, First Served (FCFS): Processes charging requests according to their arrival order.
- Priority Scheduling: Gives preference to requests with higher priority, subject to the application's eligibility and approval rules.
- Shortest Job First (SJF): Uses estimated charging duration to prioritize requests requiring less charging time.

The scheduling workflow considers charger compatibility and availability before allocating charging resources. The scheduler also contains transaction and locking logic intended to reduce conflicts when multiple requests compete for chargers. Priority and emergency handling are integrated with the broader application workflow.

User Charging Interface: Vanshika developed the charging-related user pages, including "frontend/user/booking.html" and "frontend/user/status.html". These pages provide the interface for creating charging requests and viewing their progress or status.

Overall, Vanshika's contribution focused on charging-request processing, charging-session management, the implementation of scheduling policies, and the user interface for booking and tracking charging services.

4. Shiwani – Administration, Emergency Handling, Monitoring and Administrative Interface

Shiwani was responsible for the administration and emergency-handling modules, together with the administrative frontend. Her contribution focused on enabling administrators to monitor the system, review charging-related information, and manage emergency requests through a dedicated interface.

Administrative Backend: Shiwani developed "AdminController.java" and "AdminService.java". These components provide the administrative API layer and supporting business logic for administrative operations. They form the backend foundation for exposing system information and performing supported administrative actions.

Emergency Management: She developed "EmergencyController.java" and "EmergencyService.java" to support emergency-related operations. The module provides the backend structure for handling emergency requests and their associated status and decision-making workflow. It works alongside the charging-request and scheduling modules so that emergency-related decisions can influence subsequent request processing.

Demo Data Initialization: Shiwani contributed "DemoDataInitializer.java", which provides application startup data initialization functionality. This supports the preparation of demonstration data for development and project evaluation, depending on the initializer's configured behavior.

Administrative Alerts: She developed "frontend/admin/admin-alerts.js", which provides JavaScript functionality for administrative alerts and related frontend behavior.

Administrative Frontend: Shiwani developed the administrative pages, including "frontend/admin/login.html", "frontend/admin/dashboard.html", "frontend/admin/analytics.html", "frontend/admin/emergencies.html", and "frontend/admin/requests.html". These pages provide the interface for administrative access, dashboard monitoring, analytics, emergency review, and charging-request management.

Monitoring and Oversight: The administrative interface brings together information relevant to the operation of the SmartCharge system. It provides a dedicated area for reviewing requests, observing system-level information, and handling emergency-related workflows. The specific operations available depend on the implemented backend endpoints and their integration with the frontend.
