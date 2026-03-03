# Fragments Port: Step-by-Step Implementation Guide

This guide is for developers (especially juniors) implementing the port of the "Fragments" library to an adapter-based architecture supporting HTTP4k, Javalin, Spring Boot, Quarkus, and Micronaut.

The goal is to build a framework-agnostic core engine that parses Markdown files asynchronously, and then write lightweight adapters for each web framework. We are also supporting HTMX for dynamic content loading.

---

## Phase 1: Foundation and Core Data Structures
*Goal: Establish the basic data models and the repository that reads Markdown files.*

### Step 1: Project Setup
1. Convert the project structure from the old Pippo layout. Create a multi-module Gradle or Maven build.
2. Define the exact modules:
   * `fragments-core`
   * `fragments-static-core`
   * `fragments-blog-core`
   * `fragments-rss-core`
   * `fragments-lucene-core`
   * `fragments-http4k`
   * `fragments-javalin`
   * `fragments-spring-boot`
   * `fragments-quarkus`
   * `fragments-micronaut`

### Step 2: Implement the Domain Model (`fragments-core`)
1. Create `Fragment.kt`: An immutable data class representing a parsed Markdown file.
   * Fields: `title`, `slug`, `date`, `preview`, `content` (HTML string), `frontMatter` (Map), `visible`, `template`.

### Step 3: Implement the Repository (`fragments-core`)
1. Create the `FragmentRepository` interface. **Crucially, all methods must be `suspend` functions** to support asynchronous loading.
   * `suspend fun getAllVisible(): List<Fragment>`
   * `suspend fun getBySlug(slug: String): Fragment?`
2. Implement `FileSystemFragmentRepository`.
   * Use Kotlin Coroutines (`Dispatchers.IO`) to read files asynchronously without blocking.
   * Use Flexmark-java and SnakeYAML/FastJSON to parse the Markdown and Front Matter into the `Fragment` objects.

### 🛑 Checkpoint 1: Core Unit Tests
Before moving on, verify `Phase 1` is rock-solid.
*   **Write Unit Tests**: Create a `src/test/resources/test-fragments` folder with various mock markdown files.
*   **Verify**: Ensure `FileSystemFragmentRepository` correctly parses all fields (especially dates and YAML maps) into the `Fragment` data class.

---

## Phase 2: Feature Core Engines
*Goal: Build the framework-agnostic engines that prepare data for rendering.*

### Step 4: Implement Static Pages Engine (`fragments-static-core`)
1. Create `StaticPageEngine(private val repository: FragmentRepository)`.
2. Add `suspend fun getPage(slug: String): Fragment?`.

### Step 5: Implement Blog Engine (`fragments-blog-core`)
1. Create `BlogEngine(private val repository: FragmentRepository)`.
2. Add `suspend` methods for blog logic:
   * `getOverview(page: Int): Page<Fragment>` (Implement pagination)
   * `getPost(year: String, month: String, slug: String): Fragment?`
   * `getByTag(tag: String, page: Int): Page<Fragment>`

### Step 6: Implement View Models & HTMX Support (`fragments-core`)
1. Create a `FragmentViewModel` data class.
2. Add an `isPartialRender: Boolean` field to the ViewModel. This flag represents HTMX support. If true, the template engine should render *only* the fragment content, omitting the full HTML page layout (`<head>`, `<body>`, etc.).

### 🛑 Checkpoint 2: Engine Unit Tests
*   **Write Unit Tests**: Use a mocked `FragmentRepository` (e.g., using MockK or a manual dummy class).
*   **Verify Blog Engine**: Assert that `getOverview` returns the correct 10 items for page 1, and the next 10 for page 2. Assert that `getByTag` correctly filters out unrelated fragments.

---

## Phase 3: Web Framework Adapters
*Goal: Connect the framework-agnostic core to actual HTTP web frameworks.*

### Step 7: HTTP4k Adapter (`fragments-http4k`)
1. Create a factory function returning a `RoutingHttpHandler`.
2. Map the request to the core engine.
3. **HTMX & Coroutines**: Read the `HX-Request` header. Bridge the HTTP4k synchronous handler to the `suspend` functions using `runBlocking` (or HTTP4k's async integrations if available).
4. Return the HTML response via a `TemplateRenderer`.

### Step 8: Javalin Adapter (`fragments-javalin`)
1. Create a `StaticPagesController` providing an extension function for `Javalin`.
2. **HTMX & Coroutines**: Read the `HX-Request` header. Use Javalin's `Context.future()` combined with Kotlin's `async { }` builder to handle the `suspend` functions off the main server thread.

### Step 9: Spring Boot / Quarkus / Micronaut Adapters
1. Create standard Controller classes (e.g., `@RestController`, `@GetMapping`).
2. Inject the core engines via their respective Dependency Injection frameworks (`@Bean`, `@Produces`).
3. Return the populated ViewModels to their respective template engines.

### 🛑 Checkpoint 3: Adapter Integration Tests
This is the most critical checkpoint. You must prove the adapters work in a real HTTP environment.
*   **Write Integration Tests for Each Framework**:
    *   *HTTP4k*: Call the `HttpHandler` directly with a built `Request`. Assert the `Response`.
    *   *Javalin*: Start the server on a random port in a JUnit `@BeforeAll`. Use a standard `HttpClient` to make real network requests to the endpoint. Assert HTTP 200.
    *   *Spring / Quarkus / Micronaut*: Use their built-in test runners (`@SpringBootTest`, `@QuarkusTest`, `@MicronautTest`) with `TestRestTemplate`/`RestAssured`/`@Client` to execute HTTP requests against the mock engines.
*   **Verify HTMX**: In the tests, send the `HX-Request: true` header and assert the response body is strictly the partial HTML, not the full document layout.

---

## Phase 4: Advanced Features (RSS & Lucene)
*Goal: Add syndication and search capabilities.*

### Step 10: Implement RSS Core (`fragments-rss-core`)
1. Create `RssGenerator`. Add `suspend fun generateFeed(...): String`.
2. Generate raw XML. **Unit Test** this thoroughly using string matching or an XML parser.

### Step 11: Implement Lucene Core (`fragments-lucene-core`)
1. Create `LuceneSearchEngine`.
2. Add `suspend fun index(fragments: List<Fragment>)` and `suspend fun search(query: String)`.
3. **Integration Test**: Use an in-memory `ByteBuffersDirectory` during tests to verify indexing and searching works without touching the real disk.

### Step 12: Implement Adapters for RSS & Lucene
1. Add endpoints for `.xml` and `/search` to all the framework adapters implemented in Phase 3.

### 🏆 Final Checkpoint: Full System Test
1. Create small Demo applications (e.g., `demo-javalin`, `demo-spring`).
2. Point them at a real directory of Markdown files.
3. Start the servers and manually verify the application in a web browser, confirming static pages, blog pagination, search, and HTMX navigation all function seamlessly.
