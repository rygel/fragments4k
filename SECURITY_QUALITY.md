# Security & Code Quality

This document explains the security and code quality tools configured for the Fragments project.

## Security Scanning

### Secret Detection (Gitleaks)
- **Tool**: Gitleaks
- **Purpose**: Scans git history for leaked secrets, API keys, and credentials
- **Blocking**: Yes — fails the build on any finding
- **Workflow**: Runs on every push and PR to main/develop
- **Configuration**: `.gitleaks.toml` (if present)

### SAST Scan (Semgrep)
- **Tool**: Semgrep
- **Purpose**: Static analysis for security vulnerabilities in Kotlin and Java code
- **Blocking**: Yes — fails the build on any finding
- **Workflow**: Runs on every push and PR to main/develop
- **Rulesets**: `p/default`, `p/kotlin`, `p/java`
- **SARIF**: Results uploaded to GitHub Security tab

### OWASP Dependency Check
- **Tool**: OWASP Dependency Check
- **Purpose**: Scans Maven dependencies for known CVEs
- **Blocking**: Yes — fails on CVSS >= 7.0
- **Workflow**: Runs on every push and PR to main/develop, plus weekly on Monday
- **Configuration**: `dependency-check-suppressions.xml` for vetted false positives
- **Reports**: Uploaded as GitHub Actions artifacts

#### Dependency Vulnerability Policy
- **High Severity (CVSS 7.0-10.0)**: Build failure (blocking)
- **Medium Severity (CVSS 4.0-6.9)**: Warning, but allow build
- **Low Severity (CVSS 0.0-3.9)**: Warning, but allow build
- **False Positives**: Suppressed via `dependency-check-suppressions.xml`

## Code Quality

### KtLint
- **Tool**: KtLint
- **Purpose**: Kotlin code style and linting
- **Workflow**: Bound to `mvn verify`, runs on every push
- **Reports**: `target/ktlint-report.html`

## Code Coverage

### JaCoCo
- **Tool**: JaCoCo (Java Code Coverage)
- **Purpose**: Test coverage analysis
- **Coverage Goals**:
  - **Instruction Coverage**: >= 60%
  - **Branch Coverage**: >= 50%
  - **Line Coverage**: >= 60%
  - **Complexity Coverage**: >= 50%
- **Workflow**: Bound to `mvn verify`, runs on every push
- **Reports**:
  - `target/site/jacoco/jacoco.xml` (XML)
  - `target/site/jacoco/index.html` (HTML)
  - Uploaded as GitHub Actions artifacts

## CI/CD Integration

All security and quality checks are integrated into GitHub Actions:

- **`.github/workflows/ci.yml`**: Main CI pipeline (build, test, coverage)
- **`.github/workflows/security-quality.yml`**: Security scanning (Gitleaks, Semgrep, OWASP Dependency Check)

### Blocking vs Advisory Policy

| Check | Behavior |
|-------|----------|
| Gitleaks (secret detection) | **Blocking** — any finding fails the build |
| Semgrep (SAST) | **Blocking** — any finding fails the build |
| OWASP Dependency Check (CVSS >= 7) | **Blocking** — fails the build |
| OWASP Dependency Check (CVSS < 7) | Advisory — warning only |

## Best Practices

### Security
1. Keep dependencies up-to-date
2. Review and suppress false positives in `dependency-check-suppressions.xml`
3. Address high-severity vulnerabilities promptly
4. Use security-focused code reviews

### Code Quality
1. Maintain high test coverage (> 60% for core modules)
2. Fix KtLint issues incrementally
3. Run `mvn verify` locally before pushing

## References

- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Gitleaks](https://gitleaks.io/)
- [Semgrep](https://semgrep.dev/)
- [JaCoCo](https://www.jacoco.org/jacoco/)
