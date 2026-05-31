# Commit Guide — Trace App

Agar GitHub contribution graph rame dan repo terlihat profesional, ikuti panduan ini.

---

```bash
git init
git add README.md
git commit -m "first commit"
git branch -M master
git remote add origin git@github.com:testwiththeo/trace-app.git
git push -u origin master
```

## Branch Strategy

```
main              ← protected, hanya dari PR
  └── develop     ← branch utama development
       ├── feat/xxx      ← fitur baru
       ├── fix/xxx       ← bug fix
       ├── test/xxx      ← nambah test
       ├── refactor/xxx  ← refaktor kode
       └── chore/xxx     ← dependencies, CI, dll
```

Jangan commit langsung ke `main`. Selalu bikin branch → commit → push → PR → merge.

---

## Commit Convention (Conventional Commits)

```
<type>: <description>

Body (opsional — jelaskan kenapa, bukan apa)
```

### Type

| Type | Kapan | Contoh |
|------|-------|--------|
| `feat` | Fitur baru | `feat: add wildcard matching to mock engine` |
| `fix` | Bug fix | `fix: vpn tunnel not restoring after network change` |
| `test` | Nambah / update test | `test: add edge case for empty response body` |
| `refactor` | Refaktor tanpa ubah behavior | `refactor: extract packet parser to separate module` |
| `docs` | Dokumentasi | `docs: add architecture diagram to readme` |
| `chore` | Build, CI, dependencies | `chore: bump ktor to 2.3 and bouncycastle to 1.77` |
| `style` | Formatting, styling UI | `style: adjust traffic log row spacing to 12dp` |
| `perf` | Performance improvement | `perf: reduce bitmap allocation in annotation canvas` |

### Description Rules

- Imperative: "add", "fix", "remove" — bukan "added", "fixed"
- Maksimal 72 karakter
- Bahasa Inggris

---

## Workflow Harian

```bash
# 1. Pastikan branch develop terbaru
git checkout develop
git pull origin develop

# 2. Bikin branch baru dari issue
git checkout -b feat/mock-engine

# 3. Kerjakan + commit
git add app/src/main/java/com/trace/app/proxy/
git commit -m "feat: implement mock engine with url pattern matching"

# 4. Push
git push -u origin feat/mock-engine

# 5. Buka GitHub → create PR → merge
```

---

## Initial Push (untuk pertama kali)

Jangan push 1 commit gede. Ikutin urutan ini:

```bash
# 1. Project structure + build config
git add build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle/ app/build.gradle.kts app/proguard-rules.pro app/src/main/AndroidManifest.xml app/src/main/res/
git commit -m "chore: initialize android project with compose, hilt, room"

# 2. Domain layer
git add app/src/main/java/com/trace/app/domain/
git commit -m "feat: add domain models, repository interfaces, and use cases"

# 3. Data layer
git add app/src/main/java/com/trace/app/data/
git commit -m "feat: implement data layer with room database, daos, and repositories"

# 4. DI modules
git add app/src/main/java/com/trace/app/di/
git commit -m "feat: setup hilt dependency injection modules"

# 5. Navigation + theme + application
git add app/src/main/java/com/trace/app/presentation/MainActivity.kt app/src/main/java/com/trace/app/presentation/navigation/ app/src/main/java/com/trace/app/presentation/theme/ app/src/main/java/com/trace/app/TraceApplication.kt
git commit -m "feat: add navigation graph, theme, and main activity"

# 6. VpnService + proxy layer
git add app/src/main/java/com/trace/app/proxy/
git commit -m "feat: implement vpn service with tun interface and local proxy"

# 7. Traffic capture and mock engine
git add app/src/main/java/com/trace/app/engine/
git commit -m "feat: implement traffic capturer, mock engine, blocklist, and delay"

# 8. UI screens
git add app/src/main/java/com/trace/app/presentation/screen/
git commit -m "feat: add home, traffic log, traffic detail, and mock rules screens"

# 9. Settings
git add app/src/main/java/com/trace/app/presentation/screen/settings/
git commit -m "feat: add settings screen with cert management and proxy config"

# 10. Tests
git add app/src/test/
git commit -m "test: add unit and integration tests for proxy, engine, and dao"

# 11. CI/CD
git add .github/ scripts/
git commit -m "ci: add github actions workflow with lint, build, and test"

# 12. Documentation
git add docs/ .gitignore .gitmessage
git commit -m "docs: add trace documentation and contribution guide"

# 13. README (paling akhir)
git add README.md
git commit -m "docs: add readme with architecture overview"
```

---

## Release Strategy

| Release | Waktu | Isi |
|---------|-------|-----|
| v1.0.0 | Initial | VPN tunnel, proxy, traffic capture, basic UI |
| v1.1.0 | Minggu 1 | Mock engine, delay injection, blocklist |
| v1.2.0 | Minggu 2 | Replay, export, UI polish, bug fixes |
| v1.3.0 | Minggu 3 | Integration tests, CI/CD, performance |

Tiap release: `git tag v1.x.x` → `git push origin v1.x.x` → buat release di GitHub.

---

## Target

| Metrik | Target per bulan |
|--------|------------------|
| Commits | 30-50 |
| PRs | 10-15 |
| Issues closed | 10-15 |
| Releases | 2-3 |
