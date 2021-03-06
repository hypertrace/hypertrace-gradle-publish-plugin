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
      id = "org.hypertrace.publish-plugin"
      implementationClass = "org.hypertrace.gradle.publishing.PublishPlugin"
    }
  }
}

dependencies {
  // Do not upgrade without reviewing. See PublishPlugin.createModuleMetadataPublication
  implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5")

  constraints {
    implementation("org.apache.httpcomponents:httpclient:4.5.13") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
    implementation("xerces:xercesImpl:2.12.1") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
    implementation("commons-codec:commons-codec:1.13") {
      because("version 1.12 vulnerable: https://snyk.io/vuln/SNYK-JAVA-COMMONSCODEC-561518")
    }
    implementation("org.codehaus.plexus:plexus-utils:3.0.24") {
      because("Multiple vulnerabilities in versions included via bintray plugin")
    }
    implementation("org.apache.ant:ant:1.10.9") {
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
