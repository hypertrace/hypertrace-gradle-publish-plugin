plugins {
  `java-gradle-plugin`
  id("org.hypertrace.publish-plugin")
}

java {
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
  jcenter()
}

gradlePlugin {
  plugins {
    create("gradlePlugin") {
      id = "org.hypertrace.publish-maven-central-plugin"
      implementationClass = "org.hypertrace.gradle.publishing.PublishMavenCentralPlugin"
    }
  }
}

dependencies {
  implementation(project(":hypertrace-gradle-publish-plugin"))
}