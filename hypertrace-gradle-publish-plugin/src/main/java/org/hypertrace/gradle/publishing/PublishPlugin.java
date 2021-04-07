package org.hypertrace.gradle.publishing;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;

import javax.annotation.Nonnull;
import java.util.Optional;

public class PublishPlugin implements Plugin<Project> {

  private static final String REPOSITORY_KEY = "gradle";
  private static final String EXTENSION_NAME = "hypertracePublish";
  private static final String PROPERTTY_ARTIFACTORY_USER = "artifactory_user";
  private static final String PROPERTTY_ARTIFACTORY_PASSWORD = "artifactory_password";
  private static final String PROPERTTY_ARTIFACTORY_CONTEXTURL = "artifactory_contextUrl";

  private Project project;
  private HypertracePublishExtension extension;

  @Override
  public void apply(@Nonnull Project target) {
    project = target;
    extension = project.getExtensions().create(EXTENSION_NAME, HypertracePublishExtension.class);
    this.applyMavenPublish();
    this.addPublishRepository();
    this.addKnownPublications();
  }

  private void applyMavenPublish() {
    project.getPluginManager()
      .apply(MavenPublishPlugin.class);
  }

  private void addPublishRepository() {
    Optional<String> user = getProperty(PROPERTTY_ARTIFACTORY_USER);
    Optional<String> password = getProperty(PROPERTTY_ARTIFACTORY_PASSWORD);
    Optional<String> contextUrl = getProperty(PROPERTTY_ARTIFACTORY_CONTEXTURL);

    if (contextUrl.isPresent()) {
      String url = contextUrl.get() + "/" + REPOSITORY_KEY;
      getPublishingExtension().repositories(artifactRepositories -> {
        artifactRepositories.maven(mavenArtifactRepository -> {
          mavenArtifactRepository.setUrl(url);
          if (user.isPresent() && password.isPresent()) {
            mavenArtifactRepository.credentials(passwordCredentials -> {
              passwordCredentials.setUsername(user.get());
              passwordCredentials.setPassword(password.get());
            });
          }
        });
      });
    }
  }

  private void addKnownPublications() {
    validateGradlePropertiesBeforePublishTask();
    validateExtensionBeforeGeneratePom();
    getPublishingExtension().publications(this::addJavaLibraryPublicationWhenApplied);
    getPublishingExtension().publications(this::addDistributionPublicationWhenApplied);
  }

  private void addJavaLibraryPublicationWhenApplied(PublicationContainer publications) {
    project.getPluginManager()
      .withPlugin("java-library", appliedPlugin -> {
        if (this.isJavaGradlePluginPluginApplied()) {
          return; // This already creates a publication, we don't want to duplicate
        }
        publications.create("javaLibrary", MavenPublication.class, publication -> {
          publication.from(project.getComponents().getByName("java"));
          updatePomMetadata(publication);
        });
      });
  }

  private void addDistributionPublicationWhenApplied(PublicationContainer publications) {
    project.getPluginManager()
      .withPlugin("distribution", appliedPlugin ->
        publications.create("distributionZip", MavenPublication.class, publication ->
          publication.artifact(project.getTasks()
            .getByName("distZip"))));
  }

  private void updatePomMetadata(MavenPublication publication) {
    publication.pom(mavenPom -> {
      // Add url
      mavenPom.getUrl().set(extension.pomUrl);

      // Add license
      mavenPom.licenses(mavenPomLicenseSpec -> {
        mavenPomLicenseSpec.license(mavenPomLicense -> {
          mavenPomLicense.getName().set(this.extension.license.map(License::toString));
        });
      });
    });
  }

  private PublishingExtension getPublishingExtension() {
    return project.getExtensions()
      .getByType(PublishingExtension.class);
  }

  private Optional<String> getProperty(String propertyName) {
    return Optional.ofNullable(project.findProperty(propertyName)).map(String::valueOf);
  }

  private boolean isJavaGradlePluginPluginApplied() {
    return project.getPluginManager()
      .hasPlugin("java-gradle-plugin");
  }

  private void validateGradlePropertiesBeforePublishTask() {
    project.getTasks().named("publish").configure(task -> {
      task.doFirst(unused -> {
        validateGradleProperty(PROPERTTY_ARTIFACTORY_CONTEXTURL);
        validateGradleProperty(PROPERTTY_ARTIFACTORY_USER);
        validateGradleProperty(PROPERTTY_ARTIFACTORY_PASSWORD);
      });
    });
  }

  private void validateExtensionBeforeGeneratePom() {
    project.getTasks().withType(GenerateMavenPom.class).configureEach(task -> {
      task.doFirst(unused -> {
        if (!this.extension.license.isPresent()) {
          throw new GradleException(
            "A license type must be specified in the build DSL to use the Hypertrace publish plugin");
        }
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