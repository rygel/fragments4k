# Architecture, Security, and Usability Next Steps

> For agentic workers: implement task-by-task. Use checkbox (`- [ ]`) syntax for tracking. Test method names must use camelCase only; do not use Kotlin backtick test names because Maven Surefire can fail to execute them.

**Goal:** Convert the architecture/security/usability review into an actionable improvement backlog with clear scope, files, acceptance criteria, and verification commands.

**Scope:** Kotlin/Maven codebase, framework adapters, shared adapter core, CLI generator, CI security workflows, and documentation.

**Verification baseline:**
- `mvn test-compile`
- `mvn test`
- `mvn -pl fragments-core test`
- For adapter changes, run the touched adapter module test plus `fragments-adapter-core` tests.

---

## Priority 0: Confirm Current Baseline

- [ ] **Task 0.1: Run baseline test compilation**
  - Command: `mvn test-compile`
  - Expected: test sources compile successfully.
  - If this fails, record the first failing module and fix compile errors before functional changes.

- [ ] **Task 0.2: Run baseline tests**
  - Command: `mvn test`
  - Expected: tests execute without Maven Surefire method-name parsing errors.
  - If tests fail, separate pre-existing failures from failures introduced by this plan.

- [ ] **Task 0.3: Capture quality baseline**
  - Command: `mvn verify -DskipTests`
  - Expected: lint/format/static checks that are bound to `verify` complete or produce known failures.
  - Record any known failures in the implementation notes before changing behavior.

---

## Priority 1: Fix Relationship Cache Correctness

**Problem:** `FileSystemFragmentRepository` appears to cache a single `ContentRelationships` value. Relationship results depend on at least the current slug and relationship configuration, so a single cached value can return incorrect relationships for subsequent posts.

