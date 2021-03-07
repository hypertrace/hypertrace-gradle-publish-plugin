plugins {
  `java-gradle-plugin`
  id("org.hypertrace.publish-plugin")
}

gradlePlugin {
  plugins {
    create("gradlePlugin") {
      id = "org.hypertrace.publish-maven-central-plugin"
      implementationClass = "org.hypertrace.gradle.publishing.PublishMavenCentralPlugin"
    }
  }
}

java {
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceCompatibility = JavaVersion.VERSION_1_8
}