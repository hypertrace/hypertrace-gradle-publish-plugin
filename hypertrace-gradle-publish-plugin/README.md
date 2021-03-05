# Hypertrace Publish Plugin
###### org.hypertrace.publish-plugin
[![CircleCI](https://circleci.com/gh/hypertrace/hypertrace-gradle-publish-plugin.svg?style=svg)](https://circleci.com/gh/hypertrace/hypertrace-gradle-publish-plugin)

### Purpose
This plugin configures the target project to publish its java artifacts to the Hypertrace bintray
repository. It uses the `maven-publish` and `gradle-bintray-plugin` plugins internally to do this.
By default, it will publish to https://dl.bintray.com/hypertrace/maven. The following credentials
must be provided as gradle properties in the default configuration:
- publishUser
- publishApiKey

Additionally, no default value is provide for the license. This must be set explicitly via dsl.

Each property described below can be configured in the DSL. The default values are shown for each property,
all of which, with the exception of license, can be omitted if left unchanged.
```kotlin
 hypertracePublish {
      license // REQUIRED to be a value defined in org.hypertrace.gradle.publishing.License
      vcsUrl // Optional. Defaults to the env var CIRCLE_REPOSITORY_URL
      user // Optional. Defaults to gradle property publishUser
      apiKey // Optional. Defaults to gradle property publishApiKey
      repo // Optional. Defaults to "maven"
      organization  // Optional. Defaults to "hypertrace"
      name // Optional. Publication name, defaults to project.getName()
      apiUrl // Optional. Defaults to https://api.bintray.com
  }
```

Currently supported publications:
- `java-library`: For projects applying `java-library`, the `java` component (i.e. the jar) will be registered as a publication

### Tasks
One task is added to the applied project, and one to the root project (if not defined).

`bintrayUpload` - uploads all publications to bintray based on the provided config.
This generally should not be used directly, because it requires an additional step to make
artifacts generally available - either manually in the bintray UI, or programmatically with the
`bintrayPublish` task.

`bintrayPublish` - This depends on all `bintrayUpload` tasks and makes them publicly available. This
task is hooked into the build lifecycle and will be called via `publish`  

### Example
```kotlin
plugins {
  id("org.hypertrace.publish-plugin") version "<version>"
}

hypertracePublish {
  license.set(AGPL_V3)
}
```