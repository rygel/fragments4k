# Fragments Architecture Tests

ArchUnit-based test suite enforcing structural integrity across the entire fragments4k codebase.

## Enforced Rules

### Layering
- Repository interfaces in `io.github.rygel.fragments` must be actual Kotlin interfaces (not classes)
- Each feature module (blog, cache, rss, lucene, sitemap, etc.) must not depend on any adapter package

### Naming Conventions
- Repository interfaces must end with `Repository`
- Classes implementing `FragmentRepository` must end with `Repository`
- Factory classes must end with `Factory`

### Module Dependencies
- Core and feature modules must never depend on adapter modules
- Adapters must not depend on each other (they are interchangeable)
- No cyclic dependencies between top-level packages

### Coding Standards
- Bans legacy APIs: `java.util.Date`, `Calendar`, `Hashtable`, `Vector`
- Core and feature modules must use SLF4J instead of `System.out`/`System.err`
- Production code must not depend on JUnit or MockK
- Core must not depend on Spring, Jakarta EE, Micronaut, http4k, or Javalin

## Running

```bash
./mvnw -pl fragments-architecture-tests test
```

This module has no production source — it only contains ArchUnit JUnit 5 test classes.
