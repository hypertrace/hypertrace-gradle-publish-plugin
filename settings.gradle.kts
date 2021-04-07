pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven {
      url = uri("https://hypertrace.jfrog.io/artifactory/gradle")
    }
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.1.1"
}

rootProject.name = "hypertrace-gradle-publish-plugin"

include(":hypertrace-gradle-publish-plugin")
include(":hypertrace-gradle-publish-maven-central-plugin")