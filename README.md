# Digital Twin University Building

Spring Boot backend for the university building digital twin.

## Local setup

1. Install Java 21+ and PostgreSQL.
2. Create a PostgreSQL database named `digital_twin`.
3. Copy `application-local.properties.example` to `application-local.properties` and set your local PostgreSQL password there. This local file is ignored by Git.
4. From this folder, run `mvnw.cmd spring-boot:run` on Windows.

Local development uses three HTTP Basic demo accounts: admin `1234` / `1234`, student `student` / `student-only-change-me`, and teacher `teacher` / `teacher-only-change-me`. Set `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` to change the admin credentials; use `APP_STUDENT_PASSWORD` and `APP_TEACHER_PASSWORD` for the other accounts. The student account maps to code `A7_44` and the teacher account to `ALS` by default; set `APP_STUDENT_CODE` or `APP_TEACHER_CODE` to map to other registered campus records. The admin can only upload the base timetable; class, room, and teacher changes must come through authorized teacher workflows. These in-memory accounts are for local demos only, not production.

## React web app

The responsive React client is in `frontend/`. Follow [frontend/README.md](frontend/README.md) to run it alongside the API. It includes room status, teacher lookup, timetable browsing, campus messages, and a PWA app shell.

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
- `GET /api/teachers/{staffCode}/current-location?at=YYYY-MM-DDTHH:mm` (scheduled current room/class and next class)
- `GET /api/rooms/current?at=YYYY-MM-DDTHH:mm` (all rooms with current schedule-derived status)
- `POST /api/bookings/preview` and `POST /api/bookings` (check and confirm extra-class bookings)
- `GET /api/bookings?date=YYYY-MM-DD`
- `POST /api/bookings/{id}/cancel`
- `GET /api/sections/{sectionCode}/notifications`
- `GET /api/session` (authenticated account role and campus participant code)
- `GET /api/messages/contacts` (recent direct/class chats plus system and building-wide inboxes)
- `POST /api/system/command` (free-room queries, teacher absence cancellation, extra-class proposals and confirmation)
- `POST /api/messages` (authenticated direct, section, or global message)
- `GET /api/messages/direct/{firstType}/{firstCode}/{secondType}/{secondCode}` (shared direct conversation history)
- `GET /api/messages/section/{sectionCode}` and `GET /api/messages/global` (section chat and global feed; latest 100 messages)
- `GET /api/messages/stream/...` (Server-Sent Events for live direct, section, and global messages)
- `GET /api/teachers/{staffCode}/availability?dayOfWeek=MONDAY&weekPattern=EVERY_WEEK&startTime=09:00&endTime=10:00` (timetable availability only; it does not confirm the teacher's physical location)
- `GET /api/rooms/available?dayOfWeek=MONDAY&weekPattern=EVERY_WEEK&startTime=09:00&endTime=10:00&minCapacity=40`

Messages begin with `@system`, a class such as `@A7`, a teacher code such as `@ALS` or `@AS`, or `@all`. Extra-class requests use `schedule extra class for A7 DPD on YYYY-MM-DD HH:mm-HH:mm`; the system proposes an available room and books it when the teacher replies `CONFIRM`.

Timetable times use 24-hour `HH:mm` values. Week pattern values are `EVERY_WEEK`, `WEEK_A`, and `WEEK_B`. Excel imports read the first sheet. The first row must contain these columns in order: `roomNumber`, `teacherCode`, `sectionCode`, `subjectCode`, `dayOfWeek`, `weekPattern`, `startTime`, `endTime`. Use weekdays such as `MONDAY`, week patterns `EVERY_WEEK`, `WEEK_A`, or `WEEK_B` (blank means every week), and 24-hour times such as `09:00`. Imports replace the base timetable only after every row has been parsed and validated; any invalid reference or scheduling conflict leaves the previous timetable intact.

`spring.jpa.hibernate.ddl-auto=update` is enabled for local development only. Use database migrations before deploying this service.



Admin calendar setup: PUT /api/admin/academic-calendar with {"weekAStartDate":"YYYY-MM-DD"}. The date must be a Monday. Query a date with GET /api/admin/academic-calendar/week-pattern?date=YYYY-MM-DD.







Message audiences are DIRECT, SECTION, or GLOBAL; categories are GENERAL, ANNOUNCEMENT, QUESTION, and LOST_FOUND. A direct message is stored once and returned to both participants' conversation view. SECTION messages are delivered into that section's shared chat. GLOBAL messages appear in the building-wide feed.


Current-location data is derived from the active timetable, dated cancellations, and confirmed extra-class bookings; it does not track physical presence. Omit t to use the server's current local time.
