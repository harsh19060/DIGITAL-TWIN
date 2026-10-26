# Digital Twin University Building

Spring Boot backend for the university building digital twin.

## Local setup

1. Install Java 21+ and PostgreSQL.
2. Create a PostgreSQL database named `digital_twin`.
3. Copy `application-local.properties.example` to `application-local.properties` and set your local PostgreSQL password there. This local file is ignored by Git.
4. From this folder, run `mvnw.cmd spring-boot:run` on Windows.

The local development API uses HTTP Basic authentication: username `dev`, password `dev-only-change-me` unless `APP_DEV_PASSWORD` is set. This is a development-only account; replace it with university sign-in before deployment.

## Current API

- `GET /api/health`
- `GET, POST /api/rooms`
- `GET, POST /api/teachers`
- `GET, POST /api/sections`
- `GET, POST /api/subjects`
- `GET, POST /api/timetable`
- `GET /api/teachers/{staffCode}/availability?dayOfWeek=MONDAY&weekPattern=EVERY_WEEK&startTime=09:00&endTime=10:00` (timetable availability only; it does not confirm the teacher's physical location)
- `GET /api/rooms/available?dayOfWeek=MONDAY&weekPattern=EVERY_WEEK&startTime=09:00&endTime=10:00&minCapacity=40`

Timetable times use 24-hour `HH:mm` values. Week pattern values are `EVERY_WEEK`, `WEEK_A`, and `WEEK_B`. Schedule changes and Excel import are planned next.

`spring.jpa.hibernate.ddl-auto=update` is enabled for local development only. Use database migrations before deploying this service.



Admin calendar setup: PUT /api/admin/academic-calendar with {"weekAStartDate":"YYYY-MM-DD"}. The date must be a Monday. Query a date with GET /api/admin/academic-calendar/week-pattern?date=YYYY-MM-DD.

