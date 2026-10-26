# Campus Twin frontend

Responsive React web app for the Digital Twin university-building backend. It includes room status, teacher location, dated timetables, and direct/section/global messaging with live updates. A PWA manifest and app-shell cache make the frontend installable on supported mobile and desktop browsers.

## Run locally

1. Start the Spring Boot backend on `http://localhost:8080` and PostgreSQL.
2. In this folder, run `npm install` once, then `npm run dev`.
3. Open the Vite URL shown in the terminal (normally `http://localhost:5173`).
4. Sign in with the backend development account. The default is username `1234` and password `1234`, unless `APP_ADMIN_USERNAME` or `APP_ADMIN_PASSWORD` is set for the backend.

Vite proxies `/api` requests to `http://localhost:8080`. The username and password remain in the browser tab's session storage and are cleared when you sign out.

## Build

Run `npm run lint` and `npm run build`. The production web app is written to `dist/`.
