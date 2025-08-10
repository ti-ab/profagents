# /!\ WIP /!\ snowing.ai

<h2>Real time video AI teachers platform</h2>

<b>DEV</b>: docker compose up --build

<b>PROD</b>: docker-compose -f docker-compose.yml up --build -d


## Progress tracking (added)

- Backend (`services/user-service`): exposes `POST /api/progress`, `GET /api/progress/{userId}`, `GET /api/progress/{userId}/summary`.
- Frontend (`frontend-nextjs/lib/progress.ts`): `saveProgress()` + demo page at `/demo/progress`.
- Multimodal agent (`services/multimodal-agent-node/src/progress.ts`): helper `postProgress()` and a minimal boot-time example.
- Database: `docker/postgres/user/init.sql` updated to use `BIGSERIAL` for `progress.id`.

Configure env:
- `NEXT_PUBLIC_USER_API` in `frontend-nextjs/.env.local`
- `USER_SERVICE_URL` in `services/multimodal-agent-node/.env.local`


### Progress summaries & webhook (added)
- **New endpoint** `GET /api/progress/{userId}/course/{bookId}/summary` — fetches the book tree from course-service and returns `{ totalSections, completedSections, progressPercent, totalTimeSeconds }`.
- **Simple webhook** `POST /api/progress/webhook` — accepts events with optional `status: "completed"` and maps to a Progress entry (auto-fills `endTimestamp` if completed).
- Configure `course.service.url` (defaults to `http://course-service:8082`).



### Frontend: manage & generate books
- New API in **course-service**:
  - `POST /api/books/generate` with `{ description, title?, authors? }` → returns created book (basic DTO).
  - CRUD: `POST /api/books`, `PUT /api/books/{id}`, `DELETE /api/books/{id}`.
- New frontend pages:
  - `/admin/books` — list & delete.
  - `/admin/books/new` — generate from description.
  - `/admin/books/[id]` — edit title/authors.
- Configure `NEXT_PUBLIC_COURSE_API` (ex: `http://localhost:8087`).
