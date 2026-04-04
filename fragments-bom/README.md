# Fragments BOM (Bill of Materials)

Import this BOM to manage fragments4k dependency versions in one place.

## Usage

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.rygel</groupId>
            <artifactId>fragments-bom</artifactId>
            <version>0.6.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Then declare dependencies without version -->
<dependencies>
    <dependency>
        <groupId>io.github.rygel</groupId>
        <artifactId>fragments-http4k</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.rygel</groupId>
        <artifactId>fragments-seo-core</artifactId>
    </dependency>
</dependencies>
```
