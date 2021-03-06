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

Each property described below can be configured in the DSL. The default values are shown for each property,
all of which, with the exception of license, can be omitted if left unchanged.
```kotlin
 hypertracePublishMavenCentral {
    url // The URL for the publication represented by the POM.
    scmConnection // The connection URL of the SCM.
    scmDeveloperConnection // The developer connection URL of the SCM
    scmUrl // The browsable repository URL of the SCM
    licenses // Configures the licenses for the publication represented by the POM
    developers // Configures the developers for the publication represented by the POM
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
            url.set("https://www.hypertrace.org")
            scmConnection.set("scm:git:git://github.com/hypertrace/hypertrace.git")
            scmDeveloperConnection.set("scm:git:ssh://github.com:hypertrace/hypertrace.git")
            scmUrl.set("https://github.com/hypertrace/hypertrace/tree/main")
            developer("Developer Name") {
                id.set("Developer Id")
                email.set("Developer Email")
            }
            license("Apache 2.0") {
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
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
