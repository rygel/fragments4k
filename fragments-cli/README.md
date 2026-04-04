# Fragments CLI

Command-line tool for scaffolding new fragments4k projects and running a development server with live reload.

## Commands

### `init` — Scaffold a New Project

```bash
java -jar fragments-cli.jar init my-blog --framework=http4k
```

| Option | Default | Description |
|--------|---------|-------------|
| `--framework`, `-f` | `http4k` | Framework: `http4k`, `javalin`, `spring-boot`, `quarkus`, `micronaut` |
| `--directory`, `-d` | `.` | Output directory |
| `--package`, `-p` | `io.github.rygel.fragments.demo` | Base package name |

Generates a complete project with Maven config, framework-specific templates, sample content, and a README.

### `run` — Development Server

```bash
java -jar fragments-cli.jar run --watch --content-dir=./content --port=8080
```

| Option | Default | Description |
|--------|---------|-------------|
| `--port`, `-p` | `8080` | Server port |
| `--content-dir`, `-d` | `content` | Content directory path |
| `--watch`, `-w` | `false` | Enable live reload on file changes |
| `--framework`, `-f` | auto-detect | Framework to use |
