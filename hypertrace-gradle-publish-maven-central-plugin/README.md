# Hypertrace Publish Maven Central Plugin

###### org.hypertrace.publish-maven-central-plugin

### Purpose
This plugin configures the target project to publish its java artifacts to maven central. It uses the maven-publish plugin internally to do this.

Each publication is made a dependency of a root project `tag` task, if it exists.

Currently supported publications:
- `java-library`: For projects applying `java-library`, the `java` component (i.e. the jar) will be registered as a publication
- `distribution`: For projects applying `distribution`, the `distZip` output will be registered as a publication
### Example
#### Root project

```kotlin
// Specify the publish version but don't apply it
plugins {
  id("org.hypertrace.publish-maven-central-plugin") version "<version>" apply false
}
```
#### Publishing child projects
```kotlin
plugins {
  id("org.hypertrace.publish-maven-central-plugin") // No version required, set by parent
}

```
