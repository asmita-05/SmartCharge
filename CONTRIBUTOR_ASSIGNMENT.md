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
