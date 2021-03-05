# Traceable Publish Plugin

###### ai.traceable.publish-plugin

### Purpose
This plugin configures the target project to publish its java artifacts. It uses the maven-publish plugin internally to do this.
It configures the artifactory repository to publish to, and registers any known publications.

Each publication is made a dependency of a root project `tag` task, if it exists.

Currently supported publications:
- `java-library`: For projects applying `java-library`, the `java` component (i.e. the jar) will be registered as a publication
- `distribution`: For projects applying `distribution`, the `distZip` output will be registered as a publication
### Example
#### Root project

```kotlin
// Specify the publish version but don't apply it
plugins {
  id("ai.traceable.publish-plugin") version "<version>" apply false
}
```
#### Publishing child projects
```kotlin
plugins {
  id("ai.traceable.publish-plugin") // No version required, set by parent
}

```
