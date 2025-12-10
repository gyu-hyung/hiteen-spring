# Repository Guidelines

## Project Structure & Module Organization
- Backend is Kotlin/Spring Boot WebFlux. Core code lives in `src/main/kotlin/kr/jiasoft/hiteen`, with subpackages for `config`, `admin`, `common`, `feature`, and `validation`.
- Runtime assets and configuration are in `src/main/resources` (profiles under `application*.yml`, Liquibase changelogs in `db/changelog`, static assets under `static` and `assets`).
- Tests mirror the main package layout under `src/test/kotlin/kr/jiasoft/hiteen` and follow `*Test.kt` naming.
- Deployment artifacts and manifests: `k8s/` (Minikube/K8s manifests), `docker-compose.yml`, and Helm chart in `hiteen-chart/`.

## Build, Test, and Development Commands
- `./gradlew build` — compile and run all tests; produces the bootable jar in `build/libs/`.
- `./gradlew test` — run JUnit 5/MockK/Spring tests; use before every PR.
- `./gradlew bootRun` — start the API locally with the default profile; override with `--args='--spring.profiles.active=dev'` as needed.
- `./gradlew clean build -x test` — rebuild quickly when you only need a jar (avoid for PRs).

## Coding Style & Naming Conventions
- Follow idiomatic Kotlin: 4-space indents, trailing commas allowed, prefer `data class`/`val` where possible, and keep functions small and reactive-friendly (avoid blocking in WebFlux handlers).
- Package names use the existing `kr.jiasoft.hiteen.<domain>` pattern; test classes match the production class name plus `Test`.
- Keep controllers slim, push business logic to `feature/.../app` services, and share utilities in `common/`.
- Format imports and organize code before pushing; if unsure, use IntelliJ default Kotlin style.

## Testing Guidelines
- Frameworks: JUnit 5, Spring Boot Test (WebFlux), Reactor Test, MockK, and coroutine test utilities.
- Place unit/integration tests under the matching package in `src/test/kotlin`; prefer `Given_When_Then` naming inside tests for clarity.
- For reactive flows, assert completion and emissions with `StepVerifier`; mock external calls with MockK rather than Mockito when possible.
- Run `./gradlew test` locally; aim to keep new features covered with positive and edge-case tests.

## Commit & Pull Request Guidelines
- Use Conventional Commit prefixes seen in history (`feat:`, `refactor:`, `chore:`, etc.) with concise, English or Korean summaries.
- Each PR should include: what changed, why, test evidence (`./gradlew test` output or screenshots for API docs/UI), and linked issue/Backlog reference if available.
- Keep PRs focused and reviewable (prefer smaller scoped changes). Mention config/profile impacts, especially when touching `application*.yml` or K8s manifests.

## Security & Configuration Tips
- Do not commit secrets; keep Firebase keys, DB creds, and tokens out of git. Use profile-specific `application-*.yml` or environment variables for local overrides.
- When running locally, ensure dependent services (PostgreSQL, MongoDB, Redis, Soketi) are available; use `docker-compose.yml` or the Minikube manifests under `k8s/`.
- Sensitive data in logs should be masked; avoid logging tokens or personal identifiers.***

한글로 대답해줘