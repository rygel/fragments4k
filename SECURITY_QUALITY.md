# Security & Code Quality

This document explains the security and code quality tools configured for the Fragments project.

## Security Scanning

### OWASP Dependency Check
- **Tool**: OWASP Dependency Check
- **Version**: 9.3.0
- **Purpose**: Scans dependencies for known security vulnerabilities
- **Configuration**: Fails build on CVSS >= 7.0
- **Workflow**: Runs on every push and daily at 2 AM UTC
- **Reports**: `target/dependency-check-report.html`

### SpotBugs Security Analysis
- **Tool**: SpotBugs + FindSecBugs
- **Version**: 4.8.6
- **Purpose**: Static analysis for Java/Kotlin code security vulnerabilities
- **Configuration**: Max effort, Low threshold
- **Workflow**: Runs on every push and daily at 2 AM UTC
- **Reports**: `target/spotbugsXml.xml`

## Code Quality

### KtLint
- **Tool**: KtLint
- **Version**: 3.5.0
- **Purpose**: Kotlin code style and linting
- **Workflow**: Runs on every push
- **Reports**: `target/ktlint-report.html`

## Code Coverage

### JaCoCo
- **Tool**: JaCoCo (Java Code Coverage)
- **Version**: 0.8.12
- **Purpose**: Test coverage analysis
- **Coverage Goals**:
  - **Instruction Coverage**: ≥ 60%
  - **Branch Coverage**: ≥ 50%
  - **Line Coverage**: ≥ 60%
  - **Complexity Coverage**: ≥ 50%
- **Workflow**: Runs on every push and daily
- **Reports**: 
  - `target/site/jacoco/jacoco.xml` (XML)
  - `target/site/jacoco/index.html` (HTML)
  - Uploaded to Codecov for historical tracking

## Performance Profiling

### JMH Benchmarks
- **Tool**: Java Microbenchmark Harness (JMH)
- **Version**: 1.37
- **Purpose**: Performance benchmarking and profiling
- **Location**: `fragments-core/src/test/kotlin/io/andromeda/fragments/test/PerformanceTest.kt`
- **Workflow**: Runs on performance test execution
- **Reports**: `target/benchmarks/`

## Benchmark Results

### Current Performance Targets

All performance tests should meet these criteria:

1. **Fragment Creation**: < 1.0ms average per fragment
2. **Repository Operations**: < 10.0ms average per operation
3. **Markdown Rendering**: < 5.0ms average per fragment
4. **Concurrent Access**: < 5.0ms average per operation
5. **Front Matter Parsing**: < 1.0ms average per iteration

### Running Performance Tests

```bash
# Run all performance tests
mvn clean test -Dtest=PerformanceTest

# Run specific performance test
mvn clean test -Dtest=PerformanceTest#fragmentCreationPerformance
```

## Security Policies

### Dependency Vulnerability Policy
- **High Severity (CVSS 7.0-10.0)**: Build failure
- **Medium Severity (CVSS 4.0-6.9)**: Warning, but allow build
- **Low Severity (CVSS 0.0-3.9)**: Warning, but allow build
- **False Positives**: Suppressed via `dependency-check-suppressions.xml`

### Code Quality Gates

All code quality checks must pass before merging:

1. **No critical security vulnerabilities**
2. **Code coverage ≥ 60% for core modules**
3. **No KtLint errors**
4. **No SpotBugs high-severity issues**

## CI/CD Integration

All security and quality checks are integrated into GitHub Actions:

- **`.github/workflows/ci.yml`**: Main CI pipeline
- **`.github/workflows/security-quality.yml`**: Security & quality checks
- **Scheduled Runs**: Daily at 2 AM UTC

## Reporting

### Security Reports
- OWASP dependency check reports uploaded as artifacts
- SpotBugs reports uploaded on failure
- Historical data available in GitHub Actions

### Coverage Reports
- JaCoCo reports uploaded as artifacts
- Codecov integration for trend analysis
- Badge generation for README (optional)

### Performance Reports
- Benchmark results uploaded as artifacts
- Performance trends tracked over time
- Alerts on performance degradation

## Best Practices

### Security
1. Keep dependencies up-to-date
2. Review and suppress false positives
3. Address high-severity vulnerabilities promptly
4. Use security-focused code reviews

### Code Quality
1. Maintain high test coverage (> 70% target)
2. Fix KtLint issues incrementally
3. Address SpotBugs findings before release
4. Regular performance regression testing

### Performance
1. Monitor benchmark trends
2. Investigate performance degradations
3. Profile hot paths with JMH
4. Optimize critical code paths

## References

- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [SpotBugs](https://spotbugs.github.io/)
- [JaCoCo](https://www.jacoco.org/jacoco/)
- [JMH](https://openjdk.org/projects/code-tools/jmh/)
- [Codecov](https://codecov.io/)
