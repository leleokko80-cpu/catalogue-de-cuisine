# Catalogue de cuisine

A recipe catalogue web app: browse, search, add and delete recipes, log in, and mark favourites.

**Stack:** React (Vite) + Spring Boot 4 + MySQL 8. Java 17 required.

## Prerequisites
- JDK 17
- Node.js 22 or newer, with npm
- MySQL 8, running locally (user `root`, password `root`)
- Git

## Run

**Backend** (port 8081):
```
cd backend
.\mvnw spring-boot:run
```

**Frontend** (port 5173):
```
cd frontend
npm install        (first time only)
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies every `/api` request to `http://localhost:8081`, so there are no CORS settings to configure.

**Database:** MySQL, user `root`, password `root`. The database `recipe_catalog` and all tables are created automatically on first start. Three sample recipes are inserted if the table is empty.

To use other MySQL credentials without changing the code, set the environment variables `DB_USERNAME` and `DB_PASSWORD`, or add a local `backend/config/application.properties` (it must stay out of Git).

## Project structure
```
backend/src/main/java/com/recipecatalog/backend/
  auth/       register, login, JWT, security rules
  recipe/     recipes: entity, controller, service, DTOs, seed data
  favorite/   favourites
  user/       User entity and repository
  common/     global error handler
frontend/src/
  api/        axios instance (adds the JWT token to every request)
  context/    AuthContext, FavoritesContext
  components/ Navbar, SearchBar, FavoriteButton, DeleteRecipeButton, ...
  pages/      list, detail, add recipe, login, register
```

## API contract

Base URL: `http://localhost:8081/api`. All bodies are JSON (camelCase).

**Authentication:** send the token as `Authorization: Bearer <token>`. A missing or invalid token on a protected route returns `401 {"message": "Authentication required"}`.

**Error format:**
```json
{ "message": "Validation failed", "errors": { "title": "Title is required" } }
```
`errors` only appears for validation failures (400). Other errors return just `{ "message": "..." }`.

### Auth (public)

| Method | Endpoint | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/auth/register` | `{ name, email, password }` (password at least 6 characters) | `201 { token, user: { id, name, email } }` | 400 validation, 409 email already in use |
| POST | `/auth/login` | `{ email, password }` | `200 { token, user: { id, name, email } }` | 400 validation, 401 invalid email or password |

The token is valid for 24 hours. The frontend stores it in `localStorage` under the key `token`.

### Recipes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/recipes` | public | List all recipes |
| GET | `/recipes?q=carb` | public | Search by title (partial match, case-insensitive) |
| GET | `/recipes/{id}` | public | Recipe detail (404 if it doesn't exist) |
| POST | `/recipes` | required | Create a recipe (the logged-in user becomes the author) |
| DELETE | `/recipes/{id}` | required | Delete a recipe (author only) |

**List item** (`GET /recipes`):
```json
{ "id": 1, "title": "Spaghetti Carbonara", "imageUrl": "", "prepTimeMinutes": 10,
  "cookTimeMinutes": 15, "authorName": "Anonymous" }
```

**Detail** (`GET /recipes/{id}`, and the response of `POST /recipes`):
```json
{
  "id": 1, "title": "Spaghetti Carbonara", "description": "...", "imageUrl": "",
  "prepTimeMinutes": 10, "cookTimeMinutes": 15, "servings": 4,
  "ingredients": [ { "name": "Spaghetti", "quantity": "400 g" } ],
  "steps": ["Boil the pasta.", "..."],
  "authorName": "Anonymous", "authorId": null, "createdAt": "2026-11-07T18:16:08"
}
```
Seeded recipes have no author (`authorName` is "Anonymous", `authorId` is `null`) and cannot be deleted.

**Create request** (`POST /recipes`):
```json
{
  "title": "Couscous", "description": "Friday classic", "imageUrl": "",
  "prepTimeMinutes": 30, "cookTimeMinutes": 60, "servings": 6,
  "ingredients": [ { "name": "Semolina", "quantity": "500 g" } ],
  "steps": ["Steam the semolina"]
}
```
Validation: `title` required (max 150 characters), `description` max 2000, `imageUrl` max 1000, times at least 0, `servings` at least 1, at least one ingredient (with a name), at least one non-empty step. Failures return `400` with an `errors` object.

**Delete:** `204` on success, `401` if not logged in, `403` if the recipe belongs to someone else, `404` if it doesn't exist. Deleting a recipe also removes it from everyone's favourites.

### Favourites (all require authentication)

| Method | Endpoint | Success | Notes |
|---|---|---|---|
| GET | `/favorites/ids` | `200 [1, 4, 7]` | ids of the current user's favourite recipes |
| POST | `/recipes/{id}/favorite` | `204` | Idempotent. `404` if the recipe doesn't exist |
| DELETE | `/recipes/{id}/favorite` | `204` | Idempotent. No error if it wasn't a favourite |

## Frontend routes

| Path | Page | Access |
|---|---|---|
| `/recipes` | Recipe list and search (`?q=`) | public |
| `/recipes/:id` | Recipe detail (heart, delete for the author) | public |
| `/recipes/new` | Add a recipe | logged-in users only |
| `/login`, `/register` | Authentication | public |

## Git workflow
- `main` is always working. Never push to it directly.
- One branch per issue (`feature/<issue>-<name>`), merged through a Pull Request.
- Commit messages mention the issue number, e.g. `feat: search recipes (#5)`.
- Before pushing: `git pull --rebase origin main`.
