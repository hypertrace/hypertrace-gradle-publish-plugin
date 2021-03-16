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
  api(project(":hypertrace-gradle-publish-plugin"))
  implementation("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.30.0")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
  testImplementation(gradleTestKit())
}

tasks {
  test {
    useJUnitPlatform()
    reports {
      junitXml.isOutputPerTestCase = true
    }
  }
}
