# Contributing Guidelines

Thank you for contributing to Gallery System!

## Branching Model
- `main`: Production release branch.
- `v2-hardening`: v2 hardening, testing, and optimization branch.
- Feature branches: `feature/<name>` or `fix/<name>`.

## Pull Request Checklist
1. Run `./gradlew app:testDevDebugUnitTest` — All 35 unit tests must pass.
2. Run `./gradlew app:assembleDevDebug` — Must compile cleanly without errors.
3. On `gallery-dashboard`: Run `npm run build && npm test` — Must pass without TS or test errors.
4. Log any file or dependency removals in `CLEANUP_LOG.md`.
