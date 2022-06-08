plugins {
  `java-gradle-plugin`
  id("org.hypertrace.publish-plugin")
}

java {
  targetCompatibility = JavaVersion.VERSION_1_8
  sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
  gradlePluginPortal()
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
  api(project(":hypertrace-gradle-publish-plugin"))
  implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
  testImplementation(gradleTestKit())
  testImplementation(gradleApi())
}

tasks {
  test {
    useJUnitPlatform()
    reports {
      junitXml.isOutputPerTestCase = true
    }
  }
}
