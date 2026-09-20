# Contributing to Our Project

Welcome! We are excited that you want to contribute to our software project. To ensure smooth collaboration, maintain code quality, and prevent integration issues, please follow the version control best practices and Git workflow outlined below.

---

## 1. Selected Git Workflow (GitHub Flow)

For this project, we follow the **GitHub Flow** branching strategy because it is simple, efficient, and well-suited for our collaborative development needs.

### The Development Lifecycle:
1. **Pull Latest Changes:** Always pull the latest changes from the `main` (or `master`) branch before starting any new work.
2. **Create a Feature Branch:** Branch off from `main` for your specific task.
3. **Make & Commit Changes:** Implement your feature incrementally with atomic commits.
4. **Push to Remote:** Regularly push your feature branch to the remote repository for safekeeping.
5. **Open a Pull Request (PR):** Once your feature is ready and tested, open a Pull Request to request feedback and review from other developers.
6. **Address Review Comments:** Make any requested adjustments based on code reviews.
7. **Merge & Clean Up:** Once approved, merge your PR into `main` and delete your feature branch (both locally and remotely).

---

## 2. Branching Best Practices

Branching on Git is a lightweight operation. Use it liberally to isolate your experimental or active work.

* **One Feature per Branch:** Create a dedicated branch for each feature or task. Never work on multiple unrelated features in a single branch.
* **Single Contributor:** Ideally, only one person should work on a single feature branch at any given time.
* **Short-Lived Branches:** Feature branches should be short-lived. They must be deleted immediately after their changes are merged into `main`.
* **Keep `main` Clean:** The `main` branch must always contain production-ready, tested, and working code. **Avoid working directly on the `main` branch.**
* **Merge Early and Often:** Regularly integrate updates to avoid massive merge conflicts down the line.

### Branch Naming Convention
All developers must strictly adhere to the following naming convention for feature branches:
```text
feature/<username>/task-name-in-lowercase-spaces-replaced-with-dashes
```

Each branch tracks a single GitHub issue, and the task name should reflect the issue title. For example, the branch for issue #40 ("Add shared config") would be:
```text
feature/virul/task-40-shared-config
```

---

## 3. Commit Messages

We follow the **Conventional Commits** style to keep history readable and to describe intent clearly.

### Format
```text
<type>: <short imperative summary>

[optional body]
```

### Allowed types
* `feat:` — a new feature
* `fix:` — a bug fix
* `docs:` — documentation only changes
* `refactor:` — code change that neither fixes a bug nor adds a feature
* `test:` — adding or updating tests
* `chore:` — maintenance tasks (build, tooling, dependencies)

### Rules
* **Atomic Commits:** Each commit should represent a single logical change. Avoid mixing unrelated changes in one commit.
* **Imperative Mood:** Write summaries as commands, e.g. "add shared config module", not "added" or "adds".
* **Reference Issues:** When a commit relates to a GitHub issue, reference it in the summary or body, e.g.:
```text
feat: add shared config module (#40)
```

---

## 4. Project Setup & Running

This repository is a backend-only microservices monorepo. It currently contains one service:

* `service-registry/` — Spring Cloud Netflix Eureka server (runs on port `8761`).

Future microservices will be added as sibling directories under the repository root.

### Prerequisites
* **JDK 21** — required by the build.
* **Maven** — use the included Maven Wrapper (`./mvnw`); no separate Maven install needed.

### Build & Test
Run all commands from within the service directory:
```bash
cd service-registry
```

Run the service:
```bash
./mvnw spring-boot:run
```

Run the test suite:
```bash
./mvnw test
```

---

## 5. Pull Request Guidelines

* **Title:** Use the same Conventional Commits style, e.g. `feat: add shared config (#40)`.
* **Description:** Summarize the change, link the related GitHub issue, and note anything reviewers should focus on.
* **Checklist:** Before opening (or marking ready) a PR, verify:
  - [ ] The project builds successfully (`./mvnw spring-boot:run`).
  - [ ] All tests pass (`./mvnw test`).
  - [ ] No generated or local files are committed (`.idea/`, `target/`, `.mvn/wrapper/maven-wrapper.jar`).
  - [ ] No secrets, credentials, or local configuration are committed.
  - [ ] The branch is up to date with `main` and has no merge conflicts.

---

## 6. Code Review Rules

* **Reviewers:** Verify the build/test status, check for secrets or committed configuration, and keep feedback constructive and specific.
* **Authors:** Respond to and address review comments promptly, in follow-up commits on the same feature branch.
* **Scope:** Keep the PR scoped to its issue; raise unrelated changes separately.

---

## 7. Definition of Done

A task is considered done only when all of the following are true:

* [ ] The code builds without errors.
* [ ] All tests pass.
* [ ] The change has been reviewed and approved via a Pull Request.
* [ ] The PR has been merged into `main`.
* [ ] The feature branch has been deleted (locally and remotely).

---

## 8. Configuration & Secrets

* **Never commit** credentials, API keys, or local configuration (e.g. `application.properties` containing secrets).
* **Respect `.gitignore`:** local directories such as `.idea/`, `target/`, and `.mvn/wrapper/maven-wrapper.jar` must not be staged or committed.
* **Check before you push:** run `git status` to confirm only intended files are staged.