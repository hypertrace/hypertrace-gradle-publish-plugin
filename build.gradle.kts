import org.hypertrace.gradle.publishing.License.AGPL_V3

plugins {
  `java-gradle-plugin`
  id("org.hypertrace.publish-plugin") version "0.1.3"
  id("org.hypertrace.ci-utils-plugin") version "0.1.1"
}

group = "org.hypertrace.gradle.publishing"

java {
  targetCompatibility = JavaVersion.VERSION_11
  sourceCompatibility = JavaVersion.VERSION_11
}

repositories {
  jcenter()
}

gradlePlugin {
  plugins {
    create("gradlePlugin") {
      id = "org.hypertrace.publish-plugin"
      implementationClass = "org.hypertrace.gradle.publishing.PublishPlugin"
    }
  }
}

dependencies {
  implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5")

  constraints {
    implementation("org.apache.httpcomponents:httpclient:4.5.12") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
    implementation("xerces:xercesImpl:2.12.0") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
    implementation("commons-codec:commons-codec:1.13") {
      because("version 1.12 vulnerable: https://snyk.io/vuln/SNYK-JAVA-COMMONSCODEC-561518")
    }
    implementation("org.codehaus.plexus:plexus-utils:3.0.24") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
    implementation("org.apache.ant:ant:1.9.15") {
      because("Version 1.8.0 vulnerable: https://snyk.io/vuln/SNYK-JAVA-ORGAPACHEANT-569130")
    }
    implementation("commons-collections:commons-collections:3.2.2") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
    implementation("commons-beanutils:commons-beanutils:1.9.4") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
  }
}

hypertracePublish {
  license.set(AGPL_V3)
}