**Primary files:**
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt`
- `fragments-core/src/test/kotlin/io/github/rygel/fragments/ContentRelationshipTest.kt`

- [ ] **Task 1.1: Add failing test for slug-specific relationship results**
  - Create or extend a test with at least three published fragments.
  - Call `getRelationships("first-slug", config)` and then `getRelationships("second-slug", config)`.
  - Assert that the second call is computed for `second-slug`, not reused from the first call.
  - Test method name example: `fun testRelationshipsAreCachedPerSlug()`.

- [ ] **Task 1.2: Add failing test for cache invalidation on reload**
  - Compute relationships.
  - Change repository content or use test repository state to alter relationships.
  - Call `reload()`.
  - Assert relationships are recomputed after reload.

- [ ] **Task 1.3: Implement cache keyed by slug and config**
  - Replace the single `cachedRelationships` value with a map keyed by slug plus relationship config.
  - If `RelationshipConfig` is not safely usable as a map key, introduce a small internal key data class using the fields that affect relationship calculation.
  - Keep `reload()` clearing the relationship cache.

- [ ] **Task 1.4: Verify**
  - Command: `mvn -pl fragments-core test -Dtest=ContentRelationshipTest`
  - Command: `mvn -pl fragments-core test`

**Acceptance criteria:**
- Different slugs cannot receive stale relationship results from prior calls.
- `reload()` clears all relationship cache entries.
- No public API change unless needed.

---

## Priority 2: Centralize Route and Query Input Validation

**Problem:** Public route/query inputs such as `slug`, `tag`, `category`, `author`, `page`, `q`, and `limit` are passed through adapters with inconsistent validation. This increases security and reliability risk across frameworks.

**Primary files:**
- Create: `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/RequestValidation.kt`
- Create: `fragments-adapter-core/src/test/kotlin/io/github/rygel/fragments/adapter/RequestValidationTest.kt`
- Modify framework adapters as needed:
  - `fragments-spring-boot/src/main/kotlin/io/github/rygel/fragments/spring/FragmentsSpringController.kt`
  - `fragments-quarkus/src/main/kotlin/io/github/rygel/fragments/quarkus/FragmentsQuarkusResource.kt`
  - `fragments-micronaut/src/main/kotlin/io/github/rygel/fragments/micronaut/FragmentsMicronautController.kt`
  - `fragments-javalin/src/main/kotlin/io/github/rygel/fragments/javalin/FragmentsJavalinAdapter.kt`
  - `fragments-http4k/src/main/kotlin/io/github/rygel/fragments/http4k/FragmentsHttp4kAdapter.kt`

- [ ] **Task 2.1: Define shared validation behavior**
  - Slugs/tags/categories/authors: allow lowercase alphanumeric plus hyphen by default, with a clear max length.
  - Page numbers: minimum `1`, maximum configurable or a conservative default.
  - Year: four digits and a reasonable range, for example `1970..3000`.
  - Month: `1..12`.
  - Search query: trim, reject blank for search pages or return empty results intentionally, cap length.
  - Autocomplete limit: clamp to a small maximum, for example `1..50`.

- [ ] **Task 2.2: Add focused validation tests**
  - `testValidSlugIsAccepted`
  - `testInvalidSlugIsRejected`
  - `testPageNumberIsClamped`
  - `testAutocompleteLimitIsClamped`
  - `testSearchQueryIsTrimmedAndLengthLimited`
  - `testMonthOutsideRangeIsRejected`

- [ ] **Task 2.3: Implement `RequestValidation`**
  - Prefer small pure functions returning typed results or `Result<T>`.
  - Avoid throwing from low-level helpers unless adapters can map exceptions cleanly.
  - Keep validation independent of specific web frameworks.

- [ ] **Task 2.4: Integrate into `FragmentsEngine` where appropriate**
  - Add validated helper methods for search/autocomplete/page generation if that keeps adapters thin.
  - Keep existing public methods for source compatibility if possible.

- [ ] **Task 2.5: Integrate adapters incrementally**
  - Start with Spring Boot because it is easy to verify.
  - Apply the same validation behavior to all adapters.
  - Return consistent `400 Bad Request` responses for invalid input rather than empty or ambiguous results.

- [ ] **Task 2.6: Verify**
  - Command: `mvn -pl fragments-adapter-core test`
  - Command: `mvn -pl fragments-spring-boot test`
  - Command: `mvn -pl fragments-http4k test`
  - Command: `mvn -pl fragments-javalin test`
  - Command: `mvn -pl fragments-quarkus test`
  - Command: `mvn -pl fragments-micronaut test`

**Acceptance criteria:**
- All adapters enforce the same input rules.
- Negative page/limit values cannot cause odd behavior or expensive queries.
- Overlong search queries are bounded before Lucene parsing.

---

## Priority 3: Harden Search and Autocomplete

**Problem:** `LuceneSearchEngine` parses user-supplied queries and uses wildcard autocomplete. Search should be bounded defensively even when adapter validation is bypassed.

**Primary files:**
- `fragments-lucene-core/src/main/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngine.kt`
- `fragments-lucene-core/src/test/kotlin/io/github/rygel/fragments/lucene/LuceneSearchEngineTest.kt`
- `fragments-lucene-core/src/test/kotlin/io/github/rygel/fragments/lucene/CachedLuceneSearchEngineTest.kt`

- [ ] **Task 3.1: Add engine-level bounds tests**
  - Search with blank query returns empty results.
  - Overlong query is truncated or rejected according to documented behavior.
  - Negative `maxResults` returns empty or clamps to minimum.
  - Autocomplete `limit` is clamped.

- [ ] **Task 3.2: Add safe query normalization**
  - Trim query strings.
  - Apply a maximum query length.
  - Clamp `maxResults` and autocomplete limits.
  - Consider escaping or fallback behavior for Lucene syntax errors.

- [ ] **Task 3.3: Review autocomplete wildcard behavior**
  - Avoid constructing wildcard queries for very short inputs if they are too broad.
  - Consider minimum prefix length, for example 2 characters.
  - Document the behavior in `fragments-lucene-core/README.md`.

- [ ] **Task 3.4: Verify**
  - Command: `mvn -pl fragments-lucene-core test`

**Acceptance criteria:**
- Search remains safe even when called directly.
- Invalid or expensive query shapes fail closed with empty results or a controlled error.

---

## Priority 4: Make CI Security Gates Match Documentation

**Problem:** `SECURITY_QUALITY.md` describes OWASP Dependency Check and SpotBugs/FindSecBugs, but the inspected workflow only runs Gitleaks and Semgrep, and Semgrep is non-blocking because it ends with `|| true`.

**Primary files:**
- `.github/workflows/security-quality.yml`
- `pom.xml`
- `SECURITY_QUALITY.md`
- `dependency-check-suppressions.xml`
- `owasp-suppressions.xml`

- [ ] **Task 4.1: Decide blocking vs advisory policy**
  - Blocking: high-confidence SAST, high CVSS dependency findings, secret leaks.
  - Advisory: low-confidence SAST, medium/low dependency findings.
  - Document this policy in `SECURITY_QUALITY.md`.

- [ ] **Task 4.2: Make Semgrep fail when configured as blocking**
  - Remove `|| true` from `.github/workflows/security-quality.yml`.
  - If needed, split into two Semgrep jobs: one blocking and one advisory.

- [ ] **Task 4.3: Add actual dependency vulnerability scanning**
  - Either add OWASP Dependency Check to Maven or use a GitHub workflow step.
  - Reuse the existing suppressions file, or consolidate duplicate suppression files.
  - Set failure threshold according to the documented policy.

- [ ] **Task 4.4: Add or remove SpotBugs documentation**
  - If SpotBugs/FindSecBugs is required, wire it into Maven or CI.
  - If it is not required, update `SECURITY_QUALITY.md` to avoid claiming it runs.

- [ ] **Task 4.5: Verify CI locally where possible**
  - Command: `mvn verify -DskipTests`
  - Command: run the configured dependency/security Maven goal if added.

**Acceptance criteria:**
- Security documentation matches real CI behavior.
- Secret detection remains enabled.
- Blocking security checks can fail pull requests.

---

## Priority 5: Tighten Content Security Policy Defaults

**Problem:** The default CSP allows CDN scripts and styles. That is convenient for examples but not ideal as the library default.

**Primary files:**
- `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/FragmentsEngine.kt`
- Adapter tests for CSP headers:
  - `fragments-spring-boot/src/test/kotlin/io/github/rygel/fragments/spring/FragmentsSpringControllerTest.kt`
  - `fragments-quarkus/src/test/kotlin/io/github/rygel/fragments/quarkus/FragmentsQuarkusResourceTest.kt`
  - `fragments-micronaut/src/test/kotlin/io/github/rygel/fragments/micronaut/FragmentsMicronautControllerTest.kt`
  - `fragments-http4k/src/test/kotlin/io/github/rygel/fragments/http4k/FragmentsHttp4kAdapterTest.kt`
  - `fragments-javalin/src/test/kotlin/io/github/rygel/fragments/javalin/FragmentsJavalinAdapterTest.kt`

- [ ] **Task 5.1: Add tests for default CSP**
  - Assert the default CSP is strict and self-hosted.
  - Assert custom CSP values are preserved.

- [ ] **Task 5.2: Change default CSP**
  - Suggested default: `default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'`
  - Review whether inline styles in generated/demo templates require docs changes.

