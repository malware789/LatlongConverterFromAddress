# AI Agent Workflow

This document outlines the step-by-step instructions that any AI agent should follow when continuing development on this project.

## Workflow Instructions

1. **Read `README.md` first**: Start at the root level to understand the purpose of the application and how to run it.
2. **Read all `docs/` files**: Familiarize yourself with `PROJECT_OVERVIEW.md`, `IMPLEMENTATION_STATUS.md`, `TODO_BACKEND_INTEGRATION.md`, and `ARCHITECTURE_NOTES.md` before making assumptions.
3. **Inspect current project structure**: Review `app/src/main/java/com/watsoo/addressconverter/` to see how the code is organized into `data`, `geocode`, `ui`, and `worker` packages.
4. **Run or check Gradle build**: Ensure the project currently compiles successfully by running `./gradlew assembleDebug` before writing new code.
5. **Make small changes only**: Do not rewrite the entire architecture. Implement new features incrementally and test them.
6. **Update docs after changes**: Always document your changes in the Change Log inside `IMPLEMENTATION_STATUS.md`. If required, update `AI_HANDOFF_PROMPT.md` and `README.md`.
7. **Do not remove Room persistence**: The local database queue is critical for resilience against crashes or process death. Maintain this functionality.
8. **Do not replace WorkManager with plain Service**: Background execution must remain compliant with modern Android constraints, so stick to WorkManager for the batch conversions.
9. **Keep fake API until real backend API is available**: Do not remove the fake classes (`FakeAddressApi`, `FakeGeocoderClient`) until a real backend URL is fully functional.
10. **Keep architecture modular and production-safe**: Maintain the clean separation of concerns (MVVM, Repository, DTOs, DAOs). Add DI (like Hilt) if you add new dependencies, and properly handle Android lifecycle states.
