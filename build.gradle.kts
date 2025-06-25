import org.hypertrace.gradle.publishing.HypertracePublishExtension
import org.hypertrace.gradle.publishing.License

plugins {
  id("org.hypertrace.ci-utils-plugin") version "0.2.0"
  id("org.hypertrace.publish-plugin") version "1.1.0" apply false
  id("org.hypertrace.repository-plugin") version "0.5.0"
}

subprojects {
  group = "org.hypertrace.gradle.publishing"
  pluginManager.withPlugin("org.hypertrace.publish-plugin") {
    configure<HypertracePublishExtension> {
      license.set(License.APACHE_2_0)
    }
  }
}