- [ ] **Task 5.3: Add README guidance**
  - Explain how to opt into CDN hosts.
  - Recommend self-hosting assets for production.

- [ ] **Task 5.4: Verify**
  - Command: run touched adapter tests.
  - Command: `mvn -pl fragments-adapter-core test`

**Acceptance criteria:**
- Production default is strict.
- Existing users can override CSP explicitly.
- All adapters still emit the configured CSP header.

---

## Priority 6: Add Sanitizer Modes and Documentation

**Problem:** `HtmlSanitizer` uses a relaxed safelist and allows `class`/`id` on all tags. This is practical for trusted Markdown, but multi-author sites need a stricter profile.

**Primary files:**
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/HtmlSanitizer.kt`
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/MarkdownParser.kt`
- `fragments-core/src/test/kotlin/io/github/rygel/fragments/HtmlSanitizerTest.kt`
- `fragments-core/README.md`

- [ ] **Task 6.1: Add tests for current sanitizer guarantees**
  - Script tags are removed.
  - Dangerous URL protocols are removed.
  - The `<!--more-->` marker is preserved.
  - `target` links include or retain safe `rel` behavior if implemented.

- [ ] **Task 6.2: Introduce sanitizer profile enum**
  - Example: `STRICT`, `RELAXED_TRUSTED_AUTHOR`.
  - Default should preserve existing behavior unless this is a planned breaking release.

- [ ] **Task 6.3: Make `MarkdownParser` accept sanitizer configuration**
  - Keep current constructor source-compatible if possible.
  - Document which profile is appropriate for untrusted authors.

- [ ] **Task 6.4: Consider link hardening**
  - For links with `target="_blank"`, ensure `rel="noopener noreferrer"` is present.
  - Add tests for this behavior.

- [ ] **Task 6.5: Verify**
  - Command: `mvn -pl fragments-core test -Dtest=HtmlSanitizerTest`
  - Command: `mvn -pl fragments-core test`

**Acceptance criteria:**
- Users can choose strict sanitization without replacing the parser.
- Existing content behavior is documented clearly.

---

## Priority 7: Improve Filesystem Safety

