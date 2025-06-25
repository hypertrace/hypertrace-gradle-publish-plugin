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
  private static final String PROPERTY_ARTIFACTORY_USER = "artifactory_user";
  private static final String PROPERTY_ARTIFACTORY_PASSWORD = "artifactory_password";
  private static final String PROPERTY_ARTIFACTORY_CONTEXT_URL = "artifactory_contextUrl";

  private static final String MAVEN_USER = "maven_user";
  private static final String MAVEN_PASSWORD = "maven_password";
  private static final String MAVEN_REPO_URL = "maven_repo_url";

  private Project project;
  private HypertracePublishExtension extension;

  @Override
  public void apply(@Nonnull Project target) {
    project = target;
    extension = project.getExtensions().create(EXTENSION_NAME, HypertracePublishExtension.class);
    this.applyMavenPublish();
    this.maybeAddArtifactoryPublishRepository();
    this.maybeAddMavenPublishRepository();
    this.addKnownPublications();
  }

  private void applyMavenPublish() {
    project.getPluginManager()
      .apply(MavenPublishPlugin.class);
  }

  private void maybeAddArtifactoryPublishRepository() {
    Optional<String> user = getProperty(PROPERTY_ARTIFACTORY_USER);
    Optional<String> password = getProperty(PROPERTY_ARTIFACTORY_PASSWORD);
    Optional<String> contextUrl = getProperty(PROPERTY_ARTIFACTORY_CONTEXT_URL);
    if (contextUrl.isPresent() && user.isPresent() && password.isPresent()) {
      String repoUrl = contextUrl.get() + "/" + REPOSITORY_KEY;
      addPublishRepository(repoUrl, user.get(), password.get());
    }
  }

  private void maybeAddMavenPublishRepository() {
    Optional<String> user = getProperty(MAVEN_USER);
    Optional<String> password = getProperty(MAVEN_PASSWORD);
    Optional<String> repoUrl = getProperty(MAVEN_REPO_URL);

    if (repoUrl.isPresent() && user.isPresent() && password.isPresent()) {
      addPublishRepository(repoUrl.get(), user.get(), password.get());
    }
  }

  private void addPublishRepository(String repoUrl, String user, String password) {
    getPublishingExtension().repositories(artifactRepositories -> {
      artifactRepositories.maven(mavenArtifactRepository -> {
        mavenArtifactRepository.setUrl(repoUrl);
        mavenArtifactRepository.credentials(passwordCredentials -> {
          passwordCredentials.setUsername(user);
          passwordCredentials.setPassword(password);
        });
      });
    });
  }

  private void addKnownPublications() {
    validateGradlePropertiesBeforePublishTask();
    validateExtensionBeforeGeneratePom();
    getPublishingExtension().publications(this::addJavaLibraryPublicationWhenApplied);
    getPublishingExtension().publications(this::addDistributionPublicationWhenApplied);
    getPublishingExtension().publications(this::addJavaPlatformPublicationWhenApplied);
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

  private void addJavaPlatformPublicationWhenApplied(PublicationContainer publications) {
    project
        .getPluginManager()
        .withPlugin(
            "java-platform",
            appliedPlugin -> {
              publications.create(
                  "javaPlatform",
                  MavenPublication.class,
                  publication -> {
                    publication.from(project.getComponents().getByName("javaPlatform"));
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
        if (project.hasProperty(PROPERTY_ARTIFACTORY_CONTEXT_URL)) {
          validateGradleProperty(PROPERTY_ARTIFACTORY_USER);
          validateGradleProperty(PROPERTY_ARTIFACTORY_PASSWORD);
        }
        if (project.hasProperty(MAVEN_REPO_URL)) {
          validateGradleProperty(MAVEN_USER);
          validateGradleProperty(MAVEN_PASSWORD);
        } else if (!project.hasProperty(PROPERTY_ARTIFACTORY_CONTEXT_URL)) {
          // If neither, fail the repro url validation
          validateGradleProperty(MAVEN_REPO_URL);
        }
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