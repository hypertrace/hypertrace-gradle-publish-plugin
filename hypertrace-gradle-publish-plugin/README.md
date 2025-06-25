# Hypertrace Publish Plugin

###### org.hypertrace.publish-plugin

[![Github](https://github.com/hypertrace/hypertrace-gradle-publish-plugin/actions/workflows/publish.yml/badge.svg)](https://github.com/hypertrace/hypertrace-gradle-publish-plugin/actions/workflows/publish.yml)

### Purpose

This plugin configures the target project to publish its java artifacts to either the Hypertrace
artifactory repository or any standard maven artifact repository. It uses the `maven-publish` plugin
internally to do this. At least one of the two following sets of credentials must be provided as
gradle properties in the default configuration:

- artifactory_contextUrl
- artifactory_user
- artifactory_password

- maven_repo_url
- maven_user
- maven_password

Additionally, no default value is provided for the license. This must be set explicitly via dsl.

Each property described below can be configured in the DSL. The default values are shown for each
property, all of which, with the exception of license, can be omitted if left unchanged.

```kotlin
 hypertracePublish {
  license // REQUIRED to be a value defined in org.hypertrace.gradle.publishing.License
  pomUrl  // Optional. defaults to https://www.hypertrace.org/
}
```

Currently supported publications:

- `java-library`: For projects applying `java-library`, the `java` component (i.e. the jar) will be
  registered as a publication
- `java-platform`: For projects applying `java-platform`, the `javaPlatform` component will be
  registered as a publication
- `distribution`: For projects applying `distribution`, the `distZip` artifact will be registered as
  a publication

### Example

```kotlin
plugins {
  id("org.hypertrace.publish-plugin") version "<version>"
}

hypertracePublish {
  license.set(AGPL_V3)
}
```