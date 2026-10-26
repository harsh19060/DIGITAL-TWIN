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
- `GET, POST /api/students` (student directory entries linked to a section)
- `GET, POST /api/subjects`
- `GET, POST /api/timetable`
- `GET /api/timetable/import/template` (downloads a blank Excel template)
- `POST /api/timetable/import` (atomically replaces the base schedule from an Excel workbook; multipart field `file`)
- `POST /api/timetable/cancellations/teacher/{teacherCode}` (stores dated cancellations and creates section notices)
- `GET /api/timetable/on-date?date=YYYY-MM-DD` (shows the active Week A/B schedule with dated cancellations applied)
- `POST /api/bookings/preview` and `POST /api/bookings` (check and confirm extra-class bookings)
- `GET /api/bookings?date=YYYY-MM-DD`
- `POST /api/bookings/{id}/cancel`
- `GET /api/sections/{sectionCode}/notifications`
- `POST /api/messages` (direct, section, or global message)
- `GET /api/messages/direct/{firstType}/{firstCode}/{secondType}/{secondCode}` (shared direct conversation history)
- `GET /api/messages/section/{sectionCode}` and `GET /api/messages/global` (section chat and global feed; latest 100 messages)
- `GET /api/teachers/{staffCode}/availability?dayOfWeek=MONDAY&weekPattern=EVERY_WEEK&startTime=09:00&endTime=10:00` (timetable availability only; it does not confirm the teacher's physical location)
- `GET /api/rooms/available?dayOfWeek=MONDAY&weekPattern=EVERY_WEEK&startTime=09:00&endTime=10:00&minCapacity=40`

Timetable times use 24-hour `HH:mm` values. Week pattern values are `EVERY_WEEK`, `WEEK_A`, and `WEEK_B`. Excel imports read the first sheet. The first row must contain these columns in order: `roomNumber`, `teacherCode`, `sectionCode`, `subjectCode`, `dayOfWeek`, `weekPattern`, `startTime`, `endTime`. Use weekdays such as `MONDAY`, week patterns `EVERY_WEEK`, `WEEK_A`, or `WEEK_B` (blank means every week), and 24-hour times such as `09:00`. Imports replace the base timetable only after every row has been parsed and validated; any invalid reference or scheduling conflict leaves the previous timetable intact.

`spring.jpa.hibernate.ddl-auto=update` is enabled for local development only. Use database migrations before deploying this service.



Admin calendar setup: PUT /api/admin/academic-calendar with {"weekAStartDate":"YYYY-MM-DD"}. The date must be a Monday. Query a date with GET /api/admin/academic-calendar/week-pattern?date=YYYY-MM-DD.







Message audiences are DIRECT, SECTION, or GLOBAL; categories are GENERAL, ANNOUNCEMENT, QUESTION, and LOST_FOUND. A direct message is stored once and returned to both participants' conversation view. SECTION messages are delivered into that section's shared chat. GLOBAL messages appear in the building-wide feed.
