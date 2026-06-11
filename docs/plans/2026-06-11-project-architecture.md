# Project Architecture Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Scaffold the offline Grimorio Pathfinder repository with a Spring Boot hexagonal backend, a separate Vue SPA frontend, and tests that guard layer boundaries.

**Architecture:** Keep the backend as the root Maven project with clear domain/application/ports/infrastructure/web packages. Keep the frontend isolated under `frontend/` as a standalone Vite + Vue application that can only consume the local API. Use architecture tests to prevent boundary drift before any feature work starts.

**Tech Stack:** Java 21, Spring Boot, Maven, ArchUnit, Vue 3, Vite, TypeScript.

---

### Task 1: Backend project skeleton

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/grimoriopathfinder/GrimorioPathfinderApplication.java`
- Create: `src/main/java/com/grimoriopathfinder/domain/package-info.java`
- Create: `src/main/java/com/grimoriopathfinder/application/package-info.java`
- Create: `src/main/java/com/grimoriopathfinder/application/port/in/package-info.java`
- Create: `src/main/java/com/grimoriopathfinder/application/port/out/package-info.java`
- Create: `src/main/java/com/grimoriopathfinder/infrastructure/package-info.java`
- Create: `src/main/java/com/grimoriopathfinder/web/package-info.java`
- Create: `src/test/java/com/grimoriopathfinder/GrimorioPathfinderApplicationTests.java`
- Create: `src/test/java/com/grimoriopathfinder/ArchitectureBoundariesTest.java`

**Step 1: Write the failing test**

Create ArchUnit rules for the domain/application package boundaries.

**Step 2: Run test to verify it fails**

Run: `mvn test`
Expected: fail because the project skeleton does not exist yet.

**Step 3: Write minimal implementation**

Add the Spring Boot application class, package structure, and ArchUnit dependency.

**Step 4: Run test to verify it passes**

Run: `mvn test`
Expected: pass with boundary checks green.

### Task 2: Frontend Vue SPA scaffold

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/style.css`
- Create: `frontend/src/env.d.ts`

**Step 1: Write the failing test**

No automated test framework is required for the scaffold; verify the build command instead.

**Step 2: Run build to verify it fails**

Run: `npm install`
Expected: fail until the frontend project exists.

**Step 3: Write minimal implementation**

Add a Vite + Vue app shell with a local-only API placeholder.

**Step 4: Run build to verify it passes**

Run: `npm run build`
Expected: pass once the scaffold is in place.

### Task 3: Repository hygiene and documentation alignment

**Files:**
- Modify: `README.md`
- Modify: `.gitignore`

**Step 1: Write the failing test**

Manual review against the architecture spec and roadmap.

**Step 2: Run verification**

Confirm the docs describe the new root/backend/frontend layout.

**Step 3: Write minimal implementation**

Update the repository docs and ignores so they match the new structure.

**Step 4: Run verification**

Review the final tree and ensure no generated artifacts are committed.
