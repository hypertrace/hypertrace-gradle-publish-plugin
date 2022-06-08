pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven {
      url = uri("https://hypertrace.jfrog.io/artifactory/maven")
    }
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.2.0"
}

rootProject.name = "hypertrace-gradle-publish-plugin"

include(":hypertrace-gradle-publish-plugin")
include(":hypertrace-gradle-publish-maven-central-plugin")