# SmartCharge — Contributor 4 Package

This package is a contribution slice of the original SmartCharge project.

## Important
- Preserve the paths exactly as provided.
- Upload/commit these files at the repository root so the final repository reconstructs the original `SmartCharge/` structure.
- Do not rename files or folders.
- Do not upload Maven `target/` build output; it is intentionally excluded.
- `database/schema.sql` is intentionally included **only in Contributor 1** to avoid duplicate-file conflicts.
- Do not overwrite another contributor's files.

## Your assigned files

- `backend/src/main/java/smartcharge/admin/AdminController.java`
- `backend/src/main/java/smartcharge/admin/AdminService.java`
- `backend/src/main/java/smartcharge/admin/EmergencyController.java`
- `backend/src/main/java/smartcharge/admin/EmergencyService.java`
- `backend/src/main/java/smartcharge/DemoDataInitializer.java`
- `frontend/admin/admin-alerts.js`
- `frontend/admin/analytics.html`
- `frontend/admin/dashboard.html`
- `frontend/admin/emergencies.html`
- `frontend/admin/login.html`
- `frontend/admin/requests.html`

## GitHub merge expectation
All four packages are designed to be copied into the same repository. Their source paths do not overlap. Contributor 1 supplies the single shared project configuration/database schema files.

## Scope
Your assignment includes work across the application's layers where applicable (frontend + backend + database responsibility). The database schema is centralized in Contributor 1's package solely to keep one canonical schema file.
