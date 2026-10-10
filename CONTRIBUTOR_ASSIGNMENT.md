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
