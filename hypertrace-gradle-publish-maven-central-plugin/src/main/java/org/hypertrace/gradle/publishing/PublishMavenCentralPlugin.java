package org.hypertrace.gradle.publishing;

import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;

import javax.annotation.Nonnull;
import java.util.Optional;

public class PublishMavenCentralPlugin implements Plugin<Project> {
  private static final String EXTENSION_NAME = "hypertracePublishMavenCentral";

  private static final String PROPERTY_SIGNING_KEY_ID = "signingKeyId";
  private static final String PROPERTY_SIGNING_KEY = "signingKey";
  private static final String PROPERTY_SIGNING_PASSWORD = "signingPassword";
  private static final String PROPERTY_OSSRH_USERNAME = "ossrhUsername";
  private static final String PROPERTY_OSSRH_PASSWORD = "ossrhPassword";

  private Project project;
  private HypertracePublishMavenCentralExtension extension;

  @Override
  public void apply(@Nonnull Project target) {
    project = target;
    this.extension =
      project.getExtensions().create(EXTENSION_NAME, HypertracePublishMavenCentralExtension.class);
    this.applyMavenPublish();
    this.applySigning();
    this.applyWithJavadocJar();
    this.applyWithSourcesJar();
    this.addPublishRepository();
    this.addPublications();
  }

  private void applySigning() {
    project.getPluginManager()
      .apply(SigningPlugin.class);
  }

  private void applyMavenPublish() {
    project.getPluginManager()
      .apply(MavenPublishPlugin.class);
  }

  private void applyWithJavadocJar() {
    getJavaPluginExtension().withJavadocJar();
    project.getTasks().withType(Javadoc.class).configureEach(javadoc -> {
      StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
      if (JavaVersion.current().isJava9Compatible()) {
        options.addBooleanOption("html5", true);
      }
    });
  }

  private void applyWithSourcesJar() {
    getJavaPluginExtension().withSourcesJar();
  }

  private void addPublishRepository() {
    String url;
    Optional<String> user = getProperty(PROPERTY_OSSRH_USERNAME);
    Optional<String> password = getProperty(PROPERTY_OSSRH_PASSWORD);

    if (project.getVersion().toString().endsWith("SNAPSHOT")) {
      url = "https://s01.oss.sonatype.org/content/repositories/snapshots/";
    } else {
      url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/";
    }

    getPublishingExtension().repositories(artifactRepositories ->
      artifactRepositories.maven(mavenArtifactRepository -> {
        mavenArtifactRepository.setUrl(url);
        if (user.isPresent() && password.isPresent()) {
          mavenArtifactRepository.credentials(passwordCredentials -> {
            passwordCredentials.setUsername(user.get());
            passwordCredentials.setPassword(password.get());
          });
        }
      })
    );
  }

  private void addPublications() {
    project.afterEvaluate(unused -> {
      validateExtensionAtConfigurationTime();
      getPublishingExtension().publications(this::addJavaLibraryPublicationWhenApplied);
      validateGradlePropertiesBeforePublishTask();
      // sign after publication is added
      addSigning();
    });
  }

  private void addJavaLibraryPublicationWhenApplied(PublicationContainer publications) {
    project.getPluginManager()
      .withPlugin("java-library", appliedPlugin -> {
        MavenPublication publication;

        try {
          // use existing publication from org.hypertrace.publish-plugin plugin
          publication = (MavenPublication) publications.getByName("javaLibrary");
        } catch (UnknownDomainObjectException e) {
          // create new publication
          publication = publications.create("javaLibrary", MavenPublication.class);
          publication.from(project.getComponents().getByName("java"));
        }

        // configure pom
        publication.pom(mavenPom -> {
          // url
          mavenPom.getUrl().set(this.extension.url);

          // scm
          String repoName = this.extension.repoName.get();
          String scmConnection = String.format("scm:git:git://github.com/hypertrace/%s.git", repoName);
          String scmDeveloperConnection = String.format("scm:git:ssh://github.com:hypertrace/%s.git", repoName);
          String scmUrl = String.format("https://github.com/hypertrace/%s/tree/main", repoName);
          mavenPom.scm(mavenPomScm -> {
            mavenPomScm.getConnection().set(scmConnection);
            mavenPomScm.getDeveloperConnection().set(scmDeveloperConnection);
            mavenPomScm.getUrl().set(scmUrl);
          });

          // developers
          mavenPom.developers(mavenPomDeveloperSpec -> {
            mavenPomDeveloperSpec.developer(mavenPomDeveloper -> {
              mavenPomDeveloper.getId().set(this.extension.developerId);
              mavenPomDeveloper.getName().set(this.extension.developerName);
              mavenPomDeveloper.getEmail().set(this.extension.developerEmail);
              mavenPomDeveloper.getOrganization().set(this.extension.developerOrganization);
              mavenPomDeveloper.getOrganizationUrl().set(this.extension.developerOrganizationUrl);
            });
          });

          // licenses
          mavenPom.licenses(mavenPomLicenseSpec -> {
            mavenPomLicenseSpec.license(mavenPomLicense -> {
              mavenPomLicense.getName().set(this.extension.license.get().bintrayString);
            });
          });

        });
      });
  }

  private void addSigning() {
    if (project.hasProperty(PROPERTY_SIGNING_KEY) && project.hasProperty(PROPERTY_SIGNING_PASSWORD)) {
      String signingKey = (String) project.property(PROPERTY_SIGNING_KEY);
      String signingPassword = (String) project.property(PROPERTY_SIGNING_PASSWORD);
      if (project.hasProperty(PROPERTY_SIGNING_KEY_ID)) {
        String signingKeyId = (String) project.property(PROPERTY_SIGNING_KEY_ID);
        getSigningExtension().useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword);
      } else {
        getSigningExtension().useInMemoryPgpKeys(signingKey, signingPassword);
      }
    }
    getSigningExtension()
      .sign(getPublishingExtension().getPublications().getByName("javaLibrary"));
  }

  private PublishingExtension getPublishingExtension() {
    return project.getExtensions()
      .getByType(PublishingExtension.class);
  }

  private JavaPluginExtension getJavaPluginExtension() {
    return project.getExtensions()
      .getByType(JavaPluginExtension.class);
  }

  private SigningExtension getSigningExtension() {
    return project.getExtensions()
      .getByType(SigningExtension.class);
  }

  private Optional<String> getProperty(String propertyName) {
    return Optional.ofNullable(project.findProperty(propertyName)).map(String::valueOf);
  }

  private void validateExtensionAtConfigurationTime() {
    if (!this.extension.license.isPresent()) {
      throw new GradleException(
        "A license type must be specified in the build DSL to use the Hypertrace maven central publish plugin");
    }
    if (!this.extension.repoName.isPresent()) {
      throw new GradleException(
        "Repository Name must be specified in the build DSL to use the Hypertrace maven central publish plugin");
    }
  }

  private void validateGradlePropertiesBeforePublishTask() {
    project.getTasks().withType(PublishToMavenRepository.class).stream().forEach(task -> {
      task.doFirst(unused -> {
        validateGradleProperty(PROPERTY_OSSRH_USERNAME);
        validateGradleProperty(PROPERTY_OSSRH_PASSWORD);
      });
    });
  }

  private void validateGradleProperty(String propertyName) {
    if (!project.hasProperty(propertyName)) {
      throw new GradleException("Missing expected gradle property: " + propertyName + ". It should be added " +
        "in your ~/.gradle/gradle.properties file, or in a an environment variable of the form " +
        "ORG_GRADLE_PROJECT_" + propertyName);
    }
  }

}
