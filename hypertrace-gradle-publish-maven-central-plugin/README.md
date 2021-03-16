# Hypertrace Publish Maven Central Plugin

###### org.hypertrace.publish-maven-central-plugin

### Purpose
This plugin configures the target project to publish its java artifacts to maven central. 
It uses the `maven-publish` and `signing` plugins internally to do this.
By default, it will publish to https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/. 
It also adds `javadocJar` and `sourcesJar` tasks to the project.
The following credentials must be provided as gradle properties in the default configuration:
- ossrhUsername
- ossrhPassword

For signing artifacts, following properties must be provided as gradle properties:
- signingKey
- signingPassword

The plugin also uses [gradle-nexus-staging-plugin](https://github.com/Codearte/gradle-nexus-staging-plugin) plugin for closing and releasing staged repositories.

Each property described below can be configured in the DSL. The default values are shown for each property,
all of which, with the exception of license, can be omitted if left unchanged.
```kotlin
hypertracePublishMavenCentral {
  license // REQUIRED to be a value defined in org.hypertrace.gradle.publishing.License
  repoName // REQUIRED. Name of the repository.
  url // Optional. The URL for the publication represented by the POM.
}
```

Currently supported publications:
- `java-library`: For projects applying `java-library`, the `java` component (i.e. the jar) will be registered as a publication
### Example
#### Root project

```kotlin
// Specify the publish version but don't apply it
plugins {
  id("org.hypertrace.publish-maven-central-plugin") version "<version>" apply false
}

subprojects {
  pluginManager.withPlugin("org.hypertrace.publish-maven-central-plugin") {
    configure<org.hypertrace.gradle.publishing.HypertracePublishMavenCentralExtension> {
      repoName.set("repository-name");
      license.set(org.hypertrace.gradle.publishing.License.APACHE_2_0);
    }
  }
}
```
#### Publishing child projects
```kotlin
plugins {
  id("org.hypertrace.publish-maven-central-plugin") // No version required, set by parent
}
```

#### Releasing repositories
```bash
./gradlew closeAndReleaseRepository
```