**Problem:** File-backed repositories recursively walk configured paths and write front matter updates. These operations should be explicit about canonical path boundaries and symlink behavior.

**Primary files:**
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRepository.kt`
- `fragments-core/src/main/kotlin/io/github/rygel/fragments/FileSystemFragmentRevisionRepository.kt`
- Related tests in `fragments-core/src/test/kotlin/io/github/rygel/fragments/`

- [ ] **Task 7.1: Add tests for canonical base path handling**
  - Missing directory returns empty list as today.
  - Files outside the base directory are not read through symlink traversal if symlinks are rejected.
  - Status updates only write to files under the canonical base directory.

- [ ] **Task 7.2: Define symlink policy**
  - Option A: reject symlinks by default.
  - Option B: allow symlinks but reject canonical paths outside base.
  - Document the decision in KDoc and README.

- [ ] **Task 7.3: Centralize safe file discovery**
  - Replace repeated `walkTopDown()` filtering with a helper that applies extension, file size, and canonical path checks.

- [ ] **Task 7.4: Verify**
  - Command: `mvn -pl fragments-core test`

**Acceptance criteria:**
- Repository cannot accidentally read/write outside configured content root.
- File traversal behavior is documented and tested.

---

## Priority 8: Update CLI Generator for Production-Ready Projects

**Problem:** `ProjectGenerator` hard-codes framework versions and should stay aligned with the parent project's documented Java baseline. Generated templates also rely on raw HTML rendering without explaining the sanitizer contract.

**Primary files:**
- `fragments-cli/src/main/kotlin/io/github/rygel/fragments/cli/ProjectGenerator.kt`
- `fragments-cli/src/test/kotlin/io/github/rygel/fragments/cli/ProjectGeneratorTest.kt`
- `fragments-cli/README.md`

- [ ] **Task 8.1: Add generator tests for Java and Fragments versions**
  - Generated POM uses the same Java baseline as the parent POM or a documented compatible baseline.
  - Generated POM uses `fragments-bom` or a single `fragments.version` property.
  - Generated framework versions are properties, not scattered literals.

- [ ] **Task 8.2: Replace hard-coded dependency versions**
  - Prefer using `fragments-bom` if it manages the starter dependencies.
  - Otherwise centralize generated version constants in one place.

- [ ] **Task 8.3: Add generated production checklist**
  - Generated README should include:
    - Set real `siteUrl`.
    - Configure CSP.
    - Configure `urlBuilder`.
    - Run `mvn test`.
    - Keep generated dependencies current.
    - Avoid exposing drafts in production.

- [ ] **Task 8.4: Explain raw HTML rendering**
  - Generated templates that use raw/unescaped HTML must state that content is sanitized by Fragments before rendering.
  - Provide a stricter rendering note for untrusted content.

- [ ] **Task 8.5: Verify**
  - Command: `mvn -pl fragments-cli test`

**Acceptance criteria:**
- Generated projects build with current documented prerequisites.
- Starter users get secure defaults and clear production next steps.

---

## Priority 9: Harden Image Optimization

**Problem:** Image optimization should guard against oversized inputs, unsupported formats, and mismatched metadata.

**Primary files:**
- `fragments-image-optimization-core/src/main/kotlin/io/github/rygel/fragments/image/BasicImageOptimizer.kt`
- `fragments-image-optimization-core/src/test/kotlin/io/github/rygel/fragments/image/BasicImageOptimizerTest.kt`
- `fragments-image-optimization-core/README.md`

- [ ] **Task 9.1: Add tests for invalid inputs**
  - Unsupported format returns a clear failure.
  - Huge dimensions are rejected or bounded.
  - Quality is clamped to `0.0..1.0`.
  - WebP conversion either truly writes WebP or reports PNG output accurately.

- [ ] **Task 9.2: Add configurable safety limits**
  - Max input bytes.
  - Max width/height.
  - Max total pixels.

- [ ] **Task 9.3: Fix metadata consistency**
  - `format`, `mimeType`, and actual output encoder must agree.
  - If Java ImageIO cannot write WebP, do not claim WebP output.

- [ ] **Task 9.4: Verify**
  - Command: `mvn -pl fragments-image-optimization-core test`

**Acceptance criteria:**
- Image processing fails safely for oversized or unsupported images.
- Metadata matches actual output.

---

## Priority 10: Expand Architecture Tests

**Problem:** Existing architecture tests cover adapter isolation and cycles, but not all important module boundaries.

**Primary files:**
- `fragments-architecture-tests/src/test/kotlin/io/github/rygel/fragments/architecture/ModuleDependencyRulesTest.kt`
- `fragments-architecture-tests/src/test/kotlin/io/github/rygel/fragments/architecture/LayeringRulesTest.kt`
- `fragments-architecture-tests/README.md`

- [ ] **Task 10.1: Add feature-module boundary tests**
  - Feature modules must not depend on framework adapters.
  - Feature modules must not depend on `fragments-adapter-core` unless explicitly allowed.
  - CLI dependencies are limited to generation needs.

- [ ] **Task 10.2: Add package naming and public API tests**
  - Framework-specific packages stay under their adapter package.
  - Demo packages do not appear in library modules.

- [ ] **Task 10.3: Add documentation for allowed dependencies**
  - Include a simple module dependency diagram or table in `fragments-architecture-tests/README.md`.

- [ ] **Task 10.4: Verify**
  - Command: `mvn -pl fragments-architecture-tests test`

**Acceptance criteria:**
- Future adapter/core coupling regressions fail in tests.
- Allowed exceptions are documented rather than implicit.

---

## Priority 11: Improve Adapter Error Consistency

**Problem:** Error responses differ between adapters and between HTML/API endpoints. `ErrorResponse` exists but is not uniformly used.

**Primary files:**
- `fragments-adapter-core/src/main/kotlin/io/github/rygel/fragments/adapter/ErrorResponse.kt`
- All adapter modules under `fragments-*-boot`, `fragments-http4k`, `fragments-javalin`, `fragments-quarkus`, `fragments-micronaut`

- [ ] **Task 11.1: Define common error contract**
  - HTML pages may still render `error/404`.
  - API endpoints should return structured `ErrorResponse`.
  - Invalid public input should map to `400`.
  - Missing content should map to `404`.
  - Unexpected exceptions should map to `500` without leaking exception messages in production.

- [ ] **Task 11.2: Add adapter tests**
  - Invalid search/autocomplete limit returns `400`.
  - Missing page/post returns `404`.
  - Error body does not leak stack traces.

- [ ] **Task 11.3: Implement shared mapping helpers**
  - Keep framework-specific response construction inside adapters.
  - Keep error naming/messages consistent through adapter-core constants or helpers.

- [ ] **Task 11.4: Verify**
  - Command: run all adapter module tests.

**Acceptance criteria:**
- API consumers get consistent status codes and error shapes across adapters.
- HTML users still get normal not-found pages.

---

## Priority 12: Documentation Pass

**Problem:** The project has strong README coverage, but the reviewed risks need discoverable operational guidance.

**Primary files:**
- `README.md`
- `fragments-core/README.md`
- `fragments-lucene-core/README.md`
- `fragments-cli/README.md`
- `SECURITY_QUALITY.md`

- [ ] **Task 12.1: Add security model section**
  - Trusted vs untrusted Markdown authors.
  - Sanitization profiles.
  - CSP defaults and override guidance.
  - Filesystem repository trust boundary.

- [ ] **Task 12.2: Add production readiness checklist**
  - Real `siteUrl`.
  - Strict CSP.
  - Dependency/security checks enabled.
  - Search input bounds.
  - Content path permissions.
  - No drafts exposed publicly.

- [ ] **Task 12.3: Add adapter consistency notes**
  - Explain which behaviors are guaranteed across adapters.
  - Link to adapter-specific README files.

- [ ] **Task 12.4: Verify docs**
  - Check all commands still match Maven/Surefire guidance.
  - Ensure no Kotlin test examples use backtick method names.

**Acceptance criteria:**
- New adopters can configure a production-safe Fragments4k site without reading source.
- Security documentation accurately describes implemented defaults.

---

## Suggested Implementation Order

1. Priority 0 baseline.
2. Priority 1 relationship cache correctness.
3. Priority 2 shared validation.
4. Priority 3 search hardening.
5. Priority 4 CI security gates.
6. Priority 5 CSP defaults.
7. Priority 8 CLI generator updates.
8. Priority 6, 7, 9, 10, 11, and 12 as follow-up hardening/documentation work.

## Final Verification Before Closing This Plan

- [ ] `mvn test-compile`
- [ ] `mvn test`
- [ ] `mvn verify`
- [ ] No Kotlin test method names use backticks or special characters.
- [ ] `SECURITY_QUALITY.md` matches actual CI configuration.
- [ ] All changed behavior is covered by focused tests.